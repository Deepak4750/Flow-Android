package com.deepak.flow.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepak.flow.R
import com.deepak.flow.app.components.FlowDrawerItem
import com.deepak.flow.app.components.FlowHairlineDivider
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowSurface
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary

/**
 * Flow's own drawer sheet: a flat near-black panel with hairline separation.
 * Deliberately not a Material [androidx.compose.material3.ModalDrawerSheet] —
 * no elevation, no rounded card items, no coloured selection pills.
 */
@Composable
fun FlowDrawerContent(
    selected: FlowDrawerDestination,
    userName: String?,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(FlowSurface)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = FlowSpacing.lg),
    ) {
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        FlowSectionLabel("Flow")
        Spacer(modifier = Modifier.height(FlowSpacing.xs))
        Text(
            text = userName ?: "Your reminders",
            style = MaterialTheme.typography.headlineSmall,
            color = FlowTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowHairlineDivider()
        Spacer(modifier = Modifier.height(FlowSpacing.md))

        FlowDrawerDestination.entries.forEach { destination ->
            FlowDrawerItem(
                label = destination.label,
                selected = destination == selected,
                onClick = { onDestinationClick(destination) },
            )
        }

        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowHairlineDivider()
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        Text(
            text = stringResource(R.string.about_privacy),
            style = MaterialTheme.typography.bodyMedium,
            color = FlowTextSecondary,
        )
    }
}
