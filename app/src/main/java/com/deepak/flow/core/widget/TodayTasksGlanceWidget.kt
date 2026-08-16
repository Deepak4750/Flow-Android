package com.deepak.flow.core.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.deepak.flow.FlowApplication
import com.deepak.flow.MainActivity
import com.deepak.flow.core.notification.NotificationChannelManager
import com.deepak.flow.feature.reminder.presentation.flowTimeFormatter
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

private val ReminderIdKey = ActionParameters.Key<Long>("flow_widget_reminder_id")

private val Background = Color(0xFF0A0A0A)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFC8C8C8)
private val TextTertiary = Color(0xFFADADAD)
private val Border = Color(0xFF2A2A2A)

class TodayTasksGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as FlowApplication
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val reminders = app.reminderRepository.observeReminders().first()
        val completed = app.reminderRepository.observeTodayCompletions(today.toEpochDay()).first()
        val snapshot = buildTodayWidgetSnapshot(
            reminders = reminders,
            completedIds = completed,
            today = today,
            zoneId = zoneId,
            timeFormatter = flowTimeFormatter(DateFormat.is24HourFormat(context)),
        )
        provideContent {
            TodayTasksContent(snapshot)
        }
    }
}

class TodayTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TodayTasksGlanceWidget()
}

class ToggleTodayCompletionAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val reminderId = parameters[ReminderIdKey] ?: return
        val app = context.applicationContext as FlowApplication
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val completed = app.reminderRepository.observeTodayCompletions(today).first()
        val nowCompleted = reminderId !in completed
        app.reminderRepository.setTodayCompletion(reminderId, today, nowCompleted)
        if (nowCompleted) {
            NotificationChannelManager.cancelReminderNotification(context, reminderId)
        }
        FlowWidgets.refresh(context)
    }
}

@Composable
private fun TodayTasksContent(snapshot: TodayWidgetSnapshot) {
    val openApp = actionStartActivity<MainActivity>()
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Background)
            .cornerRadius(16.dp)
            .padding(14.dp)
            .clickable(openApp),
    ) {
        Text(
            text = "TODAY",
            style = TextStyle(
                color = ColorProvider(TextTertiary),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        if (snapshot.progress.hasTasksToday) {
            Text(
                text = "${snapshot.progress.completedTasks} of ${snapshot.progress.totalTasks}",
                style = TextStyle(
                    color = ColorProvider(TextSecondary),
                    fontSize = 13.sp,
                ),
            )
        }
        Spacer(modifier = GlanceModifier.height(12.dp))
        if (snapshot.items.isEmpty()) {
            Text(
                text = "Nothing scheduled today.",
                style = TextStyle(
                    color = ColorProvider(TextPrimary),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Tap to open Flow.",
                style = TextStyle(
                    color = ColorProvider(TextSecondary),
                    fontSize = 13.sp,
                ),
            )
        } else {
            snapshot.items.forEach { item ->
                TodayTaskRow(item)
                Spacer(modifier = GlanceModifier.height(10.dp))
            }
            if (snapshot.extraCount > 0) {
                Text(
                    text = "+${snapshot.extraCount} more",
                    style = TextStyle(
                        color = ColorProvider(TextTertiary),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
            }
        }
    }
}

@Composable
private fun TodayTaskRow(item: TodayWidgetItem) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(22.dp)
                .cornerRadius(11.dp)
                .background(if (item.completed) TextPrimary else Border)
                .clickable(
                    actionRunCallback<ToggleTodayCompletionAction>(
                        actionParametersOf(ReminderIdKey to item.id),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (item.completed) {
                Box(
                    modifier = GlanceModifier
                        .size(8.dp)
                        .cornerRadius(4.dp)
                        .background(Background),
                ) {}
            }
        }
        Spacer(modifier = GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = item.title,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(if (item.completed) TextTertiary else TextPrimary),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            if (item.timeLabel.isNotBlank()) {
                Text(
                    text = item.timeLabel,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(TextSecondary),
                        fontSize = 12.sp,
                    ),
                )
            }
        }
    }
}
