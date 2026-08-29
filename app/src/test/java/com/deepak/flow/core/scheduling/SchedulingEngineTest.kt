package com.deepak.flow.core.scheduling

import com.deepak.flow.core.model.ActiveHours
import com.deepak.flow.core.model.Category
import com.deepak.flow.core.model.Reminder
import com.deepak.flow.core.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class SchedulingEngineTest {

    private val engine = SchedulingEngine()
    private val zone = ZoneId.of("America/New_York")

    private fun reminder(
        schedule: Schedule = Schedule.Daily,
        times: List<LocalTime> = listOf(LocalTime.of(9, 0)),
        startDate: LocalDate = LocalDate.of(2026, 1, 1),
        endDate: LocalDate? = null,
        activeHours: ActiveHours? = null,
        enabled: Boolean = true,
    ) = Reminder(
        title = "Test",
        category = Category.PERSONAL,
        schedule = schedule,
        reminderTimes = times,
        startDate = startDate,
        endDate = endDate,
        enabled = enabled,
        activeHours = activeHours,
    )

    private fun instant(date: LocalDate, time: LocalTime): Instant =
        ZonedDateTime.of(date, time, zone).toInstant()

    @Test
    fun daily_nextOccurrence_afterReference() {
        val ref = instant(LocalDate.of(2026, 3, 10), LocalTime.of(8, 0))
        val next = engine.calculateNextOccurrence(reminder(), ref, zone)
        assertEquals(instant(LocalDate.of(2026, 3, 10), LocalTime.of(9, 0)), next)
    }

    @Test
    fun daily_multipleTimes_picksNextTimeSameDay() {
        val ref = instant(LocalDate.of(2026, 3, 10), LocalTime.of(10, 0))
        val next = engine.calculateNextOccurrence(
            reminder(times = listOf(LocalTime.of(9, 0), LocalTime.of(13, 0), LocalTime.of(18, 0))),
            ref,
            zone,
        )
        assertEquals(instant(LocalDate.of(2026, 3, 10), LocalTime.of(13, 0)), next)
    }

    @Test
    fun daily_multipleTimes_rollsToNextDay() {
        val ref = instant(LocalDate.of(2026, 3, 10), LocalTime.of(19, 0))
        val next = engine.calculateNextOccurrence(
            reminder(times = listOf(LocalTime.of(9, 0), LocalTime.of(18, 0))),
            ref,
            zone,
        )
        assertEquals(instant(LocalDate.of(2026, 3, 11), LocalTime.of(9, 0)), next)
    }

    @Test
    fun weekly_onlyOnSelectedDays() {
        val ref = instant(LocalDate.of(2026, 3, 9), LocalTime.of(10, 0)) // Monday
        val next = engine.calculateNextOccurrence(
            reminder(schedule = Schedule.Weekly(setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))),
            ref,
            zone,
        )
        assertEquals(instant(LocalDate.of(2026, 3, 11), LocalTime.of(9, 0)), next) // Wednesday
    }

    @Test
    fun monthly_handlesShortMonths() {
        val ref = instant(LocalDate.of(2026, 1, 31), LocalTime.of(10, 0))
        val next = engine.calculateNextOccurrence(
            reminder(schedule = Schedule.Monthly(31), startDate = LocalDate.of(2026, 1, 31)),
            ref,
            zone,
        )
        assertEquals(instant(LocalDate.of(2026, 2, 28), LocalTime.of(9, 0)), next)
    }

    @Test
    fun monthly_leapYear() {
        val ref = instant(LocalDate.of(2024, 2, 28), LocalTime.of(10, 0))
        val next = engine.calculateNextOccurrence(
            reminder(schedule = Schedule.Monthly(29), startDate = LocalDate.of(2024, 1, 29)),
            ref,
            zone,
        )
        assertEquals(instant(LocalDate.of(2024, 2, 29), LocalTime.of(9, 0)), next)
    }

    @Test
    fun everyXDays_fromStartDate() {
        val start = LocalDate.of(2026, 3, 1)
        val ref = instant(LocalDate.of(2026, 3, 2), LocalTime.of(10, 0))
        val next = engine.calculateNextOccurrence(
            reminder(schedule = Schedule.EveryXDays(3), startDate = start),
            ref,
            zone,
        )
        assertEquals(instant(LocalDate.of(2026, 3, 4), LocalTime.of(9, 0)), next)
    }

    @Test
    fun everyXHours_intervalBased() {
        val ref = instant(LocalDate.of(2026, 3, 10), LocalTime.of(10, 0))
        val next = engine.calculateNextOccurrence(
            reminder(
                schedule = Schedule.EveryXHours(3),
                times = listOf(LocalTime.of(7, 0)),
                startDate = LocalDate.of(2026, 3, 10),
            ),
            ref,
            zone,
        )
        assertEquals(instant(LocalDate.of(2026, 3, 10), LocalTime.of(13, 0)), next)
    }

    @Test
    fun startDate_notBeforeStart() {
        val ref = instant(LocalDate.of(2026, 2, 28), LocalTime.of(10, 0))
        val next = engine.calculateNextOccurrence(
            reminder(startDate = LocalDate.of(2026, 3, 1)),
            ref,
            zone,
        )
        assertEquals(instant(LocalDate.of(2026, 3, 1), LocalTime.of(9, 0)), next)
    }

    @Test
    fun endDate_respected() {
        val ref = instant(LocalDate.of(2026, 3, 10), LocalTime.of(10, 0))
        val next = engine.calculateNextOccurrence(
            reminder(endDate = LocalDate.of(2026, 3, 10)),
            ref,
            zone,
        )
        assertNull(next)
    }

    @Test
    fun disabled_reminder_returnsNull() {
        val ref = instant(LocalDate.of(2026, 3, 10), LocalTime.of(8, 0))
        assertNull(engine.calculateNextOccurrence(reminder(enabled = false), ref, zone))
    }

    @Test
    fun activeHours_absoluteSchedule_skipsInactive() {
        val active = ActiveHours(LocalTime.of(8, 0), LocalTime.of(23, 0))
        val ref = instant(LocalDate.of(2026, 3, 10), LocalTime.of(7, 0))
        val next = engine.calculateNextOccurrence(
            reminder(
                times = listOf(LocalTime.of(7, 30), LocalTime.of(9, 0)),
                activeHours = active,
            ),
            ref,
            zone,
        )
        assertEquals(instant(LocalDate.of(2026, 3, 10), LocalTime.of(9, 0)), next)
    }

    @Test
    fun activeHours_everyXHours_shiftsToActiveWindow() {
        val active = ActiveHours(LocalTime.of(8, 0), LocalTime.of(23, 0))
        val ref = instant(LocalDate.of(2026, 3, 10), LocalTime.of(1, 0))
        val next = engine.calculateNextOccurrence(
            reminder(
                schedule = Schedule.EveryXHours(3),
                times = listOf(LocalTime.of(2, 0)),
                startDate = LocalDate.of(2026, 3, 10),
                activeHours = active,
            ),
            ref,
            zone,
        )
        assertNotNull(next)
        assertEquals(LocalTime.of(8, 0), next!!.atZone(zone).toLocalTime())
    }

    @Test
    fun activeHours_crossingMidnight() {
        val active = ActiveHours(LocalTime.of(22, 0), LocalTime.of(6, 0))
        assertTrue(active.isActive(LocalTime.of(1, 0)))
        assertTrue(!active.isActive(LocalTime.of(12, 0)))
    }

    @Test
    fun activeHours_24hourWindow() {
        val active = ActiveHours(LocalTime.of(8, 0), LocalTime.of(8, 0))
        assertTrue(active.isActive(LocalTime.of(3, 0)))
        assertTrue(active.isActive(LocalTime.of(15, 0)))
    }

    @Test
    fun dst_springForward() {
        val dstZone = ZoneId.of("America/New_York")
        val ref = instant(LocalDate.of(2026, 3, 7), LocalTime.of(10, 0))
        val next = engine.calculateNextOccurrence(reminder(), ref, dstZone)
        assertNotNull(next)
        assertTrue(next!!.isAfter(ref))
    }

    @Test
    fun referenceMustBeStrictlyBeforeCandidate() {
        val ref = instant(LocalDate.of(2026, 3, 10), LocalTime.of(9, 0))
        val next = engine.calculateNextOccurrence(reminder(), ref, zone)
        assertEquals(instant(LocalDate.of(2026, 3, 11), LocalTime.of(9, 0)), next)
    }

    @Test
    fun everyXHours_endDatePreventsShiftedOccurrence() {
        val active = ActiveHours(LocalTime.of(8, 0), LocalTime.of(23, 0))
        val ref = instant(LocalDate.of(2026, 3, 10), LocalTime.of(22, 0))
        val next = engine.calculateNextOccurrence(
            reminder(
                schedule = Schedule.EveryXHours(3),
                times = listOf(LocalTime.of(23, 0)),
                startDate = LocalDate.of(2026, 3, 10),
                endDate = LocalDate.of(2026, 3, 10),
                activeHours = active,
            ),
            ref,
            zone,
        )
        assertNull(next)
    }

    @Test
    fun isScheduledOnDate_dailyOnMatchingDay() {
        val date = LocalDate.of(2026, 3, 10)
        assertTrue(engine.isScheduledOnDate(reminder(), date, zone))
    }

    @Test
    fun isScheduledOnDate_falseBeforeStartDate() {
        val date = LocalDate.of(2025, 12, 31)
        assertFalse(engine.isScheduledOnDate(reminder(startDate = LocalDate.of(2026, 1, 1)), date, zone))
    }

    @Test
    fun isScheduledOnDate_weeklyOnlyOnSelectedDays() {
        val monday = LocalDate.of(2026, 3, 9)
        val tuesday = LocalDate.of(2026, 3, 10)
        val weekly = reminder(
            schedule = Schedule.Weekly(setOf(DayOfWeek.MONDAY)),
            startDate = monday,
        )
        assertTrue(engine.isScheduledOnDate(weekly, monday, zone))
        assertFalse(engine.isScheduledOnDate(weekly, tuesday, zone))
    }

    @Test
    fun expiredReminderIsNotNextUp() {
        val afterEnd = instant(LocalDate.of(2026, 3, 11), LocalTime.of(8, 0))
        assertNull(
            engine.calculateNextOccurrence(
                reminder(endDate = LocalDate.of(2026, 3, 10)),
                afterEnd,
                zone,
            ),
        )
    }

    @Test
    fun expiredReminderStillCountsOnPastHistoryDays() {
        val reminder = reminder(endDate = LocalDate.of(2026, 3, 10))
        assertTrue(engine.isScheduledOnDate(reminder, LocalDate.of(2026, 3, 10), zone))
        assertFalse(engine.isScheduledOnDate(reminder, LocalDate.of(2026, 3, 11), zone))
    }
}
