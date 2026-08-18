package com.deepak.flow.core.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.deepak.flow.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodayTasksWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                FlowWidgets.refreshNow(context)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_ITEM || intent.action == ACTION_TOGGLE) {
            val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
            val itemAction = intent.getStringExtra(EXTRA_ITEM_ACTION)
            if (intent.action == ACTION_TOGGLE || itemAction == ITEM_TOGGLE) {
                if (reminderId < 0L) return
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        FlowWidgets.toggleTodayCompletion(context, reminderId)
                    } finally {
                        pending.finish()
                    }
                }
                return
            }
            val launch = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(launch)
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_TOGGLE = "com.deepak.flow.widget.TOGGLE_TODAY"
        const val ACTION_ITEM = "com.deepak.flow.widget.TODAY_ITEM"
        const val EXTRA_REMINDER_ID = "extra_widget_reminder_id"
        const val EXTRA_ITEM_ACTION = "extra_widget_item_action"
        const val ITEM_TOGGLE = "toggle"
        const val ITEM_OPEN = "open"
    }
}
