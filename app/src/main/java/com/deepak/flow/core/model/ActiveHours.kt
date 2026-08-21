package com.deepak.flow.core.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Serializable
data class ActiveHours(
    val startTime: @Serializable(with = LocalTimeSerializer::class) LocalTime,
    val endTime: @Serializable(with = LocalTimeSerializer::class) LocalTime,
) {
    fun isActive(time: LocalTime): Boolean {
        if (startTime == endTime) return true
        return if (startTime.isBefore(endTime)) {
            !time.isBefore(startTime) && time.isBefore(endTime)
        } else {
            !time.isBefore(startTime) || time.isBefore(endTime)
        }
    }

    fun shiftIntoActive(instant: Instant, zoneId: ZoneId): Instant {
        val zdt = instant.atZone(zoneId)
        val time = zdt.toLocalTime()
        if (isActive(time)) return instant
        if (startTime == endTime) return instant

        val nextActiveStart = if (startTime.isBefore(endTime)) {
            if (time.isBefore(startTime)) {
                ZonedDateTime.of(zdt.toLocalDate(), startTime, zoneId)
            } else {
                ZonedDateTime.of(zdt.toLocalDate().plusDays(1), startTime, zoneId)
            }
        } else if (!time.isBefore(endTime) && time.isBefore(startTime)) {
            ZonedDateTime.of(zdt.toLocalDate(), startTime, zoneId)
        } else if (!time.isBefore(startTime)) {
            ZonedDateTime.of(zdt.toLocalDate().plusDays(1), startTime, zoneId)
        } else {
            ZonedDateTime.of(zdt.toLocalDate(), startTime, zoneId)
        }
        return nextActiveStart.toInstant()
    }
}
