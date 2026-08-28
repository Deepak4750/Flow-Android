package com.deepak.flow.feature.gym.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.app.components.FlowButton
import com.deepak.flow.app.components.FlowButtonVariant
import com.deepak.flow.app.components.FlowDialog
import com.deepak.flow.app.components.FlowHairlineDivider
import com.deepak.flow.app.components.FlowMetaText
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.components.FlowSwipeDeleteRow
import com.deepak.flow.app.components.FlowTextAction
import com.deepak.flow.app.components.FlowTextField
import com.deepak.flow.app.components.FlowUndoBanner
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowShell
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextDisabled
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.app.theme.FlowTextTertiary
import com.deepak.flow.app.theme.FlowWhite
import com.deepak.flow.core.gym.GymLimits
import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.GymRoutineDay
import com.deepak.flow.core.gym.GymRoutineExercise
import com.deepak.flow.core.gym.TrackingField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val DefaultDayHeightDp = 72.dp

@Composable
fun RoutineBuilderScreen(
    viewModel: RoutineBuilderViewModel,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLeavePrompt by remember { mutableStateOf(false) }

    val requestLeave: () -> Unit = {
        if (uiState.hasPendingEdits) {
            showLeavePrompt = true
        } else {
            onLeave()
        }
    }

    val dismissLeavePrompt: () -> Unit = {
        showLeavePrompt = false
    }

    BackHandler(enabled = showLeavePrompt) {
        dismissLeavePrompt()
    }

    BackHandler(enabled = uiState.hasPendingEdits && !showLeavePrompt) {
        showLeavePrompt = true
    }

    if (showLeavePrompt) {
        FlowDialog(
            title = "Save changes?",
            message = "You have unsaved changes to this routine.",
            confirmText = "Save changes",
            dismissText = "Cancel",
            confirmEnabled = uiState.canSave,
            onConfirm = {
                dismissLeavePrompt()
                viewModel.save()
            },
            onDismiss = dismissLeavePrompt,
            onDismissRequest = dismissLeavePrompt,
        )
    }

    LaunchedEffect(uiState.leave) {
        if (uiState.leave) onLeave()
    }
    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            delay(2_500)
            viewModel.clearMessage()
        }
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
        onBack = {
            if (showLeavePrompt) {
                dismissLeavePrompt()
            } else {
                requestLeave()
            }
        },
        modifier = modifier,
    ) {
        val title = if (uiState.routineId > 0L) "Edit Routine" else "New Routine"
        FlowScreenTitle(title)
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        if (uiState.loading) {
            FlowSupportingText("Loading…")
            return@FlowShell
        }

        var draggingDayKey by remember { mutableStateOf<String?>(null) }
        var dragOffsetY by remember { mutableFloatStateOf(0f) }
        val dayHeights = remember { mutableStateMapOf<String, Float>() }
        var swipeResetKey by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current
        val defaultHeightPx = with(density) { DefaultDayHeightDp.toPx() }
        val scrollState = rememberScrollState()

        val dragFromIndex = draggingDayKey?.let { key ->
            uiState.days.indexOfFirst { it.localKey == key }.takeIf { it >= 0 }
        }
        val dragTargetIndex = dragFromIndex?.let { from ->
            computeTargetDayIndex(
                fromIndex = from,
                offsetY = dragOffsetY,
                days = uiState.days,
                heights = dayHeights,
                defaultHeightPx = defaultHeightPx,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState),
        ) {
            FlowTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                placeholder = "Routine name",
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xl))

            uiState.days.forEachIndexed { dayIndex, day ->
                val dayKey = day.localKey
                val expanded = uiState.expandedDayKey == dayKey
                val canRemoveDay = uiState.days.size > GymLimits.DAY_COUNT_MIN
                val isDragging = draggingDayKey == dayKey
                val dragHeight = dayHeights[dayKey] ?: defaultHeightPx
                val displacementY = dayDisplacementY(
                    index = dayIndex,
                    dragFromIndex = dragFromIndex,
                    dragOffsetY = dragOffsetY,
                    targetIndex = dragTargetIndex,
                    draggedHeightPx = dragHeight,
                )

                Column(
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .offset { IntOffset(0, displacementY.roundToInt()) }
                        .onSizeChanged { dayHeights[dayKey] = it.height.toFloat() },
                ) {
                    FlowSwipeDeleteRow(
                        enabled = canRemoveDay,
                        resetKey = swipeResetKey,
                        onDelete = {
                            viewModel.deleteDay(dayKey)
                            swipeResetKey++
                        },
                    ) {
                        RoutineDayBlock(
                            day = day,
                            dayIndex = dayIndex,
                            expanded = expanded,
                            isDragging = isDragging,
                            canMove = uiState.days.size > 1,
                            expandedExerciseStableKey = uiState.expandedExerciseStableKey,
                            onDayNameChange = { viewModel.onDayNameChange(dayKey, it) },
                            onToggleExpanded = { viewModel.toggleDayExpanded(dayKey) },
                            onDragStart = {
                                draggingDayKey = dayKey
                                dragOffsetY = 0f
                            },
                            onDrag = { delta -> dragOffsetY += delta },
                            onDragEnd = {
                                val from = dragFromIndex
                                if (from != null) {
                                    val to = computeTargetDayIndex(
                                        fromIndex = from,
                                        offsetY = dragOffsetY,
                                        days = uiState.days,
                                        heights = dayHeights,
                                        defaultHeightPx = defaultHeightPx,
                                    )
                                    draggingDayKey = null
                                    dragOffsetY = 0f
                                    if (to != from) {
                                        viewModel.moveDay(from, to)
                                    }
                                } else {
                                    draggingDayKey = null
                                    dragOffsetY = 0f
                                }
                            },
                            onDragCancel = {
                                draggingDayKey = null
                                dragOffsetY = 0f
                            },
                            onAddExercise = { viewModel.addExercise(dayKey) },
                            onToggleExercise = { stableKey ->
                                viewModel.toggleExerciseExpanded(stableKey)
                            },
                            onExerciseNameChange = { stableKey, value ->
                                viewModel.onExerciseNameChange(dayKey, stableKey, value)
                            },
                            onExerciseNoteChange = { stableKey, value ->
                                viewModel.onExerciseNoteChange(dayKey, stableKey, value)
                            },
                            onToggleField = { stableKey, field ->
                                viewModel.toggleTrackingField(dayKey, stableKey, field)
                            },
                            onStepSetCount = { stableKey, up ->
                                viewModel.stepSetCount(dayKey, stableKey, up)
                            },
                            onDeleteExercise = { stableKey ->
                                viewModel.deleteExercise(dayKey, stableKey)
                            },
                        )
                    }
                    if (dayIndex < uiState.days.lastIndex) {
                        Spacer(modifier = Modifier.height(FlowSpacing.xl))
                        FlowHairlineDivider()
                        Spacer(modifier = Modifier.height(FlowSpacing.xl))
                    }
                }
            }

            Spacer(modifier = Modifier.height(FlowSpacing.xl))
            FlowButton(
                text = "Add Workout Day",
                onClick = viewModel::addWorkoutDay,
                variant = FlowButtonVariant.Secondary,
                leadingIcon = Icons.Default.Add,
                enabled = uiState.days.size < GymLimits.DAY_COUNT_MAX,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowButton(
                text = "Add Rest Day",
                onClick = viewModel::addRestDay,
                variant = FlowButtonVariant.Secondary,
                enabled = uiState.days.size < GymLimits.DAY_COUNT_MAX,
            )

            uiState.message?.let { message ->
                Spacer(modifier = Modifier.height(FlowSpacing.md))
                FlowSupportingText(message)
            }

            Spacer(modifier = Modifier.height(FlowSpacing.xl))
            FlowButton(
                text = if (uiState.isEditMode) "Save changes" else "Save routine",
                onClick = viewModel::save,
                enabled = uiState.canSave,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
        }

        if (uiState.dayRemovedUndoVisible) {
            FlowUndoBanner(
                message = "Day removed",
                onUndo = viewModel::undoDeleteDay,
                onDismiss = viewModel::dismissDayRemovedUndo,
            )
        }
    }
}

