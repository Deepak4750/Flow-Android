package com.deepak.flow.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.deepak.flow.R
import com.deepak.flow.app.components.FlowDrawerItem
import com.deepak.flow.app.components.FlowHairlineDivider
import com.deepak.flow.app.components.FlowIconAction
import com.deepak.flow.app.components.FlowScreenTopBar
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.theme.FlowSizes
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowSurface
import com.deepak.flow.app.theme.FlowTextPrimary
import kotlinx.coroutines.launch

/**
 * Flow's own drawer sheet: a flat near-black panel with hairline separation.
 * Deliberately not a Material [androidx.compose.material3.ModalDrawerSheet] -
 * no elevation, no rounded card items, no coloured selection pills.
 */
@Composable
fun FlowDrawerContent(
    selected: FlowDrawerDestination,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(FlowSizes.drawerWidth)
            .fillMaxHeight()
            .background(FlowSurface)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = FlowSpacing.screenHorizontal),
    ) {
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        FlowSectionLabel("Flow")
        Spacer(modifier = Modifier.height(FlowSpacing.xs))
        Text(
            text = userName ?: "Your tasks",
            style = MaterialTheme.typography.headlineMedium,
            color = FlowTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowHairlineDivider()
        Spacer(modifier = Modifier.height(FlowSpacing.md))

        FlowDrawerDestination.entries.forEach { destination ->
            if (destination == FlowDrawerDestination.SETTINGS) {
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
                FlowHairlineDivider()
                Spacer(modifier = Modifier.height(FlowSpacing.md))
            }
            FlowDrawerItem(
                label = destination.label,
                selected = destination == selected,
                enabled = destination.isEnabled(remindersEnabled, waterEnabled, gymEnabled),
                checked = destination.featureChecked(remindersEnabled, waterEnabled, gymEnabled),
                onCheckedChange = when (destination) {
                    FlowDrawerDestination.REMINDERS -> onRemindersEnabledChange
                    FlowDrawerDestination.WATER -> onWaterEnabledChange
                    FlowDrawerDestination.GYM -> onGymEnabledChange
                    else -> null
                },
                onClick = { onDestinationClick(destination) },
            )
        }

        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowHairlineDivider()
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        FlowSupportingText(stringResource(R.string.about_privacy))
    }
}

@Composable
fun FlowShell(
    selected: FlowDrawerDestination,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = onBack == null,
        modifier = modifier,
        drawerContent = {
            FlowDrawerContent(
                selected = selected,
                userName = userName,
                remindersEnabled = remindersEnabled,
                waterEnabled = waterEnabled,
                gymEnabled = gymEnabled,
                onRemindersEnabledChange = onRemindersEnabledChange,
                onWaterEnabledChange = onWaterEnabledChange,
                onGymEnabledChange = onGymEnabledChange,
                onDestinationClick = { destination ->
                    if (!destination.isEnabled(remindersEnabled, waterEnabled, gymEnabled)) {
                        return@FlowDrawerContent
                    }
                    scope.launch { drawerState.close() }
                    onDestinationClick(destination)
                },
            )
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = floatingActionButton,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = FlowSpacing.screenHorizontal),
            ) {
                FlowScreenTopBar(
                    leading = {
                        if (onBack != null) {
                            FlowIconAction(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_description_back),
                                onClick = onBack,
                            )
                        } else {
                            FlowIconAction(
                                icon = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.content_description_open_menu),
                                onClick = { scope.launch { drawerState.open() } },
                            )
                        }
                    },
                    trailing = {
                        FlowSectionLabel("Flow")
                    },
                )
                content()
            }
        }
    }
}
