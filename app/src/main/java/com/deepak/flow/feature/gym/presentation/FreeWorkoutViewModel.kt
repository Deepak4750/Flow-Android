package com.deepak.flow.feature.gym.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.gym.GymLimits
import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.GymSetMeasurements
import com.deepak.flow.core.gym.GymWorkoutExercise
import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.gym.GymWorkoutSet
import com.deepak.flow.core.gym.GymWorkoutStatus
import com.deepak.flow.core.gym.GymWorkoutSummary
import com.deepak.flow.core.gym.TrackingField
import com.deepak.flow.core.gym.WeightUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class FreeWorkoutPhase {
    SESSION,
    ADD_EXERCISE,
    EDIT_EXERCISE,
    EDIT_SET,
    ASK_REST,
    RESTING,
    END_OPTIONS,
    COMPLETED,
}

data class ExerciseDraft(
    val name: String = "",
    val fields: Set<TrackingField> = emptySet(),
    val note: String = "",
    val editingExerciseId: Long? = null,
)

data class SetDraft(
    val setId: Long? = null,
    val exerciseId: Long = 0L,
    val setNumber: Int = 1,
    val weight: String = "",
    val reps: String = "",
    val durationMinutes: String = "",
    val durationSeconds: String = "",
    val distance: String = "",
    val speed: String = "",
    val incline: String = "",
    val resistance: String = "",
    val rounds: String = "",
    val failure: Boolean = false,
)

enum class FreeWorkoutConfirm {
    DELETE_EXERCISE,
    DELETE_SET,
    DISCARD_WORKOUT,
}

data class FreeWorkoutUiState(
    val loading: Boolean = true,
    val phase: FreeWorkoutPhase = FreeWorkoutPhase.SESSION,
    val session: GymWorkoutSession? = null,
    val nowEpochMilli: Long = System.currentTimeMillis(),
    val exerciseDraft: ExerciseDraft = ExerciseDraft(),
    val setDraft: SetDraft = SetDraft(),
    val restSecondsChoice: Int = GymLimits.SET_REST_DEFAULT_SECONDS,
    val summary: GymWorkoutSummary? = null,
    val message: String? = null,
    val leaveWorkout: Boolean = false,
    val confirm: FreeWorkoutConfirm? = null,
) {
    val stopwatchLabel: String
        get() {
            val started = session?.startedAtEpochMilli ?: return "00:00:00"
            return GymLogic.formatStopwatch(started, nowEpochMilli)
        }

    val restRemainingLabel: String
        get() {
            val ends = session?.restEndsAtEpochMilli ?: return "00:00"
            return GymLogic.formatCountdown(
                GymLogic.remainingRestSeconds(ends, nowEpochMilli),
            )
        }

    val currentExercise: GymWorkoutExercise?
        get() {
            val active = session ?: return null
            if (active.exercises.isEmpty()) return null
            val index = active.currentExerciseIndex.coerceIn(0, active.exercises.lastIndex)
            return active.exercises[index]
        }

    val upNextExercise: GymWorkoutExercise?
        get() = currentExercise

    val upNextLabel: String
        get() = upNextExercise?.name ?: "Continue"
}

