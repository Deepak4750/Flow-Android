package com.deepak.flow.feature.onboarding.presentation

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.R
import com.deepak.flow.app.components.FlowButton
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowTextAction
import com.deepak.flow.app.components.FlowTextField
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            Spacer(modifier = Modifier.height(72.dp))
            FlowSectionLabel(stringResource(R.string.app_name))
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            Text(
                text = "Let's make this personal.",
                style = MaterialTheme.typography.headlineLarge,
                color = FlowTextPrimary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xl))

            FieldHeading(
                label = stringResource(R.string.settings_label_name),
                supporting = "What should we call you?",
            )
            FlowTextField(
                value = uiState.displayName,
                onValueChange = viewModel::updateDisplayName,
                placeholder = stringResource(R.string.placeholder_display_name),
            )

            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FieldHeading(
                label = stringResource(R.string.settings_label_nickname),
                supporting = "Optional — what Flow calls you in greetings",
            )
            FlowTextField(
                value = uiState.nickname,
                onValueChange = viewModel::updateNickname,
                placeholder = stringResource(R.string.placeholder_nickname),
            )

            Spacer(modifier = Modifier.height(FlowSpacing.xxl))
            FlowButton(
                text = "Continue",
                onClick = { viewModel.completeOnboarding(onComplete) },
                enabled = !uiState.isSaving,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            FlowTextAction(
                text = "Skip for now",
                onClick = { viewModel.skipOnboarding(onComplete) },
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xl))
        }
    }
}

@Composable
private fun FieldHeading(label: String, supporting: String) {
    FlowSectionLabel(label)
    Spacer(modifier = Modifier.height(FlowSpacing.xxs))
    Text(
        text = supporting,
        style = MaterialTheme.typography.bodyMedium,
        color = FlowTextSecondary,
    )
    Spacer(modifier = Modifier.height(FlowSpacing.xs))
}
