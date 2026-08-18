package com.deepak.flow.core.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.deepak.flow.R
import kotlinx.coroutines.runBlocking

class TodayTasksRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TodayTasksRemoteViewsFactory(applicationContext)
}

private class TodayTasksRemoteViewsFactory(
    private val context: Context,
) : RemoteViewsService.RemoteViewsFactory {
    private var items: List<TodayWidgetItem> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        items = WidgetSnapshotCache.snapshot?.items ?: runBlocking {
            FlowWidgets.loadTodaySnapshot(context).also { WidgetSnapshotCache.snapshot = it }
        }.items
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = items.getOrNull(position) ?: return RemoteViews(context.packageName, R.layout.widget_today_item)
        return todayItemRemoteViews(context, item)
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items.getOrNull(position)?.id ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
