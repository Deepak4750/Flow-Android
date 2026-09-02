package com.deepak.flow.feature.gym.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.app.components.FlowButton
import com.deepak.flow.app.components.FlowButtonVariant
import com.deepak.flow.app.components.FlowDialog
import com.deepak.flow.app.components.FlowHairlineDivider
import com.deepak.flow.app.components.FlowMetaText
import com.deepak.flow.app.components.FlowScreenHeading
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.components.FlowTextAction
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowShell
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextTertiary

@Composable
fun GymRoutineScreen(
    viewModel: GymHomeViewModel,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onBack: () -> Unit,
    onOpenRoutines: () -> Unit,
    onNewRoutine: () -> Unit,
    onEditRoutine: (Long) -> Unit,
    onStartRoutine: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FlowShell(
        selected = FlowDrawerDestination.GYM,
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
        FlowScreenTitle("Routine")
        if (!uiState.hasRoutine) {
            Spacer(modifier = Modifier.height(FlowSpacing.xxl))
            FlowSupportingText("No routine yet.")
            Spacer(modifier = Modifier.height(FlowSpacing.xl))
            FlowButton(
                text = "New Routine",
                onClick = onNewRoutine,
                leadingIcon = Icons.Default.Add,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowTextAction(text = "Browse routines", onClick = onOpenRoutines)
            return@FlowShell
        }

        if (uiState.showRoundFourCheckpoint) {
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            RoundFourCheckpointBlock(
                onKeepRoutine = viewModel::dismissRoundFourCheckpoint,
                onChooseAnother = {
                    viewModel.dismissRoundFourCheckpoint()
                    onOpenRoutines()
                },
            )
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FlowHairlineDivider()
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
        }

        if (uiState.routines.size > 1) {
            FlowSectionLabel("Routines")
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            uiState.routines.forEach { item ->
                val selected = item.id == uiState.routine?.id
                Text(
                    text = item.name.ifBlank { "Routine" },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) FlowTextTertiary else FlowTextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !selected, role = Role.Button) {
                            viewModel.selectRoutine(item.id)
                        }
                        .padding(vertical = FlowSpacing.xs),
                )
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
            }
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FlowHairlineDivider()
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
        }

        FlowScreenHeading(uiState.routineTitle)
        Spacer(modifier = Modifier.height(FlowSpacing.xs))
        FlowMetaText(uiState.roundsCompletedLabel)
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        Text(
            text = uiState.dayHeading,
            style = MaterialTheme.typography.headlineMedium,
            color = FlowTextPrimary,
        )

        when {
            uiState.isRestDay -> {
                Spacer(modifier = Modifier.height(FlowSpacing.md))
                FlowSupportingText("Take a break.")
                uiState.nextWorkoutHeading?.let { next ->
                    Spacer(modifier = Modifier.height(FlowSpacing.sm))
                    FlowSupportingText("Next workout: $next")
                }
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
                if (uiState.canConfirmRestDay) {
                    FlowButton(
                        text = "Done for today",
                        onClick = viewModel::confirmRestDay,
                        enabled = !uiState.progressionInFlight,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            uiState.currentDay?.exercises?.isNotEmpty() == true -> {
                val workoutDay = uiState.currentDay
                Spacer(modifier = Modifier.height(FlowSpacing.md))
                workoutDay?.exercises?.forEach { exercise ->
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = FlowTextPrimary,
                    )
                    Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                    FlowMetaText("${exercise.setCount} sets")
                    Spacer(modifier = Modifier.height(FlowSpacing.md))
                }
            }
            else -> {
                Spacer(modifier = Modifier.height(FlowSpacing.md))
                FlowSupportingText("Add an exercise to this day.")
            }
        }

        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        if (uiState.canStartRoutine) {
            FlowButton(
                text = "Start Workout",
                onClick = onStartRoutine,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (uiState.showTodayDecision && !uiState.isRestDay) {
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowButton(
                text = "Skip Day",
                onClick = viewModel::requestSkipDay,
                enabled = !uiState.progressionInFlight,
                variant = FlowButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (uiState.pendingSkipDayConfirm) {
            FlowDialog(
                title = "Skip this day?",
                message = "Move to the next day without logging a workout.",
                confirmText = "Skip Day",
                dismissText = "Cancel",
                onConfirm = viewModel::confirmSkipDay,
                onDismiss = viewModel::dismissSkipDay,
            )
        }

        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowTextAction(
            text = "Edit current routine",
            onClick = {
                uiState.routine?.id?.let(onEditRoutine)
            },
        )
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowTextAction(text = "Routines", onClick = onOpenRoutines)
    }
}

@Composable
private fun RoundFourCheckpointBlock(
    onKeepRoutine: () -> Unit,
    onChooseAnother: () -> Unit,
) {
    FlowSupportingText("You've completed this routine 4 times.")
    Spacer(modifier = Modifier.height(FlowSpacing.md))
    FlowButton(
        text = "Keep Routine",
        onClick = onKeepRoutine,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(FlowSpacing.sm))
    FlowButton(
        text = "Choose Another Routine",
        onClick = onChooseAnother,
        variant = FlowButtonVariant.Secondary,
        modifier = Modifier.fillMaxWidth(),
    )
}
