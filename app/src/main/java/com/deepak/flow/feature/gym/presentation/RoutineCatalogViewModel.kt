package com.deepak.flow.feature.gym.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.gym.GymRoutine
import com.deepak.flow.core.gym.RoutineDeleteDecision
import com.deepak.flow.core.gym.RoutineDeleteLogic
import com.deepak.flow.core.repository.GymWorkoutRepository
import kotlinx.coroutines.flow.Flow
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

internal interface RoutineCatalogStore {
    fun observeRoutines(): Flow<List<GymRoutine>>
    suspend fun getRoutine(routineId: Long): GymRoutine?
    suspend fun setRoutineStarred(routineId: Long, starred: Boolean)
    suspend fun isRoutineInActiveWorkout(routineId: Long): Boolean
    suspend fun deleteRoutine(routineId: Long)
}

class RoutineCatalogViewModel internal constructor(
    private val store: RoutineCatalogStore,
) : ViewModel() {

    private val catalogState = store.observeRoutines()
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
            val routine = store.getRoutine(routineId) ?: return@launch
            store.setRoutineStarred(routineId, !routine.starred)
        }
    }

    fun requestDeleteRoutine(routineId: Long) {
        viewModelScope.launch {
            val routine = store.getRoutine(routineId)
            applyDeleteDecision(
                RoutineDeleteLogic.request(
                    routineId = routineId,
                    routineName = routine?.name,
                    inActiveWorkout = store.isRoutineInActiveWorkout(routineId),
                    routineMissing = routine == null,
                ),
            )
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
            val routineId = _overlayState.value.confirmDeleteRoutineId
            when (
                val decision = RoutineDeleteLogic.confirm(
                    routineId = routineId,
                    inActiveWorkout = routineId != null && store.isRoutineInActiveWorkout(routineId),
                )
            ) {
                is RoutineDeleteDecision.Confirm -> {
                    store.deleteRoutine(decision.routineId)
                    _overlayState.update {
                        it.copy(
                            confirmDeleteRoutineId = null,
                            confirmDeleteRoutineName = null,
                        )
                    }
                }
                RoutineDeleteDecision.Blocked -> {
                    _overlayState.update {
                        it.copy(
                            confirmDeleteRoutineId = null,
                            confirmDeleteRoutineName = null,
                            deleteBlockedMessage = RoutineDeleteLogic.BLOCKED_MESSAGE,
                        )
                    }
                }
                RoutineDeleteDecision.Ignore -> Unit
            }
        }
    }

    fun clearDeleteBlockedMessage() {
        _overlayState.update { it.copy(deleteBlockedMessage = null) }
    }

    private fun applyDeleteDecision(decision: RoutineDeleteDecision) {
        when (decision) {
            is RoutineDeleteDecision.Confirm -> {
                _overlayState.update {
                    it.copy(
                        confirmDeleteRoutineId = decision.routineId,
                        confirmDeleteRoutineName = decision.name,
                        deleteBlockedMessage = null,
                    )
                }
            }
            RoutineDeleteDecision.Blocked -> {
                _overlayState.update {
                    it.copy(deleteBlockedMessage = RoutineDeleteLogic.BLOCKED_MESSAGE)
                }
            }
            RoutineDeleteDecision.Ignore -> Unit
        }
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

private class GymWorkoutRoutineCatalogStore(
    private val repository: GymWorkoutRepository,
) : RoutineCatalogStore {
    override fun observeRoutines(): Flow<List<GymRoutine>> = repository.observeRoutines()
    override suspend fun getRoutine(routineId: Long): GymRoutine? = repository.getRoutine(routineId)
    override suspend fun setRoutineStarred(routineId: Long, starred: Boolean) =
        repository.setRoutineStarred(routineId, starred)
    override suspend fun isRoutineInActiveWorkout(routineId: Long): Boolean =
        repository.isRoutineInActiveWorkout(routineId)
    override suspend fun deleteRoutine(routineId: Long) = repository.deleteRoutine(routineId)
}

class RoutineCatalogViewModelFactory(
    private val application: FlowApplication,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineCatalogViewModel::class.java)) {
            return RoutineCatalogViewModel(
                GymWorkoutRoutineCatalogStore(application.gymWorkoutRepository),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
