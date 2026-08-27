package com.deepak.flow.core.repository

import com.deepak.flow.core.database.GymWorkoutDao
import com.deepak.flow.core.database.HistoryDao
import com.deepak.flow.core.database.UserProfileDao
import com.deepak.flow.core.database.WaterDayDao
import com.deepak.flow.core.database.WaterDayEntity
import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.gym.GymWorkoutStatus
import com.deepak.flow.core.history.HistoryBounds
import com.deepak.flow.core.history.HistoryDaySummary
import com.deepak.flow.core.history.HistoryTaskCompletion
import com.deepak.flow.core.history.HistoryWaterDay
import com.deepak.flow.core.model.decodeWaterAddLog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryRepositoryImpl(
    private val historyDao: HistoryDao,
    private val waterDayDao: WaterDayDao,
    private val profileDao: UserProfileDao,
    private val gymWorkoutDao: GymWorkoutDao,
    private val gymWorkoutRepository: GymWorkoutRepository,
) : HistoryRepository {

    override fun observeActivityDays(
        yearMonth: YearMonth,
        zoneId: ZoneId,
    ): Flow<Set<Long>> {
        val from = yearMonth.atDay(1).toEpochDay()
        val to = yearMonth.atEndOfMonth().toEpochDay()
        val (fromMilli, toMilli) = dayRangeMillis(from, to, zoneId)
        return combine(
            historyDao.observeTaskActivityDays(from, to),
            waterDayDao.observeActivityDays(from, to),
            gymWorkoutDao.observeCompletedBetween(
                status = GymWorkoutStatus.COMPLETED.name,
                fromInclusive = fromMilli,
                toExclusive = toMilli,
            ),
        ) { tasks, water, gymWorkouts ->
            buildSet {
                addAll(tasks)
                addAll(water)
                gymWorkouts.forEach { workout ->
                    val ended = workout.endedAtEpochMilli ?: return@forEach
                    add(
                        Instant.ofEpochMilli(ended)
                            .atZone(zoneId)
                            .toLocalDate()
                            .toEpochDay(),
                    )
                }
            }
        }
    }

    override fun observeDaySummary(
        dateEpochDay: Long,
        zoneId: ZoneId,
    ): Flow<HistoryDaySummary> {
        val (fromMilli, toMilli) = dayRangeMillis(dateEpochDay, dateEpochDay, zoneId)
        return combine(
            historyDao.observeTaskCompletionCount(dateEpochDay),
            observeWaterDay(dateEpochDay, LocalDate.now(zoneId).toEpochDay()),
            gymWorkoutDao.observeCompletedBetween(
                status = GymWorkoutStatus.COMPLETED.name,
                fromInclusive = fromMilli,
                toExclusive = toMilli,
            ),
        ) { taskCount, water, gymWorkouts ->
            HistoryDaySummary(
                dateEpochDay = dateEpochDay,
                taskCount = taskCount,
                waterIntakeMl = water?.intakeMl ?: 0,
                gymWorkoutCount = gymWorkouts.size,
            )
        }
    }

    override fun observeDaySeries(
        fromEpochDay: Long,
        toEpochDay: Long,
        zoneId: ZoneId,
    ): Flow<List<HistoryDaySummary>> {
        val today = LocalDate.now(zoneId).toEpochDay()
        val (fromMilli, toMilli) = dayRangeMillis(fromEpochDay, toEpochDay, zoneId)
        return combine(
            historyDao.observeTaskCountsInRange(fromEpochDay, toEpochDay),
            waterDayDao.observeDaysInRange(fromEpochDay, toEpochDay),
            profileDao.observeProfile(),
            gymWorkoutDao.observeCompletedBetween(
                status = GymWorkoutStatus.COMPLETED.name,
                fromInclusive = fromMilli,
                toExclusive = toMilli,
            ),
        ) { taskRows, waterRows, profile, gymWorkouts ->
            val tasksByDay = taskRows.associate { it.dateEpochDay to it.completionCount }
            val waterByDay = waterRows.associateBy { it.dateEpochDay }.toMutableMap()
            if (today in fromEpochDay..toEpochDay &&
                profile != null &&
                profile.waterIntakeEpochDay == today &&
                profile.waterIntakeMl > 0 &&
                waterByDay[today] == null
            ) {
                waterByDay[today] = WaterDayEntity(
                    dateEpochDay = today,
                    intakeMl = profile.waterIntakeMl,
                    addLog = profile.waterAddLog,
                    goalMl = profile.waterGoalMl,
                )
            }
            val gymByDay = mutableMapOf<Long, Int>()
            gymWorkouts.forEach { workout ->
                val ended = workout.endedAtEpochMilli ?: return@forEach
                val day = Instant.ofEpochMilli(ended)
                    .atZone(zoneId)
                    .toLocalDate()
                    .toEpochDay()
                gymByDay[day] = (gymByDay[day] ?: 0) + 1
            }
            (fromEpochDay..toEpochDay).map { epochDay ->
                HistoryDaySummary(
                    dateEpochDay = epochDay,
                    taskCount = tasksByDay[epochDay] ?: 0,
                    waterIntakeMl = waterByDay[epochDay]?.intakeMl ?: 0,
                    gymWorkoutCount = gymByDay[epochDay] ?: 0,
                )
            }
        }
    }

    override fun observeTaskCompletions(dateEpochDay: Long): Flow<List<HistoryTaskCompletion>> =
        historyDao.observeTaskCompletions(dateEpochDay).map { rows ->
            rows.map { row ->
                HistoryTaskCompletion(
                    reminderId = row.reminderId,
                    title = row.title?.takeIf { it.isNotBlank() } ?: "Task",
                    completedAtEpochMilli = row.completedAtEpochMilli,
                )
            }
        }

    override fun observeWaterDay(
        dateEpochDay: Long,
        todayEpochDay: Long,
    ): Flow<HistoryWaterDay?> {
        return combine(
            waterDayDao.observeForDay(dateEpochDay),
            profileDao.observeProfile(),
        ) { archived, profile ->
            when {
                archived != null -> HistoryWaterDay(
                    dateEpochDay = archived.dateEpochDay,
                    intakeMl = archived.intakeMl,
                    addLog = decodeWaterAddLog(archived.addLog),
                    goalMl = archived.goalMl,
                )
                dateEpochDay == todayEpochDay &&
                    profile != null &&
                    profile.waterIntakeEpochDay == todayEpochDay &&
                    profile.waterIntakeMl > 0 -> {
                    HistoryWaterDay(
                        dateEpochDay = todayEpochDay,
                        intakeMl = profile.waterIntakeMl,
                        addLog = decodeWaterAddLog(profile.waterAddLog),
                        goalMl = profile.waterGoalMl,
                    )
                }
                else -> null
            }
        }
    }

    override fun observeGymWorkoutsForDay(
        dateEpochDay: Long,
        zoneId: ZoneId,
    ): Flow<List<GymWorkoutSession>> {
        val (fromMilli, toMilli) = dayRangeMillis(dateEpochDay, dateEpochDay, zoneId)
        return gymWorkoutRepository.observeCompletedSessionsBetween(fromMilli, toMilli)
    }

    override suspend fun getHistoryBounds(zoneId: ZoneId): HistoryBounds {
        val today = LocalDate.now(zoneId).toEpochDay()
        val candidates = mutableListOf<Long>()
        historyDao.minTaskActivityDay()?.let { candidates.add(it) }
        waterDayDao.minActivityDay()?.let { candidates.add(it) }
        val gymMin = gymWorkoutDao.minCompletedEndedAt(GymWorkoutStatus.COMPLETED.name)
        if (gymMin != null) {
            candidates.add(
                Instant.ofEpochMilli(gymMin)
                    .atZone(zoneId)
                    .toLocalDate()
                    .toEpochDay(),
            )
        }
        val profile = profileDao.getProfile()
        if (profile != null && profile.waterIntakeMl > 0 && profile.waterIntakeEpochDay != null) {
            candidates.add(profile.waterIntakeEpochDay!!)
        }
        if (candidates.isEmpty()) {
            return HistoryBounds(earliestEpochDay = today, latestEpochDay = today)
        }
        return HistoryBounds(
            earliestEpochDay = candidates.minOrNull(),
            latestEpochDay = minOf(candidates.maxOrNull() ?: today, today),
        )
    }

    override fun observeHistoryBounds(zoneId: ZoneId): Flow<HistoryBounds> {
        val today = LocalDate.now(zoneId).toEpochDay()
        val windowStart = today - 3650
        return combine(
            profileDao.observeProfile(),
            historyDao.observeTaskActivityDays(windowStart, today),
            waterDayDao.observeActivityDays(windowStart, today),
            gymWorkoutDao.observeCompletedBetween(
                status = GymWorkoutStatus.COMPLETED.name,
                fromInclusive = 0L,
                toExclusive = LocalDate.now(zoneId).plusDays(1)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli(),
            ),
        ) { _, _, _, _ ->
            Unit
        }.flatMapLatest {
            flow { emit(getHistoryBounds(zoneId)) }
        }
    }

    private fun dayRangeMillis(
        fromEpochDay: Long,
        toEpochDay: Long,
        zoneId: ZoneId,
    ): Pair<Long, Long> {
        val fromMilli = LocalDate.ofEpochDay(fromEpochDay)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val toMilli = LocalDate.ofEpochDay(toEpochDay)
            .plusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        return fromMilli to toMilli
    }
}
