package com.deepak.flow.feature.onboarding.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.notification.NotificationChannelManager
import com.deepak.flow.core.repository.ProfileRepository
import com.deepak.flow.core.repository.ReminderRepository
import com.deepak.flow.core.widget.FlowWidgets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val page: Int = PAGE_INTRO,
    val displayName: String = "",
    val nickname: String = "",
    val remindersEnabled: Boolean = false,
    val waterEnabled: Boolean = false,
    val gymEnabled: Boolean = false,
    val isSaving: Boolean = false,
)

private const val PAGE_INTRO = 0
private const val PAGE_FEATURES = 1
private const val PAGE_READY = 2
private const val PAGE_PROFILE = 3

class OnboardingViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val app = application as FlowApplication
    private val profileRepository: ProfileRepository = app.profileRepository
    private val reminderRepository: ReminderRepository = app.reminderRepository

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextPage() {
        _uiState.update { state ->
            if (state.page >= PAGE_PROFILE) state else state.copy(page = state.page + 1)
        }
    }

    fun updateDisplayName(value: String) = _uiState.update { it.copy(displayName = value) }
    fun updateNickname(value: String) = _uiState.update { it.copy(nickname = value) }

    fun setRemindersEnabled(enabled: Boolean) =
        _uiState.update { it.copy(remindersEnabled = enabled) }

    fun setWaterEnabled(enabled: Boolean) =
        _uiState.update { it.copy(waterEnabled = enabled) }

    fun setGymEnabled(enabled: Boolean) =
        _uiState.update { it.copy(gymEnabled = enabled) }

    fun continueFromFeatures() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            persistFeatureSelections(_uiState.value)
            _uiState.update { it.copy(isSaving = false, page = PAGE_READY) }
        }
    }

    fun skipToProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            if (_uiState.value.page == PAGE_FEATURES) {
                persistFeatureSelections(_uiState.value)
            }
            _uiState.update { it.copy(isSaving = false, page = PAGE_PROFILE) }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            profileRepository.completeOnboarding(
                displayName = state.displayName,
                nickname = state.nickname,
            )
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            profileRepository.completeOnboarding(displayName = null, nickname = null)
        }
    }

    private suspend fun persistFeatureSelections(state: OnboardingUiState) {
        profileRepository.updateRemindersEnabled(state.remindersEnabled)
        profileRepository.updateWaterEnabled(state.waterEnabled)
        profileRepository.updateGymEnabled(state.gymEnabled)
        if (state.remindersEnabled) {
            reminderRepository.rescheduleAllEnabledReminders()
        } else {
            reminderRepository.cancelAllScheduledReminders()
            NotificationChannelManager.cancelAllReminderNotifications(app)
        }
        app.notificationScheduler.syncWaterReminder(profileRepository.getProfile())
        FlowWidgets.refresh(app)
    }
}
