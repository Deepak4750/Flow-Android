package com.deepak.flow.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.deepak.flow.FlowApplication
import com.deepak.flow.MainActivity
import com.deepak.flow.R
import com.deepak.flow.core.model.DailyProgress
import com.deepak.flow.core.model.formatDailyProgressPercent
import com.deepak.flow.core.notification.NotificationChannelManager
import com.deepak.flow.feature.reminder.presentation.flowTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal object WidgetSnapshotCache {
    @Volatile
    var snapshot: TodayWidgetSnapshot? = null
}

object FlowWidgets {
    fun refresh(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            refreshNow(appContext)
        }
    }

    suspend fun refreshNow(context: Context) {
        val appContext = context.applicationContext
        val snapshot = try {
            loadTodaySnapshot(appContext)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            TodayWidgetSnapshot(
                items = emptyList(),
                extraCount = 0,
                progress = DailyProgress(0, 0),
            )
        }
        WidgetSnapshotCache.snapshot = snapshot
        val manager = AppWidgetManager.getInstance(appContext)
        manager.getAppWidgetIds(ComponentName(appContext, TodayTasksWidgetReceiver::class.java))
            .forEach { id ->
                manager.updateAppWidget(id, todayViews(appContext, id, snapshot))
                manager.notifyAppWidgetViewDataChanged(id, R.id.widget_today_list)
            }
        manager.getAppWidgetIds(ComponentName(appContext, ProgressMatrixWidgetReceiver::class.java))
            .forEach { id ->
                manager.updateAppWidget(id, matrixViews(appContext, id))
                manager.notifyAppWidgetViewDataChanged(id, R.id.widget_matrix_flipper)
            }
    }

    suspend fun toggleTodayCompletion(context: Context, reminderId: Long) {
        val app = context.applicationContext as FlowApplication
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val completed = app.reminderRepository.observeTodayCompletions(today).first()
        val nowCompleted = reminderId !in completed
        app.reminderRepository.setTodayCompletion(reminderId, today, nowCompleted)
        if (nowCompleted) {
            app.notificationScheduler.cancelSnooze(reminderId)
            NotificationChannelManager.cancelReminderNotification(context, reminderId)
        }
        refreshNow(context)
    }

    internal suspend fun loadTodaySnapshot(context: Context): TodayWidgetSnapshot {
        val app = context.applicationContext as FlowApplication
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val reminders = app.reminderRepository.observeReminders().first()
        val completed = app.reminderRepository.observeTodayCompletions(today.toEpochDay()).first()
        return buildTodayWidgetSnapshot(
            reminders = reminders,
            completedIds = completed,
            today = today,
            zoneId = zoneId,
            timeFormatter = flowTimeFormatter(DateFormat.is24HourFormat(context)),
            now = Instant.now(),
        )
    }

    private fun todayViews(
        context: Context,
        appWidgetId: Int,
        snapshot: TodayWidgetSnapshot,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_today)
        val openApp = openAppIntent(context, requestCode = 100 + appWidgetId)
        views.setOnClickPendingIntent(R.id.widget_today_heading, openApp)
        views.setOnClickPendingIntent(R.id.widget_today_empty_group, openApp)

        if (snapshot.progress.hasTasksToday) {
            views.setViewVisibility(R.id.widget_today_count, View.VISIBLE)
            views.setTextViewText(
                R.id.widget_today_count,
                context.getString(
                    R.string.widget_today_count,
                    snapshot.progress.completedTasks,
                    snapshot.progress.totalTasks,
                ),
            )
        } else {
            views.setViewVisibility(R.id.widget_today_count, View.GONE)
        }

        val serviceIntent = Intent(context, TodayTasksRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_today_list, serviceIntent)
        views.setEmptyView(R.id.widget_today_list, R.id.widget_today_empty_group)

        val itemTemplate = PendingIntent.getBroadcast(
            context,
            200 + appWidgetId,
            Intent(context, TodayTasksWidgetReceiver::class.java).apply {
                action = TodayTasksWidgetReceiver.ACTION_ITEM
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        views.setPendingIntentTemplate(R.id.widget_today_list, itemTemplate)
        return views
    }

    private fun matrixViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_matrix)
        val openApp = PendingIntent.getActivity(
            context,
            101 + appWidgetId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val serviceIntent = Intent(context, ProgressMatrixRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_matrix_flipper, serviceIntent)
        views.setPendingIntentTemplate(R.id.widget_matrix_flipper, openApp)
        return views
    }

    internal fun openAppIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

internal fun todayItemRemoteViews(context: Context, item: TodayWidgetItem): RemoteViews {
    val row = RemoteViews(context.packageName, R.layout.widget_today_item)
    row.setTextViewText(R.id.widget_item_title, item.title)
    row.setTextColor(
        R.id.widget_item_title,
        ContextCompat.getColor(
            context,
            if (item.completed) R.color.widget_text_tertiary else R.color.widget_text_primary,
        ),
    )
    if (item.timeLabel.isBlank()) {
        row.setViewVisibility(R.id.widget_item_time, View.GONE)
    } else {
        row.setViewVisibility(R.id.widget_item_time, View.VISIBLE)
        row.setTextViewText(R.id.widget_item_time, item.timeLabel)
        row.setTextColor(R.id.widget_item_time, todayItemTimeColor(context, item))
        row.setContentDescription(
            R.id.widget_item_time,
            if (item.isNext && !item.completed) {
                context.getString(R.string.widget_next_time, item.timeLabel)
            } else {
                item.timeLabel
            },
        )
    }
    row.setImageViewResource(
        R.id.widget_item_check,
        if (item.completed) R.drawable.widget_check_on else R.drawable.widget_check_off,
    )
    row.setContentDescription(
        R.id.widget_item_check,
        context.getString(
            if (item.completed) R.string.widget_marked_complete else R.string.widget_mark_complete,
        ),
    )
    val toggle = Intent().apply {
        putExtra(TodayTasksWidgetReceiver.EXTRA_ITEM_ACTION, TodayTasksWidgetReceiver.ITEM_TOGGLE)
        putExtra(TodayTasksWidgetReceiver.EXTRA_REMINDER_ID, item.id)
    }
    row.setOnClickFillInIntent(R.id.widget_item_check, toggle)
    val open = Intent().apply {
        putExtra(TodayTasksWidgetReceiver.EXTRA_ITEM_ACTION, TodayTasksWidgetReceiver.ITEM_OPEN)
        putExtra(TodayTasksWidgetReceiver.EXTRA_REMINDER_ID, item.id)
    }
    row.setOnClickFillInIntent(R.id.widget_item_root, open)
    return row
}

private fun todayItemTimeColor(context: Context, item: TodayWidgetItem): Int =
    ContextCompat.getColor(
        context,
        when {
            item.completed -> R.color.widget_text_tertiary
            item.isNext -> R.color.widget_accent
            else -> R.color.widget_text_secondary
        },
    )

internal fun widgetSquareSizePx(context: Context, appWidgetId: Int): Int {
    val density = context.resources.displayMetrics.density
    val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
    val dp = listOf(width, height).filter { it > 0 }.minOrNull()
    val px = if (dp != null) {
        (dp * density).toInt()
    } else {
        MatrixReferenceSizePx.toInt()
    }
    return px.coerceIn(120, 512)
}

internal fun matrixFilledCount(progress: DailyProgress): Int =
    (progress.ratio.coerceIn(0f, 1f) * MatrixDotCount).toInt()

internal fun matrixPercentLabel(progress: DailyProgress): String =
    formatDailyProgressPercent(progress.ratio)
