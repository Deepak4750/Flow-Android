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
import com.deepak.flow.core.gym.GymRestCompletionPolicy
import com.deepak.flow.core.gym.GymSetDraftSnapshot
import com.deepak.flow.core.gym.GymSetMeasurements
import com.deepak.flow.core.gym.GymWorkoutExercise
import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.gym.GymSetEditPolicy
import com.deepak.flow.core.gym.GymRestUiPolicy
import com.deepak.flow.core.gym.GymWorkoutExercisePolicy
import com.deepak.flow.core.gym.GymWorkoutFocusPolicy
import com.deepak.flow.core.gym.GymWorkoutSwitchPolicy
import com.deepak.flow.core.gym.GymWorkoutSet
import com.deepak.flow.core.gym.GymWorkoutStatus
import com.deepak.flow.core.gym.GymWorkoutSummary
import com.deepak.flow.core.gym.GymWorkoutType
import com.deepak.flow.core.gym.TrackingField
import com.deepak.flow.core.gym.WeightUnit
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
    val canonicalExerciseId: String = "",
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
    /** When set, SessionPane scrolls to this exercise id (stable identity). */
    val scrollToExerciseKey: Long? = null,
    val exerciseNameSuggestions: List<String> = emptyList(),
    val exerciseSearchResults: List<com.deepak.flow.core.gym.GymExerciseSearchHit> = emptyList(),
    val pendingSwitchExerciseIndex: Int? = null,
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
            return GymWorkoutExercisePolicy.firstIncompleteExercise(active.exercises)
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

    val hasUnsavedComposerDraft: Boolean
        get() {
            if (!composingExercise || isRoutine) return false
            if (exerciseDraft.name.isNotBlank()) return true
            if (!setEditorVisible) return false
            val fields = activeTrackingFields.ifEmpty {
                setOf(TrackingField.WEIGHT, TrackingField.REPS)
            }
            val measurements = setDraft.toMeasurements(displayWeightUnit)
            return GymLogic.hasMeaningfulMeasurement(fields, measurements) ||
                GymLogic.allSelectedFieldsFilled(fields, measurements)
        }

    val workoutCompletionBlockReason: String?
        get() = GymWorkoutExercisePolicy.workoutCompletionBlockReason(
            session = session,
            hasUnsavedComposerDraft = hasUnsavedComposerDraft,
        )

    val canCompleteWorkout: Boolean
        get() = workoutCompletionBlockReason == null

    val isResting: Boolean
        get() = phase == FreeWorkoutPhase.RESTING

    val hasActiveEditSession: Boolean
        get() = GymWorkoutFocusPolicy.hasActiveEditSession(
            composingExercise = composingExercise,
            setEditorVisible = setEditorVisible,
            awaitingNextAction = awaitingNextAction,
        )

    val activeExerciseId: Long?
        get() = currentExercise?.id
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
    private val scrollToExerciseKey = MutableStateFlow<Long?>(null)
    private val restUiSuppressed = MutableStateFlow(false)
    private val exerciseNameSuggestions = MutableStateFlow<List<String>>(emptyList())
    private val exerciseSearchResults = MutableStateFlow<List<com.deepak.flow.core.gym.GymExerciseSearchHit>>(emptyList())
    private val pendingSwitchExerciseIndex = MutableStateFlow<Int?>(null)

    private var clockJob: Job? = null
    private var undoJob: Job? = null
    private var pendingDeleteExerciseId: Long? = null
    private var pendingUndoSet: PendingUndoSet? = null
    private var pendingUndoExercise: PendingUndoExercise? = null

    private data class PendingUndoExercise(
        val workoutId: Long,
        val sortOrder: Int,
        val name: String,
        val canonicalExerciseId: String,
        val trackingFields: Set<TrackingField>,
        val note: String,
        val sets: List<GymWorkoutSet>,
        val plannedSetCount: Int = 0,
        val skipped: Boolean = false,
        val completedAtEpochMilli: Long? = null,
        val routineExerciseId: Long? = null,
        val exerciseStableKey: String? = null,
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
        combine(
            scrollToExerciseKey,
            restUiSuppressed,
            exerciseNameSuggestions,
            exerciseSearchResults,
            pendingSwitchExerciseIndex,
        ) { scrollKey, restSuppressed, nameSuggestions, searchResults, switchIndex ->
            listOf(scrollKey, restSuppressed, nameSuggestions, searchResults, switchIndex)
        },
    ) { core, editor, extras ->
        val (id, session, now) = core
        val scrollKey = extras[0] as Long?
        val restSuppressed = extras[1] as Boolean
        val nameSuggestions = extras[2] as List<String>
        val searchResults = extras[3] as List<com.deepak.flow.core.gym.GymExerciseSearchHit>
        val switchIndex = extras[4] as Int?
        latestSession.value = session
        if (session?.restEndsAtEpochMilli == null) {
            restUiSuppressed.value = false
        }
        val resolvedPhase = when {
            editor.phase == FreeWorkoutPhase.SETUP -> FreeWorkoutPhase.SETUP
            session?.status == GymWorkoutStatus.COMPLETED -> FreeWorkoutPhase.COMPLETED
            session?.restEndsAtEpochMilli != null &&
                !restSuppressed &&
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
            scrollToExerciseKey = scrollKey,
            exerciseNameSuggestions = nameSuggestions,
            exerciseSearchResults = searchResults,
            pendingSwitchExerciseIndex = switchIndex,
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
            exerciseNameSuggestions.value = repository.getExerciseNameSuggestions()
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
            sessionFlow.collect { session ->
                if (session != null && session.status == GymWorkoutStatus.ACTIVE) {
                    loadPreviousSeeds(session)
                }
            }
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
        restUiSuppressed.value = true
        latestSession.value = session.copy(
            restEndsAtEpochMilli = null,
            restKind = GymRestKind.NONE,
        )
        repository.clearRest(session.id)
        phase.value = FreeWorkoutPhase.SESSION
        when (kind) {
            GymRestKind.EXERCISE -> afterExerciseRestEnded(session.id)
            else -> {
                afterRest(repository.getSession(session.id))
            }
        }
    }

    private suspend fun afterExerciseRestEnded(workoutId: Long) {
        val session = repository.getSession(workoutId) ?: return
        val nextIndex = GymRestCompletionPolicy.nextExerciseIndexAfterExerciseRest(
            exercises = session.exercises,
            fromIndex = session.currentExerciseIndex,
        )
        if (nextIndex == null) {
            setEditorVisible.value = false
            awaitingNextAction.value = false
            composingExercise.value = false
            setDraft.value = SetDraft()
            phase.value = FreeWorkoutPhase.SESSION
            message.value = null
            return
        }
        if (nextIndex != session.currentExerciseIndex) {
            repository.setCurrentExerciseIndex(workoutId, nextIndex)
        }
        val updated = repository.getSession(workoutId) ?: session
        val nextExercise = updated.exercises.getOrNull(nextIndex) ?: return
        awaitingNextAction.value = false
        composingExercise.value = false
        setDraft.value = draftForNewSet(nextExercise)
        setEditorVisible.value = true
        phase.value = FreeWorkoutPhase.SESSION
        message.value = null
        scrollToExerciseKey.value = nextExercise.id
    }

    private suspend fun markCurrentExerciseCompleted(session: GymWorkoutSession) {
        val exercise = currentExerciseOf(session) ?: return
        repository.setExerciseCompleted(exercise.id, System.currentTimeMillis())
    }

    private fun requestScrollToCurrentExercise() {
        val exercise = uiState.value.currentExercise ?: return
        scrollToExerciseKey.value = exercise.id
    }

    fun clearScrollToExercise() {
        scrollToExerciseKey.value = null
    }

    private suspend fun startSetRest(session: GymWorkoutSession) {
        repository.startRest(session.id, setRestSeconds.value, GymRestKind.SET)
        setEditorVisible.value = false
        awaitingNextAction.value = false
        phase.value = FreeWorkoutPhase.RESTING
    }

    private suspend fun startExerciseRest(session: GymWorkoutSession) {
        markCurrentExerciseCompleted(session)
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
                if (session != null &&
                    ends != null &&
                    GymLogic.remainingRestSeconds(ends, now) <= 0
                ) {
                    handleRestEnded(session.copy(restKind = session.restKind))
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
            canonicalExerciseId = exercise.exerciseId,
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
        exerciseDraft.update { it.copy(name = value, canonicalExerciseId = "") }
        viewModelScope.launch {
            exerciseSearchResults.value = repository.searchExercises(value)
        }
    }

    fun onExerciseSelected(canonicalExerciseId: String, displayName: String) {
        exerciseDraft.update {
            it.copy(name = displayName, canonicalExerciseId = canonicalExerciseId)
        }
        exerciseSearchResults.value = emptyList()
    }

    fun onCreateCustomExercise(displayName: String) {
        viewModelScope.launch {
            val selection = repository.createCustomExercise(displayName)
            onExerciseSelected(selection.exerciseId, selection.displayName)
        }
    }

    fun browseExercises(
        query: String,
        muscleFilter: com.deepak.flow.core.gym.GymMuscleGroup? = null,
        equipmentFilter: com.deepak.flow.core.gym.GymEquipment? = null,
    ) {
        viewModelScope.launch {
            exerciseSearchResults.value = repository.browseExercises(
                query = query,
                muscleFilter = muscleFilter,
                equipmentFilter = equipmentFilter,
            )
        }
    }

    fun saveExerciseMetadataOverride(
        exerciseId: String,
        displayName: String?,
        primaryMuscle: com.deepak.flow.core.gym.GymMuscleGroup?,
        secondaryMuscles: List<com.deepak.flow.core.gym.GymMuscleGroup>,
        equipment: com.deepak.flow.core.gym.GymEquipment?,
    ) {
        viewModelScope.launch {
            if (com.deepak.flow.core.gym.GymExerciseIdentity.isBuiltinId(exerciseId)) {
                repository.saveBuiltinExerciseOverride(
                    exerciseId = exerciseId,
                    displayName = displayName,
                    primaryMuscle = primaryMuscle,
                    secondaryMuscles = secondaryMuscles,
                    equipment = equipment,
                )
            } else if (com.deepak.flow.core.gym.GymExerciseIdentity.isCustomId(exerciseId)) {
                repository.saveCustomExerciseMetadata(
                    exerciseId = exerciseId,
                    displayName = displayName,
                    primaryMuscle = primaryMuscle,
                    secondaryMuscles = secondaryMuscles,
                    equipment = equipment,
                )
            }
        }
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
        val editingId = exerciseDraft.value.editingExerciseId
        val exercise = uiState.value.session?.exercises?.firstOrNull { it.id == editingId }
        if (exercise != null && !GymSetEditPolicy.canEditTrackingFields(exercise)) {
            message.value = "Finish or remove saved sets before changing tracking fields."
            return
        }
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
                    canonicalExerciseId = draft.canonicalExerciseId,
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
                canonicalExerciseId = exercise.exerciseId,
                trackingFields = exercise.trackingFields,
                note = exercise.note,
                sets = exercise.sets.filter { it.saved },
                plannedSetCount = exercise.plannedSetCount,
                skipped = exercise.skipped,
                completedAtEpochMilli = exercise.completedAtEpochMilli,
                routineExerciseId = exercise.routineExerciseId,
                exerciseStableKey = exercise.exerciseStableKey,
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
                completedAtEpochMilli = pending.completedAtEpochMilli,
                routineExerciseId = pending.routineExerciseId,
                exerciseStableKey = pending.exerciseStableKey,
                canonicalExerciseId = pending.canonicalExerciseId,
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
            val session = repository.getSession(id) ?: return@launch
            val exercise = session.exercises.getOrNull(index) ?: return@launch
            val activeExercise = currentExerciseOf(session)
            val resting = GymRestUiPolicy.isRestActive(
                session.restEndsAtEpochMilli,
                System.currentTimeMillis(),
                restUiSuppressed.value,
            )
            val trackingFields = activeExercise?.trackingFields.orEmpty()
            if (!GymWorkoutSwitchPolicy.canSwitchToTarget(
                    activeExercise = activeExercise,
                    targetExercise = exercise,
                    trackingFields = trackingFields,
                    setDraft = setDraft.value.toSnapshot(),
                    setEditorVisible = setEditorVisible.value,
                    awaitingNextAction = awaitingNextAction.value,
                    composingExercise = composingExercise.value,
                    isResting = resting,
                )
            ) {
                return@launch
            }
            performSelectExercise(id, index, session)
        }
    }

    fun confirmSwitchExercise() {
        val index = pendingSwitchExerciseIndex.value ?: return
        pendingSwitchExerciseIndex.value = null
        viewModelScope.launch {
            val id = workoutId.value ?: return@launch
            val session = repository.getSession(id) ?: return@launch
            setEditorVisible.value = false
            awaitingNextAction.value = false
            composingExercise.value = false
            setDraft.value = SetDraft()
            exerciseDraft.value = ExerciseDraft()
            performSelectExercise(id, index, session)
        }
    }

    fun dismissSwitchExercise() {
        pendingSwitchExerciseIndex.value = null
    }

    private suspend fun performSelectExercise(
        workoutId: Long,
        index: Int,
        session: GymWorkoutSession,
    ) {
        val exercise = session.exercises.getOrNull(index) ?: return
        val resting = GymRestUiPolicy.isRestActive(
            session.restEndsAtEpochMilli,
            System.currentTimeMillis(),
            restUiSuppressed.value,
        )
        repository.setCurrentExerciseIndex(workoutId, index)
        composingExercise.value = false
        setEditorVisible.value = false
        awaitingNextAction.value = false
        setDraft.value = SetDraft()
        exerciseDraft.value = ExerciseDraft()
        scrollToExerciseKey.value = exercise.id
        if (exercise.skipped) {
            phase.value = FreeWorkoutPhase.SESSION
            return
        }
        if (GymRestUiPolicy.shouldOpenEditorAfterExerciseSelect(resting)) {
            val updated = repository.getSession(workoutId) ?: session
            afterRest(updated)
        }
    }

    fun finishExercise() {
        clearRestCompleteAlert()
        if (workoutType == GymWorkoutType.ROUTINE) {
            viewModelScope.launch {
                val session = uiState.value.session ?: return@launch
                startExerciseRest(session)
            }
        } else {
            viewModelScope.launch {
                val session = uiState.value.session ?: return@launch
                markCurrentExerciseCompleted(session)
                startComposingExercise()
            }
        }
    }

    fun skipExercise() {
        clearRestCompleteAlert()
        viewModelScope.launch {
            val session = uiState.value.session ?: return@launch
            val exercise = uiState.value.currentExercise ?: return@launch
            repository.skipRemainingPlannedSets(exercise.id)
            repository.setExerciseSkipped(exercise.id, true)
            setEditorVisible.value = false
            awaitingNextAction.value = false
            setDraft.value = SetDraft()
            startExerciseRest(session)
        }
    }

    fun unskipExercise(exerciseId: Long? = null) {
        clearRestCompleteAlert()
        viewModelScope.launch {
            val session = uiState.value.session ?: return@launch
            val exercise = exerciseId?.let { id ->
                session.exercises.firstOrNull { it.id == id }
            } ?: uiState.value.currentExercise ?: return@launch
            if (!exercise.skipped) return@launch
            repository.clearSkippedSets(exercise.id)
            repository.setExerciseSkipped(exercise.id, false)
            val refreshed = repository.getSession(session.id) ?: return@launch
            val index = refreshed.exercises.indexOfFirst { it.id == exercise.id }
            if (index >= 0) {
                performSelectExercise(session.id, index, refreshed)
            }
        }
    }

    fun saveExercise() = finishExercise()

    fun addNewSet() {
        clearRestCompleteAlert()
        val exercise = uiState.value.currentExercise
            ?: latestSession.value?.let { currentExerciseOf(it) }
            ?: return
        openNewSet(exercise)
    }

    fun openEditSet(exercise: GymWorkoutExercise, set: GymWorkoutSet) {
        viewModelScope.launch {
            if (!GymWorkoutExercisePolicy.isExerciseEditable(exercise)) return@launch
            val session = uiState.value.session ?: return@launch
            if (GymRestUiPolicy.isRestActive(session.restEndsAtEpochMilli, System.currentTimeMillis())) {
                return@launch
            }
            if (phase.value == FreeWorkoutPhase.RESTING) return@launch
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
                        name = exerciseDraft.value.name,
                        trackingFields = fields,
                        note = exerciseDraft.value.note,
                        canonicalExerciseId = exerciseDraft.value.canonicalExerciseId,
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
            repository.cancelScheduledRestAlert()
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
            val blockReason = uiState.value.workoutCompletionBlockReason
            if (blockReason != null) {
                message.value = blockReason
                return@launch
            }
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
        if (current != null &&
            GymRestCompletionPolicy.shouldOpenSetEditorAfterSetRest(current)
        ) {
            openNewSet(current)
        } else {
            enterAwaitingNextAction()
        }
    }

    private fun openNewSet(exercise: GymWorkoutExercise) {
        viewModelScope.launch {
            val session = uiState.value.session ?: latestSession.value ?: return@launch
            val index = session.exercises.indexOfFirst { it.id == exercise.id }
            if (index >= 0 && index != session.currentExerciseIndex) {
                repository.setCurrentExerciseIndex(session.id, index)
            }
            awaitingNextAction.value = false
            composingExercise.value = false
            setDraft.value = draftForNewSet(exercise)
            setEditorVisible.value = true
            phase.value = FreeWorkoutPhase.SESSION
        }
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
        viewModelScope.launch {
            val session = uiState.value.session ?: latestSession.value
            if (exercise != null && session != null) {
                val index = session.exercises.indexOfFirst { it.id == exercise.id }
                if (index >= 0 && index != session.currentExerciseIndex) {
                    repository.setCurrentExerciseIndex(session.id, index)
                }
            }
            setDraft.value = draftForNewSet(exercise)
            setEditorVisible.value = true
        }
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
                    val restKind = session.restKind
                    repository.clearRest(session.id)
                    if (restKind == GymRestKind.EXERCISE) {
                        afterExerciseRestEnded(id)
                    } else {
                        afterRest(repository.getSession(id) ?: session)
                    }
                    requestScrollToCurrentExercise()
                }
            }
            else -> afterRest(session)
        }
        startClock()
    }

    private suspend fun loadPreviousSeeds(session: GymWorkoutSession) {
        previousSeeds.value = repository.previousPerformanceSeeds(session)
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

internal fun SetDraft.toSnapshot(): GymSetDraftSnapshot = GymSetDraftSnapshot(
    setNumber = setNumber,
    weight = weight,
    reps = reps,
    durationMinutes = durationMinutes,
    durationSeconds = durationSeconds,
    distance = distance,
    speed = speed,
    incline = incline,
    resistance = resistance,
    rounds = rounds,
)

internal fun SetDraft.toMeasurements(sessionUnit: WeightUnit): GymSetMeasurements {
    val minutes = durationMinutes.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val seconds = (durationSeconds.toIntOrNull()?.coerceIn(0, 59) ?: 0)
    val durationTotal = (minutes * 60 + seconds).takeIf { it > 0 }
    return GymSetMeasurements(
        weight = weight.toDoubleOrNull()?.takeIf { it >= 0.0 },
        weightUnit = sessionUnit.takeIf { weight.isNotBlank() && weight.toDoubleOrNull() != null },
        reps = reps.toIntOrNull()?.takeIf { it >= 1 },
        durationSeconds = durationTotal,
        distance = distance.toDoubleOrNull()?.takeIf { it > 0.0 },
        speed = speed.toDoubleOrNull()?.takeIf { it > 0.0 },
        incline = incline.toIntOrNull()?.toDouble(),
        resistance = resistance.toIntOrNull()?.toDouble()?.takeIf { it > 0.0 },
        rounds = rounds.toIntOrNull()?.takeIf { it >= 1 },
    )
}
