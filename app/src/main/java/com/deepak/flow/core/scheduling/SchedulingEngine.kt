package com.deepak.flow.core.scheduling

import com.deepak.flow.core.model.ActiveHours
import com.deepak.flow.core.model.Reminder
import com.deepak.flow.core.model.Schedule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class SchedulingEngine {

    fun calculateNextOccurrence(
        reminder: Reminder,
        referenceInstant: Instant,
        zoneId: ZoneId,
        activeHours: ActiveHours? = reminder.activeHours,
    ): Instant? {
        if (!reminder.enabled) return null
        if (reminder.reminderTimes.isEmpty()) return null

        val referenceZdt = referenceInstant.atZone(zoneId)
        val referenceDate = referenceZdt.toLocalDate()
        val endDate = reminder.endDate

        if (endDate != null && referenceDate.isAfter(endDate)) return null

        val effectiveStartDate = maxOf(reminder.startDate, referenceDate.minusYears(1))
        val searchEndDate = endDate ?: referenceDate.plusYears(2)

        return when (val schedule = reminder.schedule) {
            is Schedule.EveryXHours -> calculateEveryXHours(
                reminder = reminder,
                referenceInstant = referenceInstant,
                zoneId = zoneId,
                activeHours = activeHours,
                intervalHours = schedule.intervalHours,
                endDate = endDate,
            )

            else -> calculateAbsoluteSchedule(
                reminder = reminder,
                referenceInstant = referenceInstant,
                zoneId = zoneId,
                activeHours = activeHours,
                schedule = schedule,
                searchStartDate = if (referenceDate.isBefore(reminder.startDate)) reminder.startDate else referenceDate,
                searchEndDate = searchEndDate,
                endDate = endDate,
            )
        }
    }

    private fun calculateAbsoluteSchedule(
        reminder: Reminder,
        referenceInstant: Instant,
        zoneId: ZoneId,
        activeHours: ActiveHours?,
        schedule: Schedule,
        searchStartDate: LocalDate,
        searchEndDate: LocalDate,
        endDate: LocalDate?,
    ): Instant? {
        var currentDate = searchStartDate
        val sortedTimes = reminder.reminderTimes.sorted()

        while (!currentDate.isAfter(searchEndDate)) {
            if (currentDate.isBefore(reminder.startDate)) {
                currentDate = currentDate.plusDays(1)
                continue
            }
            if (endDate != null && currentDate.isAfter(endDate)) return null

            if (matchesSchedule(schedule, currentDate, reminder.startDate)) {
                for (time in sortedTimes) {
                    val candidate = ZonedDateTime.of(currentDate, time, zoneId).toInstant()
                    if (!candidate.isAfter(referenceInstant)) continue

                    if (activeHours != null && !activeHours.isActive(time)) continue

                    if (endDate != null && currentDate.isAfter(endDate)) return null
                    return candidate
                }
            }
            currentDate = currentDate.plusDays(1)
        }
        return null
    }

    private fun matchesSchedule(schedule: Schedule, date: LocalDate, startDate: LocalDate): Boolean {
        if (date.isBefore(startDate)) return false
        return when (schedule) {
            Schedule.Daily -> true
            is Schedule.Weekly -> schedule.daysOfWeek.contains(date.dayOfWeek)
            is Schedule.Monthly -> {
                val targetDay = schedule.dayOfMonth.coerceAtMost(date.lengthOfMonth())
                date.dayOfMonth == targetDay
            }
            is Schedule.EveryXDays -> {
                val daysBetween = ChronoUnit.DAYS.between(startDate, date)
                daysBetween >= 0 && daysBetween % schedule.intervalDays == 0L
            }
            is Schedule.EveryXHours -> true
        }
    }

    private fun calculateEveryXHours(
        reminder: Reminder,
        referenceInstant: Instant,
        zoneId: ZoneId,
        activeHours: ActiveHours?,
        intervalHours: Int,
        endDate: LocalDate?,
    ): Instant? {
        val anchorTime = reminder.reminderTimes.minOrNull() ?: LocalTime.MIDNIGHT
        val anchor = ZonedDateTime.of(reminder.startDate, anchorTime, zoneId).toInstant()

        var candidate = if (referenceInstant.isBefore(anchor)) {
            anchor
        } else {
            val hoursSinceAnchor = ChronoUnit.HOURS.between(anchor, referenceInstant)
            val intervalsElapsed = hoursSinceAnchor / intervalHours + 1
            anchor.plus((intervalsElapsed * intervalHours).toLong(), ChronoUnit.HOURS)
        }

        repeat(10_000) {
            val adjusted = applyActiveHoursShift(candidate, zoneId, activeHours)
            val adjustedDate = adjusted.atZone(zoneId).toLocalDate()

            if (endDate != null && adjustedDate.isAfter(endDate)) return null
            if (adjusted.isAfter(referenceInstant)) return adjusted

            candidate = candidate.plus(intervalHours.toLong(), ChronoUnit.HOURS)
        }
        return null
    }

    private fun applyActiveHoursShift(
        instant: Instant,
        zoneId: ZoneId,
        activeHours: ActiveHours?,
    ): Instant {
        if (activeHours == null) return instant

        val zdt = instant.atZone(zoneId)
        val time = zdt.toLocalTime()

        if (activeHours.isActive(time)) return instant

        val nextActiveStart = if (activeHours.startTime == activeHours.endTime) {
            return instant
        } else if (activeHours.startTime.isBefore(activeHours.endTime)) {
            if (time.isBefore(activeHours.startTime)) {
                ZonedDateTime.of(zdt.toLocalDate(), activeHours.startTime, zoneId)
            } else {
                ZonedDateTime.of(zdt.toLocalDate().plusDays(1), activeHours.startTime, zoneId)
            }
        } else {
            if (!time.isBefore(activeHours.endTime) && time.isBefore(activeHours.startTime)) {
                ZonedDateTime.of(zdt.toLocalDate(), activeHours.startTime, zoneId)
            } else if (!time.isBefore(activeHours.startTime)) {
                ZonedDateTime.of(zdt.toLocalDate().plusDays(1), activeHours.startTime, zoneId)
            } else {
                ZonedDateTime.of(zdt.toLocalDate(), activeHours.startTime, zoneId)
            }
        }

        return nextActiveStart.toInstant()
    }

    fun isOccurrenceStillValid(
        scheduledInstant: Instant,
        reminder: Reminder,
        currentInstant: Instant,
        zoneId: ZoneId,
    ): Boolean {
        if (!reminder.enabled) return false
        val gracePeriod = java.time.Duration.ofMinutes(15)
        val scheduledZdt = scheduledInstant.atZone(zoneId)
        val currentZdt = currentInstant.atZone(zoneId)

        if (currentInstant.isBefore(scheduledInstant)) return true
        if (currentInstant.minus(gracePeriod).isAfter(scheduledInstant)) return false

        val nextFromScheduled = calculateNextOccurrence(
            reminder = reminder,
            referenceInstant = scheduledInstant.minusNanos(1),
            zoneId = zoneId,
        ) ?: return false

        return nextFromScheduled == scheduledInstant ||
            ChronoUnit.MINUTES.between(scheduledZdt, currentZdt) <= 15
    }
}
