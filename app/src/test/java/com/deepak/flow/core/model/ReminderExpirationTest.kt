package com.deepak.flow.core.model

import com.deepak.flow.core.model.ReminderExpirationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ReminderExpirationTest {

    private fun reminder(
        endDate: LocalDate? = null,
        expirationMode: ReminderExpirationMode = if (endDate != null) {
            ReminderExpirationMode.END_DATE
        } else {
            ReminderExpirationMode.NONE
        },
    ) = Reminder(
        id = 1L,
        title = "Gym",
        category = Category.FITNESS,
        schedule = Schedule.Daily,
        reminderTimes = listOf(LocalTime.of(9, 0)),
        startDate = LocalDate.of(2026, 1, 1),
        expirationMode = expirationMode,
        endDate = endDate,
    )

    @Test
    fun endDateIsInclusive() {
        val reminder = reminder(endDate = LocalDate.of(2026, 3, 10))
        assertFalse(reminder.isExpiredOn(LocalDate.of(2026, 3, 10)))
        assertTrue(reminder.isExpiredOn(LocalDate.of(2026, 3, 11)))
    }

    @Test
    fun remindersWithoutEndDateStayActive() {
        assertFalse(reminder(endDate = null).isExpiredOn(LocalDate.of(2026, 3, 11)))
    }

    @Test
    fun expiredReminderLeavesTheActiveList() {
        val today = LocalDate.of(2026, 3, 11)
        val expired = reminder(endDate = LocalDate.of(2026, 3, 10)).copy(id = 1L)
        val active = reminder(endDate = null).copy(id = 2L)
        assertEquals(listOf(active), listOf(expired, active).activeOn(today))
    }

    @Test
    fun lastDayStillAppearsInTheActiveList() {
        val lastDay = LocalDate.of(2026, 3, 10)
        val ending = reminder(endDate = lastDay)
        assertEquals(listOf(ending), listOf(ending).activeOn(lastDay))
    }

    @Test
    fun existingEndDateRemindersMigrateToEndDateMode() {
        val legacyShape = Reminder(
            id = 1L,
            title = "Legacy",
            category = Category.PERSONAL,
            schedule = Schedule.Daily,
            reminderTimes = listOf(LocalTime.of(9, 0)),
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 3, 10),
        )
        assertEquals(ReminderExpirationMode.END_DATE, legacyShape.effectiveExpirationMode())
    }

    @Test
    fun unlimitedRemindersStayActiveWithoutExpirationMode() {
        val unlimited = reminder(endDate = null, expirationMode = ReminderExpirationMode.NONE)
        assertEquals(ReminderExpirationMode.NONE, unlimited.effectiveExpirationMode())
        assertFalse(unlimited.isExpiredOn(LocalDate.of(2026, 3, 11)))
    }
}
