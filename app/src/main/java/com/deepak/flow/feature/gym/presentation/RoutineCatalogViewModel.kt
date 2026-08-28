package com.deepak.flow.feature.gym.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.GymRoutine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
) {
    val isEmpty: Boolean get() = starred.isEmpty() && others.isEmpty()
}

class RoutineCatalogViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = (application as FlowApplication).gymWorkoutRepository

    val uiState: StateFlow<RoutineCatalogUiState> = repository.observeRoutines()
        .map { routines -> routines.toCatalogState() }
        .stateIn(
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
}

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
