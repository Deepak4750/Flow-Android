package com.deepak.flow.core.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.model.SnoozeSettings
import com.deepak.flow.core.notification.NotificationCancelGuard
import com.deepak.flow.core.notification.NotificationChannelManager
import com.deepak.flow.core.notification.ReminderNotificationActionPlan
import com.deepak.flow.core.notification.ReminderNotificationIntents
import com.deepak.flow.core.notification.WATER_NOTIFICATION_GUARD_ID
import com.deepak.flow.core.notification.WaterNotificationAddAmountsMl
import com.deepak.flow.core.notification.WaterNotificationIntents
import com.deepak.flow.core.notification.WaterNotificationSession
import com.deepak.flow.core.notification.planForReminderNotificationAction
import com.deepak.flow.core.notification.reminderNotificationId
import com.deepak.flow.core.widget.FlowWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WaterNotificationIntents.ACTION_ADD,
            WaterNotificationIntents.ACTION_BUSY,
            WaterNotificationIntents.ACTION_RESTORE,
            -> {
                handleWater(context, intent)
                return
            }
        }

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId < 0) return

        if (intent.action == ACTION_RESTORE) {
            restoreAfterSwipe(context, intent, reminderId)
            return
        }

        val plan = planForReminderNotificationAction(intent.action) ?: return
        val pendingResult = goAsync()
        val app = context.applicationContext as FlowApplication

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                execute(context, app, reminderId, plan)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleWater(context: Context, intent: Intent) {
        if (intent.action == WaterNotificationIntents.ACTION_RESTORE) {
            if (NotificationCancelGuard.consume(WATER_NOTIFICATION_GUARD_ID)) return
            if (WaterNotificationSession.isFilled(context)) return
            NotificationChannelManager.postWaterReminderNotification(
                context,
                restartSession = false,
            )
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    WaterNotificationIntents.ACTION_BUSY -> {
                        NotificationChannelManager.cancelWaterReminderNotification(context)
                    }
                    WaterNotificationIntents.ACTION_ADD -> {
                        val amount = intent.getIntExtra(WaterNotificationIntents.EXTRA_AMOUNT_ML, 0)
                        if (amount !in WaterNotificationAddAmountsMl) return@launch
                        val added = FlowWidgets.addWaterMl(context, amount)
                        if (added <= 0) return@launch
                        WaterNotificationSession.add(context, added)
                        // Logging from the reminder means the nudge is done until the next schedule.
                        NotificationChannelManager.cancelWaterReminderNotification(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun execute(
        context: Context,
        app: FlowApplication,
        reminderId: Long,
        plan: ReminderNotificationActionPlan,
    ) {
        if (plan.markCompleted) {
            val reminder = app.reminderRepository.getReminder(reminderId) ?: return
            val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
            app.reminderRepository.setTodayCompletion(
                reminderId = reminder.id,
                dateEpochDay = today,
                completed = true,
            )
        }
        if (plan.scheduleSnooze) {
            val reminder = app.reminderRepository.getReminder(reminderId) ?: return
            if (!reminder.enabled) return

            val profile = app.profileRepository.getProfile()
            if (profile?.snoozeEnabled != true) return

            app.notificationScheduler.scheduleSnooze(
                reminderId = reminderId,
                snoozeMinutes = SnoozeSettings.coerceIntervalMinutes(profile.snoozeIntervalMinutes),
            )
        }
        if (plan.cancelPendingSnooze) {
            app.notificationScheduler.cancelSnooze(reminderId)
        }
        if (plan.cancelNotification) {
            NotificationChannelManager.cancelReminderNotification(context, reminderId)
        }
    }

    private fun restoreAfterSwipe(context: Context, intent: Intent, reminderId: Long) {
        if (NotificationCancelGuard.consume(reminderId)) return
        val title = intent.getStringExtra(EXTRA_TITLE)?.trim().orEmpty()
        if (title.isEmpty()) return
        val notification = NotificationChannelManager.buildReminderNotification(
            context = context,
            reminderId = reminderId,
            title = title,
            body = intent.getStringExtra(EXTRA_BODY),
            snoozeEnabled = intent.getBooleanExtra(EXTRA_SNOOZE_ENABLED, false),
        ).build()
        try {
            NotificationManagerCompat.from(context)
                .notify(reminderNotificationId(reminderId), notification)
        } catch (_: SecurityException) {
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_action_reminder_id"
        const val EXTRA_TITLE = "extra_action_title"
        const val EXTRA_BODY = "extra_action_body"
        const val EXTRA_SNOOZE_ENABLED = "extra_action_snooze_enabled"
        const val ACTION_COMPLETE = ReminderNotificationIntents.ACTION_COMPLETE
        const val ACTION_SNOOZE = ReminderNotificationIntents.ACTION_SNOOZE
        const val ACTION_DISMISS = ReminderNotificationIntents.ACTION_DISMISS
        const val ACTION_RESTORE = ReminderNotificationIntents.ACTION_RESTORE
    }
}
