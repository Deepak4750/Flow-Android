package com.deepak.flow.feature.reminder.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class CreateReminderIntervalTest {

    @Test
    fun intervalConstants_areSensible() {
        assertEquals(1, CreateReminderViewModel.INTERVAL_DAYS_MIN)
        assertEquals(365, CreateReminderViewModel.INTERVAL_DAYS_MAX)
        assertEquals(1, CreateReminderViewModel.INTERVAL_HOURS_MIN)
        assertEquals(168, CreateReminderViewModel.INTERVAL_HOURS_MAX)
    }
}
