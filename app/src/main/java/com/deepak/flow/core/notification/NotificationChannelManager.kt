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
import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.notification.receiver.NotificationActionReceiver
import com.deepak.flow.core.widget.WidgetLaunch
import com.deepak.flow.core.widget.putWidgetDestination

object NotificationChannelManager {
    const val CHANNEL_ID = "flow_reminder_alerts"
    const val WATER_CHANNEL_ID = "flow_water_alerts"
    const val GYM_CHANNEL_ID = "flow_gym_active"
    const val GYM_EVENTS_CHANNEL_ID = "flow_gym_events"
    private const val CHANNEL_NAME = "Tasks"
    private const val WATER_CHANNEL_NAME = "H₂O"
    private val RetiredChannelIds = listOf(
        "flow_reminders",
        "flow_reminders_hold",
        "flow_reminders_pinned",
    )

    private const val ACTION_REQUEST_COMPLETE = 1
    private const val ACTION_REQUEST_SNOOZE = 2
    private const val ACTION_REQUEST_DISMISS = 3
    private const val ACTION_REQUEST_RESTORE = 4
    private const val WATER_NOTIFICATION_ID = 0x6C6F7702
    private const val WATER_REQUEST_ADD_250 = 0x6C6F7711
    private const val WATER_REQUEST_ADD_500 = 0x6C6F7712
    private const val WATER_REQUEST_BUSY = 0x6C6F7714
    private const val WATER_REQUEST_RESTORE = 0x6C6F7715
    private const val ACTIVE_WORKOUT_REQUEST_OPEN = 0x6C6F7721
    private const val ACTIVE_WORKOUT_REQUEST_RESTORE = 0x6C6F7722
    private const val REST_COMPLETE_NOTIFICATION_ID = 0x6C6F7704
    private const val REST_COMPLETE_REQUEST_OPEN = 0x6C6F7723

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        RetiredChannelIds.forEach(manager::deleteNotificationChannel)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Flow task notifications"
            enableVibration(true)
            setShowBadge(true)
        }
        val waterChannel = NotificationChannel(
            WATER_CHANNEL_ID,
            WATER_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Gentle reminders to drink water"
            enableVibration(true)
            setShowBadge(true)
        }
        val gymChannel = NotificationChannel(
            GYM_CHANNEL_ID,
            context.getString(R.string.notification_channel_gym),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_gym_description)
            enableVibration(false)
            setShowBadge(false)
            setSound(null, null)
        }
        val gymEventsChannel = NotificationChannel(
            GYM_EVENTS_CHANNEL_ID,
            context.getString(R.string.notification_channel_gym_events),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_gym_events_description)
            enableVibration(true)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(waterChannel)
        manager.createNotificationChannel(gymChannel)
        manager.createNotificationChannel(gymEventsChannel)
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

    fun postWaterReminderNotification(context: Context, restartSession: Boolean = true) {
        createChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (restartSession) WaterNotificationSession.reset(context)
        val openAppIntent = PendingIntent.getActivity(
            context,
            WATER_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putWidgetDestination(WidgetLaunch.DEST_WATER)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, WATER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_water_title))
            .setContentText(WaterNotificationCopy.nextBody(context))
            .setContentIntent(openAppIntent)
            .setDeleteIntent(waterRestorePendingIntent(context))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            // Android shows at most three actions when expanded - Dismiss must be one of them.
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_water_add_250),
                waterAddPendingIntent(context, amountMl = 250, requestCode = WATER_REQUEST_ADD_250),
            )
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_water_add_500),
                waterAddPendingIntent(context, amountMl = 500, requestCode = WATER_REQUEST_ADD_500),
            )
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_water_dismiss),
                waterActionPendingIntent(
                    context,
                    WaterNotificationIntents.ACTION_BUSY,
                    WATER_REQUEST_BUSY,
                ),
            )
            .build()
        try {
            manager.notify(WATER_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was denied after the channel was created.
        }
    }

    fun cancelWaterReminderNotification(context: Context) {
        NotificationCancelGuard.arm(WATER_NOTIFICATION_GUARD_ID)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(WATER_NOTIFICATION_ID)
    }

    fun postActiveWorkoutNotification(
        context: Context,
        exerciseName: String?,
        workoutStartedAtEpochMilli: Long,
        exerciseStartedAtEpochMilli: Long?,
        nowEpochMilli: Long = System.currentTimeMillis(),
        destination: String = WidgetLaunch.DEST_GYM_FREE_WORKOUT,
    ) {
        createChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val body = GymLogic.activeWorkoutNotificationBody(
            exerciseName = exerciseName,
            workoutStartedAtEpochMilli = workoutStartedAtEpochMilli,
            exerciseStartedAtEpochMilli = exerciseStartedAtEpochMilli,
            nowEpochMilli = nowEpochMilli,
        )
        val openAppIntent = PendingIntent.getActivity(
            context,
            ACTIVE_WORKOUT_REQUEST_OPEN,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putWidgetDestination(destination)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val contentText = body.lineSequence().firstOrNull().orEmpty()
        val notification = NotificationCompat.Builder(context, GYM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_workout_in_progress))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent)
            .setDeleteIntent(activeWorkoutRestorePendingIntent(context))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .build()
        try {
            manager.notify(ACTIVE_WORKOUT_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was denied after the channel was created.
        }
    }

    fun cancelActiveWorkoutNotification(context: Context) {
        NotificationCancelGuard.arm(ACTIVE_WORKOUT_NOTIFICATION_GUARD_ID)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(ACTIVE_WORKOUT_NOTIFICATION_ID)
    }

    /**
     * One-shot alert when Set Rest reaches zero naturally (not Skip).
     * Separate from the ongoing WORKOUT IN PROGRESS notification.
     */
    fun postRestCompleteNotification(
        context: Context,
        exerciseName: String?,
        destination: String = WidgetLaunch.DEST_GYM_FREE_WORKOUT,
    ) {
        createChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val openAppIntent = PendingIntent.getActivity(
            context,
            REST_COMPLETE_REQUEST_OPEN,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putWidgetDestination(destination)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = buildString {
            if (!exerciseName.isNullOrBlank()) {
                append(exerciseName.trim().uppercase())
                append('\n')
            }
            append(context.getString(R.string.notification_rest_complete_body))
        }
        val notification = NotificationCompat.Builder(context, GYM_EVENTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_rest_complete_title))
            .setContentText(
                if (!exerciseName.isNullOrBlank()) {
                    exerciseName.trim().uppercase()
                } else {
                    context.getString(R.string.notification_rest_complete_body)
                },
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent)
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_rest_complete_action),
                openAppIntent,
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(45_000L)
            .build()
        try {
            manager.notify(REST_COMPLETE_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied.
        }
    }

    fun cancelRestCompleteNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(REST_COMPLETE_NOTIFICATION_ID)
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

    private fun waterAddPendingIntent(
        context: Context,
        amountMl: Int,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = WaterNotificationIntents.ACTION_ADD
            putExtra(WaterNotificationIntents.EXTRA_AMOUNT_ML, amountMl)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun waterActionPendingIntent(
        context: Context,
        action: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun waterRestorePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = WaterNotificationIntents.ACTION_RESTORE
            data = Uri.parse("flow://water/reminder")
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        return PendingIntent.getBroadcast(
            context,
            WATER_REQUEST_RESTORE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun activeWorkoutRestorePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ActiveWorkoutNotificationIntents.ACTION_RESTORE
            data = Uri.parse("flow://gym/active-workout")
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        return PendingIntent.getBroadcast(
            context,
            ACTIVE_WORKOUT_REQUEST_RESTORE,
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
