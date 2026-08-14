package com.deepak.flow.feature.reminder.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.deepak.flow.app.components.AnimatedReveal
import com.deepak.flow.app.components.FlowChip
import com.deepak.flow.app.components.FlowHairlineDivider
import com.deepak.flow.app.components.FlowInlinePickerRow
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSelectorRow
import com.deepak.flow.app.components.FlowStepper
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.theme.FlowSpacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleControlsSection(
    uiState: CreateReminderUiState,
    onScheduleTypeClick: () -> Unit,
    onAbsoluteTimeClick: () -> Unit,
    onIntervalStartNow: () -> Unit,
    onIntervalStartCustom: () -> Unit,
    onChooseIntervalStartTime: () -> Unit,
    onChooseIntervalStartDate: () -> Unit,
    onToggleWeekday: (DayOfWeek) -> Unit,
    onMonthlyDayChange: (String) -> Unit,
    onIncrementMonthDay: () -> Unit,
    onDecrementMonthDay: () -> Unit,
    onEveryXDaysInput: (String) -> Unit,
    onEveryXDaysInc: () -> Unit,
    onEveryXDaysDec: () -> Unit,
    onEveryXHoursInput: (String) -> Unit,
    onEveryXHoursInc: () -> Unit,
    onEveryXHoursDec: () -> Unit,
    timeFormatter: DateTimeFormatter,
    dateFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FlowInlinePickerRow(
            label = "Repeats",
            value = uiState.scheduleType.displayName,
            onClick = onScheduleTypeClick,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.sm))

        when (uiState.scheduleType) {
            ScheduleType.DAILY -> {
                FlowSelectorRow(
                    label = "At",
                    value = uiState.reminderTime.format(timeFormatter),
                    onClick = onAbsoluteTimeClick,
                )
            }

            ScheduleType.WEEKLY -> {
                FlowSectionLabel("On")
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(FlowSpacing.xs),
                ) {
                    orderedWeekdays().forEach { day ->
                        FlowChip(
                            label = day.shortLabel(),
                            selected = day in uiState.weeklyDays,
                            onClick = { onToggleWeekday(day) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                FlowSelectorRow(
                    label = "At",
                    value = uiState.reminderTime.format(timeFormatter),
                    onClick = onAbsoluteTimeClick,
                )
            }

            ScheduleType.MONTHLY -> {
                FlowStepper(
                    label = "Day of month",
                    value = uiState.monthlyDay,
                    unitLabel = "of each month",
                    valueDescription = "day of month",
                    onValueChange = onMonthlyDayChange,
                    onIncrement = onIncrementMonthDay,
                    onDecrement = onDecrementMonthDay,
                    min = 1,
                    max = 31,
                )
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                FlowSelectorRow(
                    label = "At",
                    value = uiState.reminderTime.format(timeFormatter),
                    onClick = onAbsoluteTimeClick,
                )
            }

            ScheduleType.EVERY_X_DAYS -> {
                FlowStepper(
                    label = "Repeat every",
                    value = uiState.everyXDays,
                    unitLabel = if (uiState.everyXDays == 1) "day" else "days",
                    valueDescription = "day interval",
                    onValueChange = onEveryXDaysInput,
                    onIncrement = onEveryXDaysInc,
                    onDecrement = onEveryXDaysDec,
                    min = CreateReminderViewModel.INTERVAL_DAYS_MIN,
                    max = CreateReminderViewModel.INTERVAL_DAYS_MAX,
                )
                Spacer(modifier = Modifier.height(FlowSpacing.md))
                IntervalStartControls(
                    uiState = uiState,
                    helperText = intervalDaysHelperText(uiState),
                    timeFormatter = timeFormatter,
                    dateFormatter = dateFormatter,
                    zoneId = zoneId,
                    onSelectNow = onIntervalStartNow,
                    onSelectCustom = onIntervalStartCustom,
                    onChooseStartTime = onChooseIntervalStartTime,
                    onChooseStartDate = onChooseIntervalStartDate,
                )
            }

            ScheduleType.EVERY_X_HOURS -> {
                FlowStepper(
                    label = "Repeat every",
                    value = uiState.everyXHours,
                    unitLabel = if (uiState.everyXHours == 1) "hour" else "hours",
                    valueDescription = "hour interval",
                    onValueChange = onEveryXHoursInput,
                    onIncrement = onEveryXHoursInc,
                    onDecrement = onEveryXHoursDec,
                    min = CreateReminderViewModel.INTERVAL_HOURS_MIN,
                    max = CreateReminderViewModel.INTERVAL_HOURS_MAX,
                )
                Spacer(modifier = Modifier.height(FlowSpacing.md))
                IntervalStartControls(
                    uiState = uiState,
                    helperText = intervalHoursHelperText(uiState),
                    timeFormatter = timeFormatter,
                    dateFormatter = dateFormatter,
                    zoneId = zoneId,
                    onSelectNow = onIntervalStartNow,
                    onSelectCustom = onIntervalStartCustom,
                    onChooseStartTime = onChooseIntervalStartTime,
                    onChooseStartDate = onChooseIntervalStartDate,
                )
            }
        }
    }
}

@Composable
private fun IntervalStartControls(
    uiState: CreateReminderUiState,
    helperText: String,
    timeFormatter: DateTimeFormatter,
    dateFormatter: DateTimeFormatter,
    zoneId: ZoneId,
    onSelectNow: () -> Unit,
    onSelectCustom: () -> Unit,
    onChooseStartTime: () -> Unit,
    onChooseStartDate: () -> Unit,
) {
    FlowSectionLabel("Starts")
    Spacer(modifier = Modifier.height(FlowSpacing.xs))
    Row(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xs)) {
        FlowChip(
            label = "Now",
            selected = uiState.intervalAnchorIsNow,
            onClick = onSelectNow,
        )
        FlowChip(
            label = "Pick a start",
            selected = !uiState.intervalAnchorIsNow,
            onClick = onSelectCustom,
        )
    }
    AnimatedReveal(visible = !uiState.intervalAnchorIsNow) {
        Column {
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowSelectorRow(
                label = "Date",
                value = startDateLabel(uiState.startDate, dateFormatter, zoneId),
                onClick = onChooseStartDate,
            )
            FlowHairlineDivider()
            FlowSelectorRow(
                label = "Time",
                value = uiState.reminderTime.format(timeFormatter),
                onClick = onChooseStartTime,
            )
        }
    }
    FlowSupportingText(
        text = helperText,
        modifier = Modifier.padding(top = FlowSpacing.sm),
    )
}

