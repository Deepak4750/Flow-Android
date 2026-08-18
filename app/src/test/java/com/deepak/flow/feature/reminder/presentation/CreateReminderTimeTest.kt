package com.deepak.flow.feature.reminder.presentation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class CreateReminderTimeTest {

    @Test
    fun defaultNewReminderTime_usesTheClockAtOpenAndDropsSeconds() {
        val now = LocalTime.of(16, 52, 30, 123_000_000)
        assertEquals(LocalTime.of(16, 52), defaultNewReminderTime(now))
    }

    @Test
    fun defaultNewReminderTime_keepsMorningTimes() {
        val now = LocalTime.of(4, 52)
        assertEquals(LocalTime.of(4, 52), defaultNewReminderTime(now))
    }
}
