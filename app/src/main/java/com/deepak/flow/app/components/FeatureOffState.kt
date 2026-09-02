package com.deepak.flow.app.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary

@Composable
fun FeatureOffState(
    title: String,
    message: String,
    actionLabel: String,
    onTurnBackOn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = FlowTextPrimary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = FlowTextSecondary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FlowButton(
                text = actionLabel,
                onClick = onTurnBackOn,
                fillWidth = false,
            )
        }
    }
}

@Composable
fun FeatureTurnOffDialog(
    featureLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    FlowDialog(
        title = "Turn $featureLabel off?",
        message = "You can turn it back on from the menu.",
        confirmText = "Turn off",
        dismissText = "Keep",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
