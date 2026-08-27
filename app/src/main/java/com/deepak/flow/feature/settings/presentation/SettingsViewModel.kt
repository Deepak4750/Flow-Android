package com.deepak.flow.feature.settings.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.model.SnoozeSettings
import com.deepak.flow.core.model.UserProfile
import com.deepak.flow.core.gym.GymLimits
import com.deepak.flow.core.gym.WeightUnit
import com.deepak.flow.core.notification.NotificationChannelManager
import com.deepak.flow.core.repository.ProfileRepository
import com.deepak.flow.core.repository.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val displayName: String = "",
    val nickname: String = "",
    val savedDisplayName: String = "",
    val savedNickname: String = "",
    val snoozeEnabled: Boolean = SnoozeSettings.DEFAULT_ENABLED,
    val snoozeIntervalMinutes: Int = SnoozeSettings.DEFAULT_INTERVAL_MINUTES,
    val gymWeightUnit: WeightUnit = WeightUnit.KG,
    val gymSetRestSeconds: Int = GymLimits.SET_REST_DEFAULT_SECONDS,
    val gymExerciseRestSeconds: Int = GymLimits.EXERCISE_REST_DEFAULT_SECONDS,
    val reminderCount: Int = 0,
    val keepDataOnUninstall: Boolean = true,
    val isSaving: Boolean = false,
) {
    val hasUnsavedChanges: Boolean
        get() = displayName != savedDisplayName || nickname != savedNickname
}

