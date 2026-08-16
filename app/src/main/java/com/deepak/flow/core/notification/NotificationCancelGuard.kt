package com.deepak.flow.core.notification

/**
 * Swipe-to-dismiss re-posts the reminder. Complete / Snooze / Dismiss arm this
 * guard first so the delete intent does not bring the notification back.
 */
internal object NotificationCancelGuard {
    private val armedIds = mutableSetOf<Long>()

    @Synchronized
    fun arm(reminderId: Long) {
        armedIds.add(reminderId)
    }

    @Synchronized
    fun consume(reminderId: Long): Boolean = armedIds.remove(reminderId)
}
