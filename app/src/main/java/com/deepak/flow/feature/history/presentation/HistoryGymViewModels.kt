package com.deepak.flow.feature.history.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.gym.GymLimits
import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.GymWorkoutExercise
import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.gym.GymWorkoutSet
import com.deepak.flow.core.gym.TrackingField
import com.deepak.flow.core.gym.WeightUnit
import com.deepak.flow.feature.gym.presentation.SetDraft
import com.deepak.flow.feature.gym.presentation.toDraft
import com.deepak.flow.feature.gym.presentation.toMeasurements
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HistoryGymDayUiState(
    val dateLabel: String,
    val workouts: List<HistoryGymWorkoutListItem> = emptyList(),
    val confirmDeleteWorkoutId: Long? = null,
)

data class HistoryGymWorkoutListItem(
    val workoutId: Long,
    val titleLabel: String,
    val dateTimeLabel: String,
    val durationLabel: String,
    val starred: Boolean = false,
)

data class HistoryGymWorkoutUiState(
    val loading: Boolean = true,
    val workoutId: Long = 0L,
    val titleLabel: String = "",
    val titleDraft: String = "",
    val editingTitle: Boolean = false,
    val dateLabel: String = "",
    val durationLabel: String = "",
    val canEdit: Boolean = false,
    val starred: Boolean = false,
    val displayWeightUnit: WeightUnit = WeightUnit.KG,
    val exercises: List<HistoryGymExerciseUi> = emptyList(),
    val confirmDeleteWorkout: Boolean = false,
    val leave: Boolean = false,
)

data class HistoryGymExerciseUi(
    val exerciseId: Long,
    val name: String,
    val note: String,
    val sets: List<HistoryGymSetUi>,
)

data class HistoryGymSetUi(
    val setId: Long,
    val setNumber: Int,
    val valuesLabel: String,
    val failure: Boolean,
)

data class HistoryGymEditUiState(
    val loading: Boolean = true,
    val workoutId: Long = 0L,
    val exerciseId: Long = 0L,
    val canEdit: Boolean = false,
    val name: String = "",
    val fields: Set<TrackingField> = setOf(TrackingField.WEIGHT, TrackingField.REPS),
    val note: String = "",
    val showFieldsEditor: Boolean = false,
    val sets: List<GymWorkoutSet> = emptyList(),
    val setDraft: SetDraft = SetDraft(),
    val setEditorVisible: Boolean = false,
    val displayWeightUnit: WeightUnit = WeightUnit.KG,
    val message: String? = null,
    val pendingFieldRemoval: Set<TrackingField> = emptySet(),
    val pendingNextFields: Set<TrackingField>? = null,
    val leave: Boolean = false,
) {
    val canSaveSet: Boolean
        get() {
            if (!setEditorVisible) return false
            return GymLogic.allSelectedFieldsFilled(
                fields,
                setDraft.toMeasurements(displayWeightUnit),
            )
        }
}

