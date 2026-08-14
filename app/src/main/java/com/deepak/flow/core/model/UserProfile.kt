package com.deepak.flow.core.model

data class UserProfile(
    val displayName: String? = null,
    val nickname: String? = null,
    val onboardingCompleted: Boolean = false,
)
