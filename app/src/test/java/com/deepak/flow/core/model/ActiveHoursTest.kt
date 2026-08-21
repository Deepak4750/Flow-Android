package com.deepak.flow.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ActiveHoursTest {

    @Test
    fun normalWindow_activeInside() {
        val hours = ActiveHours(LocalTime.of(8, 0), LocalTime.of(23, 0))
        assertTrue(hours.isActive(LocalTime.of(12, 0)))
        assertFalse(hours.isActive(LocalTime.of(7, 0)))
        assertFalse(hours.isActive(LocalTime.of(23, 0)))
    }

    @Test
    fun midnightCrossing_activeEarlyMorning() {
        val hours = ActiveHours(LocalTime.of(22, 0), LocalTime.of(6, 0))
        assertTrue(hours.isActive(LocalTime.of(1, 0)))
        assertFalse(hours.isActive(LocalTime.of(12, 0)))
    }

    @Test
    fun equalStartEnd_is24Hours() {
        val hours = ActiveHours(LocalTime.of(8, 0), LocalTime.of(8, 0))
        assertTrue(hours.isActive(LocalTime.of(0, 0)))
        assertTrue(hours.isActive(LocalTime.of(23, 59)))
    }

    @Test
    fun shiftIntoActive_movesPastTheWindowToTheNextStart() {
        val hours = ActiveHours(LocalTime.of(8, 0), LocalTime.of(23, 0))
        val zone = ZoneOffset.UTC
        val evening = ZonedDateTime.of(2026, 8, 19, 23, 15, 0, 0, zone).toInstant()
        val shifted = hours.shiftIntoActive(evening, zone)
        assertEquals(
            ZonedDateTime.of(2026, 8, 20, 8, 0, 0, 0, zone).toInstant(),
            shifted,
        )
        val midday = ZonedDateTime.of(2026, 8, 19, 12, 0, 0, 0, zone).toInstant()
        assertEquals(midday, hours.shiftIntoActive(midday, zone))
    }
}
