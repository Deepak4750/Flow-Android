package com.deepak.flow.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.deepak.flow.FlowApplication
import com.deepak.flow.MainActivity
import com.deepak.flow.R
import com.deepak.flow.core.model.DailyProgress
import com.deepak.flow.core.model.UserProfile
import com.deepak.flow.core.model.formatDailyProgressPercent
import com.deepak.flow.core.model.formatWaterLiters
import com.deepak.flow.core.model.remindersFeatureEnabled
import com.deepak.flow.core.model.todayWaterIntakeMl
import com.deepak.flow.core.model.withWaterAdd
import com.deepak.flow.core.notification.NotificationChannelManager
import com.deepak.flow.core.water.FlowBottleStyles
import com.deepak.flow.core.water.renderCachedBottleFrameBitmap
import com.deepak.flow.feature.reminder.presentation.flowTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal object WidgetSnapshotCache {
    @Volatile
    var snapshot: TodayWidgetSnapshot? = null
}

internal data class WaterWidgetSnapshot(
    val ready: Boolean,
    val millilitres: Int = 0,
    val goalMl: Int = 0,
    val styleIndex: Int = 0,
)

internal fun waterWidgetSnapshotFor(
    profile: UserProfile?,
    todayEpochDay: Long,
): WaterWidgetSnapshot {
    val goal = profile?.waterGoalMl
    val style = profile?.waterBottleStyleIndex
    if (profile == null || !profile.waterEnabled || goal == null || style == null) {
        return WaterWidgetSnapshot(ready = false)
    }
    return WaterWidgetSnapshot(
        ready = true,
        millilitres = profile.todayWaterIntakeMl(todayEpochDay),
        goalMl = goal,
        styleIndex = style,
    )
}

private const val WidgetBottleMaxHeightPx = 360
/** Match in-app [com.deepak.flow.app.theme.FlowMotion.REVEAL] baseline. */
private const val WaterFillAnimBaseMs = 220L

/** Disjoint PendingIntent request-code namespaces (avoid Today vs Matrix collisions). */
private const val RequestTodayBase = 1_000
private const val RequestMatrixBase = 2_000
private const val RequestWaterOpenBase = 3_000
private const val RequestWaterAddBase = 4_000
private const val RequestTodayItemBase = 5_000

object FlowWidgets {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private var fullRefreshJob: Job? = null
    private var waterRefreshJob: Job? = null
    @Volatile
    private var displayedWaterProgress: Float = -1f
    @Volatile
    private var displayedWaterStyleIndex: Int = -1

    fun refresh(context: Context) {
        val appContext = context.applicationContext
        fullRefreshJob?.cancel()
        waterRefreshJob?.cancel()
        fullRefreshJob = scope.launch {
            refreshMutex.withLock {
                refreshNow(appContext)
            }
        }
    }

    /** Water-intake-only refresh. Does not touch Tasks or Progress widgets. */
    fun refreshWater(context: Context) {
        val appContext = context.applicationContext
        // A newer full refresh owns the widgets; skip a stale water-only pass.
        if (fullRefreshJob?.isActive == true) return
        waterRefreshJob?.cancel()
        waterRefreshJob = scope.launch {
            refreshMutex.withLock {
                refreshWaterNow(appContext, animateFill = true)
            }
        }
    }

    suspend fun refreshWaterNow(context: Context, animateFill: Boolean = true) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val waterIds = manager.getAppWidgetIds(ComponentName(appContext, WaterWidgetReceiver::class.java))
        if (waterIds.isEmpty()) return
        val waterSnapshot = try {
            loadWaterSnapshot(appContext)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            WaterWidgetSnapshot(ready = false)
        }
        val targetProgress = if (!waterSnapshot.ready || waterSnapshot.goalMl <= 0) {
            0f
        } else {
            (waterSnapshot.millilitres / waterSnapshot.goalMl.toFloat()).coerceAtMost(1f)
        }
        val styleChanged = waterSnapshot.ready &&
            displayedWaterStyleIndex >= 0 &&
            displayedWaterStyleIndex != waterSnapshot.styleIndex
        val fromProgress = when {
            !animateFill || !waterSnapshot.ready || styleChanged -> targetProgress
            displayedWaterProgress < 0f -> targetProgress
            else -> displayedWaterProgress
        }
        if (waterSnapshot.ready) {
            displayedWaterStyleIndex = waterSnapshot.styleIndex
        }

