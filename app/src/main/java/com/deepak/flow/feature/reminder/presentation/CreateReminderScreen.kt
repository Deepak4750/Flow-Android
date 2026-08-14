package com.deepak.flow.feature.reminder.presentation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.R
import com.deepak.flow.app.components.AnimatedReveal
import com.deepak.flow.app.components.FlowButton
import com.deepak.flow.app.components.FlowChip
import com.deepak.flow.app.components.FlowHairlineDivider
import com.deepak.flow.app.components.FlowOptionSheet
import com.deepak.flow.app.components.FlowScreenHeader
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSelectorRow
import com.deepak.flow.app.components.FlowSwitch
import com.deepak.flow.app.components.FlowTextAction
import com.deepak.flow.app.components.FlowTextField
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.core.model.Category
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderScreen(
    viewModel: CreateReminderViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uses24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    val timeFormatter = remember(uses24Hour) { flowTimeFormatter(uses24Hour) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }
    var showScheduleSheet by remember { mutableStateOf(false) }
    val scheduleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        viewModel.onNotificationPermissionHandled()
        viewModel.saveReminder(onSaved)
    }

    if (showScheduleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showScheduleSheet = false },
            sheetState = scheduleSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            FlowOptionSheet(
                title = "Repeats",
                options = ScheduleType.entries.map { it.displayName },
                selectedIndex = ScheduleType.entries.indexOf(uiState.scheduleType),
                onSelect = { index -> viewModel.updateScheduleType(ScheduleType.entries[index]) },
                onDismiss = { showScheduleSheet = false },
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                FlowSectionLabel("Loading")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FlowSpacing.screenHorizontal),
        ) {
            FlowScreenHeader(
                title = if (uiState.isEditMode) "Edit reminder" else "New reminder",
                onBack = onBack,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.lg))

            FieldHeading(
                label = "Task",
                supporting = "What do you want to remember?",
            )
            FlowTextField(
                value = uiState.task,
                onValueChange = viewModel::updateTask,
                placeholder = stringResource(R.string.placeholder_task),
            )

            Spacer(modifier = Modifier.height(FlowSpacing.xl))
            FlowSectionLabel("Category")
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xs),
            ) {
                Category.entries.forEach { category ->
                    FlowChip(
                        label = category.displayName,
                        selected = uiState.category == category,
                        onClick = { viewModel.updateCategory(category) },
                    )
                }
            }
            AnimatedReveal(visible = uiState.category == Category.CUSTOM) {
                Column {
                    Spacer(modifier = Modifier.height(FlowSpacing.md))
                    FlowTextField(
                        value = uiState.customCategoryName,
                        onValueChange = viewModel::updateCustomCategoryName,
                        placeholder = stringResource(R.string.placeholder_custom_category),
                    )
                }
            }

            SectionBreak()
            FlowSectionLabel("When")
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            ScheduleControlsSection(
                uiState = uiState,
                onScheduleTypeClick = { showScheduleSheet = true },
                onAbsoluteTimeClick = {
                    showTimePicker(
                        context = context,
                        time = uiState.reminderTime,
                        uses24Hour = uses24Hour,
                        onTime = viewModel::updateReminderTime,
                    )
                },
                onIntervalStartNow = viewModel::setIntervalStartNow,
                onIntervalStartCustom = viewModel::setIntervalStartCustom,
                onChooseIntervalStartTime = {
                    showTimePicker(
                        context = context,
                        time = uiState.reminderTime,
                        uses24Hour = uses24Hour,
                        onTime = viewModel::setIntervalCustomStart,
                    )
                },
                onChooseIntervalStartDate = {
                    showDatePicker(
                        context = context,
                        date = uiState.startDate,
                        onDate = viewModel::setIntervalCustomStartDate,
                    )
                },
                onToggleWeekday = viewModel::toggleWeeklyDay,
                onMonthlyDayChange = viewModel::updateMonthlyDayInput,
                onIncrementMonthDay = viewModel::incrementMonthlyDay,
                onDecrementMonthDay = viewModel::decrementMonthlyDay,
                onEveryXDaysInput = viewModel::updateEveryXDaysInput,
                onEveryXDaysInc = viewModel::incrementEveryXDays,
                onEveryXDaysDec = viewModel::decrementEveryXDays,
                onEveryXHoursInput = viewModel::updateEveryXHoursInput,
                onEveryXHoursInc = viewModel::incrementEveryXHours,
                onEveryXHoursDec = viewModel::decrementEveryXHours,
                timeFormatter = timeFormatter,
                dateFormatter = dateFormatter,
            )

            SectionBreak()
            FieldHeading(
                label = "Note",
                supporting = "Shown on the notification",
            )
            FlowTextField(
                value = uiState.note,
                onValueChange = viewModel::updateNote,
                placeholder = stringResource(R.string.placeholder_note),
                minLines = 2,
                singleLine = false,
            )

            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FlowTextAction(
                text = if (uiState.showAdvanced) "Hide advanced" else "Advanced",
                onClick = viewModel::toggleAdvanced,
            )
            AnimatedReveal(visible = uiState.showAdvanced) {
                AdvancedSection(
                    viewModel = viewModel,
                    uiState = uiState,
                    uses24Hour = uses24Hour,
                    timeFormatter = timeFormatter,
                    dateFormatter = dateFormatter,
                )
            }

            Spacer(modifier = Modifier.height(FlowSpacing.xl))
            FlowButton(
                text = if (uiState.isEditMode) "Save changes" else "Create reminder",
                onClick = {
                    if (viewModel.checkNotificationPermission()) {
                        viewModel.saveReminder(onSaved)
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                enabled = uiState.canSave,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xxl))
        }
    }
}

