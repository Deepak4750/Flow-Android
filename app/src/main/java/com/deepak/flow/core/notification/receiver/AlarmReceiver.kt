package com.deepak.flow.core.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.model.remindersFeatureEnabled
import com.deepak.flow.core.model.waterDrinkRemindersOn
import com.deepak.flow.core.notification.NotificationChannelManager
import com.deepak.flow.core.notification.reminderNotificationBody
import com.deepak.flow.core.scheduling.SchedulingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isWater = intent.getBooleanExtra(EXTRA_IS_WATER, false)
        if (isWater) {
            NotificationChannelManager.createChannel(context)
            val pendingResult = goAsync()
            val app = context.applicationContext as FlowApplication
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    handleWaterAlarm(context, app)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val scheduledTimeMillis = intent.getLongExtra(EXTRA_SCHEDULED_TIME, -1L)
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)
        if (reminderId < 0) return
        if (!isSnooze && scheduledTimeMillis < 0) return

        NotificationChannelManager.createChannel(context)
        val pendingResult = goAsync()
        val app = context.applicationContext as FlowApplication

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (isSnooze) {
                    handleSnoozeAlarm(context, app, reminderId)
                } else {
                    handleAlarm(
                        context = context,
                        app = app,
                        reminderId = reminderId,
                        scheduledInstant = Instant.ofEpochMilli(scheduledTimeMillis),
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleSnoozeAlarm(
        context: Context,
        app: FlowApplication,
        reminderId: Long,
    ) {
        val reminder = app.reminderRepository.getReminder(reminderId) ?: return
        if (!reminder.enabled) return
        if (!app.profileRepository.getProfile().remindersFeatureEnabled()) {
            app.notificationScheduler.cancelSnooze(reminderId)
            return
        }

        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val completedToday = app.reminderRepository.observeTodayCompletions(today).first()
        if (reminderId in completedToday) {
            app.notificationScheduler.cancelSnooze(reminderId)
            return
        }

        showNotification(context, app, reminderId, reminder.title, reminder.note)
        app.notificationScheduler.cancelSnooze(reminderId)
    }

    private suspend fun handleAlarm(
        context: Context,
        app: FlowApplication,
        reminderId: Long,
        scheduledInstant: Instant,
    ) {
        val repository = app.reminderRepository
        val scheduler = app.notificationScheduler
        val engine = SchedulingEngine()
        val zoneId = ZoneId.systemDefault()
        val now = Instant.now()

        val reminder = repository.getReminder(reminderId) ?: return
        if (!reminder.enabled) return
        if (!app.profileRepository.getProfile().remindersFeatureEnabled()) {
            scheduler.cancelReminder(reminderId)
            return
        }

        val isValid = engine.isOccurrenceStillValid(
            scheduledInstant = scheduledInstant,
            reminder = reminder,
            currentInstant = now,
            zoneId = zoneId,
        )

        if (isValid) {
            showNotification(context, app, reminderId, reminder.title, reminder.note)
        }

        scheduler.cancelReminder(reminderId)
        scheduler.scheduleNextOccurrence(reminder)
    }

    private suspend fun showNotification(
        context: Context,
        app: FlowApplication,
        reminderId: Long,
        title: String,
        note: String?,
    ) {
        val body = reminderNotificationBody(note)
        val snoozeEnabled = app.profileRepository.getProfile()?.snoozeEnabled == true
        NotificationChannelManager.postReminderNotification(
            context = context,
            reminderId = reminderId,
            title = title,
            body = body,
            snoozeEnabled = snoozeEnabled,
        )
    }

    private suspend fun handleWaterAlarm(
        context: Context,
        app: FlowApplication,
    ) {
        val profile = app.profileRepository.getProfile()
        if (profile?.waterDrinkRemindersOn() != true) {
            app.notificationScheduler.cancelWaterReminder()
            NotificationChannelManager.cancelWaterReminderNotification(context)
            return
        }
        NotificationChannelManager.postWaterReminderNotification(context)
        app.notificationScheduler.syncWaterReminder(profile)
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
        const val EXTRA_IS_WATER = "extra_is_water"
    }
}
