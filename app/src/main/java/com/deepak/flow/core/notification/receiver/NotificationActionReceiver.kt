package com.deepak.flow.core.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.model.SnoozeSettings
import com.deepak.flow.core.notification.NotificationChannelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId < 0) return

        val pendingResult = goAsync()
        val app = context.applicationContext as FlowApplication

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_COMPLETE -> handleComplete(context, app, reminderId)
                    ACTION_SNOOZE -> handleSnooze(context, app, reminderId)
                    ACTION_DISMISS -> handleDismiss(context, reminderId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleComplete(
        context: Context,
        app: FlowApplication,
        reminderId: Long,
    ) {
        val reminder = app.reminderRepository.getReminder(reminderId) ?: return
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        app.reminderRepository.setTodayCompletion(
            reminderId = reminderId,
            dateEpochDay = today,
            completed = true,
        )
        NotificationChannelManager.cancelReminderNotification(context, reminderId)
    }

    private suspend fun handleSnooze(
        context: Context,
        app: FlowApplication,
        reminderId: Long,
    ) {
        val reminder = app.reminderRepository.getReminder(reminderId) ?: return
        if (!reminder.enabled) return

        val profile = app.profileRepository.getProfile()
        if (profile?.snoozeEnabled != true) return

        val snoozeMinutes = profile.snoozeIntervalMinutes

        app.notificationScheduler.scheduleSnooze(
            reminderId = reminderId,
            snoozeMinutes = SnoozeSettings.coerceIntervalMinutes(snoozeMinutes),
        )
        NotificationChannelManager.cancelReminderNotification(context, reminderId)
    }

    private fun handleDismiss(context: Context, reminderId: Long) {
        NotificationChannelManager.cancelReminderNotification(context, reminderId)
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_action_reminder_id"
        const val ACTION_COMPLETE = "com.deepak.flow.action.COMPLETE"
        const val ACTION_SNOOZE = "com.deepak.flow.action.SNOOZE"
        const val ACTION_DISMISS = "com.deepak.flow.action.DISMISS"
    }
}
