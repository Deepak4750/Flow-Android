package com.deepak.flow.feature.history.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.deepak.flow.app.components.FlowSwipeDeleteRow
import com.deepak.flow.app.components.FlowTextAction
import com.deepak.flow.app.components.FlowTextField
import com.deepak.flow.app.navigation.FlowDrawerDestination
import com.deepak.flow.app.navigation.FlowShell
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import com.deepak.flow.app.theme.FlowTextDisabled
import com.deepak.flow.app.theme.FlowWhite
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.deepak.flow.app.theme.FlowAccent
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.app.theme.FlowTextTertiary
import com.deepak.flow.core.gym.GymLimits
import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.TrackingField
import com.deepak.flow.core.gym.WeightUnit
import com.deepak.flow.feature.gym.presentation.SetDraft
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryGymDayScreen(
    viewModel: HistoryGymDayViewModel,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onBack: () -> Unit,
    onWorkoutClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryDetailShell(
        title = "Gym",
        dateLabel = uiState.dateLabel,
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
        if (uiState.workouts.isEmpty()) {
            FlowSupportingText("No workouts.")
        } else {
            var swipeResetKey by remember { mutableIntStateOf(0) }
            LaunchedEffect(uiState.confirmDeleteWorkoutId) {
                if (uiState.confirmDeleteWorkoutId == null) {
                    swipeResetKey++
                }
            }
            uiState.workouts.forEach { workout ->
                FlowSwipeDeleteRow(
                    modifier = Modifier.fillMaxWidth(),
                    resetKey = swipeResetKey,
                    onDelete = { viewModel.requestDeleteWorkout(workout.workoutId) },
                    onContentClick = { onWorkoutClick(workout.workoutId) },
                    contentPadding = PaddingValues(vertical = FlowSpacing.sm),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = workout.titleLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = FlowTextPrimary,
                            )
                            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
                            FlowSupportingText(workout.dateTimeLabel)
                            Spacer(modifier = Modifier.height(2.dp))
                            FlowSupportingText(workout.durationLabel)
                        }
                        IconButton(
                            onClick = { viewModel.toggleStar(workout.workoutId) },
                        ) {
                            Icon(
                                imageVector = if (workout.starred) {
                                    Icons.Filled.Star
                                } else {
                                    Icons.Outlined.StarOutline
                                },
                                contentDescription = if (workout.starred) {
                                    "Unstar workout"
                                } else {
                                    "Star workout"
                                },
                                tint = FlowTextPrimary,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(FlowSpacing.md))
            }
        }
    }

    if (uiState.confirmDeleteWorkoutId != null) {
        FlowDialog(
            title = "Delete workout?",
            message = "This will permanently delete this workout and all recorded exercises and sets.",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirm = viewModel::confirmDeleteWorkout,
            onDismiss = viewModel::dismissDeleteWorkout,
            destructive = true,
        )
    }
}

