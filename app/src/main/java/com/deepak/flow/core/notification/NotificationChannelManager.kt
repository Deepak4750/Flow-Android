package com.deepak.flow.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.deepak.flow.MainActivity
import com.deepak.flow.R
import com.deepak.flow.core.notification.receiver.NotificationActionReceiver

object NotificationChannelManager {
    const val CHANNEL_ID = "flow_reminders_hold"
    private const val CHANNEL_NAME = "Reminders"

    private const val ACTION_REQUEST_COMPLETE = 1
    private const val ACTION_REQUEST_SNOOZE = 2
    private const val ACTION_REQUEST_DISMISS = 3
    private const val ACTION_REQUEST_RESTORE = 4

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

        val restoreIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_RESTORE
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationActionReceiver.EXTRA_TITLE, title)
            putExtra(NotificationActionReceiver.EXTRA_BODY, body)
            putExtra(NotificationActionReceiver.EXTRA_SNOOZE_ENABLED, snoozeEnabled)
        }
        val restorePending = PendingIntent.getBroadcast(
            context,
            notificationActionRequestCode(reminderId, ACTION_REQUEST_RESTORE),
            restoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentIntent(openAppIntent)
            .setDeleteIntent(restorePending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_complete),
                actionPendingIntent(
                    context = context,
                    reminderId = reminderId,
                    action = NotificationActionReceiver.ACTION_COMPLETE,
                    requestCode = ACTION_REQUEST_COMPLETE,
                ),
            )
        if (!body.isNullOrBlank()) {
            builder.setContentText(body)
        }
        if (snoozeEnabled) {
            builder.addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_snooze),
                actionPendingIntent(
                    context = context,
                    reminderId = reminderId,
                    action = NotificationActionReceiver.ACTION_SNOOZE,
                    requestCode = ACTION_REQUEST_SNOOZE,
                ),
            )
        }
        return builder.addAction(
            R.drawable.ic_notification,
            context.getString(R.string.notification_action_dismiss),
            actionPendingIntent(
                context = context,
                reminderId = reminderId,
                action = NotificationActionReceiver.ACTION_DISMISS,
                requestCode = ACTION_REQUEST_DISMISS,
            ),
        )
    }

    fun cancelReminderNotification(context: Context, reminderId: Long) {
        NotificationCancelGuard.arm(reminderId)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(reminderId.toInt())
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

    private fun openAppRequestCode(reminderId: Long): Int =
        (reminderId * 10 + 9).toInt()

    private fun notificationActionRequestCode(reminderId: Long, actionType: Int): Int =
        (reminderId * 10 + actionType).toInt()
}
