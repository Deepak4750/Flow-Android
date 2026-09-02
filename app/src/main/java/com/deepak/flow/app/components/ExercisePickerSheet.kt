package com.deepak.flow.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.core.gym.GymEquipment
import com.deepak.flow.core.gym.GymExerciseSearchHit
import com.deepak.flow.core.gym.GymMuscleGroup

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExercisePickerSheet(
    visible: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<GymExerciseSearchHit>,
    selectedMuscle: GymMuscleGroup?,
    onMuscleSelected: (GymMuscleGroup?) -> Unit,
    selectedEquipment: GymEquipment?,
    onEquipmentSelected: (GymEquipment?) -> Unit,
    onSelectExercise: (exerciseId: String, displayName: String) -> Unit,
    onCreateCustomExercise: (displayName: String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FlowSpacing.lg)
                .padding(bottom = FlowSpacing.xl),
        ) {
            Text(
                text = "Exercise Library",
                style = MaterialTheme.typography.titleLarge,
                color = FlowTextPrimary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.md))
            FlowTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Search exercises...",
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.md))
            FlowSectionLabel("Muscle")
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(FlowSpacing.xs),
            ) {
                FlowChip(
                    label = "All",
                    selected = selectedMuscle == null,
                    onClick = { onMuscleSelected(null) },
                )
                GymMuscleGroup.entries.forEach { muscle ->
                    FlowChip(
                        label = muscle.displayName,
                        selected = selectedMuscle == muscle,
                        onClick = { onMuscleSelected(if (selectedMuscle == muscle) null else muscle) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(FlowSpacing.md))
            FlowSectionLabel("Equipment")
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(FlowSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(FlowSpacing.xs),
            ) {
                FlowChip(
                    label = "All",
                    selected = selectedEquipment == null,
                    onClick = { onEquipmentSelected(null) },
                )
                GymEquipment.entries.forEach { equipment ->
                    FlowChip(
                        label = equipment.displayName,
                        selected = selectedEquipment == equipment,
                        onClick = {
                            onEquipmentSelected(if (selectedEquipment == equipment) null else equipment)
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            if (results.isEmpty()) {
                FlowSupportingText("No exercises match your search.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(FlowSpacing.xxs),
                ) {
                    items(results, key = { hit ->
                        when {
                            hit.isCreateCustom -> "create:${hit.displayName}"
                            hit.exerciseId.isNotBlank() -> hit.exerciseId
                            else -> "name:${hit.displayName}"
                        }
                    }) { hit ->
                        ExercisePickerRow(
                            hit = hit,
                            onClick = {
                                if (hit.isCreateCustom) {
                                    onCreateCustomExercise(hit.displayName.trim())
                                } else if (hit.exerciseId.isNotBlank()) {
                                    onSelectExercise(hit.exerciseId, hit.displayName)
                                }
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExercisePickerRow(
    hit: GymExerciseSearchHit,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = FlowSpacing.sm),
    ) {
        Text(
            text = if (hit.isCreateCustom) "+ Create custom exercise" else hit.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = FlowTextPrimary,
        )
        if (!hit.isCreateCustom && !hit.aliasHint.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            Text(
                text = hit.aliasHint,
                style = MaterialTheme.typography.bodySmall,
                color = FlowTextSecondary,
            )
        } else if (hit.isCreateCustom) {
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            Text(
                text = hit.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = FlowTextSecondary,
            )
        }
        val meta = buildList {
            hit.primaryMuscle?.displayName?.let { add(it) }
            hit.equipment?.displayName?.let { add(it) }
        }
        if (meta.isNotEmpty()) {
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            Text(
                text = meta.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = FlowTextSecondary,
            )
        }
    }
}

@Composable
fun rememberExercisePickerState(): ExercisePickerState = remember { ExercisePickerState() }

class ExercisePickerState {
    var visible by mutableStateOf(false)
        private set

    fun show() {
        visible = true
    }

    fun hide() {
        visible = false
    }
}