@Composable
private fun AdvancedSection(
    viewModel: CreateReminderViewModel,
    uiState: CreateReminderUiState,
    uses24Hour: Boolean,
    timeFormatter: DateTimeFormatter,
    dateFormatter: DateTimeFormatter,
) {
    val context = LocalContext.current
    Column {
        SectionBreak()

        // Interval schedules set their own anchor date in the When section above.
        if (!uiState.isIntervalSchedule) {
            FieldHeading(
                label = "Start date",
                supporting = "When this reminder becomes active",
            )
            FlowSelectorRow(
                label = "From",
                value = uiState.startDate.format(dateFormatter),
                onClick = {
                    showDatePicker(
                        context = context,
                        date = uiState.startDate,
                        onDate = viewModel::updateStartDate,
                    )
                },
            )
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
        }

        ToggleRow(
            label = "End date",
            supporting = "Stop repeating after a day",
            checked = uiState.endDateEnabled,
            onCheckedChange = viewModel::setEndDateEnabled,
        )
        AnimatedReveal(visible = uiState.endDateEnabled) {
            Column {
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                FlowSelectorRow(
                    label = "Until",
                    value = (uiState.endDate ?: uiState.startDate).format(dateFormatter),
                    onClick = {
                        showDatePicker(
                            context = context,
                            date = uiState.endDate ?: uiState.startDate,
                            onDate = viewModel::updateEndDate,
                        )
                    },
                )
            }
        }

        SectionBreak()
        FieldHeading(
            label = "Why",
            supporting = "Optional — what matters about this for you?",
        )
        FlowTextField(
            value = uiState.reason,
            onValueChange = viewModel::updateReason,
            placeholder = stringResource(R.string.placeholder_why),
            minLines = 2,
            singleLine = false,
        )

        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        ToggleRow(
            label = "Active hours",
            supporting = "Only remind me while I'm awake",
            checked = uiState.activeHoursEnabled,
            onCheckedChange = viewModel::setActiveHoursEnabled,
        )
        AnimatedReveal(visible = uiState.activeHoursEnabled) {
            Column {
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                FlowSelectorRow(
                    label = "From",
                    value = uiState.activeHoursStart.format(timeFormatter),
                    onClick = {
                        showTimePicker(
                            context = context,
                            time = uiState.activeHoursStart,
                            uses24Hour = uses24Hour,
                            onTime = viewModel::updateActiveHoursStart,
                        )
                    },
                )
                FlowHairlineDivider()
                FlowSelectorRow(
                    label = "Until",
                    value = uiState.activeHoursEnd.format(timeFormatter),
                    onClick = {
                        showTimePicker(
                            context = context,
                            time = uiState.activeHoursEnd,
                            uses24Hour = uses24Hour,
                            onTime = viewModel::updateActiveHoursEnd,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun FieldHeading(label: String, supporting: String) {
    FlowSectionLabel(label)
    Spacer(modifier = Modifier.height(FlowSpacing.xxs))
    Text(
        text = supporting,
        style = MaterialTheme.typography.bodyMedium,
        color = FlowTextSecondary,
    )
    Spacer(modifier = Modifier.height(FlowSpacing.xs))
}

@Composable
private fun ToggleRow(
    label: String,
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.padding(end = FlowSpacing.md)) {
            FlowSectionLabel(label)
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = FlowTextSecondary,
            )
        }
        FlowSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SectionBreak() {
    Spacer(modifier = Modifier.height(FlowSpacing.xl))
    FlowHairlineDivider()
    Spacer(modifier = Modifier.height(FlowSpacing.lg))
}

internal fun flowTimeFormatter(uses24Hour: Boolean): DateTimeFormatter =
    DateTimeFormatter.ofPattern(if (uses24Hour) "HH:mm" else "h:mm a")

private fun showTimePicker(
    context: Context,
    time: LocalTime,
    uses24Hour: Boolean,
    onTime: (LocalTime) -> Unit,
) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onTime(LocalTime.of(hour, minute)) },
        time.hour,
        time.minute,
        uses24Hour,
    ).show()
}

private fun showDatePicker(
    context: Context,
    date: LocalDate,
    onDate: (LocalDate) -> Unit,
) {
    DatePickerDialog(
        context,
        { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day)) },
        date.year,
        date.monthValue - 1,
        date.dayOfMonth,
    ).show()
}
