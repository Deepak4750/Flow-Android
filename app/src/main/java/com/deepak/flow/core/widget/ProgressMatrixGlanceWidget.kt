package com.deepak.flow.core.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.deepak.flow.FlowApplication
import com.deepak.flow.MainActivity
import com.deepak.flow.core.model.DailyProgress
import com.deepak.flow.core.model.formatDailyProgressPercent
import com.deepak.flow.core.model.isDotMatrixCellFilled
import com.deepak.flow.core.scheduling.SchedulingEngine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

private val Background = Color(0xFF0A0A0A)
private val TextPrimary = Color(0xFFFFFFFF)
private val DotOn = Color(0xFFFFFFFF)
private val DotOff = Color(0xFF2A2A2A)

class ProgressMatrixGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as FlowApplication
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val engine = SchedulingEngine()
        val reminders = app.reminderRepository.observeReminders().first()
        val completed = app.reminderRepository.observeTodayCompletions(today.toEpochDay()).first()
        val scheduled = reminders.filter { it.enabled && engine.isScheduledOnDate(it, today, zoneId) }
        val progress = DailyProgress(
            totalTasks = scheduled.size,
            completedTasks = scheduled.count { it.id in completed },
        )
        provideContent {
            ProgressMatrixContent(progress)
        }
    }
}

class ProgressMatrixWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ProgressMatrixGlanceWidget()
}

@Composable
private fun ProgressMatrixContent(progress: DailyProgress) {
    val filledCount = matrixFilledCount(progress)
    val openApp = actionStartActivity<MainActivity>()
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Background)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(openApp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            repeat(MatrixRows) { row ->
                Row {
                    repeat(MatrixColumns) { col ->
                        val index = row * MatrixColumns + col
                        val filled = isDotMatrixCellFilled(
                            index = index,
                            filledCount = filledCount,
                            columns = MatrixColumns,
                            rows = MatrixRows,
                        )
                        Box(
                            modifier = GlanceModifier
                                .size(6.dp)
                                .cornerRadius(3.dp)
                                .background(if (filled) DotOn else DotOff),
                        ) {}
                        if (col < MatrixColumns - 1) {
                            Spacer(modifier = GlanceModifier.width(2.dp))
                        }
                    }
                }
                if (row < MatrixRows - 1) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                }
            }
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = formatDailyProgressPercent(progress.ratio),
            style = TextStyle(
                color = ColorProvider(TextPrimary),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
