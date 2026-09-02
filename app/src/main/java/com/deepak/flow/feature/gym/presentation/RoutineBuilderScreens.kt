package com.deepak.flow.feature.gym.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import com.deepak.flow.app.components.FlowDragReorderItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.R
import com.deepak.flow.app.components.ExerciseNameField
import com.deepak.flow.app.components.ExercisePickerSheet
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
import com.deepak.flow.app.theme.FlowSizes
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowSurfaceRaised
import com.deepak.flow.app.theme.FlowBorder
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.deepak.flow.app.theme.FlowTextDisabled
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.app.theme.FlowTextTertiary
import com.deepak.flow.app.theme.FlowWhite
import com.deepak.flow.core.gym.GymEquipment
import com.deepak.flow.core.gym.GymLimits
import com.deepak.flow.core.gym.GymMuscleGroup
import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.GymRoutineDay
import com.deepak.flow.core.gym.GymRoutineExercise
import com.deepak.flow.core.gym.TrackingField
import kotlinx.coroutines.delay

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
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadExerciseNameSuggestions()
    }
    var showLeavePrompt by remember { mutableStateOf(false) }

    val requestLeave: () -> Unit = {
        if (!uiState.isEditMode || uiState.hasPendingEdits) {
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

    BackHandler(enabled = uiState.hasPendingEdits && !showLeavePrompt && uiState.isEditMode) {
        showLeavePrompt = true
    }

    BackHandler(enabled = !uiState.isEditMode && !showLeavePrompt) {
        showLeavePrompt = true
    }

    if (showLeavePrompt) {
        if (uiState.isEditMode) {
            RoutineEditLeaveDialog(
                canSave = uiState.canSave,
                onSave = {
                    dismissLeavePrompt()
                    viewModel.save()
                },
                onCancel = dismissLeavePrompt,
                onDiscard = {
                    dismissLeavePrompt()
                    viewModel.discardAndLeave()
                },
            )
        } else {
            FlowDialog(
                title = "Discard routine?",
                message = "Leave without saving this new routine.",
                confirmText = "Discard",
                dismissText = "Cancel",
                destructive = true,
                onConfirm = {
                    dismissLeavePrompt()
                    viewModel.discardAndLeave()
                },
                onDismiss = dismissLeavePrompt,
                onDismissRequest = dismissLeavePrompt,
            )
        }
    }

    LaunchedEffect(uiState.leave) {
        if (uiState.leave) onLeave()
    }
    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onDeleted()
    }
    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            delay(2_500)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(uiState.deleteBlockedMessage) {
        if (uiState.deleteBlockedMessage != null) {
            delay(2_500)
            viewModel.clearDeleteBlockedMessage()
        }
    }

    if (uiState.confirmDeleteRoutine) {
        FlowDialog(
            title = "Delete routine?",
            message = "Delete ${uiState.name.trim().ifEmpty { "Routine" }}? This will remove the routine from your routine list.",
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
        var activeDragDayKey by remember { mutableStateOf<String?>(null) }
        var dragOffsetY by remember { mutableFloatStateOf(0f) }
        var lastCommittedOrderKeys by remember { mutableStateOf<List<String>>(emptyList()) }
        val dayOrderKeys = remember { mutableStateListOf<String>() }
        val dayHeights = remember { mutableStateMapOf<String, Float>() }
        var swipeResetKey by remember { mutableIntStateOf(0) }
        var pickerVisible by remember { mutableStateOf(false) }
        var pickerDayKey by remember { mutableStateOf<String?>(null) }
        var pickerExerciseStableKey by remember { mutableStateOf<String?>(null) }
        var pickerMuscle by remember { mutableStateOf<GymMuscleGroup?>(null) }
        var pickerEquipment by remember { mutableStateOf<GymEquipment?>(null) }
        val density = LocalDensity.current
        val defaultHeightPx = with(density) { DefaultDayHeightDp.toPx() }
        val dayGapPx = with(density) { (FlowSpacing.xl * 2 + 1.dp).toPx() }
        val scrollState = rememberScrollState()

        LaunchedEffect(pickerVisible, uiState.exerciseSearchQuery, pickerMuscle, pickerEquipment) {
            if (!pickerVisible) return@LaunchedEffect
            viewModel.browseExercises(
                query = uiState.exerciseSearchQuery,
                muscleFilter = pickerMuscle,
                equipmentFilter = pickerEquipment,
            )
        }

        ExercisePickerSheet(
            visible = pickerVisible,
            query = uiState.exerciseSearchQuery,
            onQueryChange = { query ->
                viewModel.browseExercises(query, pickerMuscle, pickerEquipment)
            },
            results = uiState.exerciseSearchResults,
            selectedMuscle = pickerMuscle,
            onMuscleSelected = { muscle ->
                pickerMuscle = muscle
                viewModel.browseExercises(uiState.exerciseSearchQuery, muscle, pickerEquipment)
            },
            selectedEquipment = pickerEquipment,
            onEquipmentSelected = { equipment ->
                pickerEquipment = equipment
                viewModel.browseExercises(uiState.exerciseSearchQuery, pickerMuscle, equipment)
            },
            onSelectExercise = { exerciseId, displayName ->
                val dayKey = pickerDayKey
                val stableKey = pickerExerciseStableKey
                if (dayKey != null && stableKey != null) {
                    viewModel.onExerciseSelected(dayKey, stableKey, exerciseId, displayName)
                }
                pickerVisible = false
            },
            onCreateCustomExercise = { displayName ->
                val dayKey = pickerDayKey
                val stableKey = pickerExerciseStableKey
                if (dayKey != null && stableKey != null) {
                    viewModel.onCreateCustomExercise(dayKey, stableKey, displayName)
                }
                pickerVisible = false
            },
            onDismiss = { pickerVisible = false },
        )

        LaunchedEffect(uiState.days, draggingDayKey, lastCommittedOrderKeys) {
            if (draggingDayKey != null) return@LaunchedEffect
            val vmKeys = uiState.days.map { it.localKey }
            val localKeys = dayOrderKeys.toList()
            when {
                localKeys.isEmpty() -> {
                    dayOrderKeys.addAll(vmKeys)
                    lastCommittedOrderKeys = vmKeys
                }
                localKeys.toSet() != vmKeys.toSet() -> {
                    dayOrderKeys.clear()
                    dayOrderKeys.addAll(vmKeys)
                    lastCommittedOrderKeys = vmKeys
                }
                localKeys != vmKeys && vmKeys == lastCommittedOrderKeys -> {
                    dayOrderKeys.clear()
                    dayOrderKeys.addAll(vmKeys)
                }
            }
        }

        val daysByKey = remember(uiState.days) { uiState.days.associateBy { it.localKey } }
        val orderedDays = dayOrderKeys.mapNotNull { daysByKey[it] }
        val dragFromIndex = activeDragDayKey?.let { key -> dayOrderKeys.indexOf(key) } ?: -1
        val reorderInProgress = activeDragDayKey != null

        fun beginDayReorder(dayKey: String) {
            swipeResetKey++
            activeDragDayKey = dayKey
            draggingDayKey = dayKey
            dragOffsetY = 0f
        }

        fun applyLiveDayReorder(movedKey: String, to: Int, days: List<GymRoutineDay>) {
            val from = dayOrderKeys.indexOf(movedKey)
            if (from < 0 || from == to) return
            val direction = if (to > from) 1 else -1
            var crossedPx = 0f
            if (direction > 0) {
                for (index in from until to) {
                    crossedPx += dayHeights[days[index].localKey] ?: defaultHeightPx
                    if (index < days.lastIndex) crossedPx += dayGapPx
                }
            } else {
                for (index in (to until from).reversed()) {
                    crossedPx += dayHeights[days[index].localKey] ?: defaultHeightPx
                    if (index > 0) crossedPx += dayGapPx
                }
            }
            dayOrderKeys.removeAt(from)
            dayOrderKeys.add(to, movedKey)
            val committed = dayOrderKeys.toList()
            lastCommittedOrderKeys = committed
            viewModel.reorderDaysByKeys(committed)
            dragOffsetY -= direction * crossedPx
        }

        fun clearDayDragState() {
            draggingDayKey = null
            activeDragDayKey = null
            dragOffsetY = 0f
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState, enabled = !reorderInProgress),
        ) {
            FlowTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                placeholder = "Routine name",
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xl))

            dayOrderKeys.forEachIndexed { dayIndex, dayKey ->
                val day = daysByKey[dayKey] ?: return@forEachIndexed
                key(dayKey) {
                val expanded = uiState.expandedDayKey == dayKey
                val canRemoveDay = dayOrderKeys.size > GymLimits.DAY_COUNT_MIN
                val isDragging = activeDragDayKey == dayKey
                val displacementY = dayDisplacementYSmooth(
                    index = dayIndex,
                    fromIndex = dragFromIndex,
                    dragOffsetY = dragOffsetY,
                    orderedDays = orderedDays,
                    heights = dayHeights,
                    defaultHeightPx = defaultHeightPx,
                    gapPx = dayGapPx,
                )

                FlowDragReorderItem(
                    isDragging = isDragging,
                    displacementY = displacementY,
                    modifier = Modifier.onSizeChanged { dayHeights[dayKey] = it.height.toFloat() },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        DayDragHandle(
                            dayKey = dayKey,
                            enabled = dayOrderKeys.size > 1,
                            isDragging = isDragging,
                            onDragStart = { beginDayReorder(dayKey) },
                            onDrag = { delta ->
                                if (activeDragDayKey != dayKey) return@DayDragHandle
                                dragOffsetY += delta
                                val from = dayOrderKeys.indexOf(dayKey)
                                if (from >= 0) {
                                    val currentOrderedDays = dayOrderKeys.mapNotNull { daysByKey[it] }
                                    val to = computeTargetDayIndex(
                                        fromIndex = from,
                                        offsetY = dragOffsetY,
                                        days = currentOrderedDays,
                                        heights = dayHeights,
                                        defaultHeightPx = defaultHeightPx,
                                        gapPx = dayGapPx,
                                    )
                                    if (to != from) {
                                        applyLiveDayReorder(dayKey, to, currentOrderedDays)
                                    }
                                }
                            },
                            onDragEnd = ::clearDayDragState,
                            onDragCancel = ::clearDayDragState,
                        )
                        FlowSwipeDeleteRow(
                            modifier = Modifier.weight(1f),
                            enabled = canRemoveDay,
                            resetKey = swipeResetKey,
                            swipeEnabled = !reorderInProgress,
                            dragFloatingActive = isDragging,
                            onDelete = {
                                viewModel.deleteDay(dayKey)
                                swipeResetKey++
                            },
                        ) {
                            RoutineDayBlock(
                                day = day,
                                dayIndex = dayIndex,
                                dayKey = dayKey,
                                expanded = expanded,
                                expandedExerciseStableKey = uiState.expandedExerciseStableKey,
                                exerciseNameSuggestions = uiState.exerciseNameSuggestions,
                                exerciseSearchResults = uiState.exerciseSearchResults,
                                onDayNameChange = { viewModel.onDayNameChange(dayKey, it) },
                                onToggleExpanded = { viewModel.toggleDayExpanded(dayKey) },
                                onAddExercise = { viewModel.addExercise(dayKey) },
                                onToggleExercise = { stableKey ->
                                    viewModel.toggleExerciseExpanded(dayKey, stableKey)
                                },
                                onExerciseNameChange = { stableKey, value ->
                                    viewModel.onExerciseSearchQueryChange(dayKey, stableKey, value)
                                },
                                onExerciseSelected = { stableKey, exerciseId, displayName ->
                                    viewModel.onExerciseSelected(dayKey, stableKey, exerciseId, displayName)
                                },
                                onCreateCustomExercise = { stableKey, displayName ->
                                    viewModel.onCreateCustomExercise(dayKey, stableKey, displayName)
                                },
                                onBrowseExercises = { stableKey ->
                                    pickerDayKey = dayKey
                                    pickerExerciseStableKey = stableKey
                                    pickerMuscle = null
                                    pickerEquipment = null
                                    pickerVisible = true
                                    viewModel.browseExercises("")
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
                    }
                }
                if (dayIndex < dayOrderKeys.lastIndex) {
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
            if (uiState.isEditMode) {
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                FlowTextAction(
                    text = "Delete routine",
                    onClick = viewModel::requestDeleteRoutine,
                    destructive = true,
                )
            }
            uiState.deleteBlockedMessage?.let { message ->
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                FlowSupportingText(message)
            }
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
    gapPx: Float,
): Int {
    if (offsetY == 0f || days.isEmpty()) return fromIndex
    var index = fromIndex
    if (offsetY > 0f) {
        var remaining = offsetY
        while (index < days.lastIndex) {
            val step = (heights[days[index].localKey] ?: defaultHeightPx) + gapPx
            if (remaining < step / 2f) break
            remaining -= step
            index++
        }
    } else {
        var remaining = -offsetY
        while (index > 0) {
            val step = (heights[days[index - 1].localKey] ?: defaultHeightPx) + gapPx
            if (remaining < step / 2f) break
            remaining -= step
            index--
        }
    }
    return index.coerceIn(0, days.lastIndex)
}

private fun daySlotHeightPx(
    orderedDays: List<GymRoutineDay>,
    index: Int,
    heights: Map<String, Float>,
    defaultHeightPx: Float,
    gapPx: Float,
): Float {
    val key = orderedDays.getOrNull(index)?.localKey ?: return defaultHeightPx + gapPx
    return (heights[key] ?: defaultHeightPx) + gapPx
}

internal fun dayDisplacementYSmooth(
    index: Int,
    fromIndex: Int,
    dragOffsetY: Float,
    orderedDays: List<GymRoutineDay>,
    heights: Map<String, Float>,
    defaultHeightPx: Float,
    gapPx: Float,
): Float {
    if (fromIndex < 0 || orderedDays.isEmpty()) return 0f
    if (index == fromIndex) return dragOffsetY

    if (dragOffsetY > 0f && index > fromIndex) {
        var threshold = 0f
        for (slotIndex in fromIndex until index) {
            threshold += daySlotHeightPx(orderedDays, slotIndex, heights, defaultHeightPx, gapPx)
        }
        val slot = daySlotHeightPx(orderedDays, index, heights, defaultHeightPx, gapPx)
        val overlap = (dragOffsetY - threshold).coerceIn(0f, slot)
        return -overlap
    }

    if (dragOffsetY < 0f && index < fromIndex) {
        var threshold = 0f
        for (slotIndex in index + 1..fromIndex) {
            threshold += daySlotHeightPx(orderedDays, slotIndex, heights, defaultHeightPx, gapPx)
        }
        val slot = daySlotHeightPx(orderedDays, index, heights, defaultHeightPx, gapPx)
        val overlap = (-dragOffsetY - threshold).coerceIn(0f, slot)
        return overlap
    }

    return 0f
}

@Composable
private fun RoutineDayBlock(
    day: GymRoutineDay,
    dayIndex: Int,
    dayKey: String,
    expanded: Boolean,
    expandedExerciseStableKey: String?,
    exerciseNameSuggestions: List<String>,
    exerciseSearchResults: List<com.deepak.flow.core.gym.GymExerciseSearchHit>,
    onDayNameChange: (String) -> Unit,
    onToggleExpanded: () -> Unit,
    onAddExercise: () -> Unit,
    onToggleExercise: (String) -> Unit,
    onExerciseNameChange: (String, String) -> Unit,
    onExerciseSelected: (String, String, String) -> Unit,
    onCreateCustomExercise: (String, String) -> Unit,
    onBrowseExercises: (String) -> Unit,
    onExerciseNoteChange: (String, String) -> Unit,
    onToggleField: (String, TrackingField) -> Unit,
    onStepSetCount: (String, Boolean) -> Unit,
    onDeleteExercise: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
                        nameSuggestions = exerciseNameSuggestions,
                        searchResults = exerciseSearchResults,
                        onToggle = { onToggleExercise(exercise.stableKey) },
                        onNameChange = { onExerciseNameChange(exercise.stableKey, it) },
                        onSelectExercise = { exerciseId, displayName ->
                            onExerciseSelected(exercise.stableKey, exerciseId, displayName)
                        },
                        onCreateCustomExercise = { displayName ->
                            onCreateCustomExercise(exercise.stableKey, displayName)
                        },
                        onBrowseExercises = { onBrowseExercises(exercise.stableKey) },
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

@Composable
private fun DayDragHandle(
    dayKey: String,
    enabled: Boolean,
    isDragging: Boolean,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)

    Box(
        modifier = Modifier
            .defaultMinSize(
                minWidth = FlowSizes.touchTarget,
                minHeight = FlowSizes.touchTarget,
            )
            .padding(end = FlowSpacing.sm),
        contentAlignment = Alignment.Center,
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
                .size(FlowSizes.iconLg)
                .then(
                    if (enabled) {
                        Modifier.pointerInput(dayKey) {
                            detectVerticalDragGestures(
                                onDragStart = { currentOnDragStart() },
                                onDragEnd = { currentOnDragEnd() },
                                onDragCancel = { currentOnDragCancel() },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    currentOnDrag(dragAmount)
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        )
    }
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
    nameSuggestions: List<String>,
    searchResults: List<com.deepak.flow.core.gym.GymExerciseSearchHit>,
    onToggle: () -> Unit,
    onNameChange: (String) -> Unit,
    onSelectExercise: (String, String) -> Unit,
    onCreateCustomExercise: (String) -> Unit,
    onBrowseExercises: () -> Unit,
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
    ExerciseNameField(
        value = exercise.name,
        onValueChange = onNameChange,
        suggestions = nameSuggestions,
        searchResults = searchResults,
        selectedExerciseId = exercise.exerciseId,
        onSelectExercise = onSelectExercise,
        onCreateCustomExercise = onCreateCustomExercise,
        onBrowseExercises = onBrowseExercises,
        placeholder = "Search exercises...",
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

@Composable
private fun RoutineEditLeaveDialog(
    canSave: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDiscard: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(FlowSurfaceRaised)
                .border(FlowSizes.hairline, FlowBorder, MaterialTheme.shapes.large)
                .padding(FlowSpacing.lg),
        ) {
            Text(
                text = "Save changes?",
                style = MaterialTheme.typography.titleLarge,
                color = FlowTextPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            Text(
                text = "You have unsaved changes to this routine.",
                style = MaterialTheme.typography.bodyMedium,
                color = FlowTextSecondary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlowTextAction(text = "Cancel", onClick = onCancel)
                Spacer(modifier = Modifier.width(FlowSpacing.lg))
                FlowTextAction(text = "Discard changes", onClick = onDiscard, destructive = true)
                Spacer(modifier = Modifier.width(FlowSpacing.lg))
                FlowTextAction(
                    text = "Save changes",
                    onClick = onSave,
                    enabled = canSave,
                )
            }
        }
    }
}
