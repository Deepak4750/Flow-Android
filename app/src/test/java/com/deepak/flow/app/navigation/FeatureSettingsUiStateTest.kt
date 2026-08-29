package com.deepak.flow.app.navigation

import org.junit.Assert.assertFalse
import org.junit.Test

class FeatureSettingsUiStateTest {

    @Test
    fun initialFlagsAreUnknownUntilProfileLoads() {
        val initial = FeatureSettingsUiState()
        assertFalse(initial.flagsReady)
    }
}
