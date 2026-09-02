package com.deepak.flow.core.model

/**
 * Builds create-form state from a historical reminder.
 * Expiration is cleared so the user chooses fresh limits for the new task.
 */
fun Reminder.toReuseTemplate(): Reminder = copy(
    id = 0L,
    expirationMode = ReminderExpirationMode.NONE,
    endDate = null,
    occurrenceLimit = null,
    occurrencesDelivered = 0,
)
