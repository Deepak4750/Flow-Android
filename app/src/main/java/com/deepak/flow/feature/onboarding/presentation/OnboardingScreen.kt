package com.deepak.flow.feature.onboarding.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.deepak.flow.app.components.FlowHairlineDivider
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowScreenTopBar
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.components.FlowTextAction
import com.deepak.flow.app.components.FlowTextField
import com.deepak.flow.app.components.FlowToggleRow
import com.deepak.flow.app.theme.FlowMotion
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
            FlowScreenTopBar()
            AnimatedContent(
                targetState = uiState.page,
                transitionSpec = {
                    fadeIn(tween(FlowMotion.STANDARD)) togetherWith fadeOut(tween(FlowMotion.FAST))
                },
                label = "onboardingPage",
            ) { page ->
                when (page) {
                    0 -> OnboardingIntroPage(
                        saving = uiState.isSaving,
                        onGetStarted = viewModel::nextPage,
                        onSkip = viewModel::skipToProfile,
                    )
                    1 -> OnboardingFeaturesPage(
                        remindersEnabled = uiState.remindersEnabled,
                        waterEnabled = uiState.waterEnabled,
                        gymEnabled = uiState.gymEnabled,
                        saving = uiState.isSaving,
                        onRemindersEnabledChange = viewModel::setRemindersEnabled,
                        onWaterEnabledChange = viewModel::setWaterEnabled,
                        onGymEnabledChange = viewModel::setGymEnabled,
                        onContinue = viewModel::continueFromFeatures,
                        onSkip = viewModel::skipToProfile,
                    )
                    2 -> OnboardingReadyPage(
                        saving = uiState.isSaving,
                        onContinue = viewModel::nextPage,
                    )
                    else -> OnboardingProfilePage(
                        displayName = uiState.displayName,
                        nickname = uiState.nickname,
                        saving = uiState.isSaving,
                        onDisplayNameChange = viewModel::updateDisplayName,
                        onNicknameChange = viewModel::updateNickname,
                        onContinue = viewModel::completeOnboarding,
                        onSkip = viewModel::skipOnboarding,
                    )
                }
            }
            Spacer(modifier = Modifier.height(FlowSpacing.xl))
        }
    }
}

@Composable
private fun OnboardingIntroPage(
    saving: Boolean,
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
) {
    Column {
        FlowSectionLabel(stringResource(R.string.app_name))
        Spacer(modifier = Modifier.height(FlowSpacing.xs))
        FlowScreenTitle("Your day, in one place.")
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        FlowSupportingText(
            "Flow brings together Tasks, Water, and Gym in one simple place.",
        )
        Spacer(modifier = Modifier.height(FlowSpacing.xxl))
        FlowButton(
            text = "Get started",
            onClick = onGetStarted,
            enabled = !saving,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.xs))
        FlowTextAction(
            text = "Skip",
            onClick = onSkip,
            enabled = !saving,
        )
    }
}

@Composable
private fun OnboardingFeaturesPage(
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    saving: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    Column {
        FlowScreenTitle("Choose what matters to you.")
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        FlowSupportingText("You can turn features on whenever you're ready.")
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        FlowToggleRow(
            label = "Tasks",
            checked = remindersEnabled,
            onCheckedChange = onRemindersEnabledChange,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        FlowHairlineDivider()
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        FlowToggleRow(
            label = "H₂O",
            checked = waterEnabled,
            onCheckedChange = onWaterEnabledChange,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        FlowHairlineDivider()
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        FlowToggleRow(
            label = "Gym",
            checked = gymEnabled,
            onCheckedChange = onGymEnabledChange,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.xxl))
        FlowButton(
            text = "Continue",
            onClick = onContinue,
            enabled = !saving,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.xs))
        FlowTextAction(
            text = "Skip",
            onClick = onSkip,
            enabled = !saving,
        )
    }
}

@Composable
private fun OnboardingReadyPage(
    saving: Boolean,
    onContinue: () -> Unit,
) {
    Column {
        FlowScreenTitle("You're ready.")
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        FlowSupportingText("Explore Flow and set things up whenever you want.")
        Spacer(modifier = Modifier.height(FlowSpacing.xxl))
        FlowButton(
            text = "Continue",
            onClick = onContinue,
            enabled = !saving,
        )
    }
}

@Composable
private fun OnboardingProfilePage(
    displayName: String,
    nickname: String,
    saving: Boolean,
    onDisplayNameChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    Column {
        FlowSectionLabel(stringResource(R.string.app_name))
        Spacer(modifier = Modifier.height(FlowSpacing.xs))
        FlowScreenTitle("Let's make this personal.")
        Spacer(modifier = Modifier.height(FlowSpacing.xl))

        FlowFieldHeading(
            label = stringResource(R.string.settings_label_name),
            supporting = "What should we call you?",
        )
        FlowTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            placeholder = stringResource(R.string.placeholder_display_name),
        )

        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowFieldHeading(
            label = stringResource(R.string.settings_label_nickname),
            supporting = "Optional. What Flow calls you in greetings",
        )
        FlowTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            placeholder = stringResource(R.string.placeholder_nickname),
        )

        Spacer(modifier = Modifier.height(FlowSpacing.xxl))
        FlowButton(
            text = "Continue",
            onClick = onContinue,
            enabled = !saving,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.xs))
        FlowTextAction(
            text = "Skip for now",
            onClick = onSkip,
            enabled = !saving,
        )
    }
}
