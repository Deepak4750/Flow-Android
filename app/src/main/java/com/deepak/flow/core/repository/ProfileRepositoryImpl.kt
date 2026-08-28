package com.deepak.flow.core.repository

import com.deepak.flow.core.database.UserProfileDao
import com.deepak.flow.core.database.UserProfileEntity
import com.deepak.flow.core.database.WaterDayDao
import com.deepak.flow.core.database.WaterDayEntity
import com.deepak.flow.core.model.SnoozeSettings
import com.deepak.flow.core.model.UserProfile
import com.deepak.flow.core.model.WaterReminderSettings
import com.deepak.flow.core.model.parseWaterBottleStyleIndex
import com.deepak.flow.core.model.WaterIntakeWrite
import com.deepak.flow.core.model.encodeWaterAddLog
import com.deepak.flow.core.model.encodeWaterCustomQuickAdds
import com.deepak.flow.core.gym.GymLimits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ProfileRepositoryImpl(
    private val dao: UserProfileDao,
    private val waterDayDao: WaterDayDao,
) : ProfileRepository {

    private val waterIntakeMutex = Mutex()

    override fun observeProfile(): Flow<UserProfile?> =
        dao.observeProfile().map { it?.toDomain() }

    override suspend fun getProfile(): UserProfile? =
        dao.getProfile()?.toDomain()

    override fun isOnboardingComplete(): Flow<Boolean> =
        dao.observeProfile().map { it?.onboardingCompleted == true }

    override suspend fun completeOnboarding(displayName: String?, nickname: String?) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = displayName?.trim()?.takeIf { it.isNotEmpty() },
                nickname = nickname?.trim()?.takeIf { it.isNotEmpty() },
                onboardingCompleted = true,
            ),
        )
    }

    override suspend fun updateProfile(displayName: String?, nickname: String?) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = displayName?.trim()?.takeIf { it.isNotEmpty() },
                nickname = nickname?.trim()?.takeIf { it.isNotEmpty() },
                onboardingCompleted = existing?.onboardingCompleted ?: true,
            ),
        )
    }

    override suspend fun updateSnoozeEnabled(enabled: Boolean) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                snoozeEnabled = enabled,
            ),
        )
    }

    override suspend fun updateSnoozeInterval(minutes: Int) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                snoozeIntervalMinutes = SnoozeSettings.coerceIntervalMinutes(minutes),
            ),
        )
    }

    override suspend fun updateRemindersEnabled(enabled: Boolean) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                remindersEnabled = enabled,
            ),
        )
    }

    override suspend fun updateWaterEnabled(enabled: Boolean) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                waterEnabled = enabled,
            ),
        )
    }

    override suspend fun updateGymEnabled(enabled: Boolean) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                gymEnabled = enabled,
            ),
        )
    }

    override suspend fun updateGymWeightUnit(unit: String) {
        val normalized = when (unit.uppercase()) {
            "LB" -> "LB"
            else -> "KG"
        }
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                gymWeightUnit = normalized,
            ),
        )
    }

    override suspend fun updateGymSetRestSeconds(seconds: Int) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                gymSetRestSeconds = GymLimits.clampSetRestSeconds(seconds),
            ),
        )
    }

    override suspend fun updateGymExerciseRestSeconds(seconds: Int) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                gymExerciseRestSeconds = GymLimits.clampExerciseRestSeconds(seconds),
            ),
        )
    }

    override suspend fun updateWaterGoalMl(millilitres: Int) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                waterGoalMl = millilitres,
            ),
        )
    }

    override suspend fun updateWaterBottleStyle(index: Int) {
        val parsed = parseWaterBottleStyleIndex(index) ?: return
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                waterBottleStyleIndex = parsed,
            ),
        )
    }

    override suspend fun updateWaterIntake(
        millilitres: Int,
        epochDay: Long,
        addLog: List<Int>,
    ) {
        waterIntakeMutex.withLock {
            persistWaterIntakeLocked(millilitres, epochDay, addLog)
        }
    }

    override suspend fun applyWaterIntakeWrite(
        todayEpochDay: Long,
        write: (UserProfile) -> WaterIntakeWrite?,
    ): WaterIntakeWrite? = waterIntakeMutex.withLock {
        val profile = dao.getProfile()?.toDomain() ?: return@withLock null
        val update = write(profile) ?: return@withLock null
        persistWaterIntakeLocked(update.millilitres, todayEpochDay, update.addLog)
        update
    }

    private suspend fun persistWaterIntakeLocked(
        millilitres: Int,
        epochDay: Long,
        addLog: List<Int>,
    ) {
        val existing = dao.getProfile()
        val previousDay = existing?.waterIntakeEpochDay
        val previousMl = existing?.waterIntakeMl ?: 0
        if (previousDay != null && previousDay != epochDay && previousMl > 0) {
            waterDayDao.upsert(
                WaterDayEntity(
                    dateEpochDay = previousDay,
                    intakeMl = previousMl,
                    addLog = existing?.waterAddLog.orEmpty(),
                    goalMl = existing?.waterGoalMl,
                ),
            )
        }
        val coercedMl = millilitres.coerceAtLeast(0)
        val encodedLog = encodeWaterAddLog(addLog)
        waterDayDao.upsert(
            WaterDayEntity(
                dateEpochDay = epochDay,
                intakeMl = coercedMl,
                addLog = encodedLog,
                goalMl = existing?.waterGoalMl,
            ),
        )
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                waterIntakeMl = coercedMl,
                waterIntakeEpochDay = epochDay,
                waterAddLog = encodedLog,
            ),
        )
    }

    override suspend fun updateWaterCustomQuickAdds(amounts: List<Int>) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                waterCustomQuickAddsMl = encodeWaterCustomQuickAdds(amounts),
            ),
        )
    }

    override suspend fun updateWaterRemindersEnabled(enabled: Boolean) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                waterRemindersEnabled = enabled,
            ),
        )
    }

    override suspend fun updateWaterReminderInterval(minutes: Int) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                waterReminderIntervalMinutes = WaterReminderSettings.coerceIntervalMinutes(minutes),
            ),
        )
    }

    override suspend fun updateWaterActiveHoursEnabled(enabled: Boolean) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                waterActiveHoursEnabled = enabled,
            ),
        )
    }

    override suspend fun updateWaterActiveHoursStart(minutesOfDay: Int) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                waterActiveHoursStartMinutes = WaterReminderSettings.coerceMinutesOfDay(minutesOfDay),
            ),
        )
    }

    override suspend fun updateWaterActiveHoursEnd(minutesOfDay: Int) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                waterActiveHoursEndMinutes = WaterReminderSettings.coerceMinutesOfDay(minutesOfDay),
            ),
        )
    }

    override suspend fun updateKeepDataOnUninstall(enabled: Boolean) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                keepDataOnUninstall = enabled,
            ),
        )
    }
}

