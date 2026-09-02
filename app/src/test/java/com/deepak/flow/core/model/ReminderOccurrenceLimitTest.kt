package com.deepak.flow.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ReminderOccurrenceLimitTest {

    private fun reminder(
        limit: Int,
        delivered: Int = 0,
    ) = Reminder(
        id = 1L,
        title = "Zinc",
        category = Category.PERSONAL,
        schedule = Schedule.EveryXDays(3),
        reminderTimes = listOf(LocalTime.of(21, 0)),
        startDate = LocalDate.of(2026, 1, 1),
        expirationMode = ReminderExpirationMode.OCCURRENCE_LIMIT,
        occurrenceLimit = limit,
        occurrencesDelivered = delivered,
    )

    @Test
    fun limitOneAllowsExactlyOneOccurrence() {
        val active = reminder(limit = 1, delivered = 0)
        assertTrue(active.hasOccurrencesRemaining())
        assertFalse(active.isExpiredOn(LocalDate.of(2026, 8, 31)))

        val expired = reminder(limit = 1, delivered = 1)
        assertFalse(expired.hasOccurrencesRemaining())
        assertTrue(expired.isExpiredOn(LocalDate.of(2026, 8, 31)))
        assertEquals(0, expired.remainingOccurrences())
    }

    @Test
    fun limitTenExpiresAfterTenthDelivery() {
        val beforeLast = reminder(limit = 10, delivered = 9)
        assertTrue(beforeLast.hasOccurrencesRemaining())
        assertEquals(1, beforeLast.remainingOccurrences())

        val expired = reminder(limit = 10, delivered = 10)
        assertFalse(expired.hasOccurrencesRemaining())
        assertTrue(expired.isExpiredOn(LocalDate.now()))
    }

    @Test
    fun expiredOccurrenceReminderLeavesActiveList() {
        val today = LocalDate.of(2026, 8, 31)
        val expired = reminder(limit = 3, delivered = 3)
        val active = reminder(limit = 3, delivered = 1).copy(id = 2L)
        assertEquals(listOf(active), listOf(expired, active).activeOn(today))
    }

    @Test
    fun expiredOccurrenceReminderAppearsInHistoryBucket() {
        val today = LocalDate.of(2026, 8, 31)
        val expired = reminder(limit = 3, delivered = 3)
        assertEquals(listOf(expired), listOf(expired).expiredOn(today))
    }
}
