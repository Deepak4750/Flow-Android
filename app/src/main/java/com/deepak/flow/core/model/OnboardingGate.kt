package com.deepak.flow.core.model

sealed interface OnboardingGate {
    data object Loading : OnboardingGate
    data object ShowOnboarding : OnboardingGate
    data object Ready : OnboardingGate
}

fun onboardingGate(profileLoaded: Boolean, profile: UserProfile?): OnboardingGate {
    if (!profileLoaded) return OnboardingGate.Loading
    return if (profile?.onboardingCompleted == true) {
        OnboardingGate.Ready
    } else {
        OnboardingGate.ShowOnboarding
    }
}

/** In-progress onboarding stays incomplete; completed profiles stay completed. */
fun preservedOnboardingCompleted(existingCompleted: Boolean?): Boolean =
    existingCompleted ?: false
