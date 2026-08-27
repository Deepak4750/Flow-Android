package com.deepak.flow.core.repository

import com.deepak.flow.core.model.UserProfile
import com.deepak.flow.core.model.WaterIntakeWrite
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>
    suspend fun getProfile(): UserProfile?
    fun isOnboardingComplete(): Flow<Boolean>
    suspend fun completeOnboarding(displayName: String?, nickname: String?)
    suspend fun updateProfile(displayName: String?, nickname: String?)
    suspend fun updateSnoozeEnabled(enabled: Boolean)
    suspend fun updateSnoozeInterval(minutes: Int)
    suspend fun updateRemindersEnabled(enabled: Boolean)
    suspend fun updateWaterEnabled(enabled: Boolean)
    suspend fun updateGymEnabled(enabled: Boolean)
    suspend fun updateGymWeightUnit(unit: String)
    suspend fun updateGymSetRestSeconds(seconds: Int)
    suspend fun updateGymExerciseRestSeconds(seconds: Int)
    suspend fun updateWaterGoalMl(millilitres: Int)
    suspend fun updateWaterBottleStyle(index: Int)
    suspend fun updateWaterIntake(millilitres: Int, epochDay: Long, addLog: List<Int>)
    /**
     * Atomically read profile, apply [write], and persist. Serializes concurrent
     * widget / notification / in-app water taps so rapid adds are not lost.
     */
    suspend fun applyWaterIntakeWrite(
        todayEpochDay: Long,
        write: (UserProfile) -> WaterIntakeWrite?,
    ): WaterIntakeWrite?
    suspend fun updateWaterCustomQuickAdds(amounts: List<Int>)
    suspend fun updateWaterRemindersEnabled(enabled: Boolean)
    suspend fun updateWaterReminderInterval(minutes: Int)
    suspend fun updateWaterActiveHoursEnabled(enabled: Boolean)
    suspend fun updateWaterActiveHoursStart(minutesOfDay: Int)
    suspend fun updateWaterActiveHoursEnd(minutesOfDay: Int)
    suspend fun updateKeepDataOnUninstall(enabled: Boolean)
}
