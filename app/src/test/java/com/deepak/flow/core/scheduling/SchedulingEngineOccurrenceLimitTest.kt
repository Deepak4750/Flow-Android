package com.deepak.flow.core.scheduling

import com.deepak.flow.core.model.Category
import com.deepak.flow.core.model.Reminder
import com.deepak.flow.core.model.ReminderExpirationMode
import com.deepak.flow.core.model.Schedule
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SchedulingEngineOccurrenceLimitTest {

    private val engine = SchedulingEngine()
    private val zone = ZoneId.of("UTC")

    private fun reminder(limit: Int, delivered: Int) = Reminder(
        id = 1L,
        title = "Test",
        category = Category.PERSONAL,
        schedule = Schedule.Daily,
        reminderTimes = listOf(LocalTime.of(9, 0)),
        startDate = LocalDate.of(2026, 1, 1),
        expirationMode = ReminderExpirationMode.OCCURRENCE_LIMIT,
        occurrenceLimit = limit,
        occurrencesDelivered = delivered,
    )

    @Test
    fun noNextOccurrenceAfterLimitReached() {
        val expired = reminder(limit = 2, delivered = 2)
        val next = engine.calculateNextOccurrence(
            reminder = expired,
            referenceInstant = Instant.parse("2026-08-31T08:00:00Z"),
            zoneId = zone,
        )
        assertNull(next)
    }

    @Test
    fun schedulesUntilFinalOccurrence() {
        val almostDone = reminder(limit = 2, delivered = 1)
        val next = engine.calculateNextOccurrence(
            reminder = almostDone,
            referenceInstant = Instant.parse("2026-08-31T08:00:00Z"),
            zoneId = zone,
        )
        assertNotNull(next)
    }

    @Test
    fun endDateReminderStillExpiresAfterEndDate() {
        val reminder = Reminder(
            id = 2L,
            title = "End dated",
            category = Category.PERSONAL,
            schedule = Schedule.Daily,
            reminderTimes = listOf(LocalTime.of(9, 0)),
            startDate = LocalDate.of(2026, 1, 1),
            expirationMode = ReminderExpirationMode.END_DATE,
            endDate = LocalDate.of(2026, 8, 30),
        )
        val next = engine.calculateNextOccurrence(
            reminder = reminder,
            referenceInstant = Instant.parse("2026-08-31T08:00:00Z"),
            zoneId = zone,
        )
        assertNull(next)
    }
}
