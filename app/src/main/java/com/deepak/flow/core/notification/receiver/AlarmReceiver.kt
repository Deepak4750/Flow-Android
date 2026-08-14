package com.deepak.flow.core.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.notification.NotificationChannelManager
import com.deepak.flow.core.scheduling.SchedulingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val scheduledTimeMillis = intent.getLongExtra(EXTRA_SCHEDULED_TIME, -1L)
        if (reminderId < 0 || scheduledTimeMillis < 0) return

        val pendingResult = goAsync()
        val app = context.applicationContext as FlowApplication

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handleAlarm(
                    context = context,
                    app = app,
                    reminderId = reminderId,
                    scheduledInstant = Instant.ofEpochMilli(scheduledTimeMillis),
                )
            } finally {
                pendingResult.finish()
            }
        }
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

        val isValid = engine.isOccurrenceStillValid(
            scheduledInstant = scheduledInstant,
            reminder = reminder,
            currentInstant = now,
            zoneId = zoneId,
        )

        if (isValid) {
            val body = reminder.note?.takeIf { it.isNotBlank() } ?: "Time to show up."
            val notification = NotificationChannelManager
                .buildNotification(context, reminder.title, body)
                .build()

            NotificationManagerCompat.from(context).notify(reminderId.toInt(), notification)
        }

        scheduler.cancelReminder(reminderId)
        scheduler.scheduleNextOccurrence(reminder)
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    }
}
