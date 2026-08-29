package com.deepak.flow.feature.gym.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.GymLimits
import com.deepak.flow.core.gym.GymRoutine
import com.deepak.flow.core.gym.GymRoutineDay
import com.deepak.flow.core.gym.GymRoutineExercise
import com.deepak.flow.core.gym.TrackingField
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class RoutineBuilderUiState(
    val loading: Boolean = true,
    val routineId: Long = 0L,
    val name: String = "",
    val currentDayIndex: Int = 0,
    val days: List<GymRoutineDay> = emptyList(),
    val expandedDayKey: String? = null,
    val expandedExerciseStableKey: String? = null,
    val dayRemovedUndoVisible: Boolean = false,
    val message: String? = null,
    val saved: Boolean = false,
    val leave: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
) {
    val isEditMode: Boolean get() = routineId > 0L

    val canSave: Boolean
        get() = days.any { day ->
            day.isRestDay || day.exercises.any { it.name.trim().isNotEmpty() }
        }

    val hasPendingEdits: Boolean
        get() = !loading && hasUnsavedChanges

    fun dayIndexForKey(key: String): Int = days.indexOfFirst { it.localKey == key }

    fun dayForKey(key: String): GymRoutineDay? = days.find { it.localKey == key }
}

class RoutineBuilderViewModel(
    application: Application,
    private val editingRoutineId: Long?,
) : AndroidViewModel(application) {

    private val repository = (application as FlowApplication).gymWorkoutRepository

    private val _uiState = MutableStateFlow(RoutineBuilderUiState())
    val uiState: StateFlow<RoutineBuilderUiState> = _uiState

    private var undoJob: Job? = null
    private var pendingDeletedDay: PendingDeletedDay? = null
    private var editBaseline: RoutineBuilderSnapshot? = null

    private data class PendingDeletedDay(
        val day: GymRoutineDay,
        val index: Int,
    )

    init {
        viewModelScope.launch {
            if (editingRoutineId != null) {
                val existing = repository.getRoutine(editingRoutineId)
                if (existing != null) {
                    val loaded = RoutineBuilderUiState(
                        loading = false,
                        routineId = existing.id,
                        name = existing.name,
                        currentDayIndex = existing.currentDayIndex,
                        days = existing.days.map { it.withStableLocalKey() },
                    )
                    editBaseline = RoutineBuilderSnapshot.from(loaded)
                    _uiState.value = publish(loaded)
                    return@launch
                }
            }
            val fresh = RoutineBuilderUiState(
                loading = false,
                days = listOf(emptyWorkoutDay(0)),
            )
            editBaseline = RoutineBuilderSnapshot.from(fresh)
            _uiState.value = publish(fresh)
        }
    }

    private fun publish(state: RoutineBuilderUiState): RoutineBuilderUiState {
        val baseline = editBaseline
        val hasChanges = baseline != null && RoutineBuilderSnapshot.from(state) != baseline
        return state.copy(hasUnsavedChanges = hasChanges)
    }

    private fun updateState(transform: (RoutineBuilderUiState) -> RoutineBuilderUiState) {
        _uiState.update { current -> publish(transform(current)) }
    }

    fun onNameChange(value: String) {
        updateState { it.copy(name = value, message = null) }
    }

    fun onDayNameChange(dayKey: String, value: String) {
        updateState { state ->
            state.copy(
                days = state.days.map { day ->
                    if (day.localKey == dayKey) day.copy(name = value) else day
                },
                message = null,
            )
        }
    }

    fun addWorkoutDay() {
        updateState { state ->
            if (state.days.size >= GymLimits.DAY_COUNT_MAX) return@updateState state
            val newDay = emptyWorkoutDay(state.days.size)
            state.copy(
                days = state.days + newDay,
                expandedDayKey = newDay.localKey,
                expandedExerciseStableKey = null,
                message = null,
            )
        }
    }

    fun addRestDay() {
        updateState { state ->
            if (state.days.size >= GymLimits.DAY_COUNT_MAX) return@updateState state
            val newDay = emptyRestDay(state.days.size)
            state.copy(
                days = state.days + newDay,
                expandedDayKey = newDay.localKey,
                expandedExerciseStableKey = null,
                message = null,
            )
        }
    }

    fun deleteDay(dayKey: String) {
        val state = _uiState.value
        if (state.days.size <= GymLimits.DAY_COUNT_MIN) return
        val index = state.dayIndexForKey(dayKey)
        if (index < 0) return
        val removed = state.days[index]
        val days = state.days
            .filter { it.localKey != dayKey }
            .mapIndexed { dayIndex, day -> day.copy(dayIndex = dayIndex) }
        pendingDeletedDay = PendingDeletedDay(day = removed, index = index)
        updateState {
            it.copy(
                days = days,
                expandedDayKey = it.expandedDayKey?.takeIf { key ->
                    days.any { day -> day.localKey == key }
                },
                expandedExerciseStableKey = null,
                message = null,
            )
        }
        showDayRemovedUndo()
    }

    fun undoDeleteDay() {
        val pending = pendingDeletedDay ?: return
        undoJob?.cancel()
        updateState { state ->
            if (state.days.size >= GymLimits.DAY_COUNT_MAX) return@updateState state
            val days = state.days.toMutableList()
            val insertAt = pending.index.coerceIn(0, days.size)
            days.add(insertAt, pending.day.copy(dayIndex = insertAt))
            val reindexed = days.mapIndexed { index, day -> day.copy(dayIndex = index) }
            state.copy(
                days = reindexed,
                expandedDayKey = pending.day.localKey,
                dayRemovedUndoVisible = false,
                message = null,
            )
        }
        pendingDeletedDay = null
    }

    fun dismissDayRemovedUndo() {
        undoJob?.cancel()
        pendingDeletedDay = null
        _uiState.update { it.copy(dayRemovedUndoVisible = false) }
    }

    private fun showDayRemovedUndo() {
        undoJob?.cancel()
        _uiState.update { it.copy(dayRemovedUndoVisible = true) }
        undoJob = viewModelScope.launch {
            delay(5_000)
            pendingDeletedDay = null
            _uiState.update { it.copy(dayRemovedUndoVisible = false) }
        }
    }

    fun toggleDayExpanded(dayKey: String) {
        updateState { state ->
            val nextExpanded = if (state.expandedDayKey == dayKey) null else dayKey
            state.copy(
                expandedDayKey = nextExpanded,
                expandedExerciseStableKey = null,
                message = null,
            )
        }
    }

    fun moveDay(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val keys = GymLogic.reorderListByMove(
            _uiState.value.days.map { it.localKey },
            fromIndex,
            toIndex,
        )
        reorderDaysByKeys(keys)
    }

    fun reorderDaysByKeys(orderedKeys: List<String>) {
        updateState { state ->
            val reordered = GymLogic.reorderDaysByKeys(state.days, orderedKeys) ?: return@updateState state
            state.copy(days = reordered, message = null)
        }
    }

    fun discardAndLeave() {
        _uiState.update { it.copy(leave = true, message = null, hasUnsavedChanges = false) }
    }

    fun addExercise(dayKey: String) {
        updateState { state ->
            val dayIndex = state.dayIndexForKey(dayKey)
            if (dayIndex < 0) return@updateState state
            val day = state.days[dayIndex]
            if (!day.isRestDay && day.exercises.any { it.name.trim().isEmpty() }) {
                return@updateState state.copy(message = "Exercise title is required")
            }
            val days = state.days.mapIndexed { index, day ->
                if (index != dayIndex) return@mapIndexed day
                val exercise = GymRoutineExercise(
                    stableKey = UUID.randomUUID().toString(),
                    name = "",
                    sortOrder = day.exercises.size,
                    setCount = GymLimits.SET_COUNT_DEFAULT,
                )
                day.copy(exercises = day.exercises + exercise)
            }
            val addedKey = days[dayIndex].exercises.last().stableKey
            state.copy(
                days = days,
                expandedDayKey = dayKey,
                expandedExerciseStableKey = addedKey,
                message = null,
            )
        }
    }

    fun onExerciseNameChange(dayKey: String, exerciseStableKey: String, value: String) {
        updateExercise(dayKey, exerciseStableKey) { it.copy(name = value) }
    }

    fun onExerciseNoteChange(dayKey: String, exerciseStableKey: String, value: String) {
        updateExercise(dayKey, exerciseStableKey) {
            it.copy(note = value.take(GymLimits.NOTE_MAX_CHARS))
        }
    }

    fun toggleTrackingField(dayKey: String, exerciseStableKey: String, field: TrackingField) {
        updateExercise(dayKey, exerciseStableKey) { exercise ->
            val next = exercise.trackingFields.toMutableSet()
            if (field in next) {
                if (next.size <= 1) return@updateExercise exercise
                next.remove(field)
            } else {
                next.add(field)
            }
            exercise.copy(trackingFields = next)
        }
    }

    fun stepSetCount(dayKey: String, exerciseStableKey: String, up: Boolean) {
        updateExercise(dayKey, exerciseStableKey) { exercise ->
            val next = GymLimits.clampSetCount(exercise.setCount + if (up) 1 else -1)
            exercise.copy(setCount = next)
        }
    }

    fun toggleExerciseExpanded(dayKey: String, exerciseStableKey: String) {
        updateState { state ->
            if (state.expandedExerciseStableKey == exerciseStableKey) {
                val day = state.dayForKey(dayKey)
                val exercise = day?.exercises?.find { it.stableKey == exerciseStableKey }
                if (exercise != null && exercise.name.trim().isEmpty()) {
                    return@updateState state.copy(message = "Exercise title is required")
                }
            }
            state.copy(
                expandedExerciseStableKey = if (state.expandedExerciseStableKey == exerciseStableKey) {
                    null
                } else {
                    exerciseStableKey
                },
                message = null,
            )
        }
    }

    fun deleteExercise(dayKey: String, exerciseStableKey: String) {
        updateState { state ->
            val days = state.days.map { day ->
                if (day.localKey != dayKey) return@map day
                day.copy(
                    exercises = day.exercises.filter { it.stableKey != exerciseStableKey },
                )
            }
            state.copy(
                days = days,
                expandedExerciseStableKey = null,
                message = null,
            )
        }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val days = state.days.mapIndexed { index, day ->
                day.copy(
                    dayIndex = index,
                    name = day.name.trim(),
                    exercises = if (day.isRestDay) {
                        emptyList()
                    } else {
                        day.exercises
                            .filter { it.name.trim().isNotEmpty() }
                            .mapIndexed { exerciseIndex, exercise ->
                                exercise.copy(
                                    stableKey = exercise.stableKey.ifBlank {
                                        UUID.randomUUID().toString()
                                    },
                                    sortOrder = exerciseIndex,
                                    trackingFields = exercise.trackingFields.ifEmpty {
                                        setOf(TrackingField.WEIGHT, TrackingField.REPS)
                                    },
                                    setCount = GymLimits.clampSetCount(exercise.setCount),
                                    note = GymLimits.clampNote(exercise.note),
                                )
                            }
                    },
                )
            }
            if (days.none { !it.isRestDay && it.exercises.isNotEmpty() }) {
                _uiState.update { it.copy(message = "Add at least one exercise.") }
                return@launch
            }
            runCatching {
                repository.saveRoutine(
                    GymRoutine(
                        id = state.routineId,
                        name = state.name,
                        currentDayIndex = state.currentDayIndex.coerceIn(0, days.lastIndex),
                        days = days,
                    ),
                )
                _uiState.update {
                    it.copy(saved = true, leave = true, message = null, hasUnsavedChanges = false)
                }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.message ?: "Couldn't save routine.") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun updateExercise(
        dayKey: String,
        exerciseStableKey: String,
        transform: (GymRoutineExercise) -> GymRoutineExercise,
    ) {
        updateState { state ->
            state.copy(
                days = state.days.map { day ->
                    if (day.localKey != dayKey) return@map day
                    day.copy(
                        exercises = day.exercises.map { exercise ->
                            if (exercise.stableKey != exerciseStableKey) exercise else transform(exercise)
                        },
                    )
                },
                message = null,
            )
        }
    }

    private fun emptyWorkoutDay(index: Int) = GymRoutineDay(
        dayIndex = index,
        name = "",
        isRestDay = false,
        localKey = UUID.randomUUID().toString(),
    )

    private fun emptyRestDay(index: Int) = GymRoutineDay(
        dayIndex = index,
        name = "",
        isRestDay = true,
        localKey = UUID.randomUUID().toString(),
    )

    private fun GymRoutineDay.withStableLocalKey(): GymRoutineDay {
        val key = stableLocalKey().ifBlank { UUID.randomUUID().toString() }
        return copy(localKey = key)
    }
}

private data class RoutineBuilderSnapshot(
    val name: String,
    val days: List<DaySnapshot>,
) {
    data class DaySnapshot(
        val name: String,
        val isRestDay: Boolean,
        val exercises: List<ExerciseSnapshot>,
    )

    data class ExerciseSnapshot(
        val name: String,
        val note: String,
        val trackingFields: Set<TrackingField>,
        val setCount: Int,
    )

    companion object {
        fun from(state: RoutineBuilderUiState): RoutineBuilderSnapshot {
            val days = state.days.map { day ->
                DaySnapshot(
                    name = day.name.trim(),
                    isRestDay = day.isRestDay,
                    exercises = if (day.isRestDay) {
                        emptyList()
                    } else {
                        day.exercises
                            .filter { it.name.trim().isNotEmpty() }
                            .map { exercise ->
                                ExerciseSnapshot(
                                    name = exercise.name.trim(),
                                    note = GymLimits.clampNote(exercise.note),
                                    trackingFields = exercise.trackingFields.ifEmpty {
                                        setOf(TrackingField.WEIGHT, TrackingField.REPS)
                                    },
                                    setCount = GymLimits.clampSetCount(exercise.setCount),
                                )
                            }
                    },
                )
            }
            return RoutineBuilderSnapshot(
                name = state.name.trim(),
                days = days,
            )
        }
    }
}

class RoutineBuilderViewModelFactory(
    private val application: FlowApplication,
    private val editingRoutineId: Long?,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineBuilderViewModel::class.java)) {
            return RoutineBuilderViewModel(application, editingRoutineId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
