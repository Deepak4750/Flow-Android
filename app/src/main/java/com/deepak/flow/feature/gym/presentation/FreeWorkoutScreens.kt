package com.deepak.flow.feature.gym.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.flow.app.components.FlowButton
import com.deepak.flow.app.components.FlowButtonVariant
import com.deepak.flow.app.components.FlowChip
import com.deepak.flow.app.components.FlowDialog
import com.deepak.flow.app.components.FlowHairlineDivider
import com.deepak.flow.app.components.FlowMetaText
import com.deepak.flow.app.components.FlowScreenTitle
import com.deepak.flow.app.components.FlowSectionLabel
import com.deepak.flow.app.components.FlowSupportingText
import com.deepak.flow.app.components.FlowTextAction
import com.deepak.flow.app.components.FlowTextField
import com.deepak.flow.app.components.FlowUndoBanner
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowShell
import com.deepak.flow.app.theme.FlowAccent
import com.deepak.flow.app.theme.FlowMotion
import com.deepak.flow.app.theme.FlowSizes
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextDisabled
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.app.theme.FlowTextTertiary
import com.deepak.flow.app.theme.FlowWhite
import com.deepak.flow.core.gym.GymLimits
import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.GymWorkoutExercise
import com.deepak.flow.core.gym.GymWorkoutSet
import com.deepak.flow.core.gym.TrackingField
import com.deepak.flow.core.gym.WeightUnit
import kotlinx.coroutines.delay

@Composable
fun FreeWorkoutScreen(
    viewModel: FreeWorkoutViewModel,
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

    LaunchedEffect(uiState.leaveWorkout) {
        if (uiState.leaveWorkout) onLeave()
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
            when (uiState.phase) {
                FreeWorkoutPhase.SETUP -> onLeave()
                FreeWorkoutPhase.EDIT_EXERCISE -> viewModel.cancelExerciseEditor()
                FreeWorkoutPhase.RESTING -> viewModel.skipRest()
                FreeWorkoutPhase.END_OPTIONS -> viewModel.dismissEndOptions()
                FreeWorkoutPhase.COMPLETED -> viewModel.finishAndLeave()
                FreeWorkoutPhase.SESSION -> viewModel.openEndOptions()
            }
        },
        modifier = modifier,
    ) {
        when {
            uiState.phase == FreeWorkoutPhase.SETUP && uiState.workoutId == null -> {
                FreeWorkoutSetupPane(
                    title = uiState.setupTitle,
                    onTitleChange = viewModel::onSetupTitleChange,
                    onStart = viewModel::startWorkoutFromSetup,
                )
            }
            uiState.loading -> {
                FlowScreenTitle("Free Workout")
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
                FlowSupportingText("Starting…")
            }
            uiState.phase == FreeWorkoutPhase.COMPLETED -> {
                CompletionPane(
                    summary = uiState.summary,
                    onDone = viewModel::finishAndLeave,
                )
            }
            uiState.phase == FreeWorkoutPhase.RESTING -> {
                RestPane(
                    restKind = uiState.restKind,
                    remainingLabel = uiState.restRemainingLabel,
                    currentExercise = uiState.currentExercise,
                    nextExercise = uiState.upNextExercise,
                    weightUnit = uiState.displayWeightUnit,
                    onSkip = viewModel::skipRest,
                    onMinusTen = { viewModel.addRestSeconds(-10) },
                    onAddTen = { viewModel.addRestSeconds(10) },
                )
            }
            uiState.phase == FreeWorkoutPhase.EDIT_EXERCISE -> {
                ExerciseEditorPane(
                    draft = uiState.exerciseDraft,
                    message = uiState.message,
                    onNameChange = viewModel::onExerciseNameChange,
                    onNoteChange = viewModel::onExerciseNoteChange,
                    onToggleField = viewModel::toggleTrackingField,
                    onToggleFieldsEditor = viewModel::toggleShowFieldsEditor,
                    onToggleNoteEditor = viewModel::toggleShowNoteEditor,
                    onSave = viewModel::saveExerciseEdits,
                    onCancel = viewModel::cancelExerciseEditor,
                    onDelete = {
                        uiState.exerciseDraft.editingExerciseId?.let(viewModel::deleteExercise)
                    },
                    onClearMessage = viewModel::clearMessage,
                )
            }
            uiState.phase == FreeWorkoutPhase.END_OPTIONS -> {
                EndOptionsPane(
                    stopwatchLabel = uiState.stopwatchLabel,
                    onComplete = viewModel::completeWorkout,
                    onDiscard = viewModel::discardWorkout,
                    onContinue = viewModel::dismissEndOptions,
                )
            }
            else -> {
                SessionPane(
                    uiState = uiState,
                    onFinishExercise = viewModel::finishExercise,
                    onAddNewSet = viewModel::addNewSet,
                    onSkipExercise = viewModel::skipExercise,
                    onEditExercise = viewModel::openEditExercise,
                    onSelectExercise = viewModel::selectExercise,
                    onDraftChange = viewModel::onSetDraftChange,
                    onSaveSet = viewModel::saveSet,
                    onEditSet = viewModel::openEditSet,
                    onClearEditingSet = viewModel::clearEditingSet,
                    onDeleteSet = viewModel::deleteSet,
                    onComposeNameChange = viewModel::onExerciseNameChange,
                    onComposeNoteChange = viewModel::onExerciseNoteChange,
                    onToggleComposeFields = viewModel::toggleShowFieldsEditor,
                    onToggleComposeNote = viewModel::toggleShowNoteEditor,
                    onToggleField = viewModel::toggleTrackingField,
                    onStepWeight = viewModel::stepWeight,
                    onStepReps = viewModel::stepReps,
                    onStepIncline = viewModel::stepIncline,
                    onStepResistance = viewModel::stepResistance,
                    onStepRounds = viewModel::stepRounds,
                    onEnd = viewModel::openEndOptions,
                    onClearMessage = viewModel::clearMessage,
                    onUndoDeleteSet = viewModel::undoDeleteSet,
                    onDismissSetRemovedUndo = viewModel::dismissSetRemovedUndo,
                    onUndoDeleteExercise = viewModel::undoDeleteExercise,
                    onDismissExerciseRemovedUndo = viewModel::dismissExerciseRemovedUndo,
                )
            }
        }

        when (uiState.confirm) {
            FreeWorkoutConfirm.DELETE_EXERCISE -> Unit
            FreeWorkoutConfirm.DELETE_SET -> Unit
            FreeWorkoutConfirm.DISCARD_WORKOUT -> FlowDialog(
                title = "Discard workout?",
                message = "This throws away the current session. Past workouts stay.",
                confirmText = "Discard",
                dismissText = "Cancel",
                onConfirm = viewModel::confirmPendingAction,
                onDismiss = viewModel::dismissConfirm,
                destructive = true,
            )
            null -> Unit
        }
    }
}

