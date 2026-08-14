package com.deepak.flow.feature.settings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.R
import com.deepak.flow.app.components.FlowDialog
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.components.FlowTextAction
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.core.update.AppUpdateStatus
import com.deepak.flow.core.update.AppUpdateViewModel

@Composable
fun AppUpdatePrompt(viewModel: AppUpdateViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(state.status) {
        if (state.status == AppUpdateStatus.NeedsPermission) {
            viewModel.retryInstallAfterPermission()
        }
        onPauseOrDispose { }
    }

    val available = state.available
    if (state.promptVisible && available != null) {
        val downloading = state.status == AppUpdateStatus.Downloading
        val message = when (state.status) {
            AppUpdateStatus.Downloading -> stringResource(R.string.update_downloading)
            AppUpdateStatus.NeedsPermission -> stringResource(R.string.update_needs_permission)
            AppUpdateStatus.Failed -> stringResource(R.string.update_failed)
            else -> stringResource(R.string.update_message, available.versionName)
        }
        FlowDialog(
            title = stringResource(R.string.update_title),
            message = message,
            confirmText = stringResource(R.string.action_install),
            dismissText = stringResource(R.string.action_later),
            confirmEnabled = !downloading,
            onConfirm = viewModel::installAvailableUpdate,
            onDismiss = viewModel::dismissPrompt,
        )
    }
}

@Composable
fun AppUpdateCheckRow(viewModel: AppUpdateViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val busy = state.status == AppUpdateStatus.Checking ||
        state.status == AppUpdateStatus.Downloading
    Column {
        FlowTextAction(
            text = stringResource(R.string.update_check),
            onClick = viewModel::checkNow,
            enabled = !busy,
        )
        val statusText = when (state.status) {
            AppUpdateStatus.Checking -> stringResource(R.string.update_checking)
            AppUpdateStatus.UpToDate -> stringResource(R.string.update_up_to_date)
            AppUpdateStatus.Available -> state.available?.versionName?.let {
                stringResource(R.string.update_available, it)
            }
            AppUpdateStatus.Downloading -> stringResource(R.string.update_downloading)
            AppUpdateStatus.NeedsPermission -> stringResource(R.string.update_needs_permission)
            AppUpdateStatus.Failed -> stringResource(R.string.update_failed)
            AppUpdateStatus.Idle -> null
        }
        if (statusText != null) {
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            FlowSupportingText(statusText)
        }
    }
}
