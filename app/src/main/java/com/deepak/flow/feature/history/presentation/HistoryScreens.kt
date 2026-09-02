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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.core.history.HistoryCompletionDotLevel
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
import com.deepak.flow.app.theme.FlowTextTertiary
import com.deepak.flow.app.theme.FlowWaterFill
import com.deepak.flow.core.history.HistoryGraphPeriod
import com.deepak.flow.core.history.HistorySeriesPoint
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val DAILY_VISIBLE_COLUMNS = 7.5f
private val dailyWeekdayLetterFormatter =
    DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault())

private fun dailyDayNumber(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).dayOfMonth.toString()

private fun dailyWeekdayLetter(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(dailyWeekdayLetterFormatter)

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onDayClick: (Long) -> Unit,
    onExpiredTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FlowShell(
        selected = FlowDrawerDestination.HISTORY,
        userName = userName,
        remindersEnabled = remindersEnabled,
        waterEnabled = waterEnabled,
        gymEnabled = gymEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onGymEnabledChange = onGymEnabledChange,
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
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {
                    HistoryMonthCalendar(
                        yearMonth = uiState.calendar.yearMonth,
                        activityDays = uiState.calendar.activityDays,
                        completionDots = uiState.calendar.completionDots,
                        todayEpochDay = uiState.calendar.todayEpochDay,
                        earliestEpochDay = uiState.calendar.earliestEpochDay,
                        latestEpochDay = uiState.calendar.latestEpochDay,
                        canGoPreviousMonth = uiState.calendar.canGoPreviousMonth,
                        canGoNextMonth = uiState.calendar.canGoNextMonth,
                        onPreviousMonth = viewModel::goToPreviousMonth,
                        onNextMonth = viewModel::goToNextMonth,
                        onDayClick = onDayClick,
                    )
                    if (remindersEnabled && uiState.expiredTasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(FlowSpacing.xl))
                        HistoryExpiredTasksSection(
                            tasks = uiState.expiredTasks,
                            onTaskClick = onExpiredTaskClick,
                        )
                    }
                }
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
private fun HistoryExpiredTasksSection(
    tasks: List<HistoryExpiredTaskItem>,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Expired tasks",
            style = MaterialTheme.typography.titleMedium,
            color = FlowTextPrimary,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        tasks.forEach { task ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTaskClick(task.id) }
                    .padding(vertical = FlowSpacing.sm),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = FlowTextPrimary,
                )
                Text(
                    text = task.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlowTextSecondary,
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
                enabled = state.canGoBack,
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
            valueLabel = ::formatHistoryGraphWaterPoint,
            barColor = FlowWaterFill,
            isDailyScrollable = state.period == HistoryGraphPeriod.DAILY,
        )
        Spacer(
            modifier = Modifier.height(
                if (state.period == HistoryGraphPeriod.DAILY) {
                    FlowSpacing.xxl + FlowSpacing.lg
                } else {
                    FlowSpacing.xl
                },
            ),
        )
        HistoryBarChartCard(
            title = "Tasks",
            totalLabel = formatHistoryGraphTaskTotal(state.points),
            points = state.points,
            valueOf = { it.tasksBarValue() },
            valueLabel = ::formatHistoryGraphTaskPoint,
            barColor = FlowAccent,
            isDailyScrollable = state.period == HistoryGraphPeriod.DAILY,
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
    valueLabel: (HistorySeriesPoint) -> String,
    barColor: Color,
    isDailyScrollable: Boolean = false,
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
        } else if (isDailyScrollable) {
            HistoryDailyScrollableBarChart(
                points = points,
                maxValue = maxValue,
                valueOf = valueOf,
                valueLabel = valueLabel,
                barColor = barColor,
            )
        } else {
            HistoryFixedWidthBarChart(
                points = points,
                maxValue = maxValue,
                valueOf = valueOf,
                valueLabel = valueLabel,
                barColor = barColor,
            )
        }
    }
}

