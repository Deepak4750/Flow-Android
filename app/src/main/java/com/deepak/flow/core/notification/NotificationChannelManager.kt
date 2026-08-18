package com.deepak.flow.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.deepak.flow.MainActivity
import com.deepak.flow.R
import com.deepak.flow.core.notification.receiver.NotificationActionReceiver

object NotificationChannelManager {
    const val CHANNEL_ID = "flow_reminder_alerts"
    private const val CHANNEL_NAME = "Reminders"
    private val RetiredChannelIds = listOf(
        "flow_reminders",
        "flow_reminders_hold",
        "flow_reminders_pinned",
    )

    private const val ACTION_REQUEST_COMPLETE = 1
    private const val ACTION_REQUEST_SNOOZE = 2
    private const val ACTION_REQUEST_DISMISS = 3
    private const val ACTION_REQUEST_RESTORE = 4

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        RetiredChannelIds.forEach(manager::deleteNotificationChannel)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Flow reminder notifications"
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun postReminderNotification(
        context: Context,
        reminderId: Long,
        title: String,
        body: String?,
        snoozeEnabled: Boolean,
    ) {
        createChannel(context)
        val notification = buildReminderNotification(
            context = context,
            reminderId = reminderId,
            title = title,
            body = body,
            snoozeEnabled = snoozeEnabled,
        ).build()
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        try {
            manager.notify(reminderNotificationId(reminderId), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was denied after the channel was created.
        }
    }

    fun cancelReminderNotification(context: Context, reminderId: Long) {
        NotificationCancelGuard.arm(reminderId)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(reminderNotificationId(reminderId))
    }

    fun cancelAllReminderNotifications(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.activeNotifications
            .filter { it.notification.channelId == CHANNEL_ID }
            .forEach { posted ->
                NotificationCancelGuard.arm(posted.id.toLong())
                manager.cancel(posted.id)
            }
    }

    fun buildReminderNotification(
        context: Context,
        reminderId: Long,
        title: String,
        body: String?,
        snoozeEnabled: Boolean,
    ): NotificationCompat.Builder {
        val openAppIntent = PendingIntent.getActivity(
            context,
            openAppRequestCode(reminderId),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentIntent(openAppIntent)
            .setDeleteIntent(restorePendingIntent(context, reminderId, title, body, snoozeEnabled))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_complete),
                actionPendingIntent(
                    context,
                    reminderId,
                    NotificationActionReceiver.ACTION_COMPLETE,
                    ACTION_REQUEST_COMPLETE,
                ),
            )

        if (!body.isNullOrBlank()) builder.setContentText(body)

        if (snoozeEnabled) {
            builder.addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_snooze),
                actionPendingIntent(
                    context,
                    reminderId,
                    NotificationActionReceiver.ACTION_SNOOZE,
                    ACTION_REQUEST_SNOOZE,
                ),
            )
        }

        return builder.addAction(
            R.drawable.ic_notification,
            context.getString(R.string.notification_action_dismiss),
            actionPendingIntent(
                context,
                reminderId,
                NotificationActionReceiver.ACTION_DISMISS,
                ACTION_REQUEST_DISMISS,
            ),
        )
    }

    private fun actionPendingIntent(
        context: Context,
        reminderId: Long,
        action: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationActionRequestCode(reminderId, requestCode),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun restorePendingIntent(
        context: Context,
        reminderId: Long,
        title: String,
        body: String?,
        snoozeEnabled: Boolean,
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_RESTORE
            data = Uri.parse("flow://reminder/$reminderId")
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationActionReceiver.EXTRA_TITLE, title)
            putExtra(NotificationActionReceiver.EXTRA_BODY, body)
            putExtra(NotificationActionReceiver.EXTRA_SNOOZE_ENABLED, snoozeEnabled)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationActionRequestCode(reminderId, ACTION_REQUEST_RESTORE),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppRequestCode(reminderId: Long): Int = (reminderId * 10 + 9).toInt()

    private fun notificationActionRequestCode(reminderId: Long, actionType: Int): Int =
        (reminderId * 10 + actionType).toInt()
}

internal fun reminderNotificationId(reminderId: Long): Int =
    reminderId.toInt().coerceAtLeast(1)