private fun computeTargetDayIndex(
    fromIndex: Int,
    offsetY: Float,
    days: List<GymRoutineDay>,
    heights: Map<String, Float>,
    defaultHeightPx: Float,
): Int {
    if (offsetY == 0f || days.isEmpty()) return fromIndex
    var index = fromIndex
    if (offsetY > 0f) {
        var remaining = offsetY
        while (index < days.lastIndex) {
            val step = heights[days[index].localKey] ?: defaultHeightPx
            if (remaining < step / 2f) break
            remaining -= step
            index++
        }
    } else {
        var remaining = -offsetY
        while (index > 0) {
            val step = heights[days[index - 1].localKey] ?: defaultHeightPx
            if (remaining < step / 2f) break
            remaining -= step
            index--
        }
    }
    return index.coerceIn(0, days.lastIndex)
}

private fun dayDisplacementY(
    index: Int,
    dragFromIndex: Int?,
    dragOffsetY: Float,
    targetIndex: Int?,
    draggedHeightPx: Float,
): Float {
    val from = dragFromIndex ?: return 0f
    val target = targetIndex ?: from
    return when {
        index == from -> dragOffsetY
        from < target && index in (from + 1)..target -> -draggedHeightPx
        from > target && index in target until from -> draggedHeightPx
        else -> 0f
    }
}