@Composable
private fun SessionPane(
    uiState: FreeWorkoutUiState,
    onFinishExercise: () -> Unit,
    onAddNewSet: () -> Unit,
    onSkipExercise: () -> Unit,
    onEditExercise: (GymWorkoutExercise) -> Unit,
    onSelectExercise: (Int) -> Unit,
    onDraftChange: ((SetDraft) -> SetDraft) -> Unit,
    onSaveSet: () -> Unit,
    onEditSet: (GymWorkoutExercise, GymWorkoutSet) -> Unit,
    onClearEditingSet: () -> Unit,
    onDeleteSet: (Long) -> Unit,
    onComposeNameChange: (String) -> Unit,
    onComposeNoteChange: (String) -> Unit,
    onToggleComposeFields: () -> Unit,
    onToggleComposeNote: () -> Unit,
    onToggleField: (TrackingField) -> Unit,
    onStepWeight: (Boolean) -> Unit,
    onStepReps: (Boolean) -> Unit,
    onStepIncline: (Boolean) -> Unit,
    onStepResistance: (Boolean) -> Unit,
    onStepRounds: (Boolean) -> Unit,
    onEnd: () -> Unit,
    onClearMessage: () -> Unit,
    onUndoDeleteSet: () -> Unit,
    onDismissSetRemovedUndo: () -> Unit,
    onUndoDeleteExercise: () -> Unit,
    onDismissExerciseRemovedUndo: () -> Unit,
) {
    val session = uiState.session
    val weightUnit = uiState.displayWeightUnit
    val currentIndex = session?.currentExerciseIndex ?: 0

    LaunchedEffect(uiState.message) {
        if (uiState.message != null) delayMessageClear(onClearMessage)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WorkoutPinnedHeader(
            workoutHeading = uiState.workoutHeading,
            stopwatchLabel = uiState.stopwatchLabel,
            onEnd = onEnd,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowHairlineDivider()
        Spacer(modifier = Modifier.height(FlowSpacing.lg))

        val exercises = session?.exercises.orEmpty()
        LazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(
                items = exercises,
                key = { _, exercise -> exercise.id },
            ) { index, exercise ->
                val isActive = !uiState.composingExercise && index == currentIndex
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExerciseBlock(
                        exercise = exercise,
                        isActive = isActive,
                        weightUnit = weightUnit,
                        setDraft = uiState.setDraft,
                        setEditorVisible = isActive && uiState.setEditorVisible,
                        awaitingNextAction = isActive && uiState.awaitingNextAction,
                        canSaveSet = uiState.canSaveSet,
                        message = uiState.message.takeIf { isActive },
                        onSelect = { onSelectExercise(index) },
                        onEditExercise = { onEditExercise(exercise) },
                        onEditSet = { set -> onEditSet(exercise, set) },
                        onDraftChange = onDraftChange,
                        onSaveSet = onSaveSet,
                        onClearEditingSet = onClearEditingSet,
                        onDeleteSet = onDeleteSet,
                        onAddNewSet = onAddNewSet,
                        onFinishExercise = onFinishExercise,
                        onSkipExercise = onSkipExercise.takeIf { uiState.isRoutine },
                        saveSetLabel = uiState.saveSetLabel,
                        finishExerciseLabel = uiState.finishExerciseLabel,
                        onStepWeight = onStepWeight,
                        onStepReps = onStepReps,
                        onStepIncline = onStepIncline,
                        onStepResistance = onStepResistance,
                        onStepRounds = onStepRounds,
                        showUpNext = isActive && uiState.showUpNextInSession,
                        upNextExercise = uiState.upNextExercise,
                    )
                    if (index < exercises.lastIndex || uiState.composingExercise) {
                        Spacer(modifier = Modifier.height(FlowSpacing.xl))
                        FlowHairlineDivider()
                        Spacer(modifier = Modifier.height(FlowSpacing.xl))
                    }
                }
            }
            if (uiState.composingExercise && !uiState.isRoutine) {
                item(key = "composer") {
                    InlineExerciseComposer(
                        draft = uiState.exerciseDraft,
                        setDraft = uiState.setDraft,
                        weightUnit = weightUnit,
                        setEditorVisible = uiState.setEditorVisible,
                        canSaveSet = uiState.canSaveSet,
                        message = uiState.message,
                        onNameChange = onComposeNameChange,
                        onNoteChange = onComposeNoteChange,
                        onToggleFields = onToggleComposeFields,
                        onToggleNote = onToggleComposeNote,
                        onToggleField = onToggleField,
                        onDraftChange = onDraftChange,
                        onSaveSet = onSaveSet,
                        onStepWeight = onStepWeight,
                        onStepReps = onStepReps,
                        onStepIncline = onStepIncline,
                        onStepResistance = onStepResistance,
                        onStepRounds = onStepRounds,
                    )
                }
            }
        }

        if (uiState.setRemovedUndoVisible) {
            FlowUndoBanner(
                message = "Set removed",
                onUndo = onUndoDeleteSet,
                onDismiss = onDismissSetRemovedUndo,
            )
        }
        if (uiState.exerciseRemovedUndoVisible) {
            FlowUndoBanner(
                message = "Exercise removed",
                onUndo = onUndoDeleteExercise,
                onDismiss = onDismissExerciseRemovedUndo,
            )
        }
    }
}

