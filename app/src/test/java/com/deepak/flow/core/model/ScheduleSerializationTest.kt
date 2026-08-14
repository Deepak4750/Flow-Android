package com.deepak.flow.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class ScheduleSerializationTest {

    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
    }

    @Test
    fun daily_roundTrip() {
        val schedule = Schedule.Daily
        val encoded = json.encodeToString(Schedule.serializer(), schedule)
        assertTrue(encoded.contains("daily"))
        val decoded = json.decodeFromString(Schedule.serializer(), encoded)
        assertEquals(schedule, decoded)
    }

    @Test
    fun weekly_roundTrip() {
        val schedule = Schedule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        val encoded = json.encodeToString(Schedule.serializer(), schedule)
        val decoded = json.decodeFromString(Schedule.serializer(), encoded)
        assertEquals(schedule, decoded)
    }

    @Test
    fun monthly_roundTrip() {
        val schedule = Schedule.Monthly(15)
        val encoded = json.encodeToString(Schedule.serializer(), schedule)
        val decoded = json.decodeFromString(Schedule.serializer(), encoded)
        assertEquals(schedule, decoded)
    }

    @Test
    fun everyXDays_roundTrip() {
        val schedule = Schedule.EveryXDays(3)
        val encoded = json.encodeToString(Schedule.serializer(), schedule)
        assertTrue(encoded.contains("every_x_days"))
        val decoded = json.decodeFromString(Schedule.serializer(), encoded)
        assertEquals(schedule, decoded)
    }

    @Test
    fun everyXHours_roundTrip() {
        val schedule = Schedule.EveryXHours(4)
        val encoded = json.encodeToString(Schedule.serializer(), schedule)
        assertTrue(encoded.contains("every_x_hours"))
        val decoded = json.decodeFromString(Schedule.serializer(), encoded)
        assertEquals(schedule, decoded)
    }

    @Test
    fun activeHours_roundTrip() {
        val hours = ActiveHours(LocalTime.of(8, 0), LocalTime.of(23, 0))
        val encoded = json.encodeToString(ActiveHours.serializer(), hours)
        val decoded = json.decodeFromString(ActiveHours.serializer(), encoded)
        assertEquals(hours, decoded)
    }
}
