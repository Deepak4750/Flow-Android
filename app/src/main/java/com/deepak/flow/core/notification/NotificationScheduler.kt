package com.deepak.flow.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.deepak.flow.core.model.Reminder
import com.deepak.flow.core.notification.receiver.AlarmReceiver
import com.deepak.flow.core.scheduling.SchedulingEngine
import java.time.Instant
import java.time.ZoneId

class NotificationScheduler(
    private val context: Context,
    private val schedulingEngine: SchedulingEngine = SchedulingEngine(),
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val zoneId: ZoneId get() = ZoneId.systemDefault()

    fun scheduleNextOccurrence(reminder: Reminder) {
        if (!reminder.enabled) return

        val next = schedulingEngine.calculateNextOccurrence(
            reminder = reminder,
            referenceInstant = Instant.now(),
            zoneId = zoneId,
        ) ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(AlarmReceiver.EXTRA_SCHEDULED_TIME, next.toEpochMilli())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.toEpochMilli(),
            pendingIntent,
        )
    }

    fun cancelReminder(reminderId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun cancelAll() {
        // Individual cancellation happens per reminder; no global alarm list stored.
    }

    fun calculateNextOccurrenceForReminder(reminder: Reminder, referenceInstant: Instant): Instant? {
        return schedulingEngine.calculateNextOccurrence(
            reminder = reminder,
            referenceInstant = referenceInstant,
            zoneId = zoneId,
        )
    }
}