@Composable
private fun FreeWorkoutSetupPane(
    title: String,
    onTitleChange: (String) -> Unit,
    onStart: () -> Unit,
) {
    FlowScreenTitle("Free Workout")
    Spacer(modifier = Modifier.height(FlowSpacing.xl))
    FlowTextField(
        value = title,
        onValueChange = onTitleChange,
        placeholder = "Workout title",
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(FlowSpacing.xl))
    FlowButton(text = "Start Workout", onClick = onStart)
}

@Composable
private fun WorkoutPinnedHeader(
    workoutHeading: String,
    stopwatchLabel: String,
    onEnd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                FlowScreenTitle(
                    text = workoutHeading,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                Text(
                    text = stopwatchLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    color = FlowTextPrimary,
                )
            }
            FlowTextAction(text = "End", onClick = onEnd)
        }
    }
}

@Composable
private fun InlineExerciseComposer(
    draft: ExerciseDraft,
    setDraft: SetDraft,
    weightUnit: WeightUnit,
    setEditorVisible: Boolean,
    canSaveSet: Boolean,
    message: String?,
    onNameChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onToggleFields: () -> Unit,
    onToggleNote: () -> Unit,
    onToggleField: (TrackingField) -> Unit,
    onDraftChange: ((SetDraft) -> SetDraft) -> Unit,
    onSaveSet: () -> Unit,
    onStepWeight: (Boolean) -> Unit,
    onStepReps: (Boolean) -> Unit,
    onStepIncline: (Boolean) -> Unit,
    onStepResistance: (Boolean) -> Unit,
    onStepRounds: (Boolean) -> Unit,
) {
    FlowTextField(
        value = draft.name,
        onValueChange = onNameChange,
        placeholder = "Exercise title",
    )
    Spacer(modifier = Modifier.height(FlowSpacing.md))
    FlowMetaText(trackingSummary(draft.fields))
    Spacer(modifier = Modifier.height(FlowSpacing.xs))
    FlowTextAction(
        text = if (draft.showFieldsEditor) "Hide Fields" else "Edit Fields",
        onClick = onToggleFields,
    )
    if (draft.showFieldsEditor) {
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        TrackingFieldChips(fields = draft.fields, onToggle = onToggleField)
    }
    Spacer(modifier = Modifier.height(FlowSpacing.sm))
    NoteToggle(
        expanded = draft.showNoteEditor,
        onToggle = onToggleNote,
    )
    if (draft.showNoteEditor) {
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowTextField(
            value = draft.note,
            onValueChange = onNoteChange,
            placeholder = "Your note...",
            singleLine = false,
            minLines = 2,
        )
        FlowMetaText("${draft.note.length}/${GymLimits.NOTE_MAX_CHARS}")
    }

    if (setEditorVisible) {
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        InlineSetEditor(
            draft = setDraft,
            fields = draft.fields,
            weightUnit = weightUnit,
            onDraftChange = onDraftChange,
            onStepWeight = onStepWeight,
            onStepReps = onStepReps,
            onStepIncline = onStepIncline,
            onStepResistance = onStepResistance,
            onStepRounds = onStepRounds,
        )
        if (message != null) {
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowSupportingText(message)
        }
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowButton(
            text = "Save Set",
            onClick = onSaveSet,
            enabled = canSaveSet,
        )
    }
}

