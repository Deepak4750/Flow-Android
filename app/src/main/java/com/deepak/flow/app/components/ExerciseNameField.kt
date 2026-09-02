package com.deepak.flow.app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.deepak.flow.app.theme.FlowSpacing
import com.deepak.flow.app.theme.FlowTextPrimary
import com.deepak.flow.app.theme.FlowTextSecondary
import com.deepak.flow.core.gym.GymExerciseNameCatalog
import com.deepak.flow.core.gym.GymExerciseSearchHit

@Composable
fun ExerciseNameField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    placeholder: String = "Exercise title",
    singleLine: Boolean = true,
    modifier: Modifier = Modifier,
    searchResults: List<GymExerciseSearchHit> = emptyList(),
    selectedExerciseId: String = "",
    onSelectExercise: ((exerciseId: String, displayName: String) -> Unit)? = null,
    onCreateCustomExercise: ((displayName: String) -> Unit)? = null,
    onBrowseExercises: (() -> Unit)? = null,
) {
    val usePicker = onSelectExercise != null && onCreateCustomExercise != null
    val keyboardController = LocalSoftwareKeyboardController.current
    val filteredLegacy = remember(value, suggestions) {
        GymExerciseNameCatalog.filterSuggestions(suggestions, value)
    }
    val filteredPicker = remember(value, searchResults) {
        if (searchResults.isEmpty()) {
            GymExerciseNameCatalog.searchExercises(
                query = value,
                historicalNames = suggestions,
            )
        } else {
            searchResults
        }
    }
    val filtered = if (usePicker) filteredPicker else emptyList()
    val exactMatch = remember(value, suggestions, selectedExerciseId) {
        if (selectedExerciseId.isNotBlank()) return@remember true
        val key = value.trim().lowercase()
        key.isNotEmpty() && suggestions.any { it.equals(value.trim(), ignoreCase = true) }
    }
    var showSuggestions by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }

    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length),
            )
        }
    }

    fun applySelection(selected: String) {
        textFieldValue = TextFieldValue(
            text = selected,
            selection = TextRange(selected.length),
        )
        onValueChange(selected)
        showSuggestions = false
        keyboardController?.hide()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowTextField(
                value = textFieldValue,
                onValueChange = { updated ->
                    textFieldValue = updated
                    onValueChange(updated.text)
                    showSuggestions = true
                },
                placeholder = placeholder,
                singleLine = singleLine,
                modifier = Modifier.weight(1f),
            )
            if (usePicker && onBrowseExercises != null) {
                FlowTextAction(
                    text = "Browse",
                    onClick = onBrowseExercises,
                    modifier = Modifier.padding(start = FlowSpacing.sm),
                )
            }
        }
        if (usePicker && showSuggestions && filtered.isNotEmpty() && !exactMatch) {
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            filtered.forEach { hit ->
                ExerciseSearchRow(
                    hit = hit,
                    onClick = {
                        if (hit.isCreateCustom) {
                            onCreateCustomExercise.invoke(hit.displayName.trim())
                            keyboardController?.hide()
                            showSuggestions = false
                        } else if (hit.exerciseId.isNotBlank()) {
                            applySelection(hit.displayName)
                            onSelectExercise.invoke(hit.exerciseId, hit.displayName)
                        } else {
                            applySelection(hit.displayName)
                        }
                    },
                )
            }
        } else if (!usePicker && showSuggestions && value.isNotBlank() && filteredLegacy.isNotEmpty() && !exactMatch) {
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            filteredLegacy.forEach { suggestion ->
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlowTextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) {
                            applySelection(
                                GymExerciseNameCatalog.selectSuggestion(value, suggestion),
                            )
                        }
                        .padding(vertical = FlowSpacing.xs),
                )
            }
        } else if (!usePicker && showSuggestions && value.isBlank() && filteredLegacy.isNotEmpty()) {
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            Text(
                text = "Recent",
                style = MaterialTheme.typography.labelMedium,
                color = FlowTextSecondary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            filteredLegacy.forEach { suggestion ->
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlowTextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) {
                            applySelection(
                                GymExerciseNameCatalog.selectSuggestion(value, suggestion),
                            )
                        }
                        .padding(vertical = FlowSpacing.xs),
                )
            }
        } else if (usePicker && showSuggestions && value.isBlank() && filtered.isNotEmpty()) {
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            Text(
                text = "Search exercises",
                style = MaterialTheme.typography.labelMedium,
                color = FlowTextSecondary,
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            filtered.forEach { hit ->
                ExerciseSearchRow(
                    hit = hit,
                    onClick = {
                        if (hit.isCreateCustom) {
                            onCreateCustomExercise.invoke(hit.displayName.trim())
                            keyboardController?.hide()
                            showSuggestions = false
                        } else if (hit.exerciseId.isNotBlank()) {
                            applySelection(hit.displayName)
                            onSelectExercise.invoke(hit.exerciseId, hit.displayName)
                        } else {
                            applySelection(hit.displayName)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ExerciseSearchRow(
    hit: GymExerciseSearchHit,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = FlowSpacing.xs),
    ) {
        Text(
            text = if (hit.isCreateCustom) "+ Create custom exercise" else hit.displayName,
            style = MaterialTheme.typography.bodyMedium,
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
        if (meta.isNotEmpty() && !hit.isCreateCustom) {
            Spacer(modifier = Modifier.height(FlowSpacing.xxs))
            Text(
                text = meta.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = FlowTextSecondary,
            )
        }
    }
}
