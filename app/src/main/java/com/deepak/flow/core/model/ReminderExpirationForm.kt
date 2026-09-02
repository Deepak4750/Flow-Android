package com.deepak.flow.core.model

import java.time.LocalDate

data class ReminderExpirationForm(
    val mode: ReminderExpirationMode = ReminderExpirationMode.NONE,
    val endDate: LocalDate? = null,
    val occurrenceLimit: Int = DEFAULT_OCCURRENCE_LIMIT,
) {
    fun withMode(
        newMode: ReminderExpirationMode,
        startDate: LocalDate,
    ): ReminderExpirationForm = when (newMode) {
        ReminderExpirationMode.NONE -> copy(
            mode = newMode,
            endDate = null,
            occurrenceLimit = DEFAULT_OCCURRENCE_LIMIT,
        )
        ReminderExpirationMode.END_DATE -> copy(
            mode = newMode,
            endDate = endDate ?: startDate.plusMonths(1),
            occurrenceLimit = DEFAULT_OCCURRENCE_LIMIT,
        )
        ReminderExpirationMode.OCCURRENCE_LIMIT -> copy(
            mode = newMode,
            endDate = null,
            occurrenceLimit = occurrenceLimit.coerceAtLeast(MIN_OCCURRENCE_LIMIT),
        )
    }

    fun normalizedForSave(): ReminderExpirationForm = when (mode) {
        ReminderExpirationMode.NONE -> copy(endDate = null, occurrenceLimit = DEFAULT_OCCURRENCE_LIMIT)
        ReminderExpirationMode.END_DATE -> copy(
            occurrenceLimit = DEFAULT_OCCURRENCE_LIMIT,
        )
        ReminderExpirationMode.OCCURRENCE_LIMIT -> copy(
            endDate = null,
            occurrenceLimit = occurrenceLimit.coerceIn(MIN_OCCURRENCE_LIMIT, MAX_OCCURRENCE_LIMIT),
        )
    }

    companion object {
        const val MIN_OCCURRENCE_LIMIT = 1
        const val MAX_OCCURRENCE_LIMIT = 9999
        const val DEFAULT_OCCURRENCE_LIMIT = 10
    }
}

fun ReminderExpirationForm.toReminderFields(): Triple<ReminderExpirationMode, LocalDate?, Int?> =
    when (normalizedForSave().mode) {
        ReminderExpirationMode.NONE -> Triple(ReminderExpirationMode.NONE, null, null)
        ReminderExpirationMode.END_DATE -> Triple(ReminderExpirationMode.END_DATE, endDate, null)
        ReminderExpirationMode.OCCURRENCE_LIMIT -> Triple(
            ReminderExpirationMode.OCCURRENCE_LIMIT,
            null,
            occurrenceLimit.coerceAtLeast(ReminderExpirationForm.MIN_OCCURRENCE_LIMIT),
        )
    }
