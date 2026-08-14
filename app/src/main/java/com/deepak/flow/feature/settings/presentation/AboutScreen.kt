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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.deepak.flow.BuildConfig
import com.deepak.flow.R
import com.deepak.flow.app.components.FlowHairlineDivider
import com.deepak.flow.app.components.FlowInfoRow
import com.deepak.flow.app.components.FlowScreenHeader
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            Text(
                text = stringResource(R.string.about_tagline),
                style = MaterialTheme.typography.headlineMedium,
                color = FlowTextPrimary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xl))
            FlowHairlineDivider()
            Spacer(modifier = Modifier.height(FlowSpacing.md))

            FlowInfoRow(
                label = stringResource(R.string.about_label_version),
                value = BuildConfig.VERSION_NAME,
            )
            FlowHairlineDivider()

            Spacer(modifier = Modifier.height(FlowSpacing.xl))
            FlowSectionLabel(stringResource(R.string.about_label_privacy))
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            Text(
                text = stringResource(R.string.about_privacy_detail),
                style = MaterialTheme.typography.bodyLarge,
                color = FlowTextSecondary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xxl))
        }
    }
}
