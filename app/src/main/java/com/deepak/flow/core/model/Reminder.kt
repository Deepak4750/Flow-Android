package com.deepak.flow.core.model

import java.time.LocalDate
import java.time.LocalTime

data class Reminder(
    val id: Long = 0L,
    val title: String,
    val category: Category,
    val customCategoryName: String? = null,
    val schedule: Schedule,
    val reminderTimes: List<LocalTime>,
    val startDate: LocalDate,
    val expirationMode: ReminderExpirationMode = ReminderExpirationMode.NONE,
    val endDate: LocalDate? = null,
    val occurrenceLimit: Int? = null,
    val occurrencesDelivered: Int = 0,
    val enabled: Boolean = true,
    val activeHours: ActiveHours? = null,
    val reason: String? = null,
    val note: String? = null,
    val accentColorIndex: Int? = null,
)

fun Reminder.effectiveExpirationMode(): ReminderExpirationMode = when {
    expirationMode != ReminderExpirationMode.NONE -> expirationMode
    endDate != null -> ReminderExpirationMode.END_DATE
    else -> ReminderExpirationMode.NONE
}

/** End date is inclusive. The reminder expires the day after. */
fun Reminder.isExpiredOn(date: LocalDate): Boolean = when (effectiveExpirationMode()) {
    ReminderExpirationMode.NONE -> false
    ReminderExpirationMode.END_DATE -> endDate != null && date.isAfter(endDate)
    ReminderExpirationMode.OCCURRENCE_LIMIT -> {
        val limit = occurrenceLimit ?: return false
        occurrencesDelivered >= limit
    }
}

fun Reminder.hasOccurrencesRemaining(): Boolean {
    if (effectiveExpirationMode() != ReminderExpirationMode.OCCURRENCE_LIMIT) return true
    val limit = occurrenceLimit ?: return true
    return occurrencesDelivered < limit
}

fun Reminder.remainingOccurrences(): Int? {
    if (effectiveExpirationMode() != ReminderExpirationMode.OCCURRENCE_LIMIT) return null
    val limit = occurrenceLimit ?: return null
    return (limit - occurrencesDelivered).coerceAtLeast(0)
}

fun List<Reminder>.activeOn(date: LocalDate): List<Reminder> =
    filterNot { it.isExpiredOn(date) }

fun List<Reminder>.expiredOn(date: LocalDate): List<Reminder> =
    filter { it.isExpiredOn(date) }
