package com.deepak.flow.feature.gym.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.GymRoutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutineCatalogItem(
    val id: Long,
    val name: String,
    val starred: Boolean,
    val dayCount: Int,
    val roundsCompleted: Int,
)

data class RoutineCatalogUiState(
    val starred: List<RoutineCatalogItem> = emptyList(),
    val others: List<RoutineCatalogItem> = emptyList(),
    val confirmDeleteRoutineId: Long? = null,
    val confirmDeleteRoutineName: String? = null,
    val deleteBlockedMessage: String? = null,
) {
    val isEmpty: Boolean get() = starred.isEmpty() && others.isEmpty()
}

class RoutineCatalogViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = (application as FlowApplication).gymWorkoutRepository

    private val catalogState = repository.observeRoutines()
        .map { routines -> routines.toCatalogState() }

    private val _overlayState = MutableStateFlow(RoutineCatalogOverlayState())

    val uiState: StateFlow<RoutineCatalogUiState> = combine(
        catalogState,
        _overlayState,
    ) { catalog, overlay ->
        catalog.copy(
            confirmDeleteRoutineId = overlay.confirmDeleteRoutineId,
            confirmDeleteRoutineName = overlay.confirmDeleteRoutineName,
            deleteBlockedMessage = overlay.deleteBlockedMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RoutineCatalogUiState(),
    )

    fun toggleStar(routineId: Long) {
        viewModelScope.launch {
            val routine = repository.getRoutine(routineId) ?: return@launch
            repository.setRoutineStarred(routineId, !routine.starred)
        }
    }

    fun requestDeleteRoutine(routineId: Long) {
        viewModelScope.launch {
            if (repository.isRoutineInActiveWorkout(routineId)) {
                _overlayState.update {
                    it.copy(deleteBlockedMessage = "Finish or discard the active workout before deleting this routine.")
                }
                return@launch
            }
            val routine = repository.getRoutine(routineId) ?: return@launch
            _overlayState.update {
                it.copy(
                    confirmDeleteRoutineId = routineId,
                    confirmDeleteRoutineName = routine.name.trim().ifEmpty { "Routine" },
                    deleteBlockedMessage = null,
                )
            }
        }
    }

    fun dismissDeleteRoutine() {
        _overlayState.update {
            it.copy(
                confirmDeleteRoutineId = null,
                confirmDeleteRoutineName = null,
            )
        }
    }

    fun confirmDeleteRoutine() {
        viewModelScope.launch {
            val routineId = _overlayState.value.confirmDeleteRoutineId ?: return@launch
            if (repository.isRoutineInActiveWorkout(routineId)) {
                _overlayState.update {
                    it.copy(
                        confirmDeleteRoutineId = null,
                        confirmDeleteRoutineName = null,
                        deleteBlockedMessage = "Finish or discard the active workout before deleting this routine.",
                    )
                }
                return@launch
            }
            repository.deleteRoutine(routineId)
            _overlayState.update {
                it.copy(
                    confirmDeleteRoutineId = null,
                    confirmDeleteRoutineName = null,
                )
            }
        }
    }

    fun clearDeleteBlockedMessage() {
        _overlayState.update { it.copy(deleteBlockedMessage = null) }
    }
}

private data class RoutineCatalogOverlayState(
    val confirmDeleteRoutineId: Long? = null,
    val confirmDeleteRoutineName: String? = null,
    val deleteBlockedMessage: String? = null,
)

private fun List<GymRoutine>.toCatalogState(): RoutineCatalogUiState {
    val starred = filter { it.starred }
        .sortedByDescending { it.starredAtEpochMilli ?: 0L }
        .map { it.toCatalogItem() }
    val others = filter { !it.starred }
        .sortedByDescending { it.updatedAtEpochMilli }
        .map { it.toCatalogItem() }
    return RoutineCatalogUiState(starred = starred, others = others)
}

private fun GymRoutine.toCatalogItem() = RoutineCatalogItem(
    id = id,
    name = name.trim().ifEmpty { "Routine" },
    starred = starred,
    dayCount = days.size,
    roundsCompleted = roundsCompleted,
)

class RoutineCatalogViewModelFactory(
    private val application: FlowApplication,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineCatalogViewModel::class.java)) {
            return RoutineCatalogViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