class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val profileRepository: ProfileRepository =
        (application as FlowApplication).profileRepository

    private val reminderRepository: ReminderRepository =
        (application as FlowApplication).reminderRepository

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.observeProfile().collect { profile ->
                val displayName = profile?.displayName.orEmpty()
                val nickname = profile?.nickname.orEmpty()
                _uiState.update { state ->
                    val snoozeEnabled = profile?.snoozeEnabled ?: SnoozeSettings.DEFAULT_ENABLED
                    val snooze = profile?.snoozeIntervalMinutes ?: SnoozeSettings.DEFAULT_INTERVAL_MINUTES
                    val keepData = profile?.keepDataOnUninstall
                        ?: UserProfile.DEFAULT_KEEP_DATA_ON_UNINSTALL
                    val weightUnit = when (profile?.gymWeightUnit?.uppercase()) {
                        "LB" -> WeightUnit.LB
                        else -> WeightUnit.KG
                    }
                    val setRest = GymLimits.clampSetRestSeconds(
                        profile?.gymSetRestSeconds ?: GymLimits.SET_REST_DEFAULT_SECONDS,
                    )
                    val exerciseRest = GymLimits.clampExerciseRestSeconds(
                        profile?.gymExerciseRestSeconds ?: GymLimits.EXERCISE_REST_DEFAULT_SECONDS,
                    )
                    // Never overwrite text the user is currently editing.
                    if (state.hasUnsavedChanges) {
                        state.copy(
                            savedDisplayName = displayName,
                            savedNickname = nickname,
                            snoozeEnabled = snoozeEnabled,
                            snoozeIntervalMinutes = snooze,
                            gymWeightUnit = weightUnit,
                            gymSetRestSeconds = setRest,
                            gymExerciseRestSeconds = exerciseRest,
                            keepDataOnUninstall = keepData,
                        )
                    } else {
                        state.copy(
                            displayName = displayName,
                            nickname = nickname,
                            savedDisplayName = displayName,
                            savedNickname = nickname,
                            snoozeEnabled = snoozeEnabled,
                            snoozeIntervalMinutes = snooze,
                            gymWeightUnit = weightUnit,
                            gymSetRestSeconds = setRest,
                            gymExerciseRestSeconds = exerciseRest,
                            keepDataOnUninstall = keepData,
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            reminderRepository.observeReminders().collect { reminders ->
                _uiState.update { it.copy(reminderCount = reminders.size) }
            }
        }
    }

    fun updateDisplayName(value: String) = _uiState.update { it.copy(displayName = value) }

    fun updateNickname(value: String) = _uiState.update { it.copy(nickname = value) }

    fun saveProfile() {
        val state = _uiState.value
        if (!state.hasUnsavedChanges || state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            profileRepository.updateProfile(
                displayName = state.displayName,
                nickname = state.nickname,
            )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    savedDisplayName = it.displayName,
                    savedNickname = it.nickname,
                )
            }
        }
    }

    fun deleteAllReminders() {
        viewModelScope.launch {
            NotificationChannelManager.cancelAllReminderNotifications(getApplication())
            reminderRepository.deleteAllReminders()
        }
    }

    fun setSnoozeEnabled(enabled: Boolean) {
        _uiState.update { it.copy(snoozeEnabled = enabled) }
        viewModelScope.launch {
            profileRepository.updateSnoozeEnabled(enabled)
        }
    }

    fun setKeepDataOnUninstall(enabled: Boolean) {
        _uiState.update { it.copy(keepDataOnUninstall = enabled) }
        viewModelScope.launch {
            profileRepository.updateKeepDataOnUninstall(enabled)
        }
    }

    fun cycleGymWeightUnit() {
        val next = when (_uiState.value.gymWeightUnit) {
            WeightUnit.KG -> WeightUnit.LB
            WeightUnit.LB -> WeightUnit.KG
        }
        _uiState.update { it.copy(gymWeightUnit = next) }
        viewModelScope.launch {
            profileRepository.updateGymWeightUnit(next.name)
        }
    }

    fun incrementGymSetRest() {
        updateGymSetRest(_uiState.value.gymSetRestSeconds + 10)
    }

    fun decrementGymSetRest() {
        updateGymSetRest(_uiState.value.gymSetRestSeconds - 10)
    }

    fun onGymSetRestInput(raw: String) {
        if (raw.isEmpty()) return
        val parsed = raw.toIntOrNull() ?: return
        updateGymSetRest(parsed)
    }

    fun incrementGymExerciseRest() {
        updateGymExerciseRest(_uiState.value.gymExerciseRestSeconds + 10)
    }

    fun decrementGymExerciseRest() {
        updateGymExerciseRest(_uiState.value.gymExerciseRestSeconds - 10)
    }

    fun onGymExerciseRestInput(raw: String) {
        if (raw.isEmpty()) return
        val parsed = raw.toIntOrNull() ?: return
        updateGymExerciseRest(parsed)
    }

    private fun updateGymSetRest(seconds: Int) {
        val clamped = GymLimits.clampSetRestSeconds(seconds)
        _uiState.update { it.copy(gymSetRestSeconds = clamped) }
        viewModelScope.launch {
            profileRepository.updateGymSetRestSeconds(clamped)
        }
    }

    private fun updateGymExerciseRest(seconds: Int) {
        val clamped = GymLimits.clampExerciseRestSeconds(seconds)
        _uiState.update { it.copy(gymExerciseRestSeconds = clamped) }
        viewModelScope.launch {
            profileRepository.updateGymExerciseRestSeconds(clamped)
        }
    }

    fun incrementSnoozeInterval() {
        updateSnoozeInterval(SnoozeSettings.nextInterval(uiState.value.snoozeIntervalMinutes))
    }

    fun decrementSnoozeInterval() {
        updateSnoozeInterval(SnoozeSettings.previousInterval(uiState.value.snoozeIntervalMinutes))
    }

    fun onSnoozeIntervalInput(raw: String) {
        if (raw.isEmpty()) return
        val parsed = raw.toIntOrNull() ?: return
        updateSnoozeInterval(SnoozeSettings.coerceIntervalMinutes(parsed))
    }

    private fun updateSnoozeInterval(minutes: Int) {
        _uiState.update { it.copy(snoozeIntervalMinutes = minutes) }
        viewModelScope.launch {
            profileRepository.updateSnoozeInterval(minutes)
        }
    }
}
