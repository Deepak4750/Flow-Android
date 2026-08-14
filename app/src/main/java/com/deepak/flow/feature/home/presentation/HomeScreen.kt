package com.deepak.flow.feature.home.presentation

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.R
import com.deepak.flow.app.components.FlowChip
import com.deepak.flow.app.components.FlowDialog
import com.deepak.flow.app.components.FlowDotMatrixProgress
import com.deepak.flow.app.components.FlowFab
import com.deepak.flow.app.components.FlowIconAction
import com.deepak.flow.app.components.FlowMetaText
import com.deepak.flow.app.components.FlowReminderCard
import com.deepak.flow.app.components.FlowScreenHeading
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowScreenTopBar
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSwitch
import com.deepak.flow.app.navigation.FlowDrawerContent
import com.deepak.flow.app.navigation.FlowDrawerDestination
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
import com.deepak.flow.core.scheduling.SchedulingEngine
import com.deepak.flow.feature.reminder.presentation.flowTimeFormatter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCreateReminder: () -> Unit,
    onEditReminder: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val zoneId = remember { ZoneId.systemDefault() }
    val timeFormatter = remember(context) { flowTimeFormatter(DateFormat.is24HourFormat(context)) }
    var pendingDelete by remember { mutableStateOf<Reminder?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        drawerContent = {
            FlowDrawerContent(
                selected = FlowDrawerDestination.REMINDERS,
                userName = uiState.profileName,
                onDestinationClick = { destination ->
                    scope.launch { drawerState.close() }
                    when (destination) {
                        FlowDrawerDestination.REMINDERS -> Unit
                        FlowDrawerDestination.SETTINGS -> onOpenSettings()
                        FlowDrawerDestination.ABOUT -> onOpenAbout()
                    }
                },
            )
        },
    ) {
        HomeContent(
            uiState = uiState,
            timeFormatter = timeFormatter,
            zoneId = zoneId,
            onOpenDrawer = { scope.launch { drawerState.open() } },
            onCreateReminder = onCreateReminder,
            onEditReminder = onEditReminder,
            onToggleEnabled = viewModel::toggleReminderEnabled,
            onRequestDelete = { pendingDelete = it },
            onToggleTodayCompletion = viewModel::toggleTodayCompletion,
        )
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    timeFormatter: DateTimeFormatter,
    zoneId: ZoneId,
    onOpenDrawer: () -> Unit,
    onCreateReminder: () -> Unit,
    onEditReminder: (Long) -> Unit,
    onToggleEnabled: (Long, Boolean) -> Unit,
    onRequestDelete: (Reminder) -> Unit,
    onToggleTodayCompletion: (Long, Boolean) -> Unit,
) {
    var categoryFilter by remember { mutableStateOf<Category?>(null) }
    val schedulingEngine = remember { SchedulingEngine() }
    val today = remember(zoneId) { LocalDate.now(zoneId) }
    val filteredReminders = remember(uiState.reminders, categoryFilter) {
        if (categoryFilter == null) {
            uiState.reminders
        } else {
            uiState.reminders.filter { it.category == categoryFilter }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FlowFab(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.content_description_create_reminder),
                onClick = onCreateReminder,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = FlowSpacing.screenHorizontal),
        ) {
            FlowScreenTopBar(
                leading = {
                    FlowIconAction(
                        icon = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.content_description_open_menu),
                        onClick = onOpenDrawer,
                    )
                },
            )
            FlowSectionLabel("Flow")
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            FlowScreenTitle(uiState.greeting)
            uiState.userLabel?.let { label ->
                Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlowTextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(FlowSpacing.lg))

            if (uiState.dailyProgress.hasTasksToday) {
                DailyProgressSection(progress = uiState.dailyProgress.ratio)
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
            }

            val next = uiState.nextReminder
            val nextInstant = uiState.nextReminderInstant
            if (next != null && nextInstant != null) {
                NextUpSection(
                    reminder = next,
                    instant = nextInstant,
                    timeFormatter = timeFormatter,
                    zoneId = zoneId,
                    onClick = { onEditReminder(next.id) },
                )
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
            }

            FlowScreenHeading(
                if (uiState.reminders.isEmpty()) "Reminders" else "All reminders",
            )
            if (uiState.reminders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                CategoryFilterRow(
                    selected = categoryFilter,
                    onSelect = { categoryFilter = it },
                )
            }

            if (uiState.reminders.isEmpty()) {
                EmptyState(modifier = Modifier.weight(1f))
            } else {
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
}

@Composable
private fun DailyProgressSection(progress: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FlowScreenHeading("Today")
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowDotMatrixProgress(progress = progress)
    }
}

@Composable
private fun CategoryFilterRow(
    selected: Category?,
    onSelect: (Category?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xs),
    ) {
        FlowChip(
            label = "All",
            selected = selected == null,
            onClick = { onSelect(null) },
        )
        Category.entries.forEach { category ->
            FlowChip(
                label = category.displayName,
                selected = selected == category,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun NextUpSection(
    reminder: Reminder,
    instant: Instant,
    timeFormatter: DateTimeFormatter,
    zoneId: ZoneId,
    onClick: () -> Unit,
) {
    val zoned = instant.atZone(zoneId)
    val dayLabel = formatWhenLabel(zoned.toLocalDate(), zoneId)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(FlowSizes.accentDot)
                    .clip(CircleShape)
                    .background(FlowAccent),
            )
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
private fun EmptyState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(modifier = Modifier.padding(vertical = FlowSpacing.xxl)) {
            Text(
                text = "Nothing scheduled yet.",
                style = MaterialTheme.typography.titleLarge,
                color = FlowTextPrimary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            Text(
                text = "Tap + to add your first reminder.",
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
            FlowMetaText(
                text = "${reminder.categoryLabel()} · ${scheduleSummary(reminder)}",
                color = metaColor,
            )
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
                    Box(
                        modifier = Modifier
                            .size(FlowSizes.accentDot)
                            .clip(CircleShape)
                            .background(FlowAccent),
                    )
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

private fun formatWhenLabel(date: LocalDate, zoneId: ZoneId): String {
    val today = LocalDate.now(zoneId)
    return when {
        date.isEqual(today) -> "Today"
        date.isEqual(today.plusDays(1)) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    }
}