private fun UserProfileEntity?.toUpsertEntity(
    displayName: String?,
    nickname: String?,
    onboardingCompleted: Boolean,
    snoozeEnabled: Boolean = this?.snoozeEnabled ?: SnoozeSettings.DEFAULT_ENABLED,
    snoozeIntervalMinutes: Int = this?.snoozeIntervalMinutes
        ?: SnoozeSettings.DEFAULT_INTERVAL_MINUTES,
    remindersEnabled: Boolean = this?.remindersEnabled ?: UserProfile.DEFAULT_REMINDERS_ENABLED,
    waterEnabled: Boolean = this?.waterEnabled ?: UserProfile.DEFAULT_WATER_ENABLED,
    gymEnabled: Boolean = this?.gymEnabled ?: UserProfile.DEFAULT_GYM_ENABLED,
    gymWeightUnit: String = this?.gymWeightUnit ?: UserProfile.DEFAULT_GYM_WEIGHT_UNIT,
    gymSetRestSeconds: Int = this?.gymSetRestSeconds ?: UserProfile.DEFAULT_GYM_SET_REST_SECONDS,
    gymExerciseRestSeconds: Int = this?.gymExerciseRestSeconds
        ?: UserProfile.DEFAULT_GYM_EXERCISE_REST_SECONDS,
    waterGoalMl: Int? = this?.waterGoalMl,
    waterBottleStyleIndex: Int? = this?.waterBottleStyleIndex,
    waterIntakeMl: Int = this?.waterIntakeMl ?: 0,
    waterIntakeEpochDay: Long? = this?.waterIntakeEpochDay,
    waterAddLog: String = this?.waterAddLog.orEmpty(),
    waterCustomQuickAddsMl: String = this?.waterCustomQuickAddsMl.orEmpty(),
    waterRemindersEnabled: Boolean = this?.waterRemindersEnabled
        ?: WaterReminderSettings.DEFAULT_ENABLED,
    waterReminderIntervalMinutes: Int = this?.waterReminderIntervalMinutes
        ?: WaterReminderSettings.DEFAULT_INTERVAL_MINUTES,
    waterActiveHoursEnabled: Boolean = this?.waterActiveHoursEnabled
        ?: WaterReminderSettings.DEFAULT_ACTIVE_HOURS_ENABLED,
    waterActiveHoursStartMinutes: Int = this?.waterActiveHoursStartMinutes
        ?: WaterReminderSettings.DEFAULT_ACTIVE_START_MINUTES,
    waterActiveHoursEndMinutes: Int = this?.waterActiveHoursEndMinutes
        ?: WaterReminderSettings.DEFAULT_ACTIVE_END_MINUTES,
    keepDataOnUninstall: Boolean = this?.keepDataOnUninstall
        ?: UserProfile.DEFAULT_KEEP_DATA_ON_UNINSTALL,
    activeGymRoutineId: Long? = this?.activeGymRoutineId,
) = UserProfileEntity(
    displayName = displayName,
    nickname = nickname,
    onboardingCompleted = onboardingCompleted,
    snoozeEnabled = snoozeEnabled,
    snoozeIntervalMinutes = snoozeIntervalMinutes,
    remindersEnabled = remindersEnabled,
    waterEnabled = waterEnabled,
    gymEnabled = gymEnabled,
    gymWeightUnit = gymWeightUnit,
    gymSetRestSeconds = gymSetRestSeconds,
    gymExerciseRestSeconds = gymExerciseRestSeconds,
    waterGoalMl = waterGoalMl,
    waterBottleStyleIndex = waterBottleStyleIndex,
    waterIntakeMl = waterIntakeMl,
    waterIntakeEpochDay = waterIntakeEpochDay,
    waterAddLog = waterAddLog,
    waterCustomQuickAddsMl = waterCustomQuickAddsMl,
    waterRemindersEnabled = waterRemindersEnabled,
    waterReminderIntervalMinutes = waterReminderIntervalMinutes,
    waterActiveHoursEnabled = waterActiveHoursEnabled,
    waterActiveHoursStartMinutes = waterActiveHoursStartMinutes,
    waterActiveHoursEndMinutes = waterActiveHoursEndMinutes,
    keepDataOnUninstall = keepDataOnUninstall,
    activeGymRoutineId = activeGymRoutineId,
)

