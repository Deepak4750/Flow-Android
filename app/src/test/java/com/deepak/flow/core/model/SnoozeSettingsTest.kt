package com.deepak.flow.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SnoozeSettingsTest {

    @Test
    fun coerceIntervalMinutes_clampsToRange() {
        assertEquals(5, SnoozeSettings.coerceIntervalMinutes(1))
        assertEquals(60, SnoozeSettings.coerceIntervalMinutes(99))
        assertEquals(15, SnoozeSettings.coerceIntervalMinutes(15))
    }
}