@OptIn(ExperimentalCoroutinesApi::class)
class FreeWorkoutViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = (application as FlowApplication).gymWorkoutRepository

    private val workoutId = MutableStateFlow<Long?>(null)
    private val phase = MutableStateFlow(FreeWorkoutPhase.SESSION)
    private val exerciseDraft = MutableStateFlow(ExerciseDraft())
    private val setDraft = MutableStateFlow(SetDraft())
    private val restSecondsChoice = MutableStateFlow(GymLimits.SET_REST_DEFAULT_SECONDS)
    private val summary = MutableStateFlow<GymWorkoutSummary?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val leaveWorkout = MutableStateFlow(false)
    private val confirm = MutableStateFlow<FreeWorkoutConfirm?>(null)
    private val nowEpochMilli = MutableStateFlow(System.currentTimeMillis())
    private val latestSession = MutableStateFlow<GymWorkoutSession?>(null)

    private var clockJob: Job? = null
    private var pendingDeleteExerciseId: Long? = null
    private var pendingDeleteSetId: Long? = null

    private val sessionFlow = workoutId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.observeSession(id)
    }

    private data class EditorState(
        val phase: FreeWorkoutPhase,
        val exerciseDraft: ExerciseDraft,
        val setDraft: SetDraft,
        val restSecondsChoice: Int,
        val summary: GymWorkoutSummary?,
        val message: String?,
        val leaveWorkout: Boolean,
        val confirm: FreeWorkoutConfirm?,
    )

    private val editorFlow = combine(
        combine(phase, exerciseDraft, setDraft) { selectedPhase, draft, set ->
            Triple(selectedPhase, draft, set)
        },
        combine(restSecondsChoice, summary, message, leaveWorkout, confirm) { rest, done, msg, leave, conf ->
            listOf(rest, done, msg, leave, conf)
        },
    ) { triple, restBag ->
        EditorState(
            phase = triple.first,
            exerciseDraft = triple.second,
            setDraft = triple.third,
            restSecondsChoice = restBag[0] as Int,
            summary = restBag[1] as GymWorkoutSummary?,
            message = restBag[2] as String?,
            leaveWorkout = restBag[3] as Boolean,
            confirm = restBag[4] as FreeWorkoutConfirm?,
        )
    }

    val uiState: StateFlow<FreeWorkoutUiState> = combine(
        combine(workoutId, sessionFlow, nowEpochMilli) { id, session, now ->
            Triple(id, session, now)
        },
        editorFlow,
    ) { core, editor ->
        val (id, session, now) = core
        latestSession.value = session
        val resolvedPhase = when {
            session?.status == GymWorkoutStatus.COMPLETED -> FreeWorkoutPhase.COMPLETED
            session?.restEndsAtEpochMilli != null &&
                GymLogic.remainingRestSeconds(session.restEndsAtEpochMilli, now) > 0 &&
                editor.phase !in setOf(
                    FreeWorkoutPhase.ASK_REST,
                    FreeWorkoutPhase.ADD_EXERCISE,
                    FreeWorkoutPhase.EDIT_EXERCISE,
                    FreeWorkoutPhase.EDIT_SET,
                    FreeWorkoutPhase.END_OPTIONS,
                    FreeWorkoutPhase.COMPLETED,
                ) -> FreeWorkoutPhase.RESTING
            else -> editor.phase
        }
        FreeWorkoutUiState(
            loading = id == null || (session == null && !editor.leaveWorkout),
            phase = resolvedPhase,
            session = session,
            nowEpochMilli = now,
            exerciseDraft = editor.exerciseDraft,
            setDraft = editor.setDraft,
            restSecondsChoice = editor.restSecondsChoice,
            summary = editor.summary ?: session?.takeIf {
                it.status == GymWorkoutStatus.COMPLETED
            }?.let { GymLogic.summarize(it, it.endedAtEpochMilli ?: now) },
            message = editor.message,
            leaveWorkout = editor.leaveWorkout,
            confirm = editor.confirm,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FreeWorkoutUiState(),
    )

    init {
        viewModelScope.launch {
            val id = repository.ensureActiveFreeWorkout()
            workoutId.value = id
            val session = repository.getSession(id)
            if (session?.restEndsAtEpochMilli != null &&
                GymLogic.remainingRestSeconds(
                    session.restEndsAtEpochMilli,
                    System.currentTimeMillis(),
                ) > 0
            ) {
                phase.value = FreeWorkoutPhase.RESTING
            }
            startClock()
        }
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                nowEpochMilli.value = now
                val session = latestSession.value
                val ends = session?.restEndsAtEpochMilli
                if (phase.value == FreeWorkoutPhase.RESTING &&
                    session != null &&
                    ends != null &&
                    GymLogic.remainingRestSeconds(ends, now) <= 0
                ) {
                    repository.clearRest(session.id)
                    phase.value = FreeWorkoutPhase.SESSION
                }
                delay(250)
            }
        }
    }

    fun clearMessage() {
        message.value = null
    }

    fun openAddExercise() {
        exerciseDraft.value = ExerciseDraft()
        phase.value = FreeWorkoutPhase.ADD_EXERCISE
    }

    fun openEditExercise(exercise: GymWorkoutExercise) {
        exerciseDraft.value = ExerciseDraft(
            name = exercise.name,
            fields = exercise.trackingFields,
            note = exercise.note,
            editingExerciseId = exercise.id,
        )
        phase.value = FreeWorkoutPhase.EDIT_EXERCISE
    }

    fun cancelExerciseEditor() {
        phase.value = FreeWorkoutPhase.SESSION
        exerciseDraft.value = ExerciseDraft()
    }

    fun onExerciseNameChange(value: String) {
        exerciseDraft.update { it.copy(name = value) }
    }

    fun onExerciseNoteChange(value: String) {
        exerciseDraft.update { it.copy(note = value.take(GymLimits.NOTE_MAX_CHARS)) }
    }

    fun toggleTrackingField(field: TrackingField) {
        exerciseDraft.update { draft ->
            val next = draft.fields.toMutableSet()
            if (!next.add(field)) next.remove(field)
            draft.copy(fields = next)
        }
    }

    fun saveExercise() {
        viewModelScope.launch {
            val id = workoutId.value ?: return@launch
            val draft = exerciseDraft.value
            runCatching {
                val editingId = draft.editingExerciseId
                if (editingId == null) {
                    repository.addExercise(
                        workoutId = id,
                        name = draft.name,
                        trackingFields = draft.fields,
                        note = draft.note,
                    )
                } else {
                    repository.updateExercise(
                        exerciseId = editingId,
                        name = draft.name,
                        trackingFields = draft.fields,
                        note = draft.note,
                    )
                }
                phase.value = FreeWorkoutPhase.SESSION
                exerciseDraft.value = ExerciseDraft()
            }.onFailure {
                message.value = it.message ?: "Couldn't save exercise."
            }
        }
    }

    fun deleteExercise(exerciseId: Long) {
        pendingDeleteExerciseId = exerciseId
        confirm.value = FreeWorkoutConfirm.DELETE_EXERCISE
    }

    fun deleteSet(setId: Long) {
        pendingDeleteSetId = setId
        confirm.value = FreeWorkoutConfirm.DELETE_SET
    }

    fun discardWorkout() {
        confirm.value = FreeWorkoutConfirm.DISCARD_WORKOUT
    }

    fun dismissConfirm() {
        confirm.value = null
        pendingDeleteExerciseId = null
        pendingDeleteSetId = null
    }

    fun confirmPendingAction() {
        when (confirm.value) {
            FreeWorkoutConfirm.DELETE_EXERCISE -> {
                val exerciseId = pendingDeleteExerciseId
                dismissConfirm()
                if (exerciseId != null) {
                    viewModelScope.launch {
                        repository.deleteExercise(exerciseId)
                        phase.value = FreeWorkoutPhase.SESSION
                        exerciseDraft.value = ExerciseDraft()
                    }
                }
            }
            FreeWorkoutConfirm.DELETE_SET -> {
                val setId = pendingDeleteSetId
                dismissConfirm()
                if (setId != null) {
                    viewModelScope.launch {
                        repository.deleteSet(setId)
                        phase.value = FreeWorkoutPhase.SESSION
                    }
                }
            }
            FreeWorkoutConfirm.DISCARD_WORKOUT -> {
                dismissConfirm()
                viewModelScope.launch {
                    val id = workoutId.value ?: return@launch
                    repository.discardWorkout(id)
                    leaveWorkout.value = true
                }
            }
            null -> Unit
        }
    }

    fun selectExercise(index: Int) {
        viewModelScope.launch {
            val id = workoutId.value ?: return@launch
            repository.setCurrentExerciseIndex(id, index)
        }
    }

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch {
            val id = workoutId.value ?: return@launch
            repository.setWeightUnit(id, unit)
        }
    }

    fun openNewSet() {
        val exercise = uiState.value.currentExercise ?: return
        val previous = exercise.sets.lastOrNull { it.saved }
        setDraft.value = previous.toDraft(
            setId = null,
            exerciseId = exercise.id,
            setNumber = GymLogic.nextSetNumber(exercise.sets),
        )
        phase.value = FreeWorkoutPhase.EDIT_SET
    }

    fun openEditSet(set: GymWorkoutSet) {
        val exercise = uiState.value.currentExercise ?: return
        setDraft.value = set.toDraft(
            setId = set.id,
            exerciseId = exercise.id,
            setNumber = set.setNumber,
        )
        phase.value = FreeWorkoutPhase.EDIT_SET
    }

    fun cancelSetEditor() {
        phase.value = FreeWorkoutPhase.SESSION
    }

    fun onSetDraftChange(transform: (SetDraft) -> SetDraft) {
        setDraft.update(transform)
    }

    fun saveSet() {
        viewModelScope.launch {
            val session = uiState.value.session ?: return@launch
            val exercise = uiState.value.currentExercise ?: return@launch
            val draft = setDraft.value
            val measurements = draft.toMeasurements(session.weightUnit)
            if (!GymLogic.hasMeaningfulMeasurement(exercise.trackingFields, measurements)) {
                message.value = "Enter at least one value."
                return@launch
            }
            runCatching {
                val existingId = draft.setId
                if (existingId == null) {
                    repository.addSet(
                        exerciseId = exercise.id,
                        measurements = measurements,
                        failure = draft.failure,
                        saved = true,
                    )
                } else {
                    repository.updateSet(
                        setId = existingId,
                        measurements = measurements,
                        failure = draft.failure,
                        saved = true,
                        setNumber = draft.setNumber.coerceAtLeast(1),
                    )
                }
                phase.value = FreeWorkoutPhase.ASK_REST
            }.onFailure {
                message.value = it.message ?: "Couldn't save set."
            }
        }
    }

    fun chooseRestSeconds(seconds: Int) {
        restSecondsChoice.value = GymLimits.clampSetRestSeconds(seconds)
    }

    fun confirmRest() {
        viewModelScope.launch {
            val id = workoutId.value ?: return@launch
            repository.startRest(id, restSecondsChoice.value)
            phase.value = FreeWorkoutPhase.RESTING
        }
    }

    fun skipRest() {
        viewModelScope.launch {
            val id = workoutId.value ?: return@launch
            repository.clearRest(id)
            phase.value = FreeWorkoutPhase.SESSION
        }
    }

    fun addRestSeconds(extraSeconds: Int = 10) {
        viewModelScope.launch {
            val id = workoutId.value ?: return@launch
            repository.extendRest(id, extraSeconds)
        }
    }

    fun openEndOptions() {
        phase.value = FreeWorkoutPhase.END_OPTIONS
    }

    fun dismissEndOptions() {
        phase.value = FreeWorkoutPhase.SESSION
    }

    fun completeWorkout() {
        viewModelScope.launch {
            val id = workoutId.value ?: return@launch
            val session = repository.getSession(id) ?: return@launch
            val now = System.currentTimeMillis()
            summary.value = GymLogic.summarize(session, now)
            repository.completeWorkout(id, now)
            phase.value = FreeWorkoutPhase.COMPLETED
        }
    }

    fun finishAndLeave() {
        leaveWorkout.value = true
    }

    override fun onCleared() {
        clockJob?.cancel()
        super.onCleared()
    }
}

