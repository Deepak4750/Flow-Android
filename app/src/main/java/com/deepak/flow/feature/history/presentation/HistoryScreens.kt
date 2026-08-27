package com.deepak.flow.feature.history.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.app.components.FlowChip
import com.deepak.flow.app.components.FlowIconAction
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowShell
import com.deepak.flow.app.theme.FlowAccent
import com.deepak.flow.app.theme.FlowBorder
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.app.theme.FlowTextTertiary
import com.deepak.flow.app.theme.FlowWaterFill
import com.deepak.flow.core.history.HistoryGraphPeriod
import com.deepak.flow.core.history.HistorySeriesPoint
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onDayClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FlowShell(
        selected = FlowDrawerDestination.HISTORY,
        userName = userName,
        remindersEnabled = remindersEnabled,
        waterEnabled = waterEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onDestinationClick = onDestinationClick,
        modifier = modifier,
    ) {
        FlowScreenTitle("History")
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        HistoryModeRow(
            mode = uiState.mode,
            onModeChange = viewModel::setMode,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        when (uiState.mode) {
            HistoryMainMode.CALENDAR -> {
                HistoryMonthCalendar(
                    yearMonth = uiState.calendar.yearMonth,
                    activityDays = uiState.calendar.activityDays,
                    todayEpochDay = uiState.calendar.todayEpochDay,
                    onPreviousMonth = viewModel::goToPreviousMonth,
                    onNextMonth = viewModel::goToNextMonth,
                    onDayClick = onDayClick,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                )
            }
            HistoryMainMode.GRAPHS -> {
                HistoryGraphsPane(
                    state = uiState.graphs,
                    onPeriodChange = viewModel::setGraphPeriod,
                    onPrevious = viewModel::goToPreviousGraphWindow,
                    onNext = viewModel::goToNextGraphWindow,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
private fun HistoryModeRow(
    mode: HistoryMainMode,
    onModeChange: (HistoryMainMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xs),
    ) {
        FlowChip(
            label = "Calendar",
            selected = mode == HistoryMainMode.CALENDAR,
            onClick = { onModeChange(HistoryMainMode.CALENDAR) },
        )
        FlowChip(
            label = "Graphs",
            selected = mode == HistoryMainMode.GRAPHS,
            onClick = { onModeChange(HistoryMainMode.GRAPHS) },
        )
    }
}

@Composable
private fun HistoryGraphsPane(
    state: HistoryGraphUiState,
    onPeriodChange: (HistoryGraphPeriod) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xs),
        ) {
            FlowChip(
                label = "Daily",
                selected = state.period == HistoryGraphPeriod.DAILY,
                onClick = { onPeriodChange(HistoryGraphPeriod.DAILY) },
            )
            FlowChip(
                label = "Weekly",
                selected = state.period == HistoryGraphPeriod.WEEKLY,
                onClick = { onPeriodChange(HistoryGraphPeriod.WEEKLY) },
            )
            FlowChip(
                label = "Monthly",
                selected = state.period == HistoryGraphPeriod.MONTHLY,
                onClick = { onPeriodChange(HistoryGraphPeriod.MONTHLY) },
            )
        }
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowIconAction(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous period",
                onClick = onPrevious,
            )
            Text(
                text = state.windowTitle,
                style = MaterialTheme.typography.titleSmall,
                color = FlowTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            FlowIconAction(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next period",
                onClick = onNext,
                enabled = state.canGoForward,
            )
        }
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        HistoryBarChartCard(
            title = "Water",
            totalLabel = formatHistoryGraphWaterTotal(state.points),
            points = state.points,
            valueOf = { it.waterBarValue() },
            barColor = FlowWaterFill,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        HistoryBarChartCard(
            title = "Tasks",
            totalLabel = formatHistoryGraphTaskTotal(state.points),
            points = state.points,
            valueOf = { it.tasksBarValue() },
            barColor = FlowAccent,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
    }
}

@Composable
private fun HistoryBarChartCard(
    title: String,
    totalLabel: String,
    points: List<HistorySeriesPoint>,
    valueOf: (HistorySeriesPoint) -> Float,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val maxValue = points.maxOfOrNull(valueOf)?.coerceAtLeast(1f) ?: 1f
    Column(modifier = modifier.fillMaxWidth()) {
        FlowSectionLabel(title)
        Spacer(modifier = Modifier.height(FlowSpacing.xxs))
        Text(
            text = totalLabel,
            style = MaterialTheme.typography.titleMedium,
            color = FlowTextPrimary,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        if (points.all { valueOf(it) <= 0f }) {
            FlowSupportingText("Nothing logged in this window.")
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                points.forEach { point ->
                    val fraction = (valueOf(point) / maxValue).coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        ) {
                            val barWidth = size.width * 0.62f
                            val barHeight = size.height * fraction
                            val left = (size.width - barWidth) / 2f
                            val top = size.height - barHeight
                            drawRoundRect(
                                color = if (fraction > 0f) barColor else FlowBorder,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight.coerceAtLeast(2.dp.toPx())),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                            )
                        }
                        Spacer(modifier = Modifier.height(FlowSpacing.xs))
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = FlowTextTertiary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMonthCalendar(
    yearMonth: YearMonth,
    activityDays: Set<Long>,
    todayEpochDay: Long,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthLabel = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    val title = "$monthLabel ${yearMonth.year}"
    val firstOfMonth = yearMonth.atDay(1)
    val leadingEmpty = firstOfMonth.dayOfWeek.value - 1
    val daysInMonth = yearMonth.lengthOfMonth()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowIconAction(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous month",
                onClick = onPreviousMonth,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = FlowTextPrimary,
            )
            FlowIconAction(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next month",
                onClick = onNextMonth,
            )
        }
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = FlowTextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        val totalCells = leadingEmpty + daysInMonth
        val rows = (totalCells + 6) / 7
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val cellIndex = row * 7 + column
                    val dayNumber = cellIndex - leadingEmpty + 1
                    if (dayNumber in 1..daysInMonth) {
                        val epochDay = yearMonth.atDay(dayNumber).toEpochDay()
                        HistoryDayCell(
                            dayNumber = dayNumber,
                            isToday = epochDay == todayEpochDay,
                            hasActivity = epochDay in activityDays,
                            onClick = { onDayClick(epochDay) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryDayCell(
    dayNumber: Int,
    isToday: Boolean,
    hasActivity: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(FlowSpacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(
                    if (isToday) {
                        Modifier.border(1.dp, FlowAccent, CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isToday) FlowAccent else FlowTextPrimary,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (hasActivity) FlowTextSecondary else Color.Transparent),
        )
    }
}

@Composable
fun HistoryDayScreen(
    viewModel: HistoryDayViewModel,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onBack: () -> Unit,
    onTasksClick: () -> Unit,
    onWaterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FlowShell(
        selected = FlowDrawerDestination.HISTORY,
        userName = userName,
        remindersEnabled = remindersEnabled,
        waterEnabled = waterEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onDestinationClick = onDestinationClick,
        onBack = onBack,
        modifier = modifier,
    ) {
        FlowScreenTitle(uiState.dateLabel)
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        HistoryCategoryRow(
            title = "Tasks",
            subtitle = uiState.tasksSubtitle,
            onClick = onTasksClick,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        HistoryCategoryRow(
            title = "Water",
            subtitle = uiState.waterSubtitle,
            onClick = onWaterClick,
        )
    }
}

@Composable
private fun HistoryCategoryRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = FlowSpacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = FlowTextPrimary,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.xxs))
        FlowSupportingText(subtitle)
    }
}

@Composable
fun HistoryTasksDetailScreen(
    viewModel: HistoryTasksViewModel,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryDetailShell(
        title = "Tasks",
        dateLabel = uiState.dateLabel,
        userName = userName,
        remindersEnabled = remindersEnabled,
        waterEnabled = waterEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onDestinationClick = onDestinationClick,
        onBack = onBack,
        modifier = modifier,
    ) {
        if (uiState.completions.isEmpty()) {
            FlowSupportingText("Nothing completed.")
        } else {
            uiState.completions.forEach { item ->
                HistoryDetailRow(title = item.title, detail = item.timeLabel)
                Spacer(modifier = Modifier.height(FlowSpacing.md))
            }
        }
    }
}

@Composable
fun HistoryWaterDetailScreen(
    viewModel: HistoryWaterViewModel,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryDetailShell(
        title = "Water",
        dateLabel = uiState.dateLabel,
        userName = userName,
        remindersEnabled = remindersEnabled,
        waterEnabled = waterEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onDestinationClick = onDestinationClick,
        onBack = onBack,
        modifier = modifier,
    ) {
        if (!uiState.hasIntake) {
            FlowSupportingText("No water logged.")
        } else {
            Text(
                text = uiState.totalLabel,
                style = MaterialTheme.typography.headlineMedium,
                color = FlowTextPrimary,
            )
            uiState.goalLabel?.let { goal ->
                Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                FlowSupportingText(goal)
            }
            if (uiState.addAmounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
                FlowSectionLabel("Adds")
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                uiState.addAmounts.forEach { amount ->
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.bodyLarge,
                        color = FlowTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(FlowSpacing.xs))
                }
            }
        }
    }
}

@Composable
private fun HistoryDetailShell(
    title: String,
    dateLabel: String,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlowShell(
        selected = FlowDrawerDestination.HISTORY,
        userName = userName,
        remindersEnabled = remindersEnabled,
        waterEnabled = waterEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onDestinationClick = onDestinationClick,
        onBack = onBack,
        modifier = modifier,
    ) {
        FlowScreenTitle(title)
        Spacer(modifier = Modifier.height(FlowSpacing.xxs))
        FlowSupportingText(dateLabel)
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            content()
        }
    }
}

@Composable
private fun HistoryDetailRow(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = FlowTextPrimary,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.xxs))
        FlowSupportingText(detail)
    }
}
