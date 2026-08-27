package com.deepak.flow.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FlowPlaceholderScreen(
    selected: FlowDrawerDestination,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowShell(
        selected = selected,
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
        // Intentionally empty - these destinations are placeholders until their features land.
    }
}