@Composable
fun HistoryGymWorkoutScreen(
    viewModel: HistoryGymWorkoutViewModel,
    userName: String?,
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onWaterEnabledChange: (Boolean) -> Unit,
    onGymEnabledChange: (Boolean) -> Unit,
    onDestinationClick: (FlowDrawerDestination) -> Unit,
    onBack: () -> Unit,
    onEditExercise: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.leave) {
        if (uiState.leave) onBack()
    }
    LaunchedEffect(uiState.editingTitle) {
        if (uiState.editingTitle) {
            titleFocusRequester.requestFocus()
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            viewModel.refreshEditWindow()
        }
    }

    FlowShell(
        selected = FlowDrawerDestination.HISTORY,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (uiState.editingTitle) {
                    val titleStyle = MaterialTheme.typography.headlineSmall.copy(color = FlowTextPrimary)
                    BasicTextField(
                        value = uiState.titleDraft,
                        onValueChange = viewModel::onTitleDraftChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(titleFocusRequester)
                            .onFocusChanged { focus ->
                                if (!focus.isFocused) {
                                    viewModel.commitTitleEdit()
                                }
                            },
                        textStyle = titleStyle,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        cursorBrush = SolidColor(FlowWhite),
                        decorationBox = { inner ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (uiState.titleDraft.isEmpty()) {
                                    Text(
                                        text = uiState.titleLabel,
                                        style = titleStyle.copy(color = FlowTextDisabled),
                                        maxLines = 1,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                } else {
                    Text(
                        text = uiState.titleLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        color = FlowTextPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(role = Role.Button) {
                                viewModel.startEditingTitle()
                            },
                    )
                }
                if (uiState.workoutId != 0L) {
                    IconButton(onClick = viewModel::toggleStarred) {
                        Icon(
                            imageVector = if (uiState.starred) {
                                Icons.Filled.Star
                            } else {
                                Icons.Outlined.StarOutline
                            },
                            contentDescription = if (uiState.starred) {
                                "Unstar workout"
                            } else {
                                "Star workout"
                            },
                            tint = FlowTextPrimary,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            if (uiState.loading) {
                FlowSupportingText("Loading…")
            } else if (uiState.workoutId == 0L) {
                FlowSupportingText("Workout not found.")
            } else {
                FlowSupportingText("${uiState.dateLabel} · ${uiState.durationLabel}")
                Spacer(modifier = Modifier.height(FlowSpacing.xl))
                uiState.exercises.forEachIndexed { index, exercise ->
                    HistoryGymExerciseBlock(
                        exercise = exercise,
                        canEdit = uiState.canEdit,
                        onEdit = { onEditExercise(exercise.exerciseId) },
                    )
                    if (index < uiState.exercises.lastIndex) {
                        Spacer(modifier = Modifier.height(FlowSpacing.xl))
                        FlowHairlineDivider()
                        Spacer(modifier = Modifier.height(FlowSpacing.xl))
                    }
                }
                if (uiState.workoutId != 0L) {
                    Spacer(modifier = Modifier.height(FlowSpacing.xl))
                    FlowTextAction(
                        text = "Delete Workout",
                        onClick = viewModel::requestDeleteWorkout,
                        destructive = true,
                    )
                }
            }
        }
    }

    if (uiState.confirmDeleteWorkout) {
        FlowDialog(
            title = "Delete workout?",
            message = "This will permanently delete this workout and all recorded exercises and sets.",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirm = viewModel::confirmDeleteWorkout,
            onDismiss = viewModel::dismissDeleteWorkout,
            destructive = true,
        )
    }
}

@Composable
private fun HistoryGymExerciseBlock(
    exercise: HistoryGymExerciseUi,
    canEdit: Boolean,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.titleLarge,
            color = FlowTextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (canEdit) {
            FlowTextAction(text = "Edit", onClick = onEdit)
        }
    }
    if (exercise.note.isNotBlank()) {
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
        HistoryNoteWithLinks(note = exercise.note)
    }
    Spacer(modifier = Modifier.height(FlowSpacing.md))
    if (exercise.sets.isEmpty()) {
        FlowSupportingText("No sets recorded.")
    } else {
        exercise.sets.forEach { set ->
            Text(
                text = "Set ${set.setNumber}",
                style = MaterialTheme.typography.labelLarge,
                color = FlowTextSecondary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = set.valuesLabel,
                style = MaterialTheme.typography.titleMedium,
                color = FlowTextPrimary,
            )
            if (set.failure) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Failure",
                    style = MaterialTheme.typography.bodySmall,
                    color = FlowTextTertiary,
                )
            }
            Spacer(modifier = Modifier.height(FlowSpacing.md))
        }
    }
}

@Composable
fun HistoryGymEditExerciseScreen(
    viewModel: HistoryGymEditExerciseViewModel,
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
        selected = FlowDrawerDestination.HISTORY,
        userName = userName,
        remindersEnabled = remindersEnabled,
        waterEnabled = waterEnabled,
        gymEnabled = gymEnabled,
        onRemindersEnabledChange = onRemindersEnabledChange,
        onWaterEnabledChange = onWaterEnabledChange,
        onGymEnabledChange = onGymEnabledChange,
        onDestinationClick = onDestinationClick,
        onBack = onLeave,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            FlowScreenTitle("Edit Exercise")
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            if (uiState.loading) {
                FlowSupportingText("Loading…")
            } else {
                FlowSectionLabel("Name")
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                FlowTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    placeholder = "Exercise name",
                )
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
                FlowSectionLabel("Note")
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                FlowTextField(
                    value = uiState.note,
                    onValueChange = viewModel::onNoteChange,
                    placeholder = "Your note...",
                    singleLine = false,
                    minLines = 2,
                )
                FlowMetaText("${uiState.note.length}/${GymLimits.NOTE_MAX_CHARS}")

                if (uiState.sets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(FlowSpacing.xl))
                    FlowSectionLabel("Recorded sets")
                    Spacer(modifier = Modifier.height(FlowSpacing.sm))
                    uiState.sets.forEach { set ->
                        Text(
                            text = "SET ${set.setNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            color = FlowTextSecondary,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = GymLogic.formatSetSummary(
                                set,
                                uiState.fields,
                                uiState.displayWeightUnit,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = FlowTextPrimary,
                        )
                        if (set.failure) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Failure",
                                style = MaterialTheme.typography.bodySmall,
                                color = FlowTextTertiary,
                            )
                        }
                        Spacer(modifier = Modifier.height(FlowSpacing.md))
                    }
                }

                if (uiState.message != null) {
                    Spacer(modifier = Modifier.height(FlowSpacing.sm))
                    FlowSupportingText(uiState.message!!)
                }

                Spacer(modifier = Modifier.height(FlowSpacing.xl))
                FlowButton(text = "Save", onClick = viewModel::saveExercise)
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                FlowButton(
                    text = "Cancel",
                    onClick = viewModel::cancel,
                    variant = FlowButtonVariant.Secondary,
                )
            }
        }
    }
}

