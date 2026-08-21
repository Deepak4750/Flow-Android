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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.R
import com.deepak.flow.app.components.FlowButton
import com.deepak.flow.app.components.FlowFieldHeading
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowScreenTopBar
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowTextAction
import com.deepak.flow.app.components.FlowTextField
import com.deepak.flow.app.theme.FlowSpacing

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
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
            // No navigation control here, but the bar still reserves its row so
            // the label starts at the same height as it does on every screen.
            FlowScreenTopBar()
            FlowSectionLabel(stringResource(R.string.app_name))
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            FlowScreenTitle("Let's make this personal.")
            Spacer(modifier = Modifier.height(FlowSpacing.xl))

            FlowFieldHeading(
                label = stringResource(R.string.settings_label_name),
                supporting = "What should we call you?",
            )
            FlowTextField(
                value = uiState.displayName,
                onValueChange = viewModel::updateDisplayName,
                placeholder = stringResource(R.string.placeholder_display_name),
            )

            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FlowFieldHeading(
                label = stringResource(R.string.settings_label_nickname),
                supporting = "Optional. What Flow calls you in greetings",
            )
            FlowTextField(
                value = uiState.nickname,
                onValueChange = viewModel::updateNickname,
                placeholder = stringResource(R.string.placeholder_nickname),
            )

            Spacer(modifier = Modifier.height(FlowSpacing.xxl))
            FlowButton(
                text = "Continue",
                onClick = { viewModel.completeOnboarding() },
                enabled = !uiState.isSaving,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            FlowTextAction(
                text = "Skip for now",
                onClick = { viewModel.skipOnboarding() },
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xl))
        }
    }
}