@Composable
private fun ExerciseBlock(
    exercise: GymWorkoutExercise,
    isActive: Boolean,
    weightUnit: WeightUnit,
    setDraft: SetDraft,
    setEditorVisible: Boolean,
    awaitingNextAction: Boolean,
    canSaveSet: Boolean,
    message: String?,
    onSelect: () -> Unit,
    onEditExercise: () -> Unit,
    onEditSet: (GymWorkoutSet) -> Unit,
    onDraftChange: ((SetDraft) -> SetDraft) -> Unit,
    onSaveSet: () -> Unit,
    onClearEditingSet: () -> Unit,
    onDeleteSet: (Long) -> Unit,
    onAddNewSet: () -> Unit,
    onFinishExercise: () -> Unit,
    onSkipExercise: (() -> Unit)?,
    saveSetLabel: String,
    finishExerciseLabel: String,
    onStepWeight: (Boolean) -> Unit,
    onStepReps: (Boolean) -> Unit,
    onStepIncline: (Boolean) -> Unit,
    onStepResistance: (Boolean) -> Unit,
    onStepRounds: (Boolean) -> Unit,
    showUpNext: Boolean = false,
    upNextExercise: GymWorkoutExercise? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isActive) {
                    Modifier.clickable(role = Role.Button, onClick = onSelect)
                } else {
                    Modifier
                },
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleLarge,
                color = FlowTextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            FlowMetaText(
                if (exercise.skipped) {
                    "Skipped"
                } else {
                    trackingSummary(exercise.trackingFields)
                },
            )
        }
        FlowTextAction(text = "Edit", onClick = onEditExercise)
    }

    if (exercise.note.isNotBlank()) {
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        NoteWithLinks(note = exercise.note)
    }

    Spacer(modifier = Modifier.height(FlowSpacing.md))
    val savedSets = exercise.sets.filter { it.saved }
    savedSets.forEach { set ->
        SavedSetRow(
            set = set,
            fields = exercise.trackingFields,
            weightUnit = weightUnit,
            selected = isActive && setDraft.setId == set.id,
            canDelete = isActive,
            onClick = if (isActive) {
                {
                    onSelect()
                    onEditSet(set)
                }
            } else {
                {}
            },
            onDelete = if (isActive) {
                { onDeleteSet(set.id) }
            } else {
                null
            },
            clickable = isActive,
        )
    }

    if (isActive && setEditorVisible) {
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        InlineSetEditor(
            draft = setDraft,
            fields = exercise.trackingFields,
            weightUnit = weightUnit,
            onDraftChange = onDraftChange,
            onStepWeight = onStepWeight,
            onStepReps = onStepReps,
            onStepIncline = onStepIncline,
            onStepResistance = onStepResistance,
            onStepRounds = onStepRounds,
        )
        if (message != null) {
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowSupportingText(message)
        }
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowButton(
            text = saveSetLabel,
            onClick = onSaveSet,
            enabled = canSaveSet,
        )
        if (setDraft.setId != null) {
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowTextAction(text = "Cancel edit", onClick = onClearEditingSet)
        }
        if (onSkipExercise != null) {
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowTextAction(text = "Skip Exercise", onClick = onSkipExercise)
        }
        if (showUpNext && upNextExercise != null) {
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FlowSectionLabel("Up next")
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            Text(
                text = upNextExercise.name,
                style = MaterialTheme.typography.titleMedium,
                color = FlowTextPrimary,
            )
            if (upNextExercise.plannedSetCount > 0) {
                Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                FlowMetaText("${upNextExercise.plannedSetCount} sets")
            }
        }
    }

    if (isActive && awaitingNextAction) {
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowButton(
            text = "Add New Set",
            onClick = onAddNewSet,
            leadingIcon = Icons.Default.Add,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowButton(
            text = finishExerciseLabel,
            onClick = onFinishExercise,
            variant = FlowButtonVariant.Secondary,
        )
        if (onSkipExercise != null) {
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowTextAction(text = "Skip Exercise", onClick = onSkipExercise)
        }
    }
}

