package com.deepak.flow.core.repository

import com.deepak.flow.core.database.HistoryDao
import com.deepak.flow.core.database.UserProfileDao
import com.deepak.flow.core.database.WaterDayDao
import com.deepak.flow.core.database.WaterDayEntity
import com.deepak.flow.core.history.HistoryDaySummary
import com.deepak.flow.core.history.HistoryTaskCompletion
import com.deepak.flow.core.history.HistoryWaterDay
import com.deepak.flow.core.model.decodeWaterAddLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class HistoryRepositoryImpl(
    private val historyDao: HistoryDao,
    private val waterDayDao: WaterDayDao,
    private val profileDao: UserProfileDao,
) : HistoryRepository {

    override fun observeActivityDays(
        yearMonth: YearMonth,
        zoneId: ZoneId,
    ): Flow<Set<Long>> {
        val from = yearMonth.atDay(1).toEpochDay()
        val to = yearMonth.atEndOfMonth().toEpochDay()
        return combine(
            historyDao.observeTaskActivityDays(from, to),
            waterDayDao.observeActivityDays(from, to),
        ) { tasks, water ->
            buildSet {
                addAll(tasks)
                addAll(water)
            }
        }
    }

    override fun observeDaySummary(
        dateEpochDay: Long,
        zoneId: ZoneId,
    ): Flow<HistoryDaySummary> {
        return combine(
            historyDao.observeTaskCompletionCount(dateEpochDay),
            observeWaterDay(dateEpochDay, LocalDate.now(zoneId).toEpochDay()),
        ) { taskCount, water ->
            HistoryDaySummary(
                dateEpochDay = dateEpochDay,
                taskCount = taskCount,
                waterIntakeMl = water?.intakeMl ?: 0,
            )
        }
    }

    override fun observeDaySeries(
        fromEpochDay: Long,
        toEpochDay: Long,
        zoneId: ZoneId,
    ): Flow<List<HistoryDaySummary>> {
        val today = LocalDate.now(zoneId).toEpochDay()
        return combine(
            historyDao.observeTaskCountsInRange(fromEpochDay, toEpochDay),
            waterDayDao.observeDaysInRange(fromEpochDay, toEpochDay),
            profileDao.observeProfile(),
        ) { taskRows, waterRows, profile ->
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
            (fromEpochDay..toEpochDay).map { epochDay ->
                HistoryDaySummary(
                    dateEpochDay = epochDay,
                    taskCount = tasksByDay[epochDay] ?: 0,
                    waterIntakeMl = waterByDay[epochDay]?.intakeMl ?: 0,
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
}
