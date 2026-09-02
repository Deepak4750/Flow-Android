package com.deepak.flow.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingGateTest {

    @Test
    fun unknownProfileShowsLoading() {
        assertEquals(OnboardingGate.Loading, onboardingGate(profileLoaded = false, profile = null))
        assertEquals(
            OnboardingGate.Loading,
            onboardingGate(
                profileLoaded = false,
                profile = UserProfile(onboardingCompleted = true),
            ),
        )
    }

    @Test
    fun newUserSeesOnboarding() {
        assertEquals(OnboardingGate.ShowOnboarding, onboardingGate(profileLoaded = true, profile = null))
        assertEquals(
            OnboardingGate.ShowOnboarding,
            onboardingGate(
                profileLoaded = true,
                profile = UserProfile(onboardingCompleted = false),
            ),
        )
    }

    @Test
    fun completedOnboardingOpensTheApp() {
        assertEquals(
            OnboardingGate.Ready,
            onboardingGate(
                profileLoaded = true,
                profile = UserProfile(onboardingCompleted = true),
            ),
        )
    }

    @Test
    fun restartAndOtaDoNotReopenOnboarding() {
        val existing = UserProfile(onboardingCompleted = true, remindersEnabled = true)
        assertEquals(OnboardingGate.Ready, onboardingGate(true, existing))
        assertTrue(existing.onboardingCompleted)
    }

    @Test
    fun settingsProfileEditDoesNotResetOnboarding() {
        assertTrue(preservedOnboardingCompleted(true))
        assertFalse(preservedOnboardingCompleted(false))
        assertFalse(preservedOnboardingCompleted(null))
        val afterNameEdit = UserProfile(
            onboardingCompleted = true,
            displayName = "D",
        ).copy(displayName = "Deepak")
        assertTrue(afterNameEdit.onboardingCompleted)
    }

    @Test
    fun existingEnabledFeaturesStayEnabledOnceLoaded() {
        val existing = UserProfile(
            onboardingCompleted = true,
            remindersEnabled = true,
            waterEnabled = true,
            gymEnabled = true,
        )
        assertEquals(OnboardingGate.Ready, onboardingGate(true, existing))
        assertTrue(existing.remindersEnabled)
        assertTrue(existing.waterEnabled)
        assertTrue(existing.gymEnabled)
    }
}