@Composable
private fun RoutineDayBlock(
    day: GymRoutineDay,
    dayIndex: Int,
    expanded: Boolean,
    isDragging: Boolean,
    canMove: Boolean,
    expandedExerciseStableKey: String?,
    onDayNameChange: (String) -> Unit,
    onToggleExpanded: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onAddExercise: () -> Unit,
    onToggleExercise: (String) -> Unit,
    onExerciseNameChange: (String, String) -> Unit,
    onExerciseNoteChange: (String, String) -> Unit,
    onToggleField: (String, TrackingField) -> Unit,
    onStepSetCount: (String, Boolean) -> Unit,
    onDeleteExercise: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        DayDragHandle(
            enabled = canMove,
            isDragging = isDragging,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DayTitleHeading(
                    dayKey = day.localKey,
                    dayIndex = dayIndex,
                    title = day.name,
                    isRestDay = day.isRestDay,
                    onTitleChange = onDayNameChange,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse day" else "Expand day",
                        tint = FlowTextSecondary,
                    )
                }
            }
            if (!expanded) {
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                FlowMetaText(dayCollapsedSummary(day))
            } else {
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
                if (day.isRestDay) {
                    FlowSupportingText("Rest day. No exercises.")
                } else {
                    day.exercises.forEach { exercise ->
                        val exerciseExpanded = expandedExerciseStableKey == exercise.stableKey
                        RoutineExerciseBlock(
                            exercise = exercise,
                            expanded = exerciseExpanded,
                            onToggle = { onToggleExercise(exercise.stableKey) },
                            onNameChange = { onExerciseNameChange(exercise.stableKey, it) },
                            onNoteChange = { onExerciseNoteChange(exercise.stableKey, it) },
                            onToggleField = { field -> onToggleField(exercise.stableKey, field) },
                            onStepSetCount = { up -> onStepSetCount(exercise.stableKey, up) },
                            onDelete = { onDeleteExercise(exercise.stableKey) },
                        )
                        Spacer(modifier = Modifier.height(FlowSpacing.lg))
                    }
                    FlowButton(
                        text = "Add Exercise",
                        onClick = onAddExercise,
                        variant = FlowButtonVariant.Secondary,
                        leadingIcon = Icons.Default.Add,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayDragHandle(
    enabled: Boolean,
    isDragging: Boolean,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    Icon(
        imageVector = Icons.Default.DragHandle,
        contentDescription = "Drag to reorder day",
        tint = when {
            !enabled -> FlowTextDisabled
            isDragging -> FlowTextPrimary
            else -> FlowTextSecondary
        },
        modifier = Modifier
            .padding(end = FlowSpacing.sm)
            .size(24.dp)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragCancel() },
                            onVerticalDrag = { _, dragAmount -> onDrag(dragAmount) },
                        )
                    }
                } else {
                    Modifier
                },
            ),
    )
}