@Composable
private fun HistoryFixedWidthBarChart(
    points: List<HistorySeriesPoint>,
    maxValue: Float,
    valueOf: (HistorySeriesPoint) -> Float,
    valueLabel: (HistorySeriesPoint) -> String,
    barColor: Color,
) {
    var tooltipIndex by remember { mutableStateOf<Int?>(null) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        points.forEachIndexed { index, point ->
            val fraction = (valueOf(point) / maxValue).coerceIn(0f, 1f)
            val showTooltip = tooltipIndex == index
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    if (showTooltip) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 2.dp),
                        ) {
                            Text(
                                text = point.label.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = FlowTextSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                            Text(
                                text = valueLabel(point),
                                style = MaterialTheme.typography.titleSmall,
                                color = FlowTextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                }
                HistoryGraphBarCanvas(
                    fraction = fraction,
                    barColor = barColor,
                    barWidthFraction = 0.91f,
                    gestureKey = index,
                    onTapSelect = {
                        tooltipIndex = if (tooltipIndex == index) null else index
                    },
                    onTapRelease = {},
                )
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = FlowTextTertiary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HistoryDailyScrollableBarChart(
    points: List<HistorySeriesPoint>,
    maxValue: Float,
    valueOf: (HistorySeriesPoint) -> Float,
    valueLabel: (HistorySeriesPoint) -> String,
    barColor: Color,
) {
    val scrollState = rememberScrollState()
    var tooltipIndex by remember { mutableStateOf<Int?>(null) }
    var chartWidthPx by remember { mutableStateOf(0f) }
    var viewportCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val barCanvasCoords = remember { mutableStateMapOf<Int, LayoutCoordinates>() }
    val scrollOffset = scrollState.value

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columnWidth = maxWidth / DAILY_VISIBLE_COLUMNS

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .onGloballyPositioned { coords ->
                    chartWidthPx = coords.size.width.toFloat()
                    viewportCoords = coords
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                points.forEachIndexed { index, point ->
                    val fraction = (valueOf(point) / maxValue).coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier
                            .width(columnWidth)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .onGloballyPositioned { coords ->
                                    barCanvasCoords[index] = coords
                                },
                        ) {
                            HistoryGraphBarCanvas(
                                fraction = fraction,
                                barColor = barColor,
                                barWidthFraction = 0.78f,
                                gestureKey = index,
                                onTapSelect = {
                                    tooltipIndex = if (tooltipIndex == index) null else index
                                },
                                onTapRelease = {},
                            )
                        }
                        Spacer(modifier = Modifier.height(FlowSpacing.xs))
                        Text(
                            text = dailyDayNumber(point.startEpochDay),
                            style = MaterialTheme.typography.labelMedium,
                            color = FlowTextTertiary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                        Text(
                            text = dailyWeekdayLetter(point.startEpochDay),
                            style = MaterialTheme.typography.labelMedium,
                            color = FlowTextTertiary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }

            val selectedIndex = tooltipIndex
            if (selectedIndex != null && selectedIndex in points.indices) {
                val point = points[selectedIndex]
                val canvasCoords = barCanvasCoords[selectedIndex]
                val viewport = viewportCoords
                if (
                    canvasCoords != null &&
                    viewport != null &&
                    canvasCoords.isAttached &&
                    viewport.isAttached &&
                    chartWidthPx > 0f
                ) {
                    val barTopLeft = viewport.localPositionOf(canvasCoords, Offset.Zero)
                    val barCenterX = barTopLeft.x + canvasCoords.size.width / 2f
                    val fraction = (valueOf(point) / maxValue).coerceIn(0f, 1f)
                    val actualBarTopY = barTopLeft.y +
                        canvasCoords.size.height * (1f - fraction)
                    key(selectedIndex, scrollOffset) {
                        HistoryDailyGraphTooltip(
                            weekdayLabel = point.label.uppercase(Locale.getDefault()),
                            valueText = valueLabel(point),
                            barCenterX = barCenterX,
                            barTopY = actualBarTopY,
                            containerWidthPx = chartWidthPx,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryDailyGraphTooltip(
    weekdayLabel: String,
    valueText: String,
    barCenterX: Float,
    barTopY: Float,
    containerWidthPx: Float,
) {
    val density = LocalDensity.current
    val gapPx = with(density) { 16.dp.toPx() }
    val edgePaddingPx = with(density) { 8.dp.toPx() }

    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = weekdayLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = FlowTextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleSmall,
                    color = FlowTextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
            }
        },
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(
            Constraints(
                minWidth = 0,
                maxWidth = Constraints.Infinity,
                minHeight = 0,
                maxHeight = constraints.maxHeight,
            ),
        )
        val offsetX = if (containerWidthPx <= 0f) {
            0
        } else {
            val ideal = barCenterX - placeable.width / 2f
            ideal.coerceIn(
                edgePaddingPx,
                containerWidthPx - placeable.width - edgePaddingPx,
            ).roundToInt()
        }
        val offsetY = (barTopY - placeable.height - gapPx).roundToInt()

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(offsetX, offsetY)
        }
    }
}

@Composable
private fun HistoryGraphBarCanvas(
    fraction: Float,
    barColor: Color,
    barWidthFraction: Float,
    gestureKey: Any,
    onTapSelect: () -> Unit,
    onTapRelease: () -> Unit,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .pointerInput(gestureKey) {
                detectTapGestures(onTap = { onTapSelect() })
            },
    ) {
        val barWidth = size.width * barWidthFraction
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
}

@Composable
private fun HistoryMonthCalendar(
    yearMonth: YearMonth,
    activityDays: Set<Long>,
    completionDots: Map<Long, HistoryCompletionDotLevel>,
    todayEpochDay: Long,
    earliestEpochDay: Long?,
    latestEpochDay: Long?,
    canGoPreviousMonth: Boolean,
    canGoNextMonth: Boolean,
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
                enabled = canGoPreviousMonth,
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
                enabled = canGoNextMonth,
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
                        val isSelectable = epochDay == todayEpochDay ||
                            (
                                (earliestEpochDay == null || epochDay >= earliestEpochDay) &&
                                    (latestEpochDay == null || epochDay <= latestEpochDay) &&
                                    epochDay <= todayEpochDay
                                )
                        HistoryDayCell(
                            dayNumber = dayNumber,
                            isToday = epochDay == todayEpochDay,
                            isFuture = epochDay > todayEpochDay,
                            dotLevel = completionDots[epochDay] ?: HistoryCompletionDotLevel.NONE,
                            enabled = isSelectable,
                            onClick = { if (isSelectable) onDayClick(epochDay) },
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
    isFuture: Boolean,
    dotLevel: HistoryCompletionDotLevel,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isActive = enabled || isToday
    val effectiveDotLevel = if (isFuture || !isActive) {
        HistoryCompletionDotLevel.NONE
    } else {
        dotLevel
    }
    val dotColor = when (effectiveDotLevel) {
        HistoryCompletionDotLevel.NONE -> Color.Transparent
        HistoryCompletionDotLevel.RED -> Color(0xFFE05555)
        HistoryCompletionDotLevel.NEUTRAL -> FlowTextSecondary
        HistoryCompletionDotLevel.YELLOW -> Color(0xFFE6C84A)
        HistoryCompletionDotLevel.BLUE -> Color(0xFF5B8FD4)
        HistoryCompletionDotLevel.GREEN -> Color(0xFF5BB56B)
    }
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .then(
                if (isActive) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
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
                        Modifier.border(1.5.dp, FlowTextSecondary, CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    isToday -> FlowTextPrimary
                    !isActive -> FlowTextTertiary.copy(alpha = 0.4f)
                    else -> FlowTextPrimary
                },
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
    }
}

@Composable
fun HistoryDayScreen(
    viewModel: HistoryDayViewModel,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onBack: () -> Unit,
    onTasksClick: () -> Unit,
    onWaterClick: () -> Unit,
    onGymClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FlowShell(
        selected = FlowDrawerDestination.HISTORY,
        userName = userName,
        remindersEnabled = remindersEnabled,
        waterEnabled = waterEnabled,
        gymEnabled = gymEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onGymEnabledChange = onGymEnabledChange,
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
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        HistoryCategoryRow(
            title = "Gym",
            subtitle = uiState.gymSubtitle,
            onClick = onGymClick,
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
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
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
        gymEnabled = gymEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onGymEnabledChange = onGymEnabledChange,
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
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
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
        gymEnabled = gymEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onGymEnabledChange = onGymEnabledChange,
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
internal fun HistoryDetailShell(
    title: String,
    dateLabel: String,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
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
        gymEnabled = gymEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onGymEnabledChange = onGymEnabledChange,
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