private fun UserProfileEntity.toDomain() = UserProfile(
    displayName = displayName,
    nickname = nickname,
    onboardingCompleted = onboardingCompleted,
    snoozeEnabled = snoozeEnabled,
    snoozeIntervalMinutes = snoozeIntervalMinutes,
    remindersEnabled = remindersEnabled,
    waterEnabled = waterEnabled,
    gymEnabled = gymEnabled,
    gymWeightUnit = gymWeightUnit,
    gymSetRestSeconds = gymSetRestSeconds,
    gymExerciseRestSeconds = gymExerciseRestSeconds,
    waterGoalMl = waterGoalMl,
    waterBottleStyleIndex = waterBottleStyleIndex,
    waterIntakeMl = waterIntakeMl,
    waterIntakeEpochDay = waterIntakeEpochDay,
    waterAddLog = waterAddLog,
    waterCustomQuickAddsMl = waterCustomQuickAddsMl,
    waterRemindersEnabled = waterRemindersEnabled,
    waterReminderIntervalMinutes = waterReminderIntervalMinutes,
    waterActiveHoursEnabled = waterActiveHoursEnabled,
    waterActiveHoursStartMinutes = waterActiveHoursStartMinutes,
    waterActiveHoursEndMinutes = waterActiveHoursEndMinutes,
    keepDataOnUninstall = keepDataOnUninstall,
    activeGymRoutineId = activeGymRoutineId,
)