@Composable
private fun DayTitleHeading(
    dayKey: String,
    dayIndex: Int,
    title: String,
    isRestDay: Boolean,
    onTitleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val headingStyle = MaterialTheme.typography.headlineSmall

    if (isRestDay) {
        Text(
            text = GymLogic.formatDayHeading(dayIndex, "", isRestDay = true),
            style = headingStyle,
            color = FlowTextPrimary,
            modifier = modifier,
        )
        return
    }

    var editing by remember(dayKey) { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val prefix = GymLogic.dayHeadingPrefix(dayIndex)

    LaunchedEffect(editing) {
        if (editing) focusRequester.requestFocus()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = prefix,
            style = headingStyle,
            color = FlowTextPrimary,
        )
        if (editing) {
            BasicTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = headingStyle.copy(color = FlowTextPrimary),
                singleLine = true,
                cursorBrush = SolidColor(FlowWhite),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { editing = false }),
                decorationBox = { inner ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (title.isEmpty()) {
                            Text(
                                text = "Day title",
                                style = headingStyle,
                                color = FlowTextDisabled,
                                maxLines = 1,
                            )
                        }
                        inner()
                    }
                },
            )
        } else {
            Text(
                text = GymLogic.dayHeadingSuffixLabel(title, isRestDay = false),
                style = headingStyle,
                color = if (title.isBlank()) FlowTextTertiary else FlowTextPrimary,
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Button, onClick = { editing = true }),
            )
        }
    }
}

private fun dayCollapsedSummary(day: GymRoutineDay): String {
    if (day.isRestDay) return "Rest day"
    val count = day.exercises.count { it.name.trim().isNotEmpty() }
    return when (count) {
        0 -> "No exercises"
        1 -> "1 exercise"
        else -> "$count exercises"
    }
}

@Composable
private fun RoutineExerciseBlock(
    exercise: GymRoutineExercise,
    expanded: Boolean,
    onToggle: () -> Unit,
    onNameChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onToggleField: (TrackingField) -> Unit,
    onStepSetCount: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    if (!expanded) {
        val displayTitle = exercise.name.trim().ifEmpty { "Exercise title" }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary,
                )
                Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                FlowMetaText(
                    "${trackingSummary(exercise.trackingFields)} · ${exercise.setCount} sets",
                )
            }
            FlowTextAction(text = "Edit", onClick = onToggle)
        }
        return
    }
    FlowTextField(
        value = exercise.name,
        onValueChange = onNameChange,
        placeholder = "Exercise title",
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(FlowSpacing.md))
    FlowMetaText(trackingSummary(exercise.trackingFields))
    Spacer(modifier = Modifier.height(FlowSpacing.sm))
    TrackingFieldChips(
        fields = exercise.trackingFields,
        onToggle = onToggleField,
    )
    Spacer(modifier = Modifier.height(FlowSpacing.lg))
    FlowSectionLabel("Sets")
    Spacer(modifier = Modifier.height(FlowSpacing.sm))
    SteppedCountRow(
        value = exercise.setCount,
        onMinus = { onStepSetCount(false) },
        onPlus = { onStepSetCount(true) },
        minusEnabled = exercise.setCount > GymLimits.SET_COUNT_MIN,
        plusEnabled = exercise.setCount < GymLimits.SET_COUNT_MAX,
    )
    Spacer(modifier = Modifier.height(FlowSpacing.md))
    FlowTextField(
        value = exercise.note,
        onValueChange = onNoteChange,
        placeholder = "Your note...",
        singleLine = false,
        minLines = 2,
    )
    FlowMetaText("${exercise.note.length}/${GymLimits.NOTE_MAX_CHARS}")
    Spacer(modifier = Modifier.height(FlowSpacing.sm))
    FlowTextAction(text = "Hide", onClick = onToggle)
    Spacer(modifier = Modifier.height(FlowSpacing.xs))
    FlowTextAction(text = "Delete Exercise", onClick = onDelete, destructive = true)
}

@Composable
private fun SteppedCountRow(
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean,
    plusEnabled: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xxs),
    ) {
        FlowTextAction(text = "−", onClick = onMinus, enabled = minusEnabled)
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = FlowTextPrimary,
        )
        FlowTextAction(text = "+", onClick = onPlus, enabled = plusEnabled)
    }
}
