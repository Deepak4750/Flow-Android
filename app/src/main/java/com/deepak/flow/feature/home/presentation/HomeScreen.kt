package com.deepak.flow.feature.home.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowShell
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.core.model.DailyProgress
import com.deepak.flow.core.model.formatTodayCount

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            TodayProgress(progress = uiState.dailyProgress)
        }
    }
}

@Composable
private fun TodayProgress(progress: DailyProgress) {
    FlowSectionLabel("Today")
    Spacer(modifier = Modifier.height(FlowSpacing.sm))
    if (progress.hasTasksToday) {
        Text(
            text = progress.formatTodayCount(),
            style = MaterialTheme.typography.headlineMedium,
            color = FlowTextPrimary,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.xxs))
        FlowSupportingText("completed")
    } else {
        FlowSupportingText("Nothing scheduled.")
    }
}
