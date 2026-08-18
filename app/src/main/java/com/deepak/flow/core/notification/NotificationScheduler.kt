package com.deepak.flow.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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

        scheduleExactOrFallback(next.toEpochMilli(), pendingIntent)
    }

    fun cancelReminder(reminderId: Long) {
        cancelSnooze(reminderId)
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

    fun cancelSnooze(reminderId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode(reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun scheduleSnooze(reminderId: Long, snoozeMinutes: Int) {
        cancelSnooze(reminderId)
        val triggerAtMillis = System.currentTimeMillis() + snoozeMinutes * 60_000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmReceiver.EXTRA_IS_SNOOZE, true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode(reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        scheduleExactOrFallback(triggerAtMillis, pendingIntent)
    }

    private fun snoozeRequestCode(reminderId: Long): Int =
        (reminderId + 1_000_000L).toInt()

    fun calculateNextOccurrenceForReminder(reminder: Reminder, referenceInstant: Instant): Instant? {
        return schedulingEngine.calculateNextOccurrence(
            reminder = reminder,
            referenceInstant = referenceInstant,
            zoneId = zoneId,
        )
    }

    // A reminder is only useful if it arrives when it said it would, so exact alarms are
    // the default. Both the pre-check and the catch are needed: on API 31+ the exact APIs
    // throw SecurityException without the permission, and SCHEDULE_EXACT_ALARM can be
    // revoked while this process is alive, so a check that passed earlier is no guarantee.
    // An inexact alarm still fires, so falling back always beats dropping the reminder.
    private fun scheduleExactOrFallback(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
                return
            } catch (_: SecurityException) {
                // Permission revoked between the check and the call.
            }
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )
    }

    // Below API 31 exact alarms need no permission at all. From 31 the manifest's
    // SCHEDULE_EXACT_ALARM is user-revocable, and from 33 USE_EXACT_ALARM makes
    // canScheduleExactAlarms() report true unconditionally.
    private fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
}
