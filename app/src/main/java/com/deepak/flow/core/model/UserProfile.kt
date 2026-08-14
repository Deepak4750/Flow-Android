package com.deepak.flow.core.model

data class UserProfile(
    val displayName: String? = null,
    val nickname: String? = null,
    val onboardingCompleted: Boolean = false,
    val snoozeEnabled: Boolean = SnoozeSettings.DEFAULT_ENABLED,
    val snoozeIntervalMinutes: Int = SnoozeSettings.DEFAULT_INTERVAL_MINUTES,
)
