package com.deepak.flow.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FlowPlaceholderScreen(
    selected: FlowDrawerDestination,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowShell(
        selected = selected,
        userName = userName,
        remindersEnabled = remindersEnabled,
        waterEnabled = waterEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onDestinationClick = onDestinationClick,
        modifier = modifier,
    ) {
        // Intentionally empty - these destinations are placeholders until their features land.
    }
}
