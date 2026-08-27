package com.deepak.flow.feature.gym.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
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
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowShell
import com.deepak.flow.app.theme.FlowAccent
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.app.theme.FlowTextTertiary
import com.deepak.flow.core.gym.GymLimits
import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.GymWorkoutExercise
import com.deepak.flow.core.gym.GymWorkoutSet
import com.deepak.flow.core.gym.TrackingField
import com.deepak.flow.core.gym.WeightUnit
import androidx.compose.ui.text.input.KeyboardType
@Composable
fun FreeWorkoutScreen(
    viewModel: FreeWorkoutViewModel,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
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
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onDestinationClick = onDestinationClick,
        onBack = {
            when (uiState.phase) {
                FreeWorkoutPhase.ADD_EXERCISE,
                FreeWorkoutPhase.EDIT_EXERCISE,
                -> viewModel.cancelExerciseEditor()
                FreeWorkoutPhase.EDIT_SET -> viewModel.cancelSetEditor()
                FreeWorkoutPhase.ASK_REST -> viewModel.skipRest()
                FreeWorkoutPhase.RESTING -> viewModel.skipRest()
                FreeWorkoutPhase.END_OPTIONS -> viewModel.dismissEndOptions()
                FreeWorkoutPhase.COMPLETED -> viewModel.finishAndLeave()
                FreeWorkoutPhase.SESSION -> viewModel.openEndOptions()
            }
        },
        modifier = modifier,
    ) {
        when {
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
                    remainingLabel = uiState.restRemainingLabel,
                    upNext = uiState.upNextLabel,
                    exercise = uiState.upNextExercise,
                    weightUnit = uiState.session?.weightUnit ?: WeightUnit.KG,
                    onSkip = viewModel::skipRest,
                    onAddTen = { viewModel.addRestSeconds(10) },
                    onFinish = viewModel::skipRest,
                )
            }
            uiState.phase == FreeWorkoutPhase.ASK_REST -> {
                AskRestPane(
                    seconds = uiState.restSecondsChoice,
                    onSecondsChange = viewModel::chooseRestSeconds,
                    onRest = viewModel::confirmRest,
                    onSkip = viewModel::skipRest,
                )
            }
            uiState.phase == FreeWorkoutPhase.ADD_EXERCISE ||
                uiState.phase == FreeWorkoutPhase.EDIT_EXERCISE -> {
                ExerciseEditorPane(
                    draft = uiState.exerciseDraft,
                    isEdit = uiState.phase == FreeWorkoutPhase.EDIT_EXERCISE,
                    message = uiState.message,
                    onNameChange = viewModel::onExerciseNameChange,
                    onNoteChange = viewModel::onExerciseNoteChange,
                    onToggleField = viewModel::toggleTrackingField,
                    onSave = viewModel::saveExercise,
                    onCancel = viewModel::cancelExerciseEditor,
                    onDelete = {
                        uiState.exerciseDraft.editingExerciseId?.let(viewModel::deleteExercise)
                    },
                    onClearMessage = viewModel::clearMessage,
                )
            }
            uiState.phase == FreeWorkoutPhase.EDIT_SET -> {
                val exercise = uiState.currentExercise
                if (exercise != null) {
                    SetEditorPane(
                        exercise = exercise,
                        draft = uiState.setDraft,
                        weightUnit = uiState.session?.weightUnit ?: WeightUnit.KG,
                        message = uiState.message,
                        onDraftChange = viewModel::onSetDraftChange,
                        onSave = viewModel::saveSet,
                        onCancel = viewModel::cancelSetEditor,
                        onDelete = {
                            uiState.setDraft.setId?.let(viewModel::deleteSet)
                        },
                        onClearMessage = viewModel::clearMessage,
                    )
                }
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
                    onAddExercise = viewModel::openAddExercise,
                    onEditExercise = viewModel::openEditExercise,
                    onSelectExercise = viewModel::selectExercise,
                    onWeightUnit = viewModel::setWeightUnit,
                    onNewSet = viewModel::openNewSet,
                    onEditSet = viewModel::openEditSet,
                    onEnd = viewModel::openEndOptions,
                )
            }
        }

        when (uiState.confirm) {
            FreeWorkoutConfirm.DELETE_EXERCISE -> FlowDialog(
                title = "Delete exercise?",
                message = "This removes it from the current workout only.",
                confirmText = "Delete",
                dismissText = "Cancel",
                onConfirm = viewModel::confirmPendingAction,
                onDismiss = viewModel::dismissConfirm,
                destructive = true,
            )
            FreeWorkoutConfirm.DELETE_SET -> FlowDialog(
                title = "Delete set?",
                message = "This set will be removed from the current workout.",
                confirmText = "Delete",
                dismissText = "Cancel",
                onConfirm = viewModel::confirmPendingAction,
                onDismiss = viewModel::dismissConfirm,
                destructive = true,
            )
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
    onAddExercise: () -> Unit,
    onEditExercise: (GymWorkoutExercise) -> Unit,
    onSelectExercise: (Int) -> Unit,
    onWeightUnit: (WeightUnit) -> Unit,
    onNewSet: () -> Unit,
    onEditSet: (GymWorkoutSet) -> Unit,
    onEnd: () -> Unit,
) {
    val session = uiState.session
    val exercise = uiState.currentExercise

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        FlowScreenTitle("Free Workout")
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        Text(
            text = uiState.stopwatchLabel,
            style = MaterialTheme.typography.headlineMedium,
            color = FlowTextPrimary,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        Row(
            horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowMetaText("Unit")
            WeightUnit.entries.forEach { unit ->
                FlowChip(
                    label = unit.label,
                    selected = session?.weightUnit == unit,
                    onClick = { onWeightUnit(unit) },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            FlowTextAction(text = "End", onClick = onEnd)
        }

        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowHairlineDivider()
        Spacer(modifier = Modifier.height(FlowSpacing.lg))

        if (session == null || session.exercises.isEmpty()) {
            FlowSupportingText("Add an exercise to start logging.")
            Spacer(modifier = Modifier.height(FlowSpacing.xl))
            FlowButton(
                text = "Add Exercise",
                onClick = onAddExercise,
                leadingIcon = Icons.Default.Add,
            )
            return@Column
        }

        if (session.exercises.size > 1) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
            ) {
                session.exercises.forEachIndexed { index, item ->
                    FlowChip(
                        label = item.name,
                        selected = index == session.currentExerciseIndex,
                        onClick = { onSelectExercise(index) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
        }

        if (exercise != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = exercise.name.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = FlowTextPrimary,
                    modifier = Modifier.weight(1f),
                )
                FlowTextAction(text = "Edit", onClick = { onEditExercise(exercise) })
            }
            if (exercise.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                NoteWithLinks(note = exercise.note)
            }
            Spacer(modifier = Modifier.height(FlowSpacing.md))
            FlowSectionLabel("Sets")
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            val savedSets = exercise.sets.filter { it.saved }
            if (savedSets.isEmpty()) {
                FlowSupportingText("No sets yet.")
            } else {
                savedSets.forEach { set ->
                    SetRow(
                        set = set,
                        fields = exercise.trackingFields,
                        weightUnit = session.weightUnit,
                        onClick = { onEditSet(set) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(FlowSpacing.xl))
            FlowButton(
                text = "Save Set",
                onClick = onNewSet,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowButton(
                text = "Add Exercise",
                onClick = onAddExercise,
                variant = FlowButtonVariant.Secondary,
                leadingIcon = Icons.Default.Add,
            )
        }
    }
}

@Composable
private fun SetRow(
    set: GymWorkoutSet,
    fields: Set<TrackingField>,
    weightUnit: WeightUnit,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = FlowSpacing.sm),
    ) {
        Text(
            text = "Set ${set.setNumber}",
            style = MaterialTheme.typography.labelLarge,
            color = FlowTextSecondary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = GymLogic.formatSetSummary(set, fields, weightUnit),
            style = MaterialTheme.typography.titleMedium,
            color = FlowTextPrimary,
        )
        FlowHairlineDivider(modifier = Modifier.padding(top = FlowSpacing.sm))
    }
}

@Composable
private fun ExerciseEditorPane(
    draft: ExerciseDraft,
    isEdit: Boolean,
    message: String?,
    onNameChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onToggleField: (TrackingField) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onClearMessage: () -> Unit,
) {
    LaunchedEffect(message) {
        if (message != null) {
            delayMessageClear(onClearMessage)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        FlowScreenTitle(if (isEdit) "Edit Exercise" else "Add Exercise")
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowSectionLabel("Name")
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowTextField(
            value = draft.name,
            onValueChange = onNameChange,
            placeholder = "Exercise name",
        )
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        FlowSectionLabel("Track")
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowSupportingText("Pick at least one.")
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        TrackingField.entries.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
            ) {
                row.forEach { field ->
                    FlowChip(
                        label = field.label,
                        selected = field in draft.fields,
                        onClick = { onToggleField(field) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
        }
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        FlowSectionLabel("Note")
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowTextField(
            value = draft.note,
            onValueChange = onNoteChange,
            placeholder = "Optional · links work",
            singleLine = false,
            minLines = 3,
        )
        FlowMetaText("${draft.note.length}/${GymLimits.NOTE_MAX_CHARS}")
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
        if (isEdit) {
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FlowTextAction(text = "Delete Exercise", onClick = onDelete, destructive = true)
        }
    }
}

@Composable
private fun SetEditorPane(
    exercise: GymWorkoutExercise,
    draft: SetDraft,
    weightUnit: WeightUnit,
    message: String?,
    onDraftChange: ((SetDraft) -> SetDraft) -> Unit,
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
        FlowScreenTitle(exercise.name)
        Spacer(modifier = Modifier.height(FlowSpacing.xs))
        FlowSectionLabel("Set number")
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowTextField(
            value = draft.setNumber.toString(),
            onValueChange = { value ->
                val number = filterInt(value).toIntOrNull()?.coerceAtLeast(1) ?: 1
                onDraftChange { it.copy(setNumber = number) }
            },
            placeholder = "1",
            keyboardType = KeyboardType.Number,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        exercise.trackingFields.forEach { field ->
            when (field) {
                TrackingField.WEIGHT -> {
                    FlowSectionLabel("Weight (${weightUnit.label})")
                    Spacer(modifier = Modifier.height(FlowSpacing.sm))
                    FlowTextField(
                        value = draft.weight,
                        onValueChange = { value ->
                            onDraftChange { it.copy(weight = filterDecimal(value)) }
                        },
                        placeholder = "0",
                        keyboardType = KeyboardType.Decimal,
                    )
                }
                TrackingField.REPS -> {
                    FlowSectionLabel("Reps")
                    Spacer(modifier = Modifier.height(FlowSpacing.sm))
                    FlowTextField(
                        value = draft.reps,
                        onValueChange = { value ->
                            onDraftChange { it.copy(reps = filterInt(value)) }
                        },
                        placeholder = "0",
                        keyboardType = KeyboardType.Number,
                    )
                }
                TrackingField.DURATION -> {
                    FlowSectionLabel("Duration")
                    Spacer(modifier = Modifier.height(FlowSpacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
                        FlowTextField(
                            value = draft.durationMinutes,
                            onValueChange = { value ->
                                onDraftChange { it.copy(durationMinutes = filterInt(value)) }
                            },
                            placeholder = "min",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                        )
                        FlowTextField(
                            value = draft.durationSeconds,
                            onValueChange = { value ->
                                onDraftChange { it.copy(durationSeconds = filterInt(value)) }
                            },
                            placeholder = "sec",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                TrackingField.DISTANCE -> NumberField(
                    label = "Distance (km)",
                    value = draft.distance,
                    onValueChange = { value -> onDraftChange { it.copy(distance = value) } },
                )
                TrackingField.SPEED -> NumberField(
                    label = "Speed (km/h)",
                    value = draft.speed,
                    onValueChange = { value -> onDraftChange { it.copy(speed = value) } },
                )
                TrackingField.INCLINE -> NumberField(
                    label = "Incline",
                    value = draft.incline,
                    onValueChange = { value -> onDraftChange { it.copy(incline = value) } },
                )
                TrackingField.RESISTANCE -> NumberField(
                    label = "Resistance",
                    value = draft.resistance,
                    onValueChange = { value -> onDraftChange { it.copy(resistance = value) } },
                )
                TrackingField.ROUNDS -> {
                    FlowSectionLabel("Rounds")
                    Spacer(modifier = Modifier.height(FlowSpacing.sm))
                    FlowTextField(
                        value = draft.rounds,
                        onValueChange = { value ->
                            onDraftChange { it.copy(rounds = filterInt(value)) }
                        },
                        placeholder = "0",
                        keyboardType = KeyboardType.Number,
                    )
                }
            }
            Spacer(modifier = Modifier.height(FlowSpacing.md))
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
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Failure",
                style = MaterialTheme.typography.bodyLarge,
                color = FlowTextPrimary,
            )
            FlowMetaText(if (draft.failure) "On" else "Off")
        }
        if (message != null) {
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            FlowSupportingText(message)
        }
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        FlowButton(text = "Save Set", onClick = onSave)
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowButton(
            text = "Cancel",
            onClick = onCancel,
            variant = FlowButtonVariant.Secondary,
        )
        if (draft.setId != null) {
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            FlowTextAction(text = "Delete Set", onClick = onDelete, destructive = true)
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    FlowSectionLabel(label)
    Spacer(modifier = Modifier.height(FlowSpacing.sm))
    FlowTextField(
        value = value,
        onValueChange = { onValueChange(filterDecimal(it)) },
        placeholder = "0",
        keyboardType = KeyboardType.Decimal,
    )
}

@Composable
private fun AskRestPane(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    onRest: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FlowScreenTitle("Rest?")
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        FlowSupportingText("$seconds seconds")
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
        ) {
            listOf(10, 30, 60, 90, 120).forEach { option ->
                FlowChip(
                    label = "${option}s",
                    selected = seconds == option,
                    onClick = { onSecondsChange(option) },
                )
            }
        }
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
            FlowChip(
                label = "-10s",
                selected = false,
                onClick = { onSecondsChange(seconds - 10) },
            )
            FlowChip(
                label = "+10s",
                selected = false,
                onClick = { onSecondsChange(seconds + 10) },
            )
        }
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        FlowButton(text = "Rest", onClick = onRest)
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowButton(
            text = "Skip",
            onClick = onSkip,
            variant = FlowButtonVariant.Secondary,
        )
    }
}

@Composable
private fun RestPane(
    remainingLabel: String,
    upNext: String,
    exercise: GymWorkoutExercise?,
    weightUnit: WeightUnit,
    onSkip: () -> Unit,
    onAddTen: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        FlowSectionLabel("Rest")
        Spacer(modifier = Modifier.height(FlowSpacing.md))
        Text(
            text = remainingLabel,
            style = MaterialTheme.typography.displaySmall,
            color = FlowTextPrimary,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.xxl))
        FlowSectionLabel("Up next")
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        Text(
            text = upNext.uppercase(),
            style = MaterialTheme.typography.headlineSmall,
            color = FlowTextPrimary,
        )
        if (exercise != null) {
            val saved = exercise.sets.filter { it.saved }
            if (saved.isNotEmpty()) {
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                saved.forEach { set ->
                    Text(
                        text = GymLogic.formatCompactSetLine(
                            set,
                            exercise.trackingFields,
                            weightUnit,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = FlowTextSecondary,
                    )
                }
            }
            if (exercise.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(FlowSpacing.md))
                NoteWithLinks(note = exercise.note)
            }
        }
        Spacer(modifier = Modifier.height(FlowSpacing.xxl))
        FlowButton(text = "+10 seconds", onClick = onAddTen)
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowButton(
            text = "Skip",
            onClick = onSkip,
            variant = FlowButtonVariant.Secondary,
        )
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowTextAction(text = "Finish rest", onClick = onFinish)
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
        FlowButton(text = "Save Workout", onClick = onComplete)
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        FlowButton(
            text = "Keep Going",
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
        FlowScreenTitle("Done")
        Spacer(modifier = Modifier.height(FlowSpacing.xl))
        if (summary != null) {
            SummaryLine("Duration", GymLogic.formatSummaryDuration(summary.durationSeconds))
            SummaryLine(
                "Exercises",
                summary.exerciseCount.toString(),
            )
            SummaryLine("Sets", summary.setCount.toString())
            GymLogic.formatVolumeKg(summary.volumeKg)?.let { volume ->
                SummaryLine("Volume", volume)
            }
        }
        Spacer(modifier = Modifier.height(FlowSpacing.xxl))
        FlowButton(text = "Done", onClick = onDone)
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FlowSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = FlowTextSecondary)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = FlowTextPrimary)
    }
    FlowHairlineDivider()
}

@Composable
private fun NoteWithLinks(note: String) {
    val context = LocalContext.current
    val links = GymLogic.findNoteLinks(note)
    if (links.isEmpty()) {
        FlowSupportingText(note)
        return
    }
    val annotated = buildAnnotatedString {
        var cursor = 0
        links.forEach { link ->
            if (cursor < link.start) {
                append(note.substring(cursor, link.start))
            }
            pushStringAnnotation(tag = "URL", annotation = link.url)
            withStyle(
                SpanStyle(
                    color = FlowAccent,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(note.substring(link.start, link.end))
            }
            pop()
            cursor = link.end
        }
        if (cursor < note.length) append(note.substring(cursor))
    }
    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(color = FlowTextSecondary),
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset)
                .firstOrNull()
                ?.let { annotation ->
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)),
                        )
                    }
                }
        },
    )
}

private suspend fun delayMessageClear(onClear: () -> Unit) {
    kotlinx.coroutines.delay(2_500)
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

