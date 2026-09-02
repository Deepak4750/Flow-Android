package com.deepak.flow.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ReminderReuseTest {

    @Test
    fun reuseTemplatePreservesConfigurationExceptExpiration() {
        val historical = Reminder(
            id = 42L,
            title = "Take zinc tablet",
            category = Category.HEALTH,
            schedule = Schedule.EveryXDays(3),
            reminderTimes = listOf(LocalTime.of(21, 0)),
            startDate = LocalDate.of(2026, 1, 1),
            expirationMode = ReminderExpirationMode.END_DATE,
            endDate = LocalDate.of(2026, 8, 30),
            occurrenceLimit = null,
            occurrencesDelivered = 0,
            reason = "Daily supplement",
            note = "After food",
        )

        val template = historical.toReuseTemplate()

        assertEquals(0L, template.id)
        assertEquals("Take zinc tablet", template.title)
        assertEquals(Schedule.EveryXDays(3), template.schedule)
        assertEquals(listOf(LocalTime.of(21, 0)), template.reminderTimes)
        assertEquals(LocalDate.of(2026, 1, 1), template.startDate)
        assertEquals("Daily supplement", template.reason)
        assertEquals("After food", template.note)
        assertEquals(ReminderExpirationMode.NONE, template.expirationMode)
        assertNull(template.endDate)
        assertNull(template.occurrenceLimit)
        assertEquals(0, template.occurrencesDelivered)
    }

    @Test
    fun reuseFromOccurrenceLimitedReminderClearsLimit() {
        val historical = Reminder(
            id = 7L,
            title = "Stretch",
            category = Category.FITNESS,
            schedule = Schedule.Daily,
            reminderTimes = listOf(LocalTime.of(8, 0)),
            startDate = LocalDate.of(2026, 3, 1),
            expirationMode = ReminderExpirationMode.OCCURRENCE_LIMIT,
            occurrenceLimit = 10,
            occurrencesDelivered = 10,
        )

        val template = historical.toReuseTemplate()

        assertNull(template.endDate)
        assertNull(template.occurrenceLimit)
        assertEquals(0, template.occurrencesDelivered)
    }
}
