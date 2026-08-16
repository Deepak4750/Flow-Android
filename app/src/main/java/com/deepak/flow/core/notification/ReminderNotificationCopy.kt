package com.deepak.flow.core.notification

/**
 * Notification body is only the user's note. Placeholders and canned fallback
 * copy must never appear as if the person wrote them.
 */
fun reminderNotificationBody(note: String?): String? {
    val trimmed = note?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    if (trimmed.equals(PLACEHOLDER_NOTE, ignoreCase = true)) return null
    if (trimmed.equals(FALLBACK_NOTE, ignoreCase = true)) return null
    return trimmed
}

private const val PLACEHOLDER_NOTE = "e.g. Time to show up."
private const val FALLBACK_NOTE = "Time to show up."
