package com.deepak.flow.feature.settings.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
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
    val reminderCount: Int = 0,
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
                    // Never overwrite text the user is currently editing.
                    if (state.hasUnsavedChanges) {
                        state.copy(savedDisplayName = displayName, savedNickname = nickname)
                    } else {
                        state.copy(
                            displayName = displayName,
                            nickname = nickname,
                            savedDisplayName = displayName,
                            savedNickname = nickname,
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
            reminderRepository.deleteAllReminders()
        }
    }
}
