package com.deepak.flow.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ReminderModelTest {

    @Test
    fun reminder_defaultsEnabled() {
        val reminder = Reminder(
            title = "Gym",
            category = Category.FITNESS,
            schedule = Schedule.Daily,
            reminderTimes = listOf(LocalTime.of(19, 0)),
            startDate = LocalDate.now(),
        )
        assertEquals(true, reminder.enabled)
        assertEquals("Gym", reminder.title)
        assertEquals(Category.FITNESS, reminder.category)
    }

    @Test
    fun categoryLabel_usesCustomName() {
        val reminder = Reminder(
            title = "Ship",
            category = Category.CUSTOM,
            customCategoryName = "Side project",
            schedule = Schedule.Daily,
            reminderTimes = listOf(LocalTime.of(19, 0)),
            startDate = LocalDate.now(),
        )
        assertEquals("Side project", reminder.categoryLabel())
    }

    @Test
    fun categoryLabel_fallsBackWhenCustomNameBlank() {
        val reminder = Reminder(
            title = "Ship",
            category = Category.CUSTOM,
            customCategoryName = "  ",
            schedule = Schedule.Daily,
            reminderTimes = listOf(LocalTime.of(19, 0)),
            startDate = LocalDate.now(),
        )
        assertEquals("Custom", reminder.categoryLabel())
    }
}
