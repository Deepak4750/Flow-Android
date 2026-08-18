package com.deepak.flow.core.notification

internal object ReminderNotificationIntents {
    const val ACTION_COMPLETE = "com.deepak.flow.action.COMPLETE"
    const val ACTION_SNOOZE = "com.deepak.flow.action.SNOOZE"
    const val ACTION_DISMISS = "com.deepak.flow.action.DISMISS"
    const val ACTION_RESTORE = "com.deepak.flow.action.RESTORE"
}

internal data class ReminderNotificationActionPlan(
    val markCompleted: Boolean,
    val scheduleSnooze: Boolean,
    val cancelNotification: Boolean,
    val cancelPendingSnooze: Boolean,
)

internal fun planForReminderNotificationAction(action: String?): ReminderNotificationActionPlan? =
    when (action) {
        ReminderNotificationIntents.ACTION_COMPLETE -> ReminderNotificationActionPlan(
            markCompleted = true,
            scheduleSnooze = false,
            cancelNotification = true,
            cancelPendingSnooze = true,
        )
        ReminderNotificationIntents.ACTION_SNOOZE -> ReminderNotificationActionPlan(
            markCompleted = false,
            scheduleSnooze = true,
            cancelNotification = true,
            cancelPendingSnooze = false,
        )
        ReminderNotificationIntents.ACTION_DISMISS -> ReminderNotificationActionPlan(
            markCompleted = false,
            scheduleSnooze = false,
            cancelNotification = true,
            cancelPendingSnooze = true,
        )
        else -> null
    }
