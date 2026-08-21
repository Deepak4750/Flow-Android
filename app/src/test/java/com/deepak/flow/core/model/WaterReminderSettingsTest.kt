package com.deepak.flow.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class WaterReminderSettingsTest {

    @Test
    fun intervalStaysBetweenThirtyMinutesAndThreeHours() {
        assertEquals(30, WaterReminderSettings.coerceIntervalMinutes(0))
        assertEquals(30, WaterReminderSettings.coerceIntervalMinutes(30))
        assertEquals(45, WaterReminderSettings.coerceIntervalMinutes(45))
        assertEquals(45, WaterReminderSettings.coerceIntervalMinutes(50))
        assertEquals(90, WaterReminderSettings.coerceIntervalMinutes(90))
        assertEquals(180, WaterReminderSettings.coerceIntervalMinutes(180))
        assertEquals(180, WaterReminderSettings.coerceIntervalMinutes(240))
    }

    @Test
    fun stepperIncludesFortyFiveMinutes() {
        assertEquals(45, WaterReminderSettings.nextInterval(30))
        assertEquals(60, WaterReminderSettings.nextInterval(45))
        assertEquals(45, WaterReminderSettings.previousInterval(60))
        assertEquals(180, WaterReminderSettings.nextInterval(180))
        assertEquals(150, WaterReminderSettings.previousInterval(180))
        assertEquals(30, WaterReminderSettings.previousInterval(30))
    }

    @Test
    fun nextTriggerAddsTheIntervalWhenInsideActiveHours() {
        val zone = ZoneOffset.UTC
        val from = ZonedDateTime.of(2026, 8, 19, 10, 0, 0, 0, zone).toInstant()
        val hours = ActiveHours(LocalTime.of(8, 0), LocalTime.of(23, 0))
        val next = WaterReminderSettings.nextTriggerInstant(from, 60, hours, zone)
        assertEquals(
            ZonedDateTime.of(2026, 8, 19, 11, 0, 0, 0, zone).toInstant(),
            next,
        )
    }

    @Test
    fun nextTriggerWaitsUntilMorningWhenTheIntervalLandsAsleep() {
        val zone = ZoneOffset.UTC
        val from = ZonedDateTime.of(2026, 8, 19, 22, 45, 0, 0, zone).toInstant()
        val hours = ActiveHours(LocalTime.of(8, 0), LocalTime.of(23, 0))
        val next = WaterReminderSettings.nextTriggerInstant(from, 30, hours, zone)
        assertEquals(
            ZonedDateTime.of(2026, 8, 20, 8, 0, 0, 0, zone).toInstant(),
            next,
        )
    }

    @Test
    fun nextTriggerIgnoresActiveHoursWhenTheyAreOff() {
        val from = Instant.parse("2026-08-19T22:45:00Z")
        val next = WaterReminderSettings.nextTriggerInstant(from, 30, null, ZoneOffset.UTC)
        assertEquals(Instant.parse("2026-08-19T23:15:00Z"), next)
    }
}
