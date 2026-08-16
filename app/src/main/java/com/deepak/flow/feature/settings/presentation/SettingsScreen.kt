package com.deepak.flow.feature.settings.presentation

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.R
import com.deepak.flow.app.components.AnimatedReveal
import com.deepak.flow.app.components.FlowButton
import com.deepak.flow.app.components.FlowDialog
import com.deepak.flow.app.components.FlowFieldHeading
import com.deepak.flow.app.components.FlowInfoRow
import com.deepak.flow.app.components.FlowScreenHeader
import com.deepak.flow.app.components.FlowSectionBreak
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSelectorRow
import com.deepak.flow.app.components.FlowStepper
import com.deepak.flow.app.components.FlowTextAction
import com.deepak.flow.app.components.FlowTextField
import com.deepak.flow.app.components.FlowToggleRow
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.core.model.SnoozeSettings
import com.deepak.flow.core.update.AppUpdateViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    updateViewModel: AppUpdateViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    // Re-read on resume so returning from system settings shows the new state.
    LifecycleResumeEffect(Unit) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }

    if (confirmDeleteAll) {
        FlowDialog(
            title = stringResource(R.string.settings_delete_all_title),
            message = stringResource(R.string.settings_delete_all_message, uiState.reminderCount),
            confirmText = stringResource(R.string.action_delete_all),
            dismissText = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = {
                viewModel.deleteAllReminders()
                confirmDeleteAll = false
            },
            onDismiss = { confirmDeleteAll = false },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FlowSpacing.screenHorizontal),
        ) {
            FlowScreenHeader(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xl))

            FlowFieldHeading(
                label = stringResource(R.string.settings_section_profile),
                supporting = stringResource(R.string.settings_profile_supporting),
            )
            FlowSectionLabel(stringResource(R.string.settings_label_name))
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowTextField(
                value = uiState.displayName,
                onValueChange = viewModel::updateDisplayName,
                placeholder = stringResource(R.string.placeholder_display_name),
            )
            Spacer(modifier = Modifier.height(FlowSpacing.md))
            FlowSectionLabel(stringResource(R.string.settings_label_nickname))
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowTextField(
                value = uiState.nickname,
                onValueChange = viewModel::updateNickname,
                placeholder = stringResource(R.string.placeholder_nickname),
            )
            AnimatedReveal(visible = uiState.hasUnsavedChanges) {
                Column {
                    Spacer(modifier = Modifier.height(FlowSpacing.md))
                    FlowButton(
                        text = stringResource(R.string.action_save_profile),
                        onClick = viewModel::saveProfile,
                        enabled = !uiState.isSaving,
                        fillWidth = false,
                    )
                }
            }

            FlowSectionBreak()
            FlowFieldHeading(
                label = stringResource(R.string.settings_section_notifications),
                supporting = stringResource(R.string.settings_notifications_supporting),
            )
            FlowSelectorRow(
                label = stringResource(R.string.settings_label_notifications),
                value = if (notificationsEnabled) {
                    stringResource(R.string.settings_value_allowed)
                } else {
                    stringResource(R.string.settings_value_blocked)
                },
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    context.startActivity(intent)
                },
            )
            Spacer(modifier = Modifier.height(FlowSpacing.md))
            FlowToggleRow(
                label = stringResource(R.string.settings_label_snooze_enable),
                supporting = stringResource(R.string.settings_snooze_supporting),
                checked = uiState.snoozeEnabled,
                onCheckedChange = viewModel::setSnoozeEnabled,
            )
            AnimatedReveal(visible = uiState.snoozeEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(FlowSpacing.sm))
                    FlowStepper(
                        label = stringResource(R.string.settings_label_snooze),
                        value = uiState.snoozeIntervalMinutes,
                        unitLabel = stringResource(R.string.settings_unit_minutes),
                        valueDescription = stringResource(R.string.settings_snooze_value_description),
                        onValueChange = viewModel::onSnoozeIntervalInput,
                        onIncrement = viewModel::incrementSnoozeInterval,
                        onDecrement = viewModel::decrementSnoozeInterval,
                        min = SnoozeSettings.MIN_INTERVAL_MINUTES,
                        max = SnoozeSettings.MAX_INTERVAL_MINUTES,
                    )
                }
            }

            FlowSectionBreak()
            FlowFieldHeading(
                label = stringResource(R.string.settings_section_data),
                supporting = stringResource(R.string.settings_data_supporting),
            )
            FlowInfoRow(
                label = stringResource(R.string.settings_label_reminders),
                value = uiState.reminderCount.toString(),
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            FlowTextAction(
                text = stringResource(R.string.action_delete_all),
                onClick = { confirmDeleteAll = true },
                enabled = uiState.reminderCount > 0,
                destructive = true,
            )

            FlowSectionBreak()
            FlowFieldHeading(
                label = stringResource(R.string.settings_section_app),
                supporting = stringResource(R.string.settings_app_supporting),
            )
            AppUpdateCheckRow(updateViewModel)
            Spacer(modifier = Modifier.height(FlowSpacing.xxl))
        }
    }
}

