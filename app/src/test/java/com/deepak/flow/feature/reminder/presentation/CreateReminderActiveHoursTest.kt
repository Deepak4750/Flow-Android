package com.deepak.flow.feature.reminder.presentation

import com.deepak.flow.core.model.ActiveHours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class CreateReminderActiveHoursTest {

    @Test
    fun showActiveHours_onlyForEveryFewHours() {
        assertFalse(CreateReminderUiState(scheduleType = ScheduleType.DAILY).showActiveHours)
        assertFalse(CreateReminderUiState(scheduleType = ScheduleType.WEEKLY).showActiveHours)
        assertFalse(CreateReminderUiState(scheduleType = ScheduleType.MONTHLY).showActiveHours)
        assertFalse(CreateReminderUiState(scheduleType = ScheduleType.EVERY_X_DAYS).showActiveHours)
        assertTrue(CreateReminderUiState(scheduleType = ScheduleType.EVERY_X_HOURS).showActiveHours)
    }

    @Test
    fun switchingFrequency_preservesActiveHoursInState() {
        val start = LocalTime.of(9, 30)
        val end = LocalTime.of(21, 0)
        val configured = CreateReminderUiState(
            scheduleType = ScheduleType.EVERY_X_HOURS,
            activeHoursEnabled = true,
            activeHoursStart = start,
            activeHoursEnd = end,
        )

        val onDaily = configured.copy(scheduleType = ScheduleType.DAILY)
        assertFalse(onDaily.showActiveHours)
        assertTrue(onDaily.activeHoursEnabled)
        assertEquals(start, onDaily.activeHoursStart)
        assertEquals(end, onDaily.activeHoursEnd)

        val backToHourly = onDaily.copy(scheduleType = ScheduleType.EVERY_X_HOURS)
        assertTrue(backToHourly.showActiveHours)
        assertEquals(start, backToHourly.activeHoursStart)
        assertEquals(end, backToHourly.activeHoursEnd)
    }

    @Test
    fun activeHoursForPersistence_savesWhenEnabled() {
        val state = CreateReminderUiState(
            scheduleType = ScheduleType.DAILY,
            activeHoursEnabled = true,
            activeHoursStart = LocalTime.of(7, 0),
            activeHoursEnd = LocalTime.of(22, 0),
        )
        val persisted = CreateReminderViewModel.activeHoursForPersistence(state)
        assertEquals(ActiveHours(LocalTime.of(7, 0), LocalTime.of(22, 0)), persisted)
    }

    @Test
    fun activeHoursForPersistence_clearsWhenDisabled() {
        val state = CreateReminderUiState(
            scheduleType = ScheduleType.EVERY_X_HOURS,
            activeHoursEnabled = false,
            activeHoursStart = LocalTime.of(7, 0),
            activeHoursEnd = LocalTime.of(22, 0),
        )
        assertNull(CreateReminderViewModel.activeHoursForPersistence(state))
    }
}
