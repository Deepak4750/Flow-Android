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

    @Test
    fun emptyIntervalInput_defaultsToMinimum() {
        val state = CreateReminderUiState(everyXDays = 12, everyXHours = 8)

        val daysAfterClear = state.copy(
            everyXDays = run {
                val digits = ""
                if (digits.isEmpty()) CreateReminderViewModel.INTERVAL_DAYS_MIN else 12
            },
        )
        assertEquals(1, daysAfterClear.everyXDays)

        val hoursAfterClear = state.copy(
            everyXHours = run {
                val digits = ""
                if (digits.isEmpty()) CreateReminderViewModel.INTERVAL_HOURS_MIN else 8
            },
        )
        assertEquals(1, hoursAfterClear.everyXHours)
    }
}
