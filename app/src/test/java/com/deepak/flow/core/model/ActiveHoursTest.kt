package com.deepak.flow.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

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
}