@Composable
private fun HistoryTrackingFieldChips(
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
private fun HistoryInlineSetEditor(
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
    if (TrackingField.WEIGHT in fields) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
        ) {
            FlowTextAction(text = "−", onClick = { onStepWeight(false) })
            FlowTextField(
                value = draft.weight,
                onValueChange = { value ->
                    onDraftChange { it.copy(weight = filterDecimal(value)) }
                },
                placeholder = "0",
                suffix = weightUnit.label.lowercase(),
                modifier = Modifier.weight(1f),
            )
            FlowTextAction(text = "+", onClick = { onStepWeight(true) })
        }
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
    }
    if (TrackingField.REPS in fields) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
        ) {
            FlowTextAction(text = "−", onClick = { onStepReps(false) })
            FlowTextField(
                value = draft.reps,
                onValueChange = { value ->
                    onDraftChange { it.copy(reps = filterInt(value)) }
                },
                placeholder = "0",
                suffix = "reps",
                modifier = Modifier.weight(1f),
            )
            FlowTextAction(text = "+", onClick = { onStepReps(true) })
        }
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
    }
    if (TrackingField.DURATION in fields) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
        ) {
            FlowTextField(
                value = draft.durationMinutes,
                onValueChange = { value ->
                    onDraftChange { it.copy(durationMinutes = filterInt(value)) }
                },
                placeholder = "0",
                suffix = "min",
                modifier = Modifier.weight(1f),
            )
            FlowTextField(
                value = draft.durationSeconds,
                onValueChange = { value ->
                    onDraftChange { it.copy(durationSeconds = filterDurationSeconds(value)) }
                },
                placeholder = "0",
                suffix = "sec",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
    }
    if (TrackingField.DISTANCE in fields) {
        FlowTextField(
            value = draft.distance,
            onValueChange = { value ->
                onDraftChange { it.copy(distance = filterDecimal(value)) }
            },
            placeholder = "0",
            suffix = "km",
        )
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
    }
    if (TrackingField.SPEED in fields) {
        FlowTextField(
            value = draft.speed,
            onValueChange = { value ->
                onDraftChange { it.copy(speed = filterDecimal(value)) }
            },
            placeholder = "0",
            suffix = "km/h",
        )
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
    }
    if (TrackingField.INCLINE in fields) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
        ) {
            FlowTextAction(text = "−", onClick = { onStepIncline(false) })
            FlowTextField(
                value = draft.incline,
                onValueChange = { value ->
                    onDraftChange { it.copy(incline = filterInt(value)) }
                },
                placeholder = "0",
                suffix = "incl",
                modifier = Modifier.weight(1f),
            )
            FlowTextAction(text = "+", onClick = { onStepIncline(true) })
        }
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
    }
    if (TrackingField.RESISTANCE in fields) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
        ) {
            FlowTextAction(text = "−", onClick = { onStepResistance(false) })
            FlowTextField(
                value = draft.resistance,
                onValueChange = { value ->
                    onDraftChange { it.copy(resistance = filterInt(value)) }
                },
                placeholder = "0",
                suffix = "res",
                modifier = Modifier.weight(1f),
            )
            FlowTextAction(text = "+", onClick = { onStepResistance(true) })
        }
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
    }
    if (TrackingField.ROUNDS in fields) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
        ) {
            FlowTextAction(text = "−", onClick = { onStepRounds(false) })
            FlowTextField(
                value = draft.rounds,
                onValueChange = { value ->
                    onDraftChange { it.copy(rounds = filterInt(value)) }
                },
                placeholder = "0",
                suffix = "rnd",
                modifier = Modifier.weight(1f),
            )
            FlowTextAction(text = "+", onClick = { onStepRounds(true) })
        }
        Spacer(modifier = Modifier.height(FlowSpacing.sm))
    }
    FlowTextAction(
        text = if (draft.failure) "Failure · On" else "Failure · Off",
        onClick = { onDraftChange { it.copy(failure = !it.failure) } },
    )
}

@Composable
private fun HistoryNoteWithLinks(note: String) {
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

private fun trackingSummary(fields: Set<TrackingField>): String {
    if (fields.isEmpty()) return "Weight + Reps"
    return fields.sortedBy { it.ordinal }.joinToString(" + ") { it.label }
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
