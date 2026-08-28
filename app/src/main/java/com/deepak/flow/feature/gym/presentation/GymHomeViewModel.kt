package com.deepak.flow.feature.gym.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.gym.GymLimits
import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.GymRoutineDay
import com.deepak.flow.core.gym.GymWorkoutType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GymHomeUiState(
    val routines: List<com.deepak.flow.core.gym.GymRoutine> = emptyList(),
    val routine: com.deepak.flow.core.gym.GymRoutine? = null,
    val currentDay: GymRoutineDay? = null,
    val activeType: GymWorkoutType? = null,
    val activeWorkoutTitle: String = "",
    val progressionInFlight: Boolean = false,
    val pendingSkipDayConfirm: Boolean = false,
) {
    val hasRoutine: Boolean get() = routines.isNotEmpty()

    val isRestDay: Boolean get() = currentDay?.isRestDay == true

    val routineTitle: String
        get() = routine?.name?.trim().orEmpty().ifBlank { "Routine" }

    val roundsCompletedLabel: String
        get() = GymLogic.formatRoundsCompleted(routine?.roundsCompleted ?: 0)

    val showRoundFourCheckpoint: Boolean
        get() = routine?.let {
            it.roundsCompleted == GymLimits.ROUND_FOUR_CHECKPOINT &&
                !it.roundFourCheckpointDismissed
        } == true

    val dayHeading: String
        get() {
            val day = currentDay ?: return "Workout"
            return GymLogic.formatDayHeading(day.dayIndex, day.name, day.isRestDay)
        }

    val nextWorkoutHeading: String?
        get() {
            val active = routine ?: return null
            val day = active.nextWorkoutDayAfter(active.currentDayIndex) ?: return null
            return GymLogic.formatDayHeading(day.dayIndex, day.name, day.isRestDay)
        }

    val canStartRoutine: Boolean
        get() = currentDay != null &&
            !currentDay.isRestDay &&
            currentDay.exercises.isNotEmpty() &&
            activeType == null

    val canContinue: Boolean get() = activeType != null

    val canConfirmRestDay: Boolean
        get() = isRestDay && activeType == null

    val showTodayDecision: Boolean
        get() = currentDay != null && activeType == null
}

class GymHomeViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = (application as FlowApplication).gymWorkoutRepository
    private val progressionInFlight = MutableStateFlow(false)
    private val pendingSkipDayConfirm = MutableStateFlow(false)

    val uiState: StateFlow<GymHomeUiState> = combine(
        repository.observeRoutines(),
        repository.observePrimaryRoutine(),
        repository.observeAnyActiveSession(),
        progressionInFlight,
        pendingSkipDayConfirm,
    ) { routines, routine, active, progressing, pendingSkip ->
        val day = routine?.currentDay()
        GymHomeUiState(
            routines = routines,
            routine = routine,
            currentDay = day,
            activeType = active?.type,
            activeWorkoutTitle = active?.let { repository.displayWorkoutTitle(it.title) }.orEmpty(),
            progressionInFlight = progressing,
            pendingSkipDayConfirm = pendingSkip,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GymHomeUiState(),
    )

    fun requestSkipDay() {
        if (uiState.value.routine?.id == null || progressionInFlight.value) return
        pendingSkipDayConfirm.value = true
    }

    fun dismissSkipDay() {
        pendingSkipDayConfirm.value = false
    }

    fun confirmSkipDay() {
        val routineId = uiState.value.routine?.id ?: return
        if (progressionInFlight.value) return
        pendingSkipDayConfirm.value = false
        viewModelScope.launch {
            progressionInFlight.value = true
            try {
                repository.skipRoutineDay(routineId)
            } finally {
                progressionInFlight.value = false
            }
        }
    }

    fun skipDay() = confirmSkipDay()

    fun confirmRestDay() {
        val routineId = uiState.value.routine?.id ?: return
        if (progressionInFlight.value) return
        viewModelScope.launch {
            progressionInFlight.value = true
            try {
                repository.confirmRestDay(routineId)
            } finally {
                progressionInFlight.value = false
            }
        }
    }

    fun selectRoutine(routineId: Long) {
        viewModelScope.launch {
            repository.setActiveRoutine(routineId)
        }
    }

    fun dismissRoundFourCheckpoint() {
        val routineId = uiState.value.routine?.id ?: return
        viewModelScope.launch {
            repository.dismissRoundFourCheckpoint(routineId)
        }
    }
}

class GymHomeViewModelFactory(
    private val application: FlowApplication,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GymHomeViewModel::class.java)) {
            return GymHomeViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
