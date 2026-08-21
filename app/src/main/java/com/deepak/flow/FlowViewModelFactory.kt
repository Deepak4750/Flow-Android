package com.deepak.flow

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FlowViewModelFactory(
    private val application: FlowApplication,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(com.deepak.flow.feature.home.presentation.HomeViewModel::class.java) ->
                com.deepak.flow.feature.home.presentation.HomeViewModel(application) as T
            modelClass.isAssignableFrom(com.deepak.flow.feature.reminder.presentation.CreateReminderViewModel::class.java) ->
                com.deepak.flow.feature.reminder.presentation.CreateReminderViewModel(application) as T
            modelClass.isAssignableFrom(com.deepak.flow.feature.onboarding.presentation.OnboardingViewModel::class.java) ->
                com.deepak.flow.feature.onboarding.presentation.OnboardingViewModel(application) as T
            modelClass.isAssignableFrom(com.deepak.flow.feature.settings.presentation.SettingsViewModel::class.java) ->
                com.deepak.flow.feature.settings.presentation.SettingsViewModel(application) as T
            modelClass.isAssignableFrom(com.deepak.flow.app.navigation.FeatureSettingsViewModel::class.java) ->
                com.deepak.flow.app.navigation.FeatureSettingsViewModel(application) as T
            modelClass.isAssignableFrom(com.deepak.flow.core.update.AppUpdateViewModel::class.java) ->
                com.deepak.flow.core.update.AppUpdateViewModel(application) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}

class CreateReminderViewModelFactory(
    private val application: FlowApplication,
    private val editReminderId: Long? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(com.deepak.flow.feature.reminder.presentation.CreateReminderViewModel::class.java)) {
            return com.deepak.flow.feature.reminder.presentation.CreateReminderViewModel(
                application = application,
                editReminderId = editReminderId,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
