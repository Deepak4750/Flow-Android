package com.deepak.flow.feature.home.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.deepak.flow.app.components.FlowAccentDot
import com.deepak.flow.app.components.FlowDotMatrixProgress
import com.deepak.flow.app.components.FlowScreenHeading
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.theme.FlowAccent
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.core.model.DailyProgress
import com.deepak.flow.core.model.Reminder
import com.deepak.flow.core.model.formatTodayCount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeNextUpSection(
    reminder: Reminder,
    instant: Instant,
    timeFormatter: DateTimeFormatter,
    zoneId: ZoneId,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoned = instant.atZone(zoneId)
    val dayLabel = formatReminderWhenLabel(zoned.toLocalDate(), zoneId)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlowAccentDot(color = FlowAccent)
            Spacer(modifier = Modifier.width(FlowSpacing.xs))
            FlowScreenHeading("Next up")
        }
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        Text(
            text = reminder.title,
            style = MaterialTheme.typography.headlineMedium,
            color = FlowTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.xxs))
        Text(
            text = "$dayLabel · ${zoned.toLocalTime().format(timeFormatter)}",
            style = MaterialTheme.typography.bodyMedium,
            color = FlowTextSecondary,
        )
    }
}

@Composable
fun HomeTodayProgressSection(
    progress: DailyProgress,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FlowScreenHeading("Today")
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowDotMatrixProgress(progress = progress.ratio)
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        Text(
            text = progress.formatTodayCount(),
            style = MaterialTheme.typography.headlineMedium,
            color = FlowTextPrimary,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.xxs))
        FlowSupportingText("completed")
    }
}

@Composable
fun HomeNextUpEmptyState(modifier: Modifier = Modifier) {
    FlowSupportingText(
        text = "Nothing upcoming.",
        modifier = modifier,
    )
}

fun formatReminderWhenLabel(date: LocalDate, zoneId: ZoneId): String {
    val today = LocalDate.now(zoneId)
    return when {
        date.isEqual(today) -> "Today"
        date.isEqual(today.plusDays(1)) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    }
}