@Composable
private fun SavedSetRow(
    set: GymWorkoutSet,
    fields: Set<TrackingField>,
    weightUnit: WeightUnit,
    selected: Boolean,
    onClick: () -> Unit,
    clickable: Boolean = true,
    canDelete: Boolean = false,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FlowSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            if (canDelete && onDelete != null) {
                Text(
                    text = "−",
                    style = MaterialTheme.typography.labelLarge,
                    color = FlowTextSecondary,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onDelete)
                        .padding(end = FlowSpacing.sm),
                )
            }
            Text(
                text = "SET ${set.setNumber}",
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) FlowTextPrimary else FlowTextSecondary,
                modifier = Modifier.then(
                    if (clickable) {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
            )
        }
        Text(
            text = GymLogic.formatSetSummary(set, fields, weightUnit),
            style = MaterialTheme.typography.titleMedium,
            color = FlowTextPrimary,
            modifier = Modifier.then(
                if (clickable) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            ),
        )
    }
    FlowHairlineDivider()
}

@Composable
private fun InlineSetEditor(
    draft: SetDraft,
    fields: Set<TrackingField>,
    weightUnit: WeightUnit,
    onDraftChange: ((SetDraft) -> SetDraft) -> Unit,
    onStepWeight: (Boolean) -> Unit,
    onStepReps: (Boolean) -> Unit,
    onStepIncline: (Boolean) -> Unit,
    onStepResistance: (Boolean) -> Unit,
    onStepRounds: (Boolean) -> Unit,
) {
    FlowSectionLabel("SET ${draft.setNumber}")
    Spacer(modifier = Modifier.height(FlowSpacing.sm))

    val hasWeight = TrackingField.WEIGHT in fields
    val hasReps = TrackingField.REPS in fields
    if (hasWeight || hasReps) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (hasWeight) {
                    SteppedNumberField(
                        value = draft.weight,
                        onValueChange = { value ->
                            onDraftChange { it.copy(weight = filterDecimal(value)) }
                        },
                        onMinus = { onStepWeight(false) },
                        onPlus = { onStepWeight(true) },
                        suffix = weightUnit.label.lowercase(),
                        keyboardType = KeyboardType.Decimal,
                        inputWidth = 64.dp,
                    )
                }
            }
            if (hasWeight && hasReps) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "×",
                        style = MaterialTheme.typography.titleLarge,
                        color = FlowTextSecondary,
                    )
                }
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (hasReps) {
                    SteppedNumberField(
                        value = draft.reps,
                        onValueChange = { value ->
                            onDraftChange { it.copy(reps = filterInt(value)) }
                        },
                        onMinus = { onStepReps(false) },
                        onPlus = { onStepReps(true) },
                        suffix = "reps",
                        keyboardType = KeyboardType.Number,
                        inputWidth = 48.dp,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
    }

    fields.filterNot { it == TrackingField.WEIGHT || it == TrackingField.REPS }.forEach { field ->
        when (field) {
            TrackingField.DURATION -> {
                Row(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
                    CompactNumberField(
                        value = draft.durationMinutes,
                        onValueChange = { value ->
                            onDraftChange { it.copy(durationMinutes = filterInt(value)) }
                        },
                        placeholder = "min",
                        suffix = "min",
                        keyboardType = KeyboardType.Number,
                        inputWidth = 64.dp,
                    )
                    CompactNumberField(
                        value = draft.durationSeconds,
                        onValueChange = { value ->
                            onDraftChange { it.copy(durationSeconds = filterDurationSeconds(value)) }
                        },
                        placeholder = "sec",
                        suffix = "sec",
                        keyboardType = KeyboardType.Number,
                        inputWidth = 56.dp,
                    )
                }
            }
            TrackingField.DISTANCE -> CompactNumberField(
                value = draft.distance,
                onValueChange = { value ->
                    onDraftChange { it.copy(distance = filterDecimal(value)) }
                },
                placeholder = "0",
                suffix = "km",
                keyboardType = KeyboardType.Decimal,
                inputWidth = 72.dp,
            )
            TrackingField.SPEED -> CompactNumberField(
                value = draft.speed,
                onValueChange = { value ->
                    onDraftChange { it.copy(speed = filterDecimal(value)) }
                },
                placeholder = "0",
                suffix = "km/h",
                keyboardType = KeyboardType.Decimal,
                inputWidth = 72.dp,
            )
            TrackingField.INCLINE -> {
                FlowSectionLabel("Incline")
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                SteppedNumberField(
                    value = draft.incline,
                    onValueChange = { value ->
                        onDraftChange { it.copy(incline = filterInt(value)) }
                    },
                    onMinus = { onStepIncline(false) },
                    onPlus = { onStepIncline(true) },
                    suffix = null,
                    keyboardType = KeyboardType.Number,
                    inputWidth = 56.dp,
                )
            }
            TrackingField.RESISTANCE -> {
                FlowSectionLabel("Resistance")
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                SteppedNumberField(
                    value = draft.resistance,
                    onValueChange = { value ->
                        onDraftChange { it.copy(resistance = filterInt(value)) }
                    },
                    onMinus = { onStepResistance(false) },
                    onPlus = { onStepResistance(true) },
                    suffix = null,
                    keyboardType = KeyboardType.Number,
                    inputWidth = 56.dp,
                )
            }
            TrackingField.ROUNDS -> {
                FlowSectionLabel("Rounds")
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                SteppedNumberField(
                    value = draft.rounds,
                    onValueChange = { value ->
                        onDraftChange { it.copy(rounds = filterInt(value)) }
                    },
                    onMinus = { onStepRounds(false) },
                    onPlus = { onStepRounds(true) },
                    suffix = null,
                    keyboardType = KeyboardType.Number,
                    inputWidth = 56.dp,
                )
            }
            TrackingField.WEIGHT, TrackingField.REPS -> Unit
        }
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                role = Role.Checkbox,
                onClick = { onDraftChange { it.copy(failure = !it.failure) } },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
    ) {
        Text(
            text = if (draft.failure) "●" else "○",
            style = MaterialTheme.typography.titleMedium,
            color = FlowTextPrimary,
        )
        Text(
            text = "HIT FAILURE",
            style = MaterialTheme.typography.bodyLarge,
            color = FlowTextPrimary,
        )
    }
}

