package com.deepak.flow.core.model

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Drink reminders live on the H₂O page, not as task reminders.
 * Active hours are stored on the profile for water only - tasks still keep
 * per-reminder hours, and there is no shared clock to reuse.
 */
object WaterReminderSettings {
    const val DEFAULT_ENABLED = false
    const val DEFAULT_INTERVAL_MINUTES = 60
    const val MIN_INTERVAL_MINUTES = 30
    const val MAX_INTERVAL_MINUTES = 180
    const val DEFAULT_ACTIVE_HOURS_ENABLED = true
    const val DEFAULT_ACTIVE_START_MINUTES = 8 * 60
    const val DEFAULT_ACTIVE_END_MINUTES = 23 * 60

    fun coerceIntervalMinutes(minutes: Int): Int {
        val allowed = allowedIntervals()
        return allowed.minBy { kotlin.math.abs(it - minutes) }
    }

    fun nextInterval(minutes: Int): Int {
        val current = coerceIntervalMinutes(minutes)
        return allowedIntervals().firstOrNull { it > current } ?: current
    }

    fun previousInterval(minutes: Int): Int {
        val current = coerceIntervalMinutes(minutes)
        return allowedIntervals().lastOrNull { it < current } ?: current
    }

    fun coerceMinutesOfDay(minutes: Int): Int = minutes.coerceIn(0, 23 * 60 + 59)

    fun localTimeFromMinutes(minutes: Int): LocalTime {
        val clamped = coerceMinutesOfDay(minutes)
        return LocalTime.of(clamped / 60, clamped % 60)
    }

    fun minutesOfDay(time: LocalTime): Int = time.hour * 60 + time.minute

    fun nextTriggerInstant(
        from: Instant,
        intervalMinutes: Int,
        activeHours: ActiveHours?,
        zoneId: ZoneId,
    ): Instant {
        val candidate = from.plusSeconds(coerceIntervalMinutes(intervalMinutes) * 60L)
        return activeHours?.shiftIntoActive(candidate, zoneId) ?: candidate
    }

    /** 30, 45, then every 30 minutes up to 3 hours. */
    private fun allowedIntervals(): List<Int> = listOf(30, 45, 60, 90, 120, 150, 180)
}
