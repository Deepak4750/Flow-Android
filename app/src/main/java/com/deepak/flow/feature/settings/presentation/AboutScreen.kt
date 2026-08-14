package com.deepak.flow.feature.settings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.deepak.flow.BuildConfig
import com.deepak.flow.R
import com.deepak.flow.app.components.FlowInfoRow
import com.deepak.flow.app.components.FlowScreenHeader
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowSectionBreak
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.core.update.AppUpdateViewModel

@Composable
fun AboutScreen(
    updateViewModel: AppUpdateViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppUpdatePrompt(updateViewModel)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FlowSpacing.screenHorizontal),
        ) {
            FlowScreenHeader(
                title = stringResource(R.string.about_title),
                onBack = onBack,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xl))

            FlowSectionLabel(stringResource(R.string.app_name))
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            FlowScreenTitle(stringResource(R.string.about_tagline))

            FlowSectionBreak()
            FlowInfoRow(
                label = stringResource(R.string.about_label_version),
                value = BuildConfig.VERSION_NAME,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            AppUpdateCheckRow(updateViewModel)

            FlowSectionBreak()
            FlowSectionLabel(stringResource(R.string.about_label_privacy))
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            FlowSupportingText(stringResource(R.string.about_privacy_detail))
            Spacer(modifier = Modifier.height(FlowSpacing.xxl))
        }
    }
}
