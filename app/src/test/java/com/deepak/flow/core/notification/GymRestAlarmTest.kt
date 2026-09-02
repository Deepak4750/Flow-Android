package com.deepak.flow.core.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GymRestAlarmTest {

    @Test
    fun shouldAlertOnlyAtOrAfterScheduledCompletion() {
        val endsAt = 60_000L
        assertFalse(GymRestAlarm.shouldAlert(59_999L, endsAt))
        assertTrue(GymRestAlarm.shouldAlert(60_000L, endsAt))
        assertTrue(GymRestAlarm.shouldAlert(60_250L, endsAt))
    }

    @Test
    fun shouldNotAlertBeforeScheduledCompletion() {
        assertFalse(GymRestAlarm.shouldAlert(57_000L, 60_000L))
        assertFalse(GymRestAlarm.shouldAlert(59_000L, 60_000L))
    }
}
