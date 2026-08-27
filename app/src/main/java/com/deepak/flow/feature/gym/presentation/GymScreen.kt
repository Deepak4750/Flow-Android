package com.deepak.flow.feature.gym.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.deepak.flow.app.components.FlowButton
import com.deepak.flow.app.components.FlowButtonVariant
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowShell
import com.deepak.flow.app.theme.FlowSpacing

/**
 * Gym landing. Step 1: first-time / empty state only.
 * No routines are created or loaded here.
 */
@Composable
fun GymScreen(
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onNewRoutine: () -> Unit,
    onFreeWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowShell(
        selected = FlowDrawerDestination.GYM,
        userName = userName,
        remindersEnabled = remindersEnabled,
        waterEnabled = waterEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onDestinationClick = onDestinationClick,
        modifier = modifier,
    ) {
        FlowScreenTitle("Gym")
        Spacer(modifier = Modifier.height(FlowSpacing.xxl))
        FlowSupportingText("What are you doing today?")
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        FlowButton(
            text = "New Routine",
            onClick = onNewRoutine,
            leadingIcon = Icons.Default.Add,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowButton(
            text = "Free Workout",
            onClick = onFreeWorkout,
            variant = FlowButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Temporary destination until Routine Builder / Free Workout are built.
 * No workout data is created.
 */
@Composable
fun GymPlaceholderDestinationScreen(
    title: String,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowShell(
        selected = FlowDrawerDestination.GYM,
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
    }
}
