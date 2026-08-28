package com.deepak.flow.feature.gym.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.gym.GymLimits
import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.GymRestKind
import com.deepak.flow.core.gym.GymSetMeasurements
import com.deepak.flow.core.gym.GymWorkoutExercise
import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.gym.GymWorkoutSet
import com.deepak.flow.core.gym.GymWorkoutStatus
import com.deepak.flow.core.gym.GymWorkoutSummary
import com.deepak.flow.core.gym.GymWorkoutType
import com.deepak.flow.core.gym.TrackingField
import com.deepak.flow.core.gym.WeightUnit
import com.deepak.flow.core.gym.vibrateRestComplete
import com.deepak.flow.core.model.UserProfile
import com.deepak.flow.core.notification.NotificationChannelManager
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
    SETUP,
    SESSION,
    EDIT_EXERCISE,
    RESTING,
    END_OPTIONS,
    COMPLETED,
}

data class ExerciseDraft(
    val name: String = "",
    val fields: Set<TrackingField> = setOf(TrackingField.WEIGHT, TrackingField.REPS),
    val note: String = "",
    val editingExerciseId: Long? = null,
    val showFieldsEditor: Boolean = false,
    val showNoteEditor: Boolean = false,
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
    /** True when composing a new exercise inline (not yet persisted, or just opened). */
    val composingExercise: Boolean = false,
    /** True when the set editor row is visible. */
    val setEditorVisible: Boolean = false,
    /** After set rest: show + Add New Set / Save Exercise. */
    val awaitingNextAction: Boolean = false,
    val preferredWeightUnit: WeightUnit = WeightUnit.KG,
    val setRestSeconds: Int = GymLimits.SET_REST_DEFAULT_SECONDS,
    val exerciseRestSeconds: Int = GymLimits.EXERCISE_REST_DEFAULT_SECONDS,
    val summary: GymWorkoutSummary? = null,
    val message: String? = null,
    /** Transient undo affordance after removing a saved set. */
    val setRemovedUndoVisible: Boolean = false,
    /** Transient undo affordance after removing an exercise. */
    val exerciseRemovedUndoVisible: Boolean = false,
    val leaveWorkout: Boolean = false,
    val confirm: FreeWorkoutConfirm? = null,
    val setupTitle: String = "",
    val workoutId: Long? = null,
    val workoutType: GymWorkoutType = GymWorkoutType.FREE,
) {
    val isRoutine: Boolean
        get() = session?.type == GymWorkoutType.ROUTINE || workoutType == GymWorkoutType.ROUTINE

    val workoutTitle: String
        get() = session?.title?.trim().takeUnless { it.isNullOrEmpty() } ?: setupTitle

    val workoutHeading: String
        get() = GymLogic.workoutDisplayTitle(workoutTitle, session?.type ?: workoutType)
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
        get() {
            val active = session ?: return null
            val currentIndex = active.currentExerciseIndex
            val nextIndex = active.exercises.indices.firstOrNull { index ->
                index > currentIndex && !active.exercises[index].skipped
            } ?: return null
            return active.exercises[nextIndex]
        }

    val restKind: GymRestKind
        get() = session?.restKind ?: GymRestKind.NONE

    val showUpNextInSession: Boolean
        get() {
            if (!isRoutine || !setEditorVisible) return false
            val planned = currentExercise?.plannedSetCount ?: 0
            if (planned <= 0) return false
            return GymLogic.isLastPlannedSetNumber(setDraft.setNumber, planned) &&
                upNextExercise != null
        }

    val upNextLabel: String
        get() = upNextExercise?.name ?: "Continue"

    val displayWeightUnit: WeightUnit
        get() = preferredWeightUnit

    val activeTrackingFields: Set<TrackingField>
        get() = when {
            composingExercise -> exerciseDraft.fields
            else -> currentExercise?.trackingFields.orEmpty()
        }

    val canSaveSet: Boolean
        get() {
            if (!setEditorVisible) return false
            if (composingExercise && exerciseDraft.name.trim().isEmpty()) return false
            val fields = activeTrackingFields.ifEmpty {
                setOf(TrackingField.WEIGHT, TrackingField.REPS)
            }
            return GymLogic.allSelectedFieldsFilled(
                fields,
                setDraft.toMeasurements(displayWeightUnit),
            )
        }

    val saveSetLabel: String
        get() {
            if (!isRoutine) return "Save Set"
            if (setDraft.setId != null) return "Save Set"
            val planned = currentExercise?.plannedSetCount ?: 0
            return if (GymLogic.isLastPlannedSetNumber(setDraft.setNumber, planned)) {
                "Complete Exercise"
            } else {
                "Complete Set"
            }
        }

    val finishExerciseLabel: String
        get() = if (isRoutine) "Complete Exercise" else "Save Exercise"
}