        suspend fun pushFrame(progress: Float) {
            val sharedBottle = if (waterSnapshot.ready) {
                waterBottleBitmap(
                    context = appContext,
                    styleIndex = waterSnapshot.styleIndex,
                    progress = progress,
                )
            } else {
                null
            }
            waterIds.forEach { id ->
                manager.updateAppWidget(
                    id,
                    waterViews(appContext, id, waterSnapshot, sharedBottleBitmap = sharedBottle),
                )
            }
            displayedWaterProgress = if (waterSnapshot.ready) progress else -1f
        }

        val delta = kotlin.math.abs(targetProgress - fromProgress)
        if (!waterSnapshot.ready || delta < 0.005f) {
            pushFrame(targetProgress)
            return
        }
        // Wall-clock ease: finish on time even when a frame is slow to render.
        // Small adds stay snappy; large adds get a touch more duration, not more lag.
        val durationMs = when {
            delta < 0.15f -> 160L
            delta < 0.35f -> WaterFillAnimBaseMs
            else -> 260L
        }
        val startMs = android.os.SystemClock.elapsedRealtime()
        while (true) {
            val elapsed = android.os.SystemClock.elapsedRealtime() - startMs
            val t = (elapsed / durationMs.toFloat()).coerceIn(0f, 1f)
            val eased = t * t * (3f - 2f * t)
            pushFrame(fromProgress + (targetProgress - fromProgress) * eased)
            if (t >= 1f) break
            kotlinx.coroutines.delay(16L)
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
        val todayIds = manager.getAppWidgetIds(ComponentName(appContext, TodayTasksWidgetReceiver::class.java))
        val matrixIds = manager.getAppWidgetIds(ComponentName(appContext, ProgressMatrixWidgetReceiver::class.java))
        todayIds.forEach { id ->
            manager.updateAppWidget(id, todayViews(appContext, id, snapshot))
            manager.notifyAppWidgetViewDataChanged(id, R.id.widget_today_list)
        }
        matrixIds.forEach { id ->
            manager.updateAppWidget(id, matrixViews(appContext, id))
            manager.notifyAppWidgetViewDataChanged(id, R.id.widget_matrix_flipper)
        }
        // Full refresh snaps water fill (no animation) so startup stays quick.
        refreshWaterNow(appContext, animateFill = false)
    }

    suspend fun addWaterMl(context: Context, amountMl: Int): Int {
        if (amountMl <= 0) return 0
        val app = context.applicationContext as FlowApplication
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val before = app.profileRepository.getProfile()?.todayWaterIntakeMl(today) ?: 0
        val update = app.profileRepository.applyWaterIntakeWrite(today) { profile ->
            if (!profile.waterEnabled) return@applyWaterIntakeWrite null
            if (profile.waterGoalMl == null) return@applyWaterIntakeWrite null
            if (profile.waterBottleStyleIndex == null) return@applyWaterIntakeWrite null
            profile.withWaterAdd(amountMl, today)
        } ?: return 0
        NotificationChannelManager.cancelWaterReminderNotification(context)
        refreshWaterNow(context, animateFill = true)
        return update.millilitres - before
    }

