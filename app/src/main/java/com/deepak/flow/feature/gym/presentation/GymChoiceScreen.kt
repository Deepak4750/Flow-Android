package com.deepak.flow.feature.gym.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.app.components.FeatureOffState
import com.deepak.flow.app.components.FlowButton
import com.deepak.flow.app.components.FlowButtonVariant
import com.deepak.flow.app.components.FlowHairlineDivider
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.components.FlowTextAction
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowShell
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.core.gym.GymWorkoutType

@Composable
fun GymChoiceScreen(
    viewModel: GymHomeViewModel,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onRoutine: () -> Unit,
    onFreeWorkout: () -> Unit,
    onExerciseLibrary: () -> Unit,
    onContinueWorkout: (GymWorkoutType) -> Unit,
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
        modifier = modifier,
    ) {
        FlowScreenTitle("Gym")

        if (!gymEnabled) {
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FeatureOffState(
                title = "Gym is off.",
                message = "Turn it on when you want it back.",
                actionLabel = "Turn Gym back on",
                onTurnBackOn = { onGymEnabledChange(true) },
                modifier = Modifier.weight(1f),
            )
            return@FlowShell
        }

        if (uiState.canContinue) {
            Spacer(modifier = Modifier.height(FlowSpacing.xl))
            ActiveWorkoutBlock(
                title = uiState.activeWorkoutTitle,
                onContinue = {
                    val type = uiState.activeType ?: GymWorkoutType.FREE
                    onContinueWorkout(type)
                },
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xl))
            FlowHairlineDivider()
            Spacer(modifier = Modifier.height(FlowSpacing.xl))
        }

        Spacer(modifier = Modifier.height(if (uiState.canContinue) FlowSpacing.lg else FlowSpacing.xxl))
        FlowSupportingText("What are you doing today?")
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        FlowButton(
            text = "Routine",
            onClick = onRoutine,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowButton(
            text = "Free Workout",
            onClick = onFreeWorkout,
            variant = FlowButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        FlowTextAction(
            text = "Exercise Library",
            onClick = onExerciseLibrary,
        )
    }
}

@Composable
private fun ActiveWorkoutBlock(
    title: String,
    onContinue: () -> Unit,
) {
    FlowSectionLabel("Workout in progress")
    Spacer(modifier = Modifier.height(FlowSpacing.sm))
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = FlowTextPrimary,
    )
    Spacer(modifier = Modifier.height(FlowSpacing.lg))
    FlowButton(
        text = "Continue",
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth(),
    )
}
