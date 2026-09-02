package com.deepak.flow.core.gym

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class GymRestVibrationPatternTest {

    @Test
    fun `notification channel pattern mirrors vibrator segments with leading delay`() {
        val expected = longArrayOf(
            0,
            150, 180,
            150, 180,
            480, 180,
            480, 180,
            150, 180,
            150, 180,
            480, 180,
            480, 180,
        )
        assertArrayEquals(expected, GymRestVibrationPattern.notificationChannelPattern())
    }

    @Test
    fun `vibrator segments are two PEP-PEP-PEEEP cycles`() {
        assertEquals(16, GymRestVibrationPattern.vibratorSegments().size)
    }
}