@Composable
private fun SteppedNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    suffix: String?,
    keyboardType: KeyboardType,
    inputWidth: Dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xxs),
    ) {
        FlowTextAction(text = "−", onClick = onMinus)
        CompactValueField(
            value = value,
            onValueChange = onValueChange,
            keyboardType = keyboardType,
            width = inputWidth,
        )
        FlowTextAction(text = "+", onClick = onPlus)
        if (!suffix.isNullOrBlank()) {
            Text(
                text = suffix,
                style = MaterialTheme.typography.titleMedium,
                color = FlowTextSecondary,
                modifier = Modifier.padding(start = FlowSpacing.xxs),
            )
        }
    }
}

@Composable
private fun CompactNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    suffix: String,
    keyboardType: KeyboardType,
    inputWidth: Dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xxs),
    ) {
        CompactValueField(
            value = value,
            onValueChange = onValueChange,
            keyboardType = keyboardType,
            width = inputWidth,
            placeholder = placeholder,
        )
        Text(
            text = suffix,
            style = MaterialTheme.typography.labelLarge,
            color = FlowTextSecondary,
        )
    }
}

@Composable
private fun CompactValueField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    width: Dp,
    placeholder: String = "0",
) {
    val fieldStyle = MaterialTheme.typography.titleLarge.copy(
        color = FlowTextPrimary,
        textAlign = TextAlign.Center,
    )
    Column(
        modifier = Modifier
            .width(width)
            .widthIn(min = 48.dp, max = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FlowSizes.touchTarget),
            textStyle = fieldStyle,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(FlowWhite),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = fieldStyle.copy(color = FlowTextDisabled),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    inner()
                }
            },
        )
        FlowHairlineDivider()
    }
}

