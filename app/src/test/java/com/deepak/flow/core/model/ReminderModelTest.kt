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
    fun category_displayNames() {
        assertEquals("Health", Category.HEALTH.displayName)
        assertEquals("Fitness", Category.FITNESS.displayName)
        assertEquals("Study", Category.STUDY.displayName)
    }
}
