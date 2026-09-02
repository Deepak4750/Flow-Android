package com.deepak.flow.feature.settings.presentation

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.deepak.flow.BuildConfig
import com.deepak.flow.R
import com.deepak.flow.app.components.AnimatedReveal
import com.deepak.flow.app.components.FeatureTurnOffDialog
import com.deepak.flow.app.components.FlowChip
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
import com.deepak.flow.app.theme.FlowError
import com.deepak.flow.app.theme.FlowTextTertiary
import com.deepak.flow.core.gym.GymLimits
import com.deepak.flow.core.gym.GymRestSettingsField
import com.deepak.flow.core.gym.WeightUnit
import com.deepak.flow.core.model.SnoozeSettings
import com.deepak.flow.core.update.AppUpdateViewModel
import com.deepak.flow.core.update.formatInstalledVersionLabel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    updateViewModel: AppUpdateViewModel,
    onBack: () -> Unit,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var pendingDisableLabel by remember { mutableStateOf<String?>(null) }

    // Re-read on resume so returning from system settings shows the new state.
    LifecycleResumeEffect(Unit) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }

    pendingDisableLabel?.let { label ->
        FeatureTurnOffDialog(
            featureLabel = label,
            onConfirm = {
                when (label) {
                    "Tasks" -> onRemindersEnabledChange(false)
                    "H₂O" -> onWaterEnabledChange(false)
                    "Gym" -> onGymEnabledChange(false)
                }
                pendingDisableLabel = null
            },
            onDismiss = { pendingDisableLabel = null },
        )
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

    val leaveSettings: () -> Unit = { viewModel.tryLeaveSettings(onBack) }

    BackHandler { leaveSettings() }

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
                onBack = leaveSettings,
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
                label = stringResource(R.string.settings_section_features),
                supporting = stringResource(R.string.settings_features_supporting),
            )
            FlowToggleRow(
                label = stringResource(R.string.settings_label_tasks),
                checked = remindersEnabled,
                onCheckedChange = { checked ->
                    if (checked) onRemindersEnabledChange(true) else {
                        pendingDisableLabel = "Tasks"
                    }
                },
            )
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowToggleRow(
                label = stringResource(R.string.settings_label_water),
                checked = waterEnabled,
                onCheckedChange = { checked ->
                    if (checked) onWaterEnabledChange(true) else {
                        pendingDisableLabel = "H₂O"
                    }
                },
            )
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowToggleRow(
                label = stringResource(R.string.settings_label_gym_feature),
                checked = gymEnabled,
                onCheckedChange = { checked ->
                    if (checked) onGymEnabledChange(true) else {
                        pendingDisableLabel = "Gym"
                    }
                },
            )

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
                label = stringResource(R.string.settings_section_gym),
                supporting = stringResource(R.string.settings_gym_supporting),
            )
            FlowSectionLabel(stringResource(R.string.settings_label_weight_unit))
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xs),
            ) {
                FlowChip(
                    label = stringResource(R.string.settings_weight_unit_kg),
                    selected = uiState.gymWeightUnit == WeightUnit.KG,
                    onClick = { viewModel.setGymWeightUnit(WeightUnit.KG) },
                    modifier = Modifier.weight(1f),
                )
                FlowChip(
                    label = stringResource(R.string.settings_weight_unit_lb),
                    selected = uiState.gymWeightUnit == WeightUnit.LB,
                    onClick = { viewModel.setGymWeightUnit(WeightUnit.LB) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowStepper(
                label = stringResource(R.string.settings_label_set_rest),
                value = uiState.gymSetRestSeconds,
                unitLabel = stringResource(R.string.settings_unit_seconds),
                valueDescription = stringResource(R.string.settings_set_rest_value_description),
                onValueChange = viewModel::onGymSetRestInput,
                onIncrement = viewModel::incrementGymSetRest,
                onDecrement = viewModel::decrementGymSetRest,
                min = GymLimits.SET_REST_MIN_SECONDS,
                max = GymLimits.SET_REST_MAX_SECONDS,
                deferMinClampWhileEditing = true,
                rejectBelowMinCommit = true,
                onEditingTextChange = viewModel::onGymSetRestEditingChange,
                onCommitRejected = viewModel::onGymSetRestCommitRejected,
            )

            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowStepper(
                label = stringResource(R.string.settings_label_exercise_rest),
                value = uiState.gymExerciseRestSeconds,
                unitLabel = stringResource(R.string.settings_unit_seconds),
                valueDescription = stringResource(R.string.settings_exercise_rest_value_description),
                onValueChange = viewModel::onGymExerciseRestInput,
                onIncrement = viewModel::incrementGymExerciseRest,
                onDecrement = viewModel::decrementGymExerciseRest,
                min = GymLimits.EXERCISE_REST_MIN_SECONDS,
                max = GymLimits.EXERCISE_REST_MAX_SECONDS,
                deferMinClampWhileEditing = true,
                rejectBelowMinCommit = true,
                onEditingTextChange = viewModel::onGymExerciseRestEditingChange,
                onCommitRejected = viewModel::onGymExerciseRestCommitRejected,
            )
            uiState.gymRestBlockField?.let { field ->
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                Text(
                    text = when (field) {
                        GymRestSettingsField.SET_REST -> stringResource(R.string.settings_rest_invalid_set)
                        GymRestSettingsField.EXERCISE_REST -> stringResource(
                            R.string.settings_rest_invalid_exercise,
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FlowError,
                )
            }

            FlowSectionBreak()
            FlowFieldHeading(
                label = stringResource(R.string.settings_section_data),
                supporting = stringResource(R.string.settings_data_supporting),
            )
            FlowToggleRow(
                label = stringResource(R.string.settings_label_keep_data),
                checked = uiState.keepDataOnUninstall,
                onCheckedChange = viewModel::setKeepDataOnUninstall,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            Text(
                text = stringResource(R.string.settings_keep_data_reason),
                style = MaterialTheme.typography.bodySmall,
                color = FlowTextTertiary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.md))
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
            Spacer(modifier = Modifier.height(FlowSpacing.md))
            FlowInfoRow(
                label = stringResource(R.string.about_label_version),
                value = formatInstalledVersionLabel(
                    versionName = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE,
                    betaIteration = BuildConfig.FLOW_BETA_ITERATION,
                ),
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            AppUpdateCheckRow(updateViewModel)
            Spacer(modifier = Modifier.height(FlowSpacing.xxl))
        }
    }
}

