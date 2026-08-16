package com.deepak.flow.core.widget

import android.content.Context
import com.deepak.flow.core.model.DailyProgress
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object FlowWidgets {
    fun refresh(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            TodayTasksGlanceWidget().updateAll(appContext)
            ProgressMatrixGlanceWidget().updateAll(appContext)
        }
    }
}

internal const val MatrixColumns = 8
internal const val MatrixRows = 8

internal fun matrixFilledCount(progress: DailyProgress): Int {
    val cells = MatrixColumns * MatrixRows
    return (progress.ratio.coerceIn(0f, 1f) * cells).toInt()
}