private fun orderedWeekdays(): List<DayOfWeek> {
    val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    return (0L until 7L).map { firstDay.plus(it) }
}

private fun DayOfWeek.shortLabel(): String =
    getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2)

private fun startDateLabel(
    startDate: LocalDate,
    dateFormatter: DateTimeFormatter,
    zoneId: ZoneId,
): String {
    val today = LocalDate.now(zoneId)
    return when {
        startDate.isEqual(today) -> "Today"
        startDate.isEqual(today.plusDays(1)) -> "Tomorrow"
        else -> startDate.format(dateFormatter)
    }
}

private fun intervalDaysHelperText(uiState: CreateReminderUiState): String {
    val unit = if (uiState.everyXDays == 1) "day" else "${uiState.everyXDays} days"
    return if (uiState.intervalAnchorIsNow) {
        "Counts from the moment you save, then every $unit."
    } else {
        "Counts from the start above, then every $unit."
    }
}

private fun intervalHoursHelperText(uiState: CreateReminderUiState): String {
    val unit = if (uiState.everyXHours == 1) "hour" else "${uiState.everyXHours} hours"
    return if (uiState.intervalAnchorIsNow) {
        "Counts from the moment you save, then every $unit."
    } else {
        "Counts from the start above, then every $unit."
    }
}
