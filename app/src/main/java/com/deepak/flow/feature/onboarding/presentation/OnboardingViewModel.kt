package com.deepak.flow.feature.onboarding.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val displayName: String = "",
    val nickname: String = "",
    val isSaving: Boolean = false,
)

class OnboardingViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val profileRepository: ProfileRepository =
        (application as FlowApplication).profileRepository

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateDisplayName(value: String) = _uiState.update { it.copy(displayName = value) }
    fun updateNickname(value: String) = _uiState.update { it.copy(nickname = value) }

    // Both paths persist the profile and stop there: FlowApp observes
    // isOnboardingComplete() and swaps the UI itself, so there is nothing to call back to.
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
}
