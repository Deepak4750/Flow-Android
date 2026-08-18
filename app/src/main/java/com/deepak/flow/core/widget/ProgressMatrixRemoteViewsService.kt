package com.deepak.flow.core.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import com.deepak.flow.R
import com.deepak.flow.core.model.DailyProgress
import kotlinx.coroutines.runBlocking

class ProgressMatrixRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        return ProgressMatrixRemoteViewsFactory(applicationContext, appWidgetId)
    }
}

private class ProgressMatrixRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {
    private var progress: DailyProgress = DailyProgress(0, 0)

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        progress = WidgetSnapshotCache.snapshot?.progress ?: runBlocking {
            FlowWidgets.loadTodaySnapshot(context).also { WidgetSnapshotCache.snapshot = it }
        }.progress
    }

    override fun onDestroy() = Unit

    override fun getCount(): Int = 2

    override fun getViewAt(position: Int): RemoteViews {
        val sizePx = (widgetSquareSizePx(context, appWidgetId) * 2).coerceAtMost(1024)
        val onColor = ContextCompat.getColor(context, R.color.widget_text_primary)
        val offColor = ContextCompat.getColor(context, R.color.widget_dot_off)
        val background = Color.TRANSPARENT
        val percent = matrixPercentLabel(progress)
        val bitmap = if (position == PAGE_PERCENT) {
            renderDotMatrixTextBitmap(
                text = percent,
                sizePx = sizePx,
                onColor = onColor,
                offColor = offColor,
                backgroundColor = background,
            )
        } else {
            renderProgressMatrixBitmap(
                filledCount = matrixFilledCount(progress),
                sizePx = sizePx,
                onColor = onColor,
                offColor = offColor,
                backgroundColor = background,
            )
        }
        val page = RemoteViews(context.packageName, R.layout.widget_matrix_page)
        page.setImageViewBitmap(R.id.widget_matrix_page_image, bitmap)
        page.setContentDescription(
            R.id.widget_matrix_page_image,
            if (position == PAGE_PERCENT) percent else context.getString(R.string.widget_matrix_label),
        )
        page.setOnClickFillInIntent(
            R.id.widget_matrix_page_image,
            Intent().apply {
                data = Uri.parse("flow://widget/matrix/$appWidgetId/$position")
                putExtra(EXTRA_OPEN_APP, true)
            },
        )
        return page
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    private companion object {
        const val PAGE_PERCENT = 1
        const val EXTRA_OPEN_APP = "com.deepak.flow.widget.OPEN_APP"
    }
}