@Composable
internal fun TrackingFieldChips(
    fields: Set<TrackingField>,
    onToggle: (TrackingField) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xs),
    ) {
        TrackingField.entries.forEach { field ->
            FlowChip(
                label = field.label,
                selected = field in fields,
                onClick = { onToggle(field) },
            )
        }
    }
}

@Composable
private fun ExerciseEditorPane(
    draft: ExerciseDraft,
    message: String?,
    onNameChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onToggleField: (TrackingField) -> Unit,
    onToggleFieldsEditor: () -> Unit,
    onToggleNoteEditor: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onClearMessage: () -> Unit,
) {
    LaunchedEffect(message) {
        if (message != null) delayMessageClear(onClearMessage)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        FlowScreenTitle("Edit Exercise")
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowTextField(
            value = draft.name,
            onValueChange = onNameChange,
            placeholder = "Exercise title",
        )
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowSectionLabel("Tracking")
        Spacer(modifier = Modifier.height(FlowSpacing.xs))
        FlowSupportingText(trackingSummary(draft.fields))
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowTextAction(
            text = if (draft.showFieldsEditor) "Hide Fields" else "Edit Fields",
            onClick = onToggleFieldsEditor,
        )
        if (draft.showFieldsEditor) {
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowSupportingText("At least one field stays on.")
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            TrackingFieldChips(fields = draft.fields, onToggle = onToggleField)
        }

        Spacer(modifier = Modifier.height(FlowSpacing.md))
        NoteToggle(
            expanded = draft.showNoteEditor,
            onToggle = onToggleNoteEditor,
        )
        if (draft.showNoteEditor) {
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowTextField(
                value = draft.note,
                onValueChange = onNoteChange,
                placeholder = "Your note...",
                singleLine = false,
                minLines = 2,
            )
            FlowMetaText("${draft.note.length}/${GymLimits.NOTE_MAX_CHARS}")
        }

        if (message != null) {
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowSupportingText(message, color = FlowTextTertiary)
        }
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        FlowButton(text = "Save", onClick = onSave)
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowButton(
            text = "Cancel",
            onClick = onCancel,
            variant = FlowButtonVariant.Secondary,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowTextAction(text = "Delete Exercise", onClick = onDelete, destructive = true)
    }
}

@Composable
private fun RestPane(
    restKind: com.deepak.flow.core.gym.GymRestKind,
    remainingLabel: String,
    currentExercise: GymWorkoutExercise?,
    nextExercise: GymWorkoutExercise?,
    weightUnit: WeightUnit,
    onSkip: () -> Unit,
    onMinusTen: () -> Unit,
    onAddTen: () -> Unit,
) {
    val isExerciseRest = restKind == com.deepak.flow.core.gym.GymRestKind.EXERCISE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
    ) {
        FlowSectionLabel(if (isExerciseRest) "Exercise Rest" else "Set Rest")
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        Text(
            text = remainingLabel,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 72.sp,
                lineHeight = 80.sp,
            ),
            color = FlowTextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                space = FlowSpacing.md,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowButton(
                text = "−10",
                onClick = onMinusTen,
                variant = FlowButtonVariant.Secondary,
                fillWidth = false,
            )
            FlowButton(
                text = "Skip",
                onClick = onSkip,
                fillWidth = false,
            )
            FlowButton(
                text = "+10",
                onClick = onAddTen,
                variant = FlowButtonVariant.Secondary,
                fillWidth = false,
            )
        }
        if (isExerciseRest && nextExercise != null) {
            Spacer(modifier = Modifier.height(FlowSpacing.xxl))
            FlowHairlineDivider()
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FlowSectionLabel("Next")
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            Text(
                text = nextExercise.name,
                style = MaterialTheme.typography.headlineSmall,
                color = FlowTextPrimary,
            )
            if (nextExercise.plannedSetCount > 0) {
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                FlowMetaText("${nextExercise.plannedSetCount} sets")
            }
        } else if (!isExerciseRest && currentExercise != null) {
            Spacer(modifier = Modifier.height(FlowSpacing.xxl))
            FlowHairlineDivider()
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FlowSectionLabel("Current exercise")
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            Text(
                text = currentExercise.name,
                style = MaterialTheme.typography.headlineSmall,
                color = FlowTextPrimary,
            )
            val saved = currentExercise.sets.filter { it.saved }
            if (saved.isNotEmpty()) {
                Spacer(modifier = Modifier.height(FlowSpacing.md))
                saved.forEach { set ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = FlowSpacing.xs),
                    ) {
                        FlowMetaText("Set ${set.setNumber}")
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = GymLogic.formatCompactSetLine(
                                set,
                                currentExercise.trackingFields,
                                weightUnit,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = FlowTextPrimary,
                        )
                        if (set.failure) {
                            Spacer(modifier = Modifier.height(2.dp))
                            FlowMetaText("Failure")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NoteToggle(
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(FlowMotion.STANDARD),
        label = "noteToggleRotation",
    )
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .heightIn(min = FlowSizes.touchTarget)
            .clickable(
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle()
                },
            )
            .padding(vertical = FlowSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xxs),
    ) {
        Text(
            text = "+",
            style = MaterialTheme.typography.labelLarge,
            color = FlowTextPrimary,
            modifier = Modifier.graphicsLayer { rotationZ = rotation },
        )
        Text(
            text = "NOTE",
            style = MaterialTheme.typography.labelLarge,
            color = FlowTextPrimary,
        )
    }
}

@Composable
private fun EndOptionsPane(
    stopwatchLabel: String,
    onComplete: () -> Unit,
    onDiscard: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FlowScreenTitle("End Workout")
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowSupportingText(stopwatchLabel)
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        FlowButton(text = "End & Save", onClick = onComplete)
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowButton(
            text = "Cancel",
            onClick = onContinue,
            variant = FlowButtonVariant.Secondary,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowTextAction(text = "Discard", onClick = onDiscard, destructive = true)
    }
}

@Composable
private fun CompletionPane(
    summary: com.deepak.flow.core.gym.GymWorkoutSummary?,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FlowScreenTitle("Workout Complete")
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        if (summary != null) {
            SummaryLine(
                label = "Duration",
                value = GymLogic.formatSummaryDuration(summary.durationSeconds),
            )
            SummaryLine(label = "Exercises", value = summary.exerciseCount.toString())
            SummaryLine(label = "Sets", value = summary.setCount.toString())
            summary.volumeKg?.let { volume ->
                SummaryLine(
                    label = "Volume",
                    value = "${GymLogic.formatNumber(volume)} kg",
                )
            }
        }
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        FlowButton(text = "Done", onClick = onDone)
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FlowSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        FlowMetaText(label)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = FlowTextPrimary,
        )
    }
}