    suspend fun toggleTodayCompletion(context: Context, reminderId: Long) {
        val app = context.applicationContext as FlowApplication
        if (!app.profileRepository.getProfile().remindersFeatureEnabled()) return
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
        if (!app.profileRepository.getProfile().remindersFeatureEnabled()) {
            return TodayWidgetSnapshot(
                items = emptyList(),
                extraCount = 0,
                progress = DailyProgress(0, 0),
            )
        }
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

    private suspend fun loadWaterSnapshot(context: Context): WaterWidgetSnapshot {
        val app = context.applicationContext as FlowApplication
        val profile = app.profileRepository.getProfile()
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        return waterWidgetSnapshotFor(profile, today)
    }

    private fun waterViews(
        context: Context,
        appWidgetId: Int,
        snapshot: WaterWidgetSnapshot,
        sharedBottleBitmap: Bitmap? = null,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_water)
        val openApp = openAppIntent(
            context = context,
            requestCode = RequestWaterOpenBase + appWidgetId,
            destination = WidgetLaunch.DEST_WATER,
        )
        views.setOnClickPendingIntent(R.id.widget_water_root, openApp)
        views.setOnClickPendingIntent(R.id.widget_water_heading, openApp)
        views.setOnClickPendingIntent(R.id.widget_water_setup, openApp)
        views.setOnClickPendingIntent(R.id.widget_water_bottle, openApp)
        views.setOnClickPendingIntent(R.id.widget_water_count, openApp)
        if (!snapshot.ready) {
            views.setViewVisibility(R.id.widget_water_setup, View.VISIBLE)
            views.setViewVisibility(R.id.widget_water_count, View.GONE)
            views.setViewVisibility(R.id.widget_water_bottle, View.GONE)
            views.setViewVisibility(R.id.widget_water_actions, View.GONE)
            return views
        }
        views.setViewVisibility(R.id.widget_water_setup, View.GONE)
        views.setViewVisibility(R.id.widget_water_count, View.VISIBLE)
        views.setViewVisibility(R.id.widget_water_bottle, View.VISIBLE)
        views.setViewVisibility(R.id.widget_water_actions, View.VISIBLE)
        views.setTextViewText(
            R.id.widget_water_count,
            context.getString(
                R.string.widget_water_count,
                formatWaterLiters(snapshot.millilitres),
                formatWaterLiters(snapshot.goalMl),
            ),
        )
        val progress = if (snapshot.goalMl <= 0) {
            0f
        } else {
            (snapshot.millilitres / snapshot.goalMl.toFloat()).coerceAtMost(1f)
        }
        views.setImageViewBitmap(
            R.id.widget_water_bottle,
            sharedBottleBitmap ?: waterBottleBitmap(context, snapshot.styleIndex, progress),
        )
        val canAdd = snapshot.millilitres < UserProfile.MAX_WATER_INTAKE_ML
        bindWaterAddButton(
            views = views,
            context = context,
            appWidgetId = appWidgetId,
            viewId = R.id.widget_water_add_250,
            amountMl = 250,
            slot = 0,
            enabled = canAdd,
        )
        bindWaterAddButton(
            views = views,
            context = context,
            appWidgetId = appWidgetId,
            viewId = R.id.widget_water_add_500,
            amountMl = 500,
            slot = 1,
            enabled = canAdd,
        )
        bindWaterAddButton(
            views = views,
            context = context,
            appWidgetId = appWidgetId,
            viewId = R.id.widget_water_add_1l,
            amountMl = 1000,
            slot = 2,
            enabled = canAdd,
        )
        return views
    }

    private fun bindWaterAddButton(
        views: RemoteViews,
        context: Context,
        appWidgetId: Int,
        viewId: Int,
        amountMl: Int,
        slot: Int,
        enabled: Boolean,
    ) {
        views.setTextColor(
            viewId,
            ContextCompat.getColor(
                context,
                if (enabled) R.color.widget_text_primary else R.color.widget_text_disabled,
            ),
        )
        views.setBoolean(viewId, "setEnabled", enabled)
        views.setOnClickPendingIntent(
            viewId,
            if (enabled) {
                addWaterIntent(context, appWidgetId, amountMl = amountMl, slot = slot)
            } else {
                null
            },
        )
    }

    private fun addWaterIntent(
        context: Context,
        appWidgetId: Int,
        amountMl: Int,
        slot: Int,
    ): PendingIntent {
        val intent = Intent(context, WaterWidgetReceiver::class.java).apply {
            action = WaterWidgetReceiver.ACTION_ADD
            putExtra(WaterWidgetReceiver.EXTRA_AMOUNT_ML, amountMl)
        }
        return PendingIntent.getBroadcast(
            context,
            RequestWaterAddBase + appWidgetId * 4 + slot,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun waterBottleBitmap(
        context: Context,
        styleIndex: Int,
        progress: Float,
    ): Bitmap = renderCachedBottleFrameBitmap(
        resources = context.resources,
        bottleRes = FlowBottleStyles.drawableRes(styleIndex),
        cacheKey = "style-$styleIndex",
        maxContentHeightPx = WidgetBottleMaxHeightPx,
        progress = progress,
    )

    private fun todayViews(
        context: Context,
        appWidgetId: Int,
        snapshot: TodayWidgetSnapshot,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_today)
        val openApp = openAppIntent(
            context = context,
            requestCode = RequestTodayBase + appWidgetId,
            destination = WidgetLaunch.DEST_REMINDERS,
        )
        views.setOnClickPendingIntent(R.id.widget_today_heading, openApp)
        views.setOnClickPendingIntent(R.id.widget_today_empty_group, openApp)
        views.setOnClickPendingIntent(R.id.widget_today_count, openApp)

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
            RequestTodayItemBase + appWidgetId,
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
            RequestMatrixBase + appWidgetId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putWidgetDestination(WidgetLaunch.DEST_REMINDERS)
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

    internal fun openAppIntent(
        context: Context,
        requestCode: Int,
        destination: String,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putWidgetDestination(destination)
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
