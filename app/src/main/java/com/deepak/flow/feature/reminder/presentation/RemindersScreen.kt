package com.deepak.flow.feature.reminder.presentation

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.R
import com.deepak.flow.app.components.FlowAccentDot
import com.deepak.flow.app.components.FlowChip
import com.deepak.flow.app.components.FlowDialog
import com.deepak.flow.app.components.FlowDotMatrixProgress
import com.deepak.flow.app.components.FlowFab
import com.deepak.flow.app.components.FlowIconAction
import com.deepak.flow.app.components.FlowMetaText
import com.deepak.flow.app.components.FlowReminderCard
import com.deepak.flow.app.components.FlowScreenHeading
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowSwitch
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowShell
import com.deepak.flow.app.theme.CategoryAccent
import com.deepak.flow.app.theme.FlowAccent
import com.deepak.flow.app.theme.FlowSizes
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextDisabled
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.app.theme.FlowTextTertiary
import com.deepak.flow.core.model.Category
import com.deepak.flow.core.model.Reminder
import com.deepak.flow.core.model.Schedule
import com.deepak.flow.core.model.categoryLabel
import com.deepak.flow.core.model.savedCustomCategories
import com.deepak.flow.core.scheduling.SchedulingEngine
import com.deepak.flow.feature.home.presentation.HomeNextUpSection
import com.deepak.flow.feature.home.presentation.HomeTodayProgressSection
import com.deepak.flow.feature.home.presentation.HomeUiState
import com.deepak.flow.feature.home.presentation.HomeViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RemindersScreen(
    viewModel: HomeViewModel,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onCreateReminder: () -> Unit,
    onEditReminder: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val zoneId = remember { ZoneId.systemDefault() }
    val timeFormatter = remember(context) { flowTimeFormatter(DateFormat.is24HourFormat(context)) }
    var pendingDelete by remember { mutableStateOf<Reminder?>(null) }

    pendingDelete?.let { reminder ->
        FlowDialog(
            title = stringResource(R.string.reminder_delete_title),
            message = stringResource(R.string.reminder_delete_message, reminder.title),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_keep),
            destructive = true,
            onConfirm = {
                viewModel.deleteReminder(reminder.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    FlowShell(
        selected = FlowDrawerDestination.REMINDERS,
        userName = uiState.profileName,
        remindersEnabled = remindersEnabled,
        waterEnabled = waterEnabled,
        gymEnabled = gymEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onGymEnabledChange = onGymEnabledChange,
        onDestinationClick = onDestinationClick,
        modifier = modifier,
        floatingActionButton = {
            if (remindersEnabled) {
                FlowFab(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(R.string.content_description_create_reminder),
                    onClick = onCreateReminder,
                )
            }
        },
    ) {
        RemindersContent(
            uiState = uiState,
            remindersEnabled = remindersEnabled,
            timeFormatter = timeFormatter,
            zoneId = zoneId,
            onEditReminder = onEditReminder,
            onToggleEnabled = viewModel::toggleReminderEnabled,
            onRequestDelete = { pendingDelete = it },
            onToggleTodayCompletion = viewModel::toggleTodayCompletion,
        )
    }
}

@Composable
private fun ColumnScope.RemindersContent(
    uiState: HomeUiState,
    remindersEnabled: Boolean,
    timeFormatter: DateTimeFormatter,
    zoneId: ZoneId,
    onEditReminder: (Long) -> Unit,
    onToggleEnabled: (Long, Boolean) -> Unit,
    onRequestDelete: (Reminder) -> Unit,
    onToggleTodayCompletion: (Long, Boolean) -> Unit,
) {
    var categoryFilter by remember { mutableStateOf<RemindersCategoryFilter>(RemindersCategoryFilter.All) }
    val schedulingEngine = remember { SchedulingEngine() }
    val today = remember(zoneId) { LocalDate.now(zoneId) }
    val savedCustomCategories = remember(uiState.reminders) {
        uiState.reminders.savedCustomCategories()
    }
    val customCategoryNames = remember(savedCustomCategories) {
        savedCustomCategories.map { it.name }
    }
    val customAccentByName = remember(savedCustomCategories) {
        savedCustomCategories.associate { it.name to it.accentColorIndex }
    }
    val filteredReminders = remember(uiState.reminders, categoryFilter) {
        when (val filter = categoryFilter) {
            RemindersCategoryFilter.All -> uiState.reminders
            is RemindersCategoryFilter.BuiltIn -> uiState.reminders.filter { it.category == filter.category }
            is RemindersCategoryFilter.Named -> uiState.reminders.filter {
                it.category == Category.CUSTOM && it.categoryLabel() == filter.name
            }
        }
    }

    FlowScreenTitle("Tasks")
    Spacer(modifier = Modifier.height(FlowSpacing.lg))

    if (!remindersEnabled) {
        FeatureOffState(
            title = "Tasks are off.",
            message = "Turn it on from the menu when you want them back.",
            modifier = Modifier.weight(1f),
        )
    } else {
    if (uiState.dailyProgress.hasTasksToday) {
        HomeTodayProgressSection(progress = uiState.dailyProgress)
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
    }

    val next = uiState.nextReminder
    val nextInstant = uiState.nextReminderInstant
    if (next != null && nextInstant != null) {
        HomeNextUpSection(
            reminder = next,
            instant = nextInstant,
            timeFormatter = timeFormatter,
            zoneId = zoneId,
            onClick = { onEditReminder(next.id) },
        )
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
    }

    if (uiState.reminders.isEmpty()) {
        EmptyState(modifier = Modifier.weight(1f))
    } else {
        FlowScreenHeading("All tasks")
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        CategoryFilterRow(
            selected = categoryFilter,
            customNames = customCategoryNames,
            customAccentByName = customAccentByName,
            onSelect = { categoryFilter = it },
        )
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        ReminderList(
            reminders = filteredReminders,
            nextReminderId = next?.id,
            timeFormatter = timeFormatter,
            completedTodayIds = uiState.completedTodayIds,
            schedulingEngine = schedulingEngine,
            today = today,
            zoneId = zoneId,
            onToggleEnabled = onToggleEnabled,
            onRequestDelete = onRequestDelete,
            onEdit = onEditReminder,
            onToggleTodayCompletion = onToggleTodayCompletion,
            modifier = Modifier.weight(1f),
        )
    }
    }
}

@Composable
private fun CategoryFilterRow(
    selected: RemindersCategoryFilter,
    customNames: List<String>,
    customAccentByName: Map<String, Int?>,
    onSelect: (RemindersCategoryFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xs),
    ) {
        FlowChip(
            label = "All",
            selected = selected is RemindersCategoryFilter.All,
            onClick = { onSelect(RemindersCategoryFilter.All) },
        )
        Category.entries
            .filter { it != Category.CUSTOM }
            .forEach { category ->
                FlowChip(
                    label = category.displayName,
                    selected = selected is RemindersCategoryFilter.BuiltIn && selected.category == category,
                    onClick = { onSelect(RemindersCategoryFilter.BuiltIn(category)) },
                    accent = CategoryAccent.forCategory(category),
                )
            }
        customNames.forEach { name ->
            FlowChip(
                label = name,
                selected = selected is RemindersCategoryFilter.Named && selected.name == name,
                onClick = { onSelect(RemindersCategoryFilter.Named(name)) },
                accent = CategoryAccent.forCategory(
                    category = Category.CUSTOM,
                    customName = name,
                    paletteIndex = customAccentByName[name],
                ),
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column {
            Text(
                text = "Nothing scheduled yet.",
                style = MaterialTheme.typography.titleLarge,
                color = FlowTextPrimary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            Text(
                text = "Tap + to add your first task.",
                style = MaterialTheme.typography.bodyMedium,
                color = FlowTextSecondary,
            )
        }
    }
}

@Composable
private fun FeatureOffState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = FlowTextPrimary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = FlowTextSecondary,
            )
        }
    }
}

@Composable
private fun ReminderList(
    reminders: List<Reminder>,
    nextReminderId: Long?,
    timeFormatter: DateTimeFormatter,
    completedTodayIds: Set<Long>,
    schedulingEngine: SchedulingEngine,
    today: LocalDate,
    zoneId: ZoneId,
    onToggleEnabled: (Long, Boolean) -> Unit,
    onRequestDelete: (Reminder) -> Unit,
    onEdit: (Long) -> Unit,
    onToggleTodayCompletion: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = FlowSizes.fabClearance),
        verticalArrangement = Arrangement.spacedBy(FlowSpacing.xs),
    ) {
        items(reminders, key = { it.id }) { reminder ->
            val scheduledToday = schedulingEngine.isScheduledOnDate(reminder, today, zoneId)
            val completedToday = reminder.id in completedTodayIds
            FlowReminderCard(modifier = Modifier.clickable { onEdit(reminder.id) }) {
                ReminderRowContent(
                    reminder = reminder,
                    isNext = reminder.id == nextReminderId,
                    timeFormatter = timeFormatter,
                    scheduledToday = scheduledToday,
                    completedToday = completedToday,
                    onToggleEnabled = { onToggleEnabled(reminder.id, it) },
                    onDelete = { onRequestDelete(reminder) },
                    onToggleTodayCompletion = { onToggleTodayCompletion(reminder.id, it) },
                )
            }
        }
    }
}