@OptIn(ExperimentalCoroutinesApi::class)
class FreeWorkoutViewModel(
    application: Application,
    private val workoutType: GymWorkoutType = GymWorkoutType.FREE,
) : AndroidViewModel(application) {

    private val repository = (application as FlowApplication).gymWorkoutRepository
    private val profileRepository = (application as FlowApplication).profileRepository

    private val workoutId = MutableStateFlow<Long?>(null)
    private val phase = MutableStateFlow(FreeWorkoutPhase.SESSION)
    private val exerciseDraft = MutableStateFlow(ExerciseDraft())
    private val setDraft = MutableStateFlow(SetDraft())
    private val composingExercise = MutableStateFlow(false)
    private val setEditorVisible = MutableStateFlow(false)
    private val awaitingNextAction = MutableStateFlow(false)
    private val preferredWeightUnit = MutableStateFlow(WeightUnit.KG)
    private val setRestSeconds = MutableStateFlow(GymLimits.SET_REST_DEFAULT_SECONDS)
    private val exerciseRestSeconds = MutableStateFlow(GymLimits.EXERCISE_REST_DEFAULT_SECONDS)
    private val summary = MutableStateFlow<GymWorkoutSummary?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val setRemovedUndoVisible = MutableStateFlow(false)
    private val exerciseRemovedUndoVisible = MutableStateFlow(false)
    private val leaveWorkout = MutableStateFlow(false)
    private val confirm = MutableStateFlow<FreeWorkoutConfirm?>(null)
    private val setupTitle = MutableStateFlow("")
    private val nowEpochMilli = MutableStateFlow(System.currentTimeMillis())
    private val latestSession = MutableStateFlow<GymWorkoutSession?>(null)

    private var clockJob: Job? = null
    private var undoJob: Job? = null
    private var pendingDeleteExerciseId: Long? = null
    private var pendingUndoSet: PendingUndoSet? = null
    private var pendingUndoExercise: PendingUndoExercise? = null

    private data class PendingUndoExercise(
        val workoutId: Long,
        val sortOrder: Int,
        val name: String,
        val trackingFields: Set<TrackingField>,
        val note: String,
        val sets: List<GymWorkoutSet>,
        val plannedSetCount: Int = 0,
        val skipped: Boolean = false,
        val routineExerciseId: Long? = null,
    )

    private val previousSeeds = MutableStateFlow<Map<Long, GymSetMeasurements>>(emptyMap())

    private val sessionFlow = workoutId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.observeSession(id)
    }

    private data class PendingUndoSet(
        val exerciseId: Long,
        val setNumber: Int,
        val measurements: GymSetMeasurements,
        val failure: Boolean,
    )

    private data class EditorState(
        val phase: FreeWorkoutPhase,
        val exerciseDraft: ExerciseDraft,
        val setDraft: SetDraft,
        val composingExercise: Boolean,
        val setEditorVisible: Boolean,
        val awaitingNextAction: Boolean,
        val preferredWeightUnit: WeightUnit,
        val setRestSeconds: Int,
        val exerciseRestSeconds: Int,
        val summary: GymWorkoutSummary?,
        val message: String?,
        val setRemovedUndoVisible: Boolean,
        val exerciseRemovedUndoVisible: Boolean,
        val leaveWorkout: Boolean,
        val confirm: FreeWorkoutConfirm?,
        val setupTitle: String,
    )

    private val editorFlow = combine(
        combine(phase, exerciseDraft, setDraft) { selectedPhase, draft, set ->
            Triple(selectedPhase, draft, set)
        },
        combine(composingExercise, setEditorVisible, awaitingNextAction) { composing, editor, awaiting ->
            Triple(composing, editor, awaiting)
        },
        combine(
            combine(preferredWeightUnit, setRestSeconds, exerciseRestSeconds) { unit, setRest, exerciseRest ->
                Triple(unit, setRest, exerciseRest)
            },
            combine(summary, message, setRemovedUndoVisible, exerciseRemovedUndoVisible) {
                    done, msg, setUndo, exerciseUndo ->
                listOf<Any?>(done, msg, setUndo, exerciseUndo)
            },
        ) { restPrefs, undoPrefs ->
            listOf(
                restPrefs.first,
                restPrefs.second,
                restPrefs.third,
                undoPrefs[0],
                undoPrefs[1],
                undoPrefs[2],
                undoPrefs[3],
            )
        },
        combine(leaveWorkout, confirm, setupTitle) { leave, conf, setup ->
            Triple(leave, conf, setup)
        },
    ) { triple, flags, prefs, leaveSetup ->
        EditorState(
            phase = triple.first,
            exerciseDraft = triple.second,
            setDraft = triple.third,
            composingExercise = flags.first,
            setEditorVisible = flags.second,
            awaitingNextAction = flags.third,
            preferredWeightUnit = prefs[0] as WeightUnit,
            setRestSeconds = prefs[1] as Int,
            exerciseRestSeconds = prefs[2] as Int,
            summary = prefs[3] as GymWorkoutSummary?,
            message = prefs[4] as String?,
            setRemovedUndoVisible = prefs[5] as Boolean,
            exerciseRemovedUndoVisible = prefs[6] as Boolean,
            leaveWorkout = leaveSetup.first,
            confirm = leaveSetup.second,
            setupTitle = leaveSetup.third,
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
            editor.phase == FreeWorkoutPhase.SETUP -> FreeWorkoutPhase.SETUP
            session?.status == GymWorkoutStatus.COMPLETED -> FreeWorkoutPhase.COMPLETED
            session?.restEndsAtEpochMilli != null &&
                GymLogic.remainingRestSeconds(session.restEndsAtEpochMilli, now) > 0 &&
                editor.phase !in setOf(
                    FreeWorkoutPhase.SETUP,
                    FreeWorkoutPhase.EDIT_EXERCISE,
                    FreeWorkoutPhase.END_OPTIONS,
                    FreeWorkoutPhase.COMPLETED,
                ) -> FreeWorkoutPhase.RESTING
            else -> editor.phase
        }
        FreeWorkoutUiState(
            loading = editor.phase != FreeWorkoutPhase.SETUP &&
                (id == null || (session == null && !editor.leaveWorkout)),
            phase = resolvedPhase,
            session = session,
            nowEpochMilli = now,
            workoutId = id,
            exerciseDraft = editor.exerciseDraft,
            setDraft = editor.setDraft,
            composingExercise = editor.composingExercise,
            setEditorVisible = editor.setEditorVisible,
            awaitingNextAction = editor.awaitingNextAction,
            preferredWeightUnit = editor.preferredWeightUnit,
            setRestSeconds = editor.setRestSeconds,
            exerciseRestSeconds = editor.exerciseRestSeconds,
            summary = editor.summary ?: session?.takeIf {
                it.status == GymWorkoutStatus.COMPLETED
            }?.let { GymLogic.summarize(it, it.endedAtEpochMilli ?: now) },
            message = editor.message,
            setRemovedUndoVisible = editor.setRemovedUndoVisible,
            exerciseRemovedUndoVisible = editor.exerciseRemovedUndoVisible,
            leaveWorkout = editor.leaveWorkout,
            confirm = editor.confirm,
            setupTitle = editor.setupTitle,
            workoutType = workoutType,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FreeWorkoutUiState(
            phase = FreeWorkoutPhase.SETUP,
            loading = false,
        ),
    )

    init {
        viewModelScope.launch {
            val profile = profileRepository.getProfile()
            val unit = when (profile?.gymWeightUnit?.uppercase()) {
                "LB" -> WeightUnit.LB
                else -> WeightUnit.KG
            }
            preferredWeightUnit.value = unit
            setRestSeconds.value = GymLimits.clampSetRestSeconds(
                profile?.gymSetRestSeconds ?: UserProfile.DEFAULT_GYM_SET_REST_SECONDS,
            )
            exerciseRestSeconds.value = GymLimits.clampExerciseRestSeconds(
                profile?.gymExerciseRestSeconds ?: UserProfile.DEFAULT_GYM_EXERCISE_REST_SECONDS,
            )
            if (workoutType == GymWorkoutType.ROUTINE) {
                resumeRoutineSession(unit)
                return@launch
            }
            val active = repository.getActiveSession(GymWorkoutType.FREE)
            if (active == null) {
                phase.value = FreeWorkoutPhase.SETUP
                setupTitle.value = ""
                return@launch
            }
            workoutId.value = active.id
            val session = repository.getSession(active.id)
            when {
                session == null || session.exercises.isEmpty() -> startComposingExercise()
                session.restEndsAtEpochMilli != null -> {
                    val remainingRest = GymLogic.remainingRestSeconds(
                        session.restEndsAtEpochMilli,
                        System.currentTimeMillis(),
                    )
                    if (remainingRest > 0) {
                        phase.value = FreeWorkoutPhase.RESTING
                        setEditorVisible.value = false
                        awaitingNextAction.value = false
                    } else {
                        repository.clearRest(session.id)
                        val refreshed = repository.getSession(active.id) ?: session
                        afterRest(refreshed)
                    }
                }
                else -> {
                    val current = currentExerciseOf(session)
                    if (current != null && current.sets.any { it.saved }) {
                        afterRest(session)
                    } else {
                        startSetEditorForExercise(current)
                    }
                }
            }
            startClock()
        }
        viewModelScope.launch {
            var previousUnit: WeightUnit? = null
            profileRepository.observeProfile().collect { profile ->
                val nextUnit = when (profile?.gymWeightUnit?.uppercase()) {
                    "LB" -> WeightUnit.LB
                    else -> WeightUnit.KG
                }
                val fromUnit = previousUnit
                if (fromUnit != null && fromUnit != nextUnit) {
                    if (setEditorVisible.value) {
                        setDraft.update { draft ->
                            val raw = draft.weight.toDoubleOrNull() ?: return@update draft
                            draft.copy(
                                weight = GymLogic.formatNumber(
                                    GymLogic.convertWeight(raw, from = fromUnit, to = nextUnit),
                                ),
                            )
                        }
                    }
                    workoutId.value?.let { id ->
                        repository.setWeightUnit(id, nextUnit)
                    }
                }
                previousUnit = nextUnit
                preferredWeightUnit.value = nextUnit
                setRestSeconds.value = GymLimits.clampSetRestSeconds(
                    profile?.gymSetRestSeconds ?: UserProfile.DEFAULT_GYM_SET_REST_SECONDS,
                )
                exerciseRestSeconds.value = GymLimits.clampExerciseRestSeconds(
                    profile?.gymExerciseRestSeconds ?: UserProfile.DEFAULT_GYM_EXERCISE_REST_SECONDS,
                )
            }
        }
    }

    private suspend fun handleRestEnded(session: GymWorkoutSession) {
        val kind = session.restKind
        repository.clearRest(session.id)
        phase.value = FreeWorkoutPhase.SESSION
        when (kind) {
            GymRestKind.EXERCISE -> moveToNextRoutineExerciseInternal()
            else -> afterRest(repository.getSession(session.id))
        }
    }

    private suspend fun startSetRest(session: GymWorkoutSession) {
        repository.startRest(session.id, setRestSeconds.value, GymRestKind.SET)
        setEditorVisible.value = false
        awaitingNextAction.value = false
        phase.value = FreeWorkoutPhase.RESTING
    }

    private suspend fun startExerciseRest(session: GymWorkoutSession) {
        repository.startRest(session.id, exerciseRestSeconds.value, GymRestKind.EXERCISE)
        setEditorVisible.value = false
        awaitingNextAction.value = false
        phase.value = FreeWorkoutPhase.RESTING
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
                    val exerciseName = currentExerciseOf(session)?.name
                    repository.clearRest(session.id)
                    handleRestEnded(session.copy(restKind = session.restKind))
                    signalRestComplete(exerciseName)
                }
                delay(250)
            }
        }
    }

    fun onSetupTitleChange(value: String) {
        setupTitle.value = value
    }

    fun startWorkoutFromSetup() {
        viewModelScope.launch {
            val unit = preferredWeightUnit.value
            val id = repository.startFreeWorkout(unit)
            repository.setWorkoutTitle(id, setupTitle.value)
            workoutId.value = id
            phase.value = FreeWorkoutPhase.SESSION
            startComposingExercise()
            startClock()
        }
    }

    fun onWorkoutTitleChange(value: String) {
        val id = workoutId.value ?: return
        viewModelScope.launch {
            repository.setWorkoutTitle(id, value)
        }
    }

    fun clearMessage() {
        message.value = null
    }

    fun startComposingExercise() {
        composingExercise.value = true
        awaitingNextAction.value = false
        exerciseDraft.value = ExerciseDraft()
        setDraft.value = SetDraft(setNumber = 1)
        setEditorVisible.value = true
        phase.value = FreeWorkoutPhase.SESSION
    }

    fun openEditExercise(exercise: GymWorkoutExercise) {
        exerciseDraft.value = ExerciseDraft(
            name = exercise.name,
            fields = exercise.trackingFields.ifEmpty {
                setOf(TrackingField.WEIGHT, TrackingField.REPS)
            },
            note = exercise.note,
            editingExerciseId = exercise.id,
            showFieldsEditor = false,
            showNoteEditor = exercise.note.isNotBlank(),
        )
        phase.value = FreeWorkoutPhase.EDIT_EXERCISE
    }

    fun cancelExerciseEditor() {
        phase.value = FreeWorkoutPhase.SESSION
        if (exerciseDraft.value.editingExerciseId != null) {
            exerciseDraft.value = ExerciseDraft()
        }
    }

    fun onExerciseNameChange(value: String) {
        exerciseDraft.update { it.copy(name = value) }
    }

    fun onExerciseNoteChange(value: String) {
        exerciseDraft.update { it.copy(note = value.take(GymLimits.NOTE_MAX_CHARS)) }
    }

    fun toggleShowFieldsEditor() {
        exerciseDraft.update { it.copy(showFieldsEditor = !it.showFieldsEditor) }
    }

    fun toggleShowNoteEditor() {
        exerciseDraft.update { it.copy(showNoteEditor = !it.showNoteEditor) }
    }

    fun toggleTrackingField(field: TrackingField) {
        exerciseDraft.update { draft ->
            val next = draft.fields.toMutableSet()
            if (field in next) {
                if (next.size <= 1) return@update draft
                next.remove(field)
            } else {
                next.add(field)
            }
            draft.copy(fields = next)
        }
    }

    /** Persist rename / fields / note for an existing exercise. */
    fun saveExerciseEdits() {
        viewModelScope.launch {
            val draft = exerciseDraft.value
            val editingId = draft.editingExerciseId ?: return@launch
            val fields = draft.fields.ifEmpty {
                setOf(TrackingField.WEIGHT, TrackingField.REPS)
            }
            if (draft.name.trim().isEmpty()) {
                message.value = "Name can't be empty."
                return@launch
            }
            runCatching {
                repository.updateExercise(
                    exerciseId = editingId,
                    name = draft.name,
                    trackingFields = fields,
                    note = draft.note,
                )
                phase.value = FreeWorkoutPhase.SESSION
                exerciseDraft.value = ExerciseDraft()
            }.onFailure {
                message.value = it.message ?: "Couldn't save exercise."
            }
        }
    }

    fun deleteExercise(exerciseId: Long) {
        viewModelScope.launch {
            val session = uiState.value.session ?: return@launch
            val exercise = session.exercises.firstOrNull { it.id == exerciseId } ?: return@launch
            pendingUndoExercise = PendingUndoExercise(
                workoutId = session.id,
                sortOrder = exercise.sortOrder,
                name = exercise.name,
                trackingFields = exercise.trackingFields,
                note = exercise.note,
                sets = exercise.sets.filter { it.saved },
                plannedSetCount = exercise.plannedSetCount,
                skipped = exercise.skipped,
                routineExerciseId = exercise.routineExerciseId,
            )
            repository.deleteExercise(exerciseId)
            phase.value = FreeWorkoutPhase.SESSION
            exerciseDraft.value = ExerciseDraft()
            setEditorVisible.value = false
            setDraft.value = SetDraft()
            val updated = workoutId.value?.let { repository.getSession(it) }
            if (updated == null || updated.exercises.isEmpty()) {
                if (workoutType == GymWorkoutType.ROUTINE) {
                    composingExercise.value = false
                    awaitingNextAction.value = false
                    setEditorVisible.value = false
                } else {
                    composingExercise.value = true
                    awaitingNextAction.value = false
                    exerciseDraft.value = ExerciseDraft()
                    setDraft.value = SetDraft(setNumber = 1)
                    setEditorVisible.value = true
                }
            } else {
                composingExercise.value = false
                enterAwaitingNextAction()
            }
            showExerciseRemovedUndo()
        }
    }

    fun undoDeleteExercise() {
        val pending = pendingUndoExercise ?: return
        undoJob?.cancel()
        exerciseRemovedUndoVisible.value = false
        pendingUndoExercise = null
        viewModelScope.launch {
            repository.restoreExercise(
                workoutId = pending.workoutId,
                sortOrder = pending.sortOrder,
                name = pending.name,
                trackingFields = pending.trackingFields,
                note = pending.note,
                sets = pending.sets,
                plannedSetCount = pending.plannedSetCount,
                skipped = pending.skipped,
                routineExerciseId = pending.routineExerciseId,
            )
            composingExercise.value = false
            enterAwaitingNextAction()
        }
    }

    fun dismissExerciseRemovedUndo() {
        undoJob?.cancel()
        exerciseRemovedUndoVisible.value = false
        pendingUndoExercise = null
    }

    /** Removes a saved set immediately; Undo restores it for a few seconds. */
    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            val session = uiState.value.session ?: return@launch
            val set = session.exercises
                .asSequence()
                .flatMap { it.sets.asSequence() }
                .firstOrNull { it.id == setId }
                ?: return@launch
            if (!set.saved) return@launch
            pendingUndoSet = PendingUndoSet(
                exerciseId = set.workoutExerciseId,
                setNumber = set.setNumber,
                measurements = set.measurements,
                failure = set.failure,
            )
            repository.deleteSet(setId)
            if (setDraft.value.setId == setId) {
                setEditorVisible.value = false
                setDraft.value = SetDraft()
            }
            phase.value = FreeWorkoutPhase.SESSION
            if (!composingExercise.value) {
                enterAwaitingNextAction()
            }
            showSetRemovedUndo()
        }
    }

    fun undoDeleteSet() {
        val pending = pendingUndoSet ?: return
        undoJob?.cancel()
        setRemovedUndoVisible.value = false
        pendingUndoSet = null
        viewModelScope.launch {
            repository.restoreSet(
                exerciseId = pending.exerciseId,
                measurements = pending.measurements,
                failure = pending.failure,
                setNumber = pending.setNumber,
            )
            enterAwaitingNextAction()
        }
    }

    fun dismissSetRemovedUndo() {
        undoJob?.cancel()
        setRemovedUndoVisible.value = false
        pendingUndoSet = null
    }

    fun discardWorkout() {
        confirm.value = FreeWorkoutConfirm.DISCARD_WORKOUT
    }

    fun dismissConfirm() {
        confirm.value = null
        pendingDeleteExerciseId = null
    }

    fun confirmPendingAction() {
        when (confirm.value) {
            FreeWorkoutConfirm.DELETE_EXERCISE -> {
                dismissConfirm()
            }
            FreeWorkoutConfirm.DELETE_SET -> {
                dismissConfirm()
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
            composingExercise.value = false
            val session = repository.getSession(id) ?: return@launch
            val exercise = session.exercises.getOrNull(index)
            if (exercise?.skipped == true) {
                repository.setExerciseSkipped(exercise.id, false)
            }
            afterRest(repository.getSession(id) ?: session)
        }
    }

    /** Finished with this exercise - open the next empty exercise inline. */
    fun finishExercise() {
        clearRestCompleteAlert()
        if (workoutType == GymWorkoutType.ROUTINE) {
            viewModelScope.launch {
                val session = uiState.value.session ?: return@launch
                startExerciseRest(session)
            }
        } else {
            startComposingExercise()
        }
    }

    fun skipExercise() {
        clearRestCompleteAlert()
        viewModelScope.launch {
            val session = uiState.value.session ?: return@launch
            val exercise = uiState.value.currentExercise ?: return@launch
            repository.setExerciseSkipped(exercise.id, true)
            startExerciseRest(session)
        }
    }

    fun addNewSet() {
        clearRestCompleteAlert()
        val exercise = uiState.value.currentExercise
            ?: latestSession.value?.let { currentExerciseOf(it) }
            ?: return
        openNewSet(exercise)
    }

    fun openEditSet(exercise: GymWorkoutExercise, set: GymWorkoutSet) {
        viewModelScope.launch {
            val session = uiState.value.session ?: return@launch
            val index = session.exercises.indexOfFirst { it.id == exercise.id }
            if (index >= 0 && index != session.currentExerciseIndex) {
                repository.setCurrentExerciseIndex(session.id, index)
            }
            composingExercise.value = false
            awaitingNextAction.value = false
            setDraft.value = set.toDraft(
                setId = set.id,
                exerciseId = exercise.id,
                setNumber = set.setNumber,
                displayUnit = preferredWeightUnit.value,
            )
            setEditorVisible.value = true
            phase.value = FreeWorkoutPhase.SESSION
        }
    }

    fun clearEditingSet() {
        enterAwaitingNextAction()
    }

    fun onSetDraftChange(transform: (SetDraft) -> SetDraft) {
        setDraft.update(transform)
    }

    fun stepWeight(up: Boolean) {
        setDraft.update { it.copy(weight = GymLogic.stepWeightValue(it.weight, up)) }
    }

    fun stepReps(up: Boolean) {
        setDraft.update { it.copy(reps = GymLogic.stepWholeValue(it.reps, up, minimum = 1)) }
    }

    fun stepIncline(up: Boolean) {
        setDraft.update { it.copy(incline = GymLogic.stepWholeValue(it.incline, up, minimum = 0)) }
    }

    fun stepResistance(up: Boolean) {
        setDraft.update {
            it.copy(resistance = GymLogic.stepWholeValue(it.resistance, up, minimum = 0))
        }
    }

    fun stepRounds(up: Boolean) {
        setDraft.update { it.copy(rounds = GymLogic.stepWholeValue(it.rounds, up, minimum = 1)) }
    }

    fun saveSet() {
        viewModelScope.launch {
            val session = uiState.value.session ?: return@launch
            val draft = setDraft.value
            val unit = preferredWeightUnit.value

            if (composingExercise.value) {
                val name = exerciseDraft.value.name.trim()
                val fields = exerciseDraft.value.fields.ifEmpty {
                    setOf(TrackingField.WEIGHT, TrackingField.REPS)
                }
                if (name.isEmpty()) {
                    message.value = "Name can't be empty."
                    return@launch
                }
                if (fields.isEmpty()) {
                    message.value = "Pick at least one tracking field."
                    return@launch
                }
                val measurements = draft.toMeasurements(unit)
                if (!GymLogic.allSelectedFieldsFilled(fields, measurements)) {
                    message.value = "Fill every tracking field."
                    return@launch
                }
                runCatching {
                    val exerciseId = repository.addExercise(
                        workoutId = session.id,
                        name = name,
                        trackingFields = fields,
                        note = exerciseDraft.value.note,
                    )
                    repository.addSet(
                        exerciseId = exerciseId,
                        measurements = measurements,
                        failure = draft.failure,
                        saved = true,
                    )
                    composingExercise.value = false
                    exerciseDraft.value = ExerciseDraft()
                    setEditorVisible.value = false
                    awaitingNextAction.value = false
                    repository.startRest(session.id, setRestSeconds.value, GymRestKind.SET)
                    phase.value = FreeWorkoutPhase.RESTING
                }.onFailure {
                    message.value = it.message ?: "Couldn't save set."
                }
                return@launch
            }

            val exercise = uiState.value.currentExercise ?: return@launch
            val fields = exercise.trackingFields
            val measurements = draft.toMeasurements(unit)
            if (!GymLogic.allSelectedFieldsFilled(fields, measurements)) {
                message.value = "Fill every tracking field."
                return@launch
            }
            runCatching {
                val existingId = draft.setId
                val isNew = existingId == null
                if (isNew) {
                    repository.addSet(
                        exerciseId = exercise.id,
                        measurements = measurements,
                        failure = draft.failure,
                        saved = true,
                    )
                    setEditorVisible.value = false
                    awaitingNextAction.value = false
                    val planned = exercise.plannedSetCount
                    val isLastPlanned = workoutType == GymWorkoutType.ROUTINE &&
                        GymLogic.isLastPlannedSetNumber(draft.setNumber, planned)
                    if (isLastPlanned) {
                        startExerciseRest(session)
                    } else {
                        startSetRest(session)
                    }
                } else {
                    repository.updateSet(
                        setId = existingId,
                        measurements = measurements,
                        failure = draft.failure,
                        saved = true,
                        setNumber = null,
                    )
                    enterAwaitingNextAction()
                }
            }.onFailure {
                message.value = it.message ?: "Couldn't save set."
            }
        }
    }

    fun skipRest() {
        viewModelScope.launch {
            val id = workoutId.value ?: return@launch
            val session = repository.getSession(id) ?: return@launch
            handleRestEnded(session)
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

    private fun enterAwaitingNextAction() {
        setEditorVisible.value = false
        awaitingNextAction.value = true
        composingExercise.value = false
        setDraft.value = SetDraft()
        phase.value = FreeWorkoutPhase.SESSION
    }

    private fun afterRest(session: GymWorkoutSession?) {
        val current = session?.let { currentExerciseOf(it) }
        if (workoutType == GymWorkoutType.ROUTINE &&
            current != null &&
            !current.skipped &&
            GymLogic.shouldAutoOpenNextPlannedSet(
                savedCount = current.sets.count { it.saved },
                plannedSetCount = current.plannedSetCount,
            )
        ) {
            openNewSet(current)
        } else if (workoutType == GymWorkoutType.ROUTINE &&
            current != null &&
            current.sets.none { it.saved }
        ) {
            startSetEditorForExercise(current)
        } else {
            enterAwaitingNextAction()
        }
    }

    private fun openNewSet(exercise: GymWorkoutExercise) {
        awaitingNextAction.value = false
        composingExercise.value = false
        setDraft.value = draftForNewSet(exercise)
        setEditorVisible.value = true
        phase.value = FreeWorkoutPhase.SESSION
    }

    private fun startSetEditorForExercise(exercise: GymWorkoutExercise?) {
        if (exercise == null) {
            if (workoutType == GymWorkoutType.ROUTINE) {
                composingExercise.value = false
                setEditorVisible.value = false
                awaitingNextAction.value = false
                return
            }
            startComposingExercise()
            return
        }
        composingExercise.value = false
        awaitingNextAction.value = false
        setDraft.value = draftForNewSet(exercise)
        setEditorVisible.value = true
    }

    private fun draftForNewSet(exercise: GymWorkoutExercise): SetDraft {
        val measurements = GymLogic.seedMeasurementsForNextSet(
            currentSets = exercise.sets,
            previousOccurrenceLastSet = previousSeeds.value[exercise.id],
        )
        val seeded = GymWorkoutSet(
            workoutExerciseId = exercise.id,
            setNumber = GymLogic.nextSetNumber(exercise.sets),
            measurements = measurements,
            failure = false,
            saved = false,
        )
        return seeded.toDraft(
            setId = null,
            exerciseId = exercise.id,
            setNumber = seeded.setNumber,
            displayUnit = preferredWeightUnit.value,
        ).copy(failure = false)
    }

    private fun moveToNextRoutineExerciseInternal() {
        viewModelScope.launch {
            val session = uiState.value.session ?: return@launch
            val currentIndex = session.currentExerciseIndex
            val nextIndex = session.exercises.indices.firstOrNull { index ->
                index > currentIndex && !session.exercises[index].skipped
            }
            composingExercise.value = false
            if (nextIndex == null) {
                setEditorVisible.value = false
                awaitingNextAction.value = false
                setDraft.value = SetDraft()
                phase.value = FreeWorkoutPhase.SESSION
                return@launch
            }
            repository.setCurrentExerciseIndex(session.id, nextIndex)
            val updated = repository.getSession(session.id) ?: session
            startSetEditorForExercise(currentExerciseOf(updated))
        }
    }

    private fun moveToNextRoutineExercise() {
        moveToNextRoutineExerciseInternal()
    }

    private suspend fun resumeRoutineSession(unit: WeightUnit) {
        val id = repository.ensureActiveRoutineWorkout(unit)
        if (id == null) {
            leaveWorkout.value = true
            return
        }
        workoutId.value = id
        phase.value = FreeWorkoutPhase.SESSION
        val session = repository.getSession(id) ?: return
        loadPreviousSeeds(session)
        when {
            session.restEndsAtEpochMilli != null -> {
                val remainingRest = GymLogic.remainingRestSeconds(
                    session.restEndsAtEpochMilli,
                    System.currentTimeMillis(),
                )
                if (remainingRest > 0) {
                    phase.value = FreeWorkoutPhase.RESTING
                    setEditorVisible.value = false
                    awaitingNextAction.value = false
                } else {
                    repository.clearRest(session.id)
                    afterRest(repository.getSession(id) ?: session)
                }
            }
            else -> afterRest(session)
        }
        startClock()
    }

    private suspend fun loadPreviousSeeds(session: GymWorkoutSession) {
        previousSeeds.value = if (session.type == GymWorkoutType.ROUTINE) {
            repository.previousOccurrenceSeeds(session)
        } else {
            emptyMap()
        }
    }

    private fun showSetRemovedUndo() {
        dismissExerciseRemovedUndo()
        undoJob?.cancel()
        setRemovedUndoVisible.value = true
        undoJob = viewModelScope.launch {
            delay(5_000)
            setRemovedUndoVisible.value = false
            pendingUndoSet = null
        }
    }

    private fun showExerciseRemovedUndo() {
        dismissSetRemovedUndo()
        undoJob?.cancel()
        exerciseRemovedUndoVisible.value = true
        undoJob = viewModelScope.launch {
            delay(5_000)
            exerciseRemovedUndoVisible.value = false
            pendingUndoExercise = null
        }
    }

    private fun signalRestComplete(exerciseName: String?) {
        val app = getApplication<Application>()
        vibrateRestComplete(app)
        NotificationChannelManager.postRestCompleteNotification(
            app,
            exerciseName,
            destination = if (workoutType == GymWorkoutType.ROUTINE) {
                com.deepak.flow.core.widget.WidgetLaunch.DEST_GYM_ROUTINE_WORKOUT
            } else {
                com.deepak.flow.core.widget.WidgetLaunch.DEST_GYM_FREE_WORKOUT
            },
        )
    }

    private fun clearRestCompleteAlert() {
        NotificationChannelManager.cancelRestCompleteNotification(getApplication())
    }

    private fun currentExerciseOf(session: GymWorkoutSession): GymWorkoutExercise? {
        if (session.exercises.isEmpty()) return null
        val index = session.currentExerciseIndex.coerceIn(0, session.exercises.lastIndex)
        return session.exercises[index]
    }

    override fun onCleared() {
        clockJob?.cancel()
        undoJob?.cancel()
        super.onCleared()
    }
}

class FreeWorkoutViewModelFactory(
    private val application: FlowApplication,
    private val workoutType: GymWorkoutType = GymWorkoutType.FREE,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FreeWorkoutViewModel::class.java)) {
            return FreeWorkoutViewModel(application, workoutType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}

internal fun GymWorkoutSet?.toDraft(
    setId: Long?,
    exerciseId: Long,
    setNumber: Int,
    displayUnit: WeightUnit,
): SetDraft {
    val measurements = this?.measurements ?: GymSetMeasurements()
    val duration = measurements.durationSeconds ?: 0
    val storedWeight = measurements.weight
    val storedUnit = measurements.weightUnit ?: displayUnit
    return SetDraft(
        setId = setId,
        exerciseId = exerciseId,
        setNumber = setNumber,
        weight = storedWeight?.let {
            GymLogic.formatWeight(it, storedUnit = storedUnit, displayUnit = displayUnit)
        }.orEmpty(),
        reps = measurements.reps?.toString().orEmpty(),
        durationMinutes = if (duration > 0) (duration / 60).toString() else "",
        durationSeconds = if (duration > 0) (duration % 60).toString() else "",
        distance = measurements.distance?.let { GymLogic.formatNumber(it) }.orEmpty(),
        speed = measurements.speed?.let { GymLogic.formatNumber(it) }.orEmpty(),
        incline = measurements.incline?.let { GymLogic.formatNumber(it).substringBefore('.') }.orEmpty(),
        resistance = measurements.resistance?.let {
            GymLogic.formatNumber(it).substringBefore('.')
        }.orEmpty(),
        rounds = measurements.rounds?.toString().orEmpty(),
        failure = this?.failure == true,
    )
}

internal fun SetDraft.toMeasurements(sessionUnit: WeightUnit): GymSetMeasurements {
    val minutes = durationMinutes.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val seconds = (durationSeconds.toIntOrNull()?.coerceIn(0, 59) ?: 0)
    val durationTotal = (minutes * 60 + seconds).takeIf { it > 0 }
    return GymSetMeasurements(
        weight = weight.toDoubleOrNull()?.takeIf { it > 0.0 },
        weightUnit = sessionUnit.takeIf { weight.toDoubleOrNull() != null },
        reps = reps.toIntOrNull()?.takeIf { it >= 1 },
        durationSeconds = durationTotal,
        distance = distance.toDoubleOrNull()?.takeIf { it > 0.0 },
        speed = speed.toDoubleOrNull()?.takeIf { it > 0.0 },
        incline = incline.toIntOrNull()?.toDouble(),
        resistance = resistance.toIntOrNull()?.toDouble()?.takeIf { it > 0.0 },
        rounds = rounds.toIntOrNull()?.takeIf { it >= 1 },
    )
}
