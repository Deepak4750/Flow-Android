package com.deepak.flow.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SnoozeSettingsTest {

    @Test
    fun coerceIntervalMinutes_keepsTwoThenExistingSteps() {
        assertEquals(2, SnoozeSettings.coerceIntervalMinutes(1))
        assertEquals(2, SnoozeSettings.coerceIntervalMinutes(2))
        assertEquals(2, SnoozeSettings.coerceIntervalMinutes(3))
        assertEquals(5, SnoozeSettings.coerceIntervalMinutes(4))
        assertEquals(15, SnoozeSettings.coerceIntervalMinutes(15))
        assertEquals(60, SnoozeSettings.coerceIntervalMinutes(99))
    }

    @Test
    fun nextInterval_goesFromTwoOntoFiveMinuteSteps() {
        assertEquals(5, SnoozeSettings.nextInterval(2))
        assertEquals(10, SnoozeSettings.nextInterval(5))
        assertEquals(60, SnoozeSettings.nextInterval(60))
    }

    @Test
    fun previousInterval_returnsToTwoAfterFive() {
        assertEquals(2, SnoozeSettings.previousInterval(2))
        assertEquals(2, SnoozeSettings.previousInterval(5))
        assertEquals(5, SnoozeSettings.previousInterval(10))
    }
}