@Composable
private fun ReminderRowContent(
    reminder: Reminder,
    isNext: Boolean,
    timeFormatter: DateTimeFormatter,
    scheduledToday: Boolean,
    completedToday: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onToggleTodayCompletion: (Boolean) -> Unit,
) {
    val titleColor = if (reminder.enabled) FlowTextPrimary else FlowTextDisabled
    val supportingColor = if (reminder.enabled) FlowTextSecondary else FlowTextDisabled
    val metaColor = if (reminder.enabled) FlowTextTertiary else FlowTextDisabled

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        if (scheduledToday) {
            FlowIconAction(
                icon = if (completedToday) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (completedToday) {
                    "Mark ${reminder.title} incomplete"
                } else {
                    "Mark ${reminder.title} done for today"
                },
                onClick = { onToggleTodayCompletion(!completedToday) },
                iconSize = FlowSizes.iconMd,
            )
            Spacer(modifier = Modifier.width(FlowSpacing.xs))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.titleLarge,
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlowAccentDot(
                    color = CategoryAccent.forCategory(
                        category = reminder.category,
                        customName = reminder.customCategoryName,
                        paletteIndex = reminder.accentColorIndex,
                    ),
                )
                Spacer(modifier = Modifier.width(FlowSpacing.xxs))
                FlowMetaText(
                    text = "${reminder.categoryLabel()} · ${scheduleSummary(reminder)}",
                    color = metaColor,
                )
            }
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            Text(
                text = reminderTimeLabel(reminder, timeFormatter),
                style = MaterialTheme.typography.bodyMedium,
                color = supportingColor,
            )
            reminder.note?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportingColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isNext && reminder.enabled) {
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlowAccentDot(color = FlowAccent)
                    Spacer(modifier = Modifier.width(FlowSpacing.xxs))
                    FlowMetaText("Next", color = FlowAccent)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            FlowSwitch(
                checked = reminder.enabled,
                onCheckedChange = onToggleEnabled,
            )
            FlowIconAction(
                icon = Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(
                    R.string.content_description_delete_reminder,
                    reminder.title,
                ),
                onClick = onDelete,
                iconSize = FlowSizes.iconSm,
            )
        }
    }
}

private fun scheduleSummary(reminder: Reminder): String = when (val schedule = reminder.schedule) {
    Schedule.Daily -> "Daily"
    is Schedule.Weekly -> "Weekly"
    is Schedule.Monthly -> "Monthly"
    is Schedule.EveryXDays -> if (schedule.intervalDays == 1) "Daily" else "Every ${schedule.intervalDays} days"
    is Schedule.EveryXHours -> if (schedule.intervalHours == 1) "Hourly" else "Every ${schedule.intervalHours} hours"
}

private fun reminderTimeLabel(reminder: Reminder, formatter: DateTimeFormatter): String {
    val times = reminder.reminderTimes.joinToString(", ") { it.format(formatter) }
    return when (reminder.schedule) {
        is Schedule.EveryXHours -> if (times.isBlank()) "" else "From $times"
        is Schedule.EveryXDays -> if (times.isBlank()) "" else "At $times"
        else -> times
    }
}

private sealed interface RemindersCategoryFilter {
    data object All : RemindersCategoryFilter
    data class BuiltIn(val category: Category) : RemindersCategoryFilter
    data class Named(val name: String) : RemindersCategoryFilter
}