class FreeWorkoutViewModelFactory(
    private val application: FlowApplication,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FreeWorkoutViewModel::class.java)) {
            return FreeWorkoutViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}

private fun GymWorkoutSet?.toDraft(
    setId: Long?,
    exerciseId: Long,
    setNumber: Int,
): SetDraft {
    val measurements = this?.measurements ?: GymSetMeasurements()
    val duration = measurements.durationSeconds ?: 0
    return SetDraft(
        setId = setId,
        exerciseId = exerciseId,
        setNumber = setNumber,
        weight = measurements.weight?.let { GymLogic.formatNumber(it) }.orEmpty(),
        reps = measurements.reps?.toString().orEmpty(),
        durationMinutes = if (duration > 0) (duration / 60).toString() else "",
        durationSeconds = if (duration > 0) (duration % 60).toString() else "",
        distance = measurements.distance?.let { GymLogic.formatNumber(it) }.orEmpty(),
        speed = measurements.speed?.let { GymLogic.formatNumber(it) }.orEmpty(),
        incline = measurements.incline?.let { GymLogic.formatNumber(it) }.orEmpty(),
        resistance = measurements.resistance?.let { GymLogic.formatNumber(it) }.orEmpty(),
        rounds = measurements.rounds?.toString().orEmpty(),
        failure = this?.failure == true,
    )
}

private fun SetDraft.toMeasurements(sessionUnit: WeightUnit): GymSetMeasurements {
    val minutes = durationMinutes.toIntOrNull() ?: 0
    val seconds = durationSeconds.toIntOrNull() ?: 0
    val durationTotal = (minutes * 60 + seconds).takeIf { it > 0 }
    return GymSetMeasurements(
        weight = weight.toDoubleOrNull()?.takeIf { it > 0.0 },
        weightUnit = sessionUnit.takeIf { weight.toDoubleOrNull() != null },
        reps = reps.toIntOrNull()?.takeIf { it > 0 },
        durationSeconds = durationTotal,
        distance = distance.toDoubleOrNull()?.takeIf { it > 0.0 },
        speed = speed.toDoubleOrNull()?.takeIf { it > 0.0 },
        incline = incline.toDoubleOrNull(),
        resistance = resistance.toDoubleOrNull()?.takeIf { it > 0.0 },
        rounds = rounds.toIntOrNull()?.takeIf { it > 0 },
    )
}

