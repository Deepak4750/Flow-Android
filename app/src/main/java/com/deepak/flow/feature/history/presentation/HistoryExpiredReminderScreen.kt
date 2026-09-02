package com.deepak.flow.feature.history.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.app.components.FlowButton
import com.deepak.flow.app.components.FlowMetaText
import com.deepak.flow.app.components.FlowScreenHeader
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowShell
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary

@Composable
fun HistoryExpiredReminderScreen(
    viewModel: HistoryExpiredReminderViewModel,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onBack: () -> Unit,
    onUseAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FlowShell(
        selected = FlowDrawerDestination.HISTORY,
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
        FlowScreenHeader(title = "Expired task", onBack = onBack)
        when {
            uiState.isLoading -> Unit
            uiState.notFound -> {
                FlowSupportingText("This task is no longer available.")
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = FlowTextPrimary,
                    )
                    Spacer(modifier = Modifier.height(FlowSpacing.xs))
                    FlowMetaText("${uiState.categoryLabel} · ${uiState.scheduleLabel}")
                    Spacer(modifier = Modifier.height(FlowSpacing.sm))
                    FlowSupportingText("At ${uiState.timeLabel}")
                    Spacer(modifier = Modifier.height(FlowSpacing.xs))
                    FlowSupportingText(uiState.expirationLabel)
                    uiState.reason?.let { reason ->
                        Spacer(modifier = Modifier.height(FlowSpacing.lg))
                        Text(
                            text = "Why",
                            style = MaterialTheme.typography.labelLarge,
                            color = FlowTextSecondary,
                        )
                        Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyLarge,
                            color = FlowTextPrimary,
                        )
                    }
                    uiState.note?.let { note ->
                        Spacer(modifier = Modifier.height(FlowSpacing.lg))
                        Text(
                            text = "Note",
                            style = MaterialTheme.typography.labelLarge,
                            color = FlowTextSecondary,
                        )
                        Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodyLarge,
                            color = FlowTextPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.height(FlowSpacing.xl))
                    FlowButton(
                        text = "Use again",
                        onClick = onUseAgain,
                    )
                }
            }
        }
    }
}
