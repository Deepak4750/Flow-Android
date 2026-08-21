package com.deepak.flow.app.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.model.UserProfile
import com.deepak.flow.core.model.WaterIntakeWrite
import com.deepak.flow.core.model.WaterReminderSettings
import com.deepak.flow.core.model.canUndoWater
import com.deepak.flow.core.model.todayWaterIntakeMl
import com.deepak.flow.core.model.waterDrinkRemindersOn
import com.deepak.flow.core.model.waterCustomQuickAdds
import com.deepak.flow.core.model.withWaterCustomQuickAdd
import com.deepak.flow.core.model.withoutWaterCustomQuickAdd
import com.deepak.flow.core.model.withWaterAdd
import com.deepak.flow.core.model.withWaterUndo
import com.deepak.flow.core.notification.NotificationChannelManager
import com.deepak.flow.core.repository.ProfileRepository
import com.deepak.flow.core.repository.ReminderRepository
import com.deepak.flow.core.widget.FlowWidgets
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class FeatureSettingsUiState(
    val profileName: String? = null,
    val remindersEnabled: Boolean = UserProfile.DEFAULT_REMINDERS_ENABLED,
    val waterEnabled: Boolean = UserProfile.DEFAULT_WATER_ENABLED,
    val waterGoalMl: Int? = null,
    val waterBottleStyleIndex: Int? = null,
    val waterIntakeMl: Int = 0,
    val canUndoWater: Boolean = false,
    val waterCustomQuickAddsMl: List<Int> = emptyList(),
    val waterRemindersEnabled: Boolean = WaterReminderSettings.DEFAULT_ENABLED,
    val waterReminderIntervalMinutes: Int = WaterReminderSettings.DEFAULT_INTERVAL_MINUTES,
    val waterActiveHoursEnabled: Boolean = WaterReminderSettings.DEFAULT_ACTIVE_HOURS_ENABLED,
    val waterActiveHoursStartMinutes: Int = WaterReminderSettings.DEFAULT_ACTIVE_START_MINUTES,
    val waterActiveHoursEndMinutes: Int = WaterReminderSettings.DEFAULT_ACTIVE_END_MINUTES,
)

class FeatureSettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val app = getApplication<FlowApplication>()
    private val profileRepository: ProfileRepository = app.profileRepository
    private val reminderRepository: ReminderRepository = app.reminderRepository

    val uiState: StateFlow<FeatureSettingsUiState> = profileRepository.observeProfile()
        .map { profile ->
            val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
            FeatureSettingsUiState(
                profileName = profile?.displayName?.takeIf { it.isNotBlank() }
                    ?: profile?.nickname?.takeIf { it.isNotBlank() },
                remindersEnabled = profile?.remindersEnabled ?: UserProfile.DEFAULT_REMINDERS_ENABLED,
                waterEnabled = profile?.waterEnabled ?: UserProfile.DEFAULT_WATER_ENABLED,
                waterGoalMl = profile?.waterGoalMl,
                waterBottleStyleIndex = profile?.waterBottleStyleIndex,
                waterIntakeMl = profile?.todayWaterIntakeMl(today) ?: 0,
                canUndoWater = profile?.canUndoWater(today) == true,
                waterCustomQuickAddsMl = profile?.waterCustomQuickAdds().orEmpty(),
                waterRemindersEnabled = profile?.waterRemindersEnabled
                    ?: WaterReminderSettings.DEFAULT_ENABLED,
                waterReminderIntervalMinutes = profile?.waterReminderIntervalMinutes
                    ?: WaterReminderSettings.DEFAULT_INTERVAL_MINUTES,
                waterActiveHoursEnabled = profile?.waterActiveHoursEnabled
                    ?: WaterReminderSettings.DEFAULT_ACTIVE_HOURS_ENABLED,
                waterActiveHoursStartMinutes = profile?.waterActiveHoursStartMinutes
                    ?: WaterReminderSettings.DEFAULT_ACTIVE_START_MINUTES,
                waterActiveHoursEndMinutes = profile?.waterActiveHoursEndMinutes
                    ?: WaterReminderSettings.DEFAULT_ACTIVE_END_MINUTES,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FeatureSettingsUiState(),
        )

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            profileRepository.updateRemindersEnabled(enabled)
            if (enabled) {
                reminderRepository.rescheduleAllEnabledReminders()
            } else {
                reminderRepository.cancelAllScheduledReminders()
                NotificationChannelManager.cancelAllReminderNotifications(app)
            }
            FlowWidgets.refresh(app)
        }
    }

    fun setWaterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            profileRepository.updateWaterEnabled(enabled)
            syncWaterReminders()
            FlowWidgets.refresh(app)
        }
    }

    fun setWaterGoalMl(millilitres: Int) {
        viewModelScope.launch {
            profileRepository.updateWaterGoalMl(millilitres)
            FlowWidgets.refreshWater(app)
        }
    }

    fun setWaterBottleStyle(index: Int) {
        viewModelScope.launch {
            profileRepository.updateWaterBottleStyle(index)
            FlowWidgets.refreshWater(app)
        }
    }

    fun saveWaterSettings(goalMl: Int, bottleStyleIndex: Int) {
        viewModelScope.launch {
            profileRepository.updateWaterGoalMl(goalMl)
            profileRepository.updateWaterBottleStyle(bottleStyleIndex)
            FlowWidgets.refreshWater(app)
        }
    }

    fun addWaterMl(amount: Int) {
        viewModelScope.launch {
            persistWaterWrite { profile, today -> profile.withWaterAdd(amount, today) }
            NotificationChannelManager.cancelWaterReminderNotification(app)
            FlowWidgets.refreshWater(app)
        }
    }

    fun addCustomWaterQuickAdd(amount: Int, saveAsButton: Boolean) {
        viewModelScope.launch {
            if (saveAsButton) {
                val profile = profileRepository.getProfile()
                val next = profile?.withWaterCustomQuickAdd(amount)
                if (next != null) {
                    profileRepository.updateWaterCustomQuickAdds(next)
                }
            }
            persistWaterWrite { profile, today -> profile.withWaterAdd(amount, today) }
            NotificationChannelManager.cancelWaterReminderNotification(app)
            FlowWidgets.refreshWater(app)
        }
    }

    fun removeWaterCustomQuickAdd(amount: Int) {
        viewModelScope.launch {
            val profile = profileRepository.getProfile() ?: return@launch
            profileRepository.updateWaterCustomQuickAdds(profile.withoutWaterCustomQuickAdd(amount))
            FlowWidgets.refreshWater(app)
        }
    }

    fun undoWater() {
        viewModelScope.launch {
            persistWaterWrite { profile, today -> profile.withWaterUndo(today) }
            FlowWidgets.refreshWater(app)
        }
    }

    fun setWaterRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            profileRepository.updateWaterRemindersEnabled(enabled)
            if (enabled) {
                profileRepository.updateWaterActiveHoursEnabled(true)
            }
            syncWaterReminders()
        }
    }

    fun incrementWaterReminderInterval() {
        setWaterReminderInterval(
            WaterReminderSettings.nextInterval(uiState.value.waterReminderIntervalMinutes),
        )
    }

    fun decrementWaterReminderInterval() {
        setWaterReminderInterval(
            WaterReminderSettings.previousInterval(uiState.value.waterReminderIntervalMinutes),
        )
    }

    fun onWaterReminderIntervalInput(raw: String) {
        if (raw.isEmpty()) return
        val parsed = raw.toIntOrNull() ?: return
        setWaterReminderInterval(WaterReminderSettings.coerceIntervalMinutes(parsed))
    }

    fun setWaterActiveHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            profileRepository.updateWaterActiveHoursEnabled(enabled)
            syncWaterReminders()
        }
    }

    fun setWaterActiveHoursStart(minutesOfDay: Int) {
        viewModelScope.launch {
            profileRepository.updateWaterActiveHoursStart(minutesOfDay)
            syncWaterReminders()
        }
    }

    fun setWaterActiveHoursEnd(minutesOfDay: Int) {
        viewModelScope.launch {
            profileRepository.updateWaterActiveHoursEnd(minutesOfDay)
            syncWaterReminders()
        }
    }

    private fun setWaterReminderInterval(minutes: Int) {
        viewModelScope.launch {
            profileRepository.updateWaterReminderInterval(minutes)
            syncWaterReminders()
        }
    }

    private suspend fun syncWaterReminders() {
        val profile = profileRepository.getProfile()
        app.notificationScheduler.syncWaterReminder(profile)
        if (profile?.waterDrinkRemindersOn() != true) {
            NotificationChannelManager.cancelWaterReminderNotification(app)
        }
    }

    private suspend fun persistWaterWrite(
        write: (UserProfile, Long) -> WaterIntakeWrite?,
    ) {
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        profileRepository.applyWaterIntakeWrite(today) { profile -> write(profile, today) }
    }
}