class HistoryGymDayViewModel(
    application: Application,
    private val dateEpochDay: Long,
) : AndroidViewModel(application) {

    private val historyRepository = (application as FlowApplication).historyRepository
    private val gymRepository = (application as FlowApplication).gymWorkoutRepository
    private val zoneId = ZoneId.systemDefault()
    private val dateLabel = formatHistoryDate(dateEpochDay, zoneId)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMM · h:mm a", Locale.getDefault())
    private val confirmDeleteWorkoutId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<HistoryGymDayUiState> = combine(
        historyRepository.observeGymWorkoutsForDay(dateEpochDay, zoneId),
        confirmDeleteWorkoutId,
    ) { sessions, pendingDelete ->
        HistoryGymDayUiState(
            dateLabel = dateLabel,
            workouts = sessions.map {
                it.toListItem(zoneId, dateTimeFormatter, gymRepository::displayWorkoutTitle)
            },
            confirmDeleteWorkoutId = pendingDelete,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryGymDayUiState(dateLabel = dateLabel),
    )

    fun toggleStar(workoutId: Long) {
        viewModelScope.launch {
            val session = gymRepository.getSession(workoutId) ?: return@launch
            gymRepository.setWorkoutStarred(workoutId, !session.starred)
        }
    }

    fun requestDeleteWorkout(workoutId: Long) {
        confirmDeleteWorkoutId.value = workoutId
    }

    fun dismissDeleteWorkout() {
        confirmDeleteWorkoutId.value = null
    }

    fun confirmDeleteWorkout() {
        val id = confirmDeleteWorkoutId.value ?: return
        confirmDeleteWorkoutId.value = null
        viewModelScope.launch {
            gymRepository.discardWorkout(id)
        }
    }
}

class HistoryGymWorkoutViewModel(
    application: Application,
    private val workoutId: Long,
) : AndroidViewModel(application) {

    private val gymRepository = (application as FlowApplication).gymWorkoutRepository
    private val profileRepository = (application as FlowApplication).profileRepository
    private val zoneId = ZoneId.systemDefault()
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    private val nowEpochMilli = MutableStateFlow(System.currentTimeMillis())
    private val confirmDeleteWorkout = MutableStateFlow(false)
    private val leave = MutableStateFlow(false)
    private val editingTitle = MutableStateFlow(false)
    private val titleDraft = MutableStateFlow("")

    val uiState: StateFlow<HistoryGymWorkoutUiState> = combine(
        combine(
            gymRepository.observeSession(workoutId),
            profileRepository.observeProfile(),
            nowEpochMilli,
        ) { session, profile, now -> Triple(session, profile, now) },
        combine(
            confirmDeleteWorkout,
            leave,
            editingTitle,
            titleDraft,
        ) { confirmDelete, leaveFlag, editing, draft ->
            listOf(confirmDelete, leaveFlag, editing, draft)
        },
    ) { core, flags ->
        val session = core.first
        val profile = core.second
        val now = core.third
        val confirmDelete = flags[0] as Boolean
        val leaveFlag = flags[1] as Boolean
        val editing = flags[2] as Boolean
        val draft = flags[3] as String
        if (session == null) {
            HistoryGymWorkoutUiState(
                loading = false,
                confirmDeleteWorkout = confirmDelete,
                leave = leaveFlag,
            )
        } else {
            val displayUnit = when (profile?.gymWeightUnit?.uppercase()) {
                "LB" -> WeightUnit.LB
                else -> WeightUnit.KG
            }
            val end = session.endedAtEpochMilli ?: session.startedAtEpochMilli
            val dateLabel = Instant.ofEpochMilli(end)
                .atZone(zoneId)
                .toLocalDate()
                .format(dateFormatter)
            val summary = GymLogic.summarize(session, now)
            val titleLabel = gymRepository.displayWorkoutTitle(session.title)
            HistoryGymWorkoutUiState(
                loading = false,
                workoutId = session.id,
                titleLabel = titleLabel,
                titleDraft = if (editing) draft else session.title,
                editingTitle = editing,
                dateLabel = dateLabel,
                durationLabel = GymLogic.formatSummaryDuration(summary.durationSeconds),
                canEdit = GymLogic.isWithinPostWorkoutEditWindow(session.endedAtEpochMilli, now),
                starred = session.starred,
                displayWeightUnit = displayUnit,
                exercises = session.exercises.map { exercise ->
                    exercise.toHistoryUi(displayUnit)
                },
                confirmDeleteWorkout = confirmDelete,
                leave = leaveFlag,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryGymWorkoutUiState(),
    )

    fun startEditingTitle() {
        viewModelScope.launch {
            val session = gymRepository.getSession(workoutId) ?: return@launch
            titleDraft.value = session.title
            editingTitle.value = true
        }
    }

    fun onTitleDraftChange(value: String) {
        titleDraft.value = value
    }

    fun commitTitleEdit() {
        if (!editingTitle.value) return
        viewModelScope.launch {
            gymRepository.setWorkoutTitle(workoutId, titleDraft.value)
            editingTitle.value = false
        }
    }

    fun cancelTitleEdit() {
        editingTitle.value = false
    }

    fun refreshEditWindow() {
        nowEpochMilli.value = System.currentTimeMillis()
    }

    fun requestDeleteWorkout() {
        confirmDeleteWorkout.value = true
    }

    fun dismissDeleteWorkout() {
        confirmDeleteWorkout.value = false
    }

    fun confirmDeleteWorkout() {
        confirmDeleteWorkout.value = false
        viewModelScope.launch {
            gymRepository.discardWorkout(workoutId)
            leave.value = true
        }
    }

    fun toggleStarred() {
        viewModelScope.launch {
            val session = gymRepository.getSession(workoutId) ?: return@launch
            gymRepository.setWorkoutStarred(workoutId, !session.starred)
        }
    }
}

class HistoryGymEditExerciseViewModel(
    application: Application,
    private val workoutId: Long,
    private val exerciseId: Long,
) : AndroidViewModel(application) {

    private val gymRepository = (application as FlowApplication).gymWorkoutRepository
    private val profileRepository = (application as FlowApplication).profileRepository

    private val name = MutableStateFlow("")
    private val fields = MutableStateFlow(setOf(TrackingField.WEIGHT, TrackingField.REPS))
    private val note = MutableStateFlow("")
    private val showFieldsEditor = MutableStateFlow(false)
    private val setDraft = MutableStateFlow(SetDraft())
    private val setEditorVisible = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val pendingFieldRemoval = MutableStateFlow<Set<TrackingField>>(emptySet())
    private val pendingNextFields = MutableStateFlow<Set<TrackingField>?>(null)
    private val leave = MutableStateFlow(false)
    private val nowEpochMilli = MutableStateFlow(System.currentTimeMillis())
    private val hydrated = MutableStateFlow(false)

    private val sessionFlow = gymRepository.observeSession(workoutId)

    private data class EditFlags(
        val message: String?,
        val removal: Set<TrackingField>,
        val nextFields: Set<TrackingField>?,
        val leave: Boolean,
    )

    init {
        viewModelScope.launch {
            sessionFlow.collect { session ->
                if (hydrated.value) return@collect
                val exercise = session?.exercises?.firstOrNull { it.id == exerciseId } ?: return@collect
                name.value = exercise.name
                fields.value = exercise.trackingFields.ifEmpty {
                    setOf(TrackingField.WEIGHT, TrackingField.REPS)
                }
                note.value = exercise.note
                hydrated.value = true
            }
        }
    }

    val uiState: StateFlow<HistoryGymEditUiState> = combine(
        combine(sessionFlow, profileRepository.observeProfile(), nowEpochMilli) { session, profile, now ->
            Triple(session, profile, now)
        },
        combine(name, fields, note) { n, f, no -> Triple(n, f, no) },
        combine(showFieldsEditor, setDraft, setEditorVisible) { show, draft, visible ->
            Triple(show, draft, visible)
        },
        combine(message, pendingFieldRemoval, pendingNextFields, leave) { msg, removal, next, leaveFlag ->
            EditFlags(msg, removal, next, leaveFlag)
        },
    ) { sessionTriple, draftTriple, editorTriple, flags ->
        val session = sessionTriple.first
        val profile = sessionTriple.second
        val now = sessionTriple.third
        val displayUnit = when (profile?.gymWeightUnit?.uppercase()) {
            "LB" -> WeightUnit.LB
            else -> WeightUnit.KG
        }
        val exercise = session?.exercises?.firstOrNull { it.id == exerciseId }
        val canEdit = GymLogic.isWithinPostWorkoutEditWindow(session?.endedAtEpochMilli, now)
        HistoryGymEditUiState(
            loading = session == null || exercise == null || !hydrated.value,
            workoutId = workoutId,
            exerciseId = exerciseId,
            canEdit = canEdit,
            name = draftTriple.first,
            fields = draftTriple.second,
            note = draftTriple.third,
            showFieldsEditor = editorTriple.first,
            sets = exercise?.sets?.filter { it.saved }.orEmpty(),
            setDraft = editorTriple.second,
            setEditorVisible = editorTriple.third,
            displayWeightUnit = displayUnit,
            message = flags.message,
            pendingFieldRemoval = flags.removal,
            pendingNextFields = flags.nextFields,
            leave = flags.leave || (hydrated.value && session != null && exercise != null && !canEdit),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryGymEditUiState(workoutId = workoutId, exerciseId = exerciseId),
    )

    fun onNameChange(value: String) {
        name.value = value
    }

    fun onNoteChange(value: String) {
        note.value = value.take(GymLimits.NOTE_MAX_CHARS)
    }

    fun toggleShowFieldsEditor() {
        showFieldsEditor.update { !it }
    }

    fun toggleTrackingField(field: TrackingField) {
        val current = fields.value
        val next = current.toMutableSet()
        if (field in next) {
            if (next.size <= 1) return
            next.remove(field)
        } else {
            next.add(field)
        }
        val losing = GymLogic.fieldsLosingRecordedValues(
            currentFields = current,
            nextFields = next,
            sets = uiState.value.sets,
        )
        if (losing.isNotEmpty()) {
            pendingFieldRemoval.value = losing
            pendingNextFields.value = next
            return
        }
        fields.value = next
    }

    fun confirmFieldRemoval() {
        val next = pendingNextFields.value ?: return
        val clearing = pendingFieldRemoval.value
        pendingNextFields.value = null
        pendingFieldRemoval.value = emptySet()
        fields.value = next
        viewModelScope.launch {
            clearRemovedFieldValues(clearing)
        }
    }

    fun dismissFieldRemoval() {
        pendingNextFields.value = null
        pendingFieldRemoval.value = emptySet()
    }

    fun clearMessage() {
        message.value = null
    }

    fun openEditSet(set: GymWorkoutSet) {
        setDraft.value = set.toDraft(
            setId = set.id,
            exerciseId = exerciseId,
            setNumber = set.setNumber,
            displayUnit = uiState.value.displayWeightUnit,
        )
        setEditorVisible.value = true
    }

    fun clearEditingSet() {
        setEditorVisible.value = false
        setDraft.value = SetDraft()
    }

    fun addSet() {
        val previous = uiState.value.sets.lastOrNull()
        setDraft.value = previous.toDraft(
            setId = null,
            exerciseId = exerciseId,
            setNumber = GymLogic.nextSetNumber(uiState.value.sets),
            displayUnit = uiState.value.displayWeightUnit,
        )
        setEditorVisible.value = true
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
        setDraft.update {
            it.copy(incline = GymLogic.stepWholeValue(it.incline, up, minimum = 0))
        }
    }

    fun stepResistance(up: Boolean) {
        setDraft.update {
            it.copy(resistance = GymLogic.stepWholeValue(it.resistance, up, minimum = 1))
        }
    }

    fun stepRounds(up: Boolean) {
        setDraft.update {
            it.copy(rounds = GymLogic.stepWholeValue(it.rounds, up, minimum = 1))
        }
    }

    fun saveSet() {
        if (!uiState.value.canEdit) {
            leave.value = true
            return
        }
        viewModelScope.launch {
            val draft = setDraft.value
            val unit = uiState.value.displayWeightUnit
            val measurements = draft.toMeasurements(unit)
            if (!GymLogic.allSelectedFieldsFilled(fields.value, measurements)) {
                message.value = "Fill every selected field."
                return@launch
            }
            runCatching {
                val setId = draft.setId
                if (setId == null) {
                    gymRepository.addSet(
                        exerciseId = exerciseId,
                        measurements = measurements,
                        failure = draft.failure,
                        saved = true,
                    )
                } else {
                    gymRepository.updateSet(
                        setId = setId,
                        measurements = measurements,
                        failure = draft.failure,
                        saved = true,
                    )
                }
                setEditorVisible.value = false
                setDraft.value = SetDraft()
            }.onFailure {
                message.value = it.message ?: "Couldn't save set."
            }
        }
    }

    fun deleteSet(setId: Long) {
        if (!uiState.value.canEdit) {
            leave.value = true
            return
        }
        viewModelScope.launch {
            gymRepository.deleteSet(setId)
            if (setDraft.value.setId == setId) {
                clearEditingSet()
            }
        }
    }

    fun saveExercise() {
        if (!uiState.value.canEdit) {
            leave.value = true
            return
        }
        viewModelScope.launch {
            val trimmed = name.value.trim()
            if (trimmed.isEmpty()) {
                message.value = "Name can't be empty."
                return@launch
            }
            val exercise = gymRepository.getSession(workoutId)
                ?.exercises
                ?.firstOrNull { it.id == exerciseId }
                ?: return@launch
            runCatching {
                gymRepository.updateExercise(
                    exerciseId = exerciseId,
                    name = trimmed,
                    trackingFields = exercise.trackingFields,
                    note = note.value,
                )
                leave.value = true
            }.onFailure {
                message.value = it.message ?: "Couldn't save exercise."
            }
        }
    }

    fun cancel() {
        leave.value = true
    }

    private suspend fun clearRemovedFieldValues(clearing: Set<TrackingField>) {
        if (clearing.isEmpty()) return
        val exercise = gymRepository.getSession(workoutId)
            ?.exercises
            ?.firstOrNull { it.id == exerciseId }
            ?: return
        exercise.sets.filter { it.saved }.forEach { set ->
            val cleared = GymLogic.clearMeasurementsForFields(set.measurements, clearing)
            if (cleared != set.measurements) {
                gymRepository.updateSet(
                    setId = set.id,
                    measurements = cleared,
                    failure = set.failure,
                    saved = set.saved,
                )
            }
        }
    }
}

class HistoryGymViewModelFactory(
    private val application: FlowApplication,
    private val dateEpochDay: Long? = null,
    private val workoutId: Long? = null,
    private val exerciseId: Long? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HistoryGymDayViewModel::class.java) ->
                HistoryGymDayViewModel(application, requireNotNull(dateEpochDay)) as T
            modelClass.isAssignableFrom(HistoryGymWorkoutViewModel::class.java) ->
                HistoryGymWorkoutViewModel(application, requireNotNull(workoutId)) as T
            modelClass.isAssignableFrom(HistoryGymEditExerciseViewModel::class.java) ->
                HistoryGymEditExerciseViewModel(
                    application,
                    requireNotNull(workoutId),
                    requireNotNull(exerciseId),
                ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}

private fun GymWorkoutSession.toListItem(
    zoneId: ZoneId,
    dateTimeFormatter: DateTimeFormatter,
    displayTitle: (String) -> String,
): HistoryGymWorkoutListItem {
    val end = endedAtEpochMilli ?: startedAtEpochMilli
    val dateTimeLabel = Instant.ofEpochMilli(end)
        .atZone(zoneId)
        .format(dateTimeFormatter)
    val summary = GymLogic.summarize(this, end)
    return HistoryGymWorkoutListItem(
        workoutId = id,
        titleLabel = displayTitle(title),
        dateTimeLabel = dateTimeLabel,
        durationLabel = GymLogic.formatSummaryDuration(summary.durationSeconds),
        starred = starred,
    )
}

private fun GymWorkoutExercise.toHistoryUi(displayUnit: WeightUnit): HistoryGymExerciseUi {
    val saved = sets.filter { it.saved }
    return HistoryGymExerciseUi(
        exerciseId = id,
        name = name,
        note = note,
        sets = saved.map { set ->
            HistoryGymSetUi(
                setId = set.id,
                setNumber = set.setNumber,
                valuesLabel = GymLogic.formatSetValues(set, trackingFields, displayUnit),
                failure = set.failure,
            )
        },
    )
}
