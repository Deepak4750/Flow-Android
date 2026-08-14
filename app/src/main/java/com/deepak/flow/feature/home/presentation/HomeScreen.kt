package com.deepak.flow.feature.home.presentation

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.R
import com.deepak.flow.app.components.FlowButton
import com.deepak.flow.app.components.FlowButtonVariant
import com.deepak.flow.app.components.FlowDialog
import com.deepak.flow.app.components.FlowFab
import com.deepak.flow.app.components.FlowHairlineDivider
import com.deepak.flow.app.components.FlowIconAction
import com.deepak.flow.app.components.FlowListRow
import com.deepak.flow.app.components.FlowMetaText
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSwitch
import com.deepak.flow.app.navigation.FlowDrawerContent
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.theme.FlowAccent
import com.deepak.flow.app.theme.FlowSizes
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.core.model.Reminder
import com.deepak.flow.core.model.Schedule
import com.deepak.flow.core.model.categoryLabel
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
) {
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
            Spacer(modifier = Modifier.height(FlowSpacing.md))
            // Offset so the glyph optically aligns with the content's left edge
            // rather than the icon button's 48dp touch target.
            FlowIconAction(
                icon = Icons.Default.Menu,
                contentDescription = stringResource(R.string.content_description_open_menu),
                onClick = onOpenDrawer,
                modifier = Modifier.offset(x = -FlowSpacing.sm),
            )
            Spacer(modifier = Modifier.height(FlowSpacing.md))
            FlowSectionLabel("Flow")
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            Text(
                text = uiState.greeting,
                style = MaterialTheme.typography.headlineLarge,
                color = FlowTextPrimary,
            )
            uiState.userLabel?.let { label ->
                Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = FlowTextSecondary,
                )
            }

            val next = uiState.nextReminder
            val nextInstant = uiState.nextReminderInstant
            if (next != null && nextInstant != null) {
                Spacer(modifier = Modifier.height(FlowSpacing.xl))
                NextUpSection(
                    reminder = next,
                    instant = nextInstant,
                    timeFormatter = timeFormatter,
                    zoneId = zoneId,
                    onClick = { onEditReminder(next.id) },
                )
            }

            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FlowSectionLabel(if (uiState.reminders.isEmpty()) "Reminders" else "All reminders")
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowHairlineDivider()

            if (uiState.reminders.isEmpty()) {
                EmptyState(
                    onCreateReminder = onCreateReminder,
                    modifier = Modifier.weight(1f),
                )
            } else {
                ReminderList(
                    reminders = uiState.reminders,
                    nextReminderId = next?.id,
                    timeFormatter = timeFormatter,
                    onToggleEnabled = onToggleEnabled,
                    onRequestDelete = onRequestDelete,
                    onEdit = onEditReminder,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * The one thing the user needs on opening: what happens next. Given the whole
 * palette is grey, the accent dot is what makes this scannable in under a second.
 */
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
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(FlowAccent),
            )
            Spacer(modifier = Modifier.width(FlowSpacing.xs))
            FlowMetaText("Next up")
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
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        FlowHairlineDivider()
    }
}

@Composable
private fun EmptyState(
    onCreateReminder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(
                horizontal = FlowSpacing.lg,
                vertical = FlowSpacing.xxl,
            ),
        ) {
            Text(
                text = "Nothing scheduled yet.",
                style = MaterialTheme.typography.titleLarge,
                color = FlowTextPrimary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            Text(
                text = "Add one thing that matters. Flow keeps the schedule.",
                style = MaterialTheme.typography.bodyMedium,
                color = FlowTextSecondary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FlowButton(
                text = "New reminder",
                onClick = onCreateReminder,
                variant = FlowButtonVariant.Secondary,
                fillWidth = false,
            )
        }
    }
}

@Composable
private fun ReminderList(
    reminders: List<Reminder>,
    nextReminderId: Long?,
    timeFormatter: DateTimeFormatter,
    onToggleEnabled: (Long, Boolean) -> Unit,
    onRequestDelete: (Reminder) -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        items(reminders, key = { it.id }) { reminder ->
            ReminderRow(
                reminder = reminder,
                isNext = reminder.id == nextReminderId,
                timeFormatter = timeFormatter,
                onToggleEnabled = { onToggleEnabled(reminder.id, it) },
                onDelete = { onRequestDelete(reminder) },
                onClick = { onEdit(reminder.id) },
            )
            FlowHairlineDivider()
        }
    }
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    isNext: Boolean,
    timeFormatter: DateTimeFormatter,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    FlowListRow(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (reminder.enabled) 1f else 0.45f),
            ) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = FlowTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                FlowMetaText("${reminder.categoryLabel()} · ${scheduleSummary(reminder)}")
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                Text(
                    text = reminderTimeLabel(reminder, timeFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlowTextSecondary,
                )
                reminder.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FlowTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (isNext && reminder.enabled) {
                    Spacer(modifier = Modifier.height(FlowSpacing.xs))
                    FlowMetaText("Next", color = FlowAccent)
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
