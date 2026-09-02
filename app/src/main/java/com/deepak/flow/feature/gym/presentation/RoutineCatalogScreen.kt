package com.deepak.flow.feature.gym.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.R
import com.deepak.flow.app.components.FlowButton
import com.deepak.flow.app.components.FlowDialog
import com.deepak.flow.app.components.FlowHairlineDivider
import com.deepak.flow.app.components.FlowIconAction
import com.deepak.flow.app.components.FlowMetaText
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.components.FlowSwipeDeleteRow
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowShell
import com.deepak.flow.app.theme.FlowSizes
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.core.gym.GymLogic
import kotlinx.coroutines.delay

@Composable
fun RoutineCatalogScreen(
    viewModel: RoutineCatalogViewModel,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onBack: () -> Unit,
    onOpenRoutine: (Long) -> Unit,
    onNewRoutine: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var swipeResetKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.confirmDeleteRoutineId) {
        if (uiState.confirmDeleteRoutineId == null) {
            swipeResetKey++
        }
    }

    LaunchedEffect(uiState.deleteBlockedMessage) {
        if (uiState.deleteBlockedMessage != null) {
            delay(2_500)
            viewModel.clearDeleteBlockedMessage()
        }
    }

    if (uiState.confirmDeleteRoutineId != null && uiState.confirmDeleteRoutineName != null) {
        FlowDialog(
            title = "Delete routine?",
            message = "Delete ${uiState.confirmDeleteRoutineName}? This will remove the routine from your routine list.",
            confirmText = "Delete",
            dismissText = "Cancel",
            destructive = true,
            onConfirm = viewModel::confirmDeleteRoutine,
            onDismiss = viewModel::dismissDeleteRoutine,
        )
    }

    FlowShell(
        selected = FlowDrawerDestination.GYM,
        userName = userName,
        remindersEnabled = remindersEnabled,
        waterEnabled = waterEnabled,
        gymEnabled = gymEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onGymEnabledChange = onGymEnabledChange,
        onDestinationClick = onDestinationClick,
        onBack = onBack,
        modifier = modifier,
    ) {
        FlowScreenTitle("Routines")
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            if (uiState.starred.isNotEmpty()) {
                FlowSectionLabel("Starred")
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                uiState.starred.forEach { item ->
                    RoutineCatalogRow(
                        item = item,
                        swipeResetKey = swipeResetKey,
                        onOpen = { onOpenRoutine(item.id) },
                        onToggleStar = { viewModel.toggleStar(item.id) },
                        onDelete = { viewModel.requestDeleteRoutine(item.id) },
                    )
                    Spacer(modifier = Modifier.height(FlowSpacing.md))
                }
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
            }

            if (uiState.others.isNotEmpty()) {
                if (uiState.starred.isNotEmpty()) {
                    FlowSectionLabel("All routines")
                    Spacer(modifier = Modifier.height(FlowSpacing.sm))
                }
                uiState.others.forEach { item ->
                    RoutineCatalogRow(
                        item = item,
                        swipeResetKey = swipeResetKey,
                        onOpen = { onOpenRoutine(item.id) },
                        onToggleStar = { viewModel.toggleStar(item.id) },
                        onDelete = { viewModel.requestDeleteRoutine(item.id) },
                    )
                    Spacer(modifier = Modifier.height(FlowSpacing.md))
                }
            }

            if (uiState.isEmpty) {
                Spacer(modifier = Modifier.height(FlowSpacing.xxl))
                FlowSupportingText("No routines yet.")
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
            }

            uiState.deleteBlockedMessage?.let { message ->
                Spacer(modifier = Modifier.height(FlowSpacing.md))
                FlowSupportingText(message)
            }
        }

        FlowHairlineDivider()
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowButton(
            text = "New Routine",
            onClick = onNewRoutine,
            leadingIcon = Icons.Default.Add,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
    }
}

@Composable
private fun RoutineCatalogRow(
    item: RoutineCatalogItem,
    swipeResetKey: Int,
    onOpen: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit,
) {
    FlowSwipeDeleteRow(
        modifier = Modifier.fillMaxWidth(),
        resetKey = swipeResetKey,
        onDelete = onDelete,
        onContentClick = onOpen,
        contentPadding = PaddingValues(vertical = FlowSpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleStar) {
                Icon(
                    imageVector = if (item.starred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = if (item.starred) "Unstar routine" else "Star routine",
                    tint = FlowTextPrimary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary,
                )
                Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                FlowMetaText(routineCatalogSubtitle(item))
            }
            FlowIconAction(
                icon = Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(
                    R.string.content_description_delete_routine,
                    item.name,
                ),
                onClick = onDelete,
                iconSize = FlowSizes.iconSm,
            )
        }
    }
}

private fun routineCatalogSubtitle(item: RoutineCatalogItem): String {
    val daysLabel = when (item.dayCount) {
        1 -> "1 day"
        else -> "${item.dayCount} days"
    }
    val roundsLabel = GymLogic.formatRoundsCompleted(item.roundsCompleted)
    return "$daysLabel · $roundsLabel"
}
