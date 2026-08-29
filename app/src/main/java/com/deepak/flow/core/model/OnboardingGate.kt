package com.deepak.flow.core.model

sealed interface OnboardingGate {
    data object Loading : OnboardingGate
    data object ShowTutorial : OnboardingGate
    data object Ready : OnboardingGate
}

fun onboardingGate(profileLoaded: Boolean, profile: UserProfile?): OnboardingGate {
    if (!profileLoaded) return OnboardingGate.Loading
    return if (profile?.onboardingCompleted == true) {
        OnboardingGate.Ready
    } else {
        OnboardingGate.ShowTutorial
    }
}

/** In-progress onboarding stays incomplete; completed profiles stay completed. */
fun preservedOnboardingCompleted(existingCompleted: Boolean?): Boolean =
    existingCompleted ?: false
