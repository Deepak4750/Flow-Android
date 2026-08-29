package com.deepak.flow.feature.home.presentation

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.app.components.FlowButton
import com.deepak.flow.app.components.FlowButtonVariant
import com.deepak.flow.app.components.FlowHairlineDivider
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.components.FlowTextAction
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowShell
import com.deepak.flow.app.theme.FlowMotion
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.core.model.UserProfile
import com.deepak.flow.core.model.formatWaterLiters
import com.deepak.flow.core.model.waterQuickAddAmountsMl
import com.deepak.flow.core.model.waterQuickAddLabel
import com.deepak.flow.feature.gym.presentation.GymHomeViewModel
import com.deepak.flow.feature.reminder.presentation.flowTimeFormatter
import java.time.ZoneId

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    gymViewModel: GymHomeViewModel,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    waterGoalMl: Int?,
    waterIntakeMl: Int,
    waterCustomQuickAddsMl: List<Int>,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onEditReminder: (Long) -> Unit,
    onAddWaterMl: (Int) -> Unit,
    onOpenWater: () -> Unit,
    onOpenGymRoutine: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gymState by gymViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val zoneId = remember { ZoneId.systemDefault() }
    val timeFormatter = remember(context) { flowTimeFormatter(DateFormat.is24HourFormat(context)) }
    val scrollState = rememberScrollState()

    FlowShell(
        selected = FlowDrawerDestination.HOME,
        userName = uiState.profileName,
        remindersEnabled = remindersEnabled,
        waterEnabled = waterEnabled,
        gymEnabled = gymEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onGymEnabledChange = onGymEnabledChange,
        onDestinationClick = onDestinationClick,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
        ) {
            FlowScreenTitle(uiState.greeting)
            uiState.userLabel?.let { label ->
                Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlowTextSecondary,
                )
            }

            if (remindersEnabled) {
                Spacer(modifier = Modifier.height(FlowSpacing.xl))
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
                } else if (uiState.dailyProgress.hasTasksToday) {
                    HomeTodayProgressSection(progress = uiState.dailyProgress)
                } else {
                    HomeNextUpEmptyState()
                }
            }

            if (waterEnabled) {
                Spacer(modifier = Modifier.height(FlowSpacing.xl))
                HomeWaterSection(
                    goalMl = waterGoalMl,
                    intakeMl = waterIntakeMl,
                    customQuickAddsMl = waterCustomQuickAddsMl,
                    onAddWaterMl = onAddWaterMl,
                    onOpenWater = onOpenWater,
                )
            }

            if (gymEnabled && gymState.routine != null && gymState.currentDay != null) {
                Spacer(modifier = Modifier.height(FlowSpacing.xl))
                HomeGymWorkoutSection(
                    dayHeading = gymState.dayHeading,
                    exercises = gymState.currentDay?.exercises.orEmpty().map { it.name },
                    isRestDay = gymState.isRestDay,
                    onOpenGymRoutine = onOpenGymRoutine,
                )
            }

            Spacer(modifier = Modifier.height(FlowSpacing.lg))
        }
    }
}

@Composable
private fun HomeWaterSection(
    goalMl: Int?,
    intakeMl: Int,
    customQuickAddsMl: List<Int>,
    onAddWaterMl: (Int) -> Unit,
    onOpenWater: () -> Unit,
) {
    FlowSectionLabel("Water")
    Spacer(modifier = Modifier.height(FlowSpacing.sm))

    if (goalMl == null) {
        FlowSupportingText("Set a daily goal to start tracking.")
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowTextAction(text = "Set up in H₂O", onClick = onOpenWater)
        return
    }

    Text(
        text = "${formatWaterLiters(intakeMl)} / ${formatWaterLiters(goalMl)}",
        style = MaterialTheme.typography.headlineMedium,
        color = FlowTextPrimary,
    )
    Spacer(modifier = Modifier.height(FlowSpacing.md))

    val quickAdds = remember(customQuickAddsMl) { waterQuickAddAmountsMl(customQuickAddsMl) }
    val canAdd = intakeMl < UserProfile.MAX_WATER_INTAKE_ML
    quickAdds.chunked(2).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
        ) {
            row.forEach { amount ->
                FlowButton(
                    text = waterQuickAddLabel(amount),
                    onClick = { onAddWaterMl(amount) },
                    enabled = canAdd,
                    variant = FlowButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
    }
}

@Composable
private fun HomeGymWorkoutSection(
    dayHeading: String,
    exercises: List<String>,
    isRestDay: Boolean,
    onOpenGymRoutine: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val canExpand = !isRestDay && exercises.isNotEmpty()

    FlowSectionLabel("Today's gym workout")
    Spacer(modifier = Modifier.height(FlowSpacing.sm))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = dayHeading,
            style = MaterialTheme.typography.titleLarge,
            color = FlowTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onOpenGymRoutine)
                .padding(vertical = FlowSpacing.xs),
        )
        if (canExpand) {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse exercises" else "Expand exercises",
                    tint = FlowTextSecondary,
                )
            }
        }
    }

    AnimatedVisibility(
        visible = expanded && canExpand,
        enter = fadeIn(tween(FlowMotion.STANDARD)) + expandVertically(tween(FlowMotion.STANDARD)),
        exit = fadeOut(tween(FlowMotion.FAST)) + shrinkVertically(tween(FlowMotion.FAST)),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowHairlineDivider()
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            exercises.forEach { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary,
                )
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
            }
        }
    }
}