@Composable
private fun NoteWithLinks(note: String) {
    val context = LocalContext.current
    val spans = GymLogic.findNoteLinks(note)
    if (spans.isEmpty()) {
        FlowSupportingText(note)
        return
    }
    val annotated = buildAnnotatedString {
        var cursor = 0
        spans.forEach { span ->
            if (span.start > cursor) {
                append(note.substring(cursor, span.start))
            }
            pushStringAnnotation(tag = "URL", annotation = span.url)
            withStyle(
                SpanStyle(
                    color = FlowAccent,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(note.substring(span.start, span.end))
            }
            pop()
            cursor = span.end
        }
        if (cursor < note.length) {
            append(note.substring(cursor))
        }
    }
    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(color = FlowTextSecondary),
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset)
                .firstOrNull()
                ?.let { annotation ->
                    val uri = Uri.parse(annotation.item)
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
        },
    )
}

internal fun trackingSummary(fields: Set<TrackingField>): String {
    if (fields.isEmpty()) return "Weight + Reps"
    return fields.sortedBy { it.ordinal }.joinToString(" + ") { it.label }
}

private suspend fun delayMessageClear(onClear: () -> Unit) {
    delay(2_500)
    onClear()
}

private fun filterDecimal(raw: String): String {
    val builder = StringBuilder()
    var seenDot = false
    for (char in raw) {
        when {
            char.isDigit() -> builder.append(char)
            (char == '.' || char == ',') && !seenDot -> {
                seenDot = true
                builder.append('.')
            }
        }
    }
    return builder.toString()
}

private fun filterInt(raw: String): String = raw.filter { it.isDigit() }

private fun filterDurationSeconds(raw: String): String {
    val digits = filterInt(raw)
    if (digits.isEmpty()) return ""
    val value = digits.toIntOrNull()?.coerceIn(0, 59) ?: return digits.take(2)
    return value.toString()
}
