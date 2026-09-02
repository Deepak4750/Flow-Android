package com.deepak.flow.feature.gym.presentation



import android.app.Application

import androidx.lifecycle.AndroidViewModel

import androidx.lifecycle.ViewModel

import androidx.lifecycle.ViewModelProvider

import androidx.lifecycle.viewModelScope

import com.deepak.flow.FlowApplication

import com.deepak.flow.core.gym.GymCustomExercisePolicy
import com.deepak.flow.core.gym.GymEquipment

import com.deepak.flow.core.gym.GymExerciseIdentity

import com.deepak.flow.core.gym.GymLibraryExercise

import com.deepak.flow.core.gym.GymLibrarySourceFilter

import com.deepak.flow.core.gym.GymMuscleGroup

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.update

import kotlinx.coroutines.launch



data class ExerciseLibraryUiState(

    val loading: Boolean = true,

    val query: String = "",

    val sourceFilter: GymLibrarySourceFilter = GymLibrarySourceFilter.ALL,

    val muscleFilter: GymMuscleGroup? = null,

    val equipmentFilter: GymEquipment? = null,

    val exercises: List<GymLibraryExercise> = emptyList(),

    val selectedExercise: GymLibraryExercise? = null,

    val showDetail: Boolean = false,

    val showEdit: Boolean = false,

    val showCreate: Boolean = false,

    val editDisplayName: String = "",

    val editPrimaryMuscle: GymMuscleGroup? = null,

    val editSecondaryMuscles: List<GymMuscleGroup> = emptyList(),

    val editEquipment: GymEquipment? = null,

    val createName: String = "",

    val createPrimaryMuscle: GymMuscleGroup? = null,

    val createSecondaryMuscles: List<GymMuscleGroup> = emptyList(),

    val createEquipment: GymEquipment? = null,

    val message: String? = null,

)



class ExerciseLibraryViewModel(

    application: Application,

) : AndroidViewModel(application) {



    private val repository = (application as FlowApplication).gymWorkoutRepository

    private val _uiState = MutableStateFlow(ExerciseLibraryUiState())

    val uiState: StateFlow<ExerciseLibraryUiState> = _uiState.asStateFlow()



    init {

        refresh()

    }



    fun refresh() {

        viewModelScope.launch {

            _uiState.update { it.copy(loading = true) }

            val state = _uiState.value

            val exercises = repository.listLibraryExercises(

                query = state.query,

                muscleFilter = state.muscleFilter,

                equipmentFilter = state.equipmentFilter,

                sourceFilter = state.sourceFilter,

            )

            _uiState.update { it.copy(loading = false, exercises = exercises) }

        }

    }



    fun onQueryChange(value: String) {

        _uiState.update { it.copy(query = value) }

        refresh()

    }



    fun onSourceFilterSelected(filter: GymLibrarySourceFilter) {

        _uiState.update { it.copy(sourceFilter = filter) }

        refresh()

    }



    fun onMuscleFilterSelected(muscle: GymMuscleGroup?) {

        _uiState.update { it.copy(muscleFilter = muscle) }

        refresh()

    }



    fun onEquipmentFilterSelected(equipment: GymEquipment?) {

        _uiState.update { it.copy(equipmentFilter = equipment) }

        refresh()

    }



    fun openExercise(exerciseId: String) {

        viewModelScope.launch {

            val exercise = repository.getLibraryExercise(exerciseId) ?: return@launch

            _uiState.update {

                it.copy(

                    selectedExercise = exercise,

                    showDetail = true,

                    showEdit = false,

                    message = null,

                )

            }

        }

    }



    fun dismissDetail() {

        _uiState.update {

            it.copy(showDetail = false, selectedExercise = null, showEdit = false)

        }

    }



    fun openEdit() {

        val exercise = _uiState.value.selectedExercise ?: return

        if (!GymCustomExercisePolicy.canEditInLibrary(exercise)) return

        _uiState.update {

            it.copy(

                showDetail = false,

                showEdit = true,

                editDisplayName = exercise.displayName,

                editPrimaryMuscle = exercise.primaryMuscle,

                editSecondaryMuscles = exercise.secondaryMuscles,

                editEquipment = exercise.equipment,

                message = null,

            )

        }

    }



    fun dismissEdit() {

        _uiState.update { it.copy(showEdit = false) }

    }



    fun onEditDisplayNameChange(value: String) {

        _uiState.update { it.copy(editDisplayName = value) }

    }



    fun onEditPrimaryMuscleSelected(muscle: GymMuscleGroup?) {

        _uiState.update { state ->

            val secondary = state.editSecondaryMuscles.filterNot { it == muscle }

            state.copy(editPrimaryMuscle = muscle, editSecondaryMuscles = secondary)

        }

    }



    fun onEditSecondaryMuscleToggled(muscle: GymMuscleGroup) {

        _uiState.update { state ->

            if (muscle == state.editPrimaryMuscle) return@update state

            val secondary = if (muscle in state.editSecondaryMuscles) {

                state.editSecondaryMuscles - muscle

            } else {

                state.editSecondaryMuscles + muscle

            }

            state.copy(editSecondaryMuscles = secondary)

        }

    }



    fun onEditEquipmentSelected(equipment: GymEquipment?) {

        _uiState.update { it.copy(editEquipment = equipment) }

    }



    fun saveEdit() {

        val exercise = _uiState.value.selectedExercise ?: return

        if (!GymCustomExercisePolicy.canEditInLibrary(exercise)) return

        val name = _uiState.value.editDisplayName.trim()

        if (name.isEmpty()) {

            _uiState.update { it.copy(message = "Name can't be empty.") }

            return

        }

        viewModelScope.launch {

            repository.saveCustomExerciseMetadata(

                exerciseId = exercise.exerciseId,

                displayName = name,

                primaryMuscle = _uiState.value.editPrimaryMuscle,

                secondaryMuscles = _uiState.value.editSecondaryMuscles,

                equipment = _uiState.value.editEquipment,

            )

            val updated = repository.getLibraryExercise(exercise.exerciseId)

            _uiState.update {

                it.copy(

                    showEdit = false,

                    showDetail = updated != null,

                    selectedExercise = updated,

                    message = null,

                )

            }

            refresh()

        }

    }



    fun deleteCustomExercise() {

        val exercise = _uiState.value.selectedExercise ?: return

        if (!GymCustomExercisePolicy.canDeleteFromLibrary(exercise.exerciseId)) return

        viewModelScope.launch {

            repository.deleteCustomExercise(exercise.exerciseId)

            _uiState.update {

                it.copy(

                    showDetail = false,

                    showEdit = false,

                    selectedExercise = null,

                    message = null,

                )

            }

            refresh()

        }

    }



    fun deleteCustomExercise(exerciseId: String) {
        if (!GymCustomExercisePolicy.canDeleteFromLibrary(exerciseId)) return
        viewModelScope.launch {
            repository.deleteCustomExercise(exerciseId)
            _uiState.update {
                it.copy(
                    showDetail = false,
                    showEdit = false,
                    selectedExercise = null,
                    message = null,
                )
            }
            refresh()
        }
    }



    fun resetBuiltinOverride() {

        val exercise = _uiState.value.selectedExercise ?: return

        if (!GymExerciseIdentity.isBuiltinId(exercise.exerciseId)) return

        viewModelScope.launch {

            repository.clearBuiltinExerciseOverride(exercise.exerciseId)

            val updated = repository.getLibraryExercise(exercise.exerciseId)

            _uiState.update {

                it.copy(

                    selectedExercise = updated,

                    showDetail = updated != null,

                    showEdit = false,

                    message = null,

                )

            }

            refresh()

        }

    }



    fun openCreate(prefill: String = "") {

        _uiState.update {

            it.copy(

                showCreate = true,

                showDetail = false,

                showEdit = false,

                createName = prefill,

                createPrimaryMuscle = null,

                createSecondaryMuscles = emptyList(),

                createEquipment = null,

                message = null,

            )

        }

    }



    fun dismissCreate() {

        _uiState.update { it.copy(showCreate = false, createName = "") }

    }



    fun onCreateNameChange(value: String) {

        _uiState.update { it.copy(createName = value) }

    }



    fun onCreatePrimaryMuscleSelected(muscle: GymMuscleGroup?) {

        _uiState.update { state ->

            val secondary = state.createSecondaryMuscles.filterNot { it == muscle }

            state.copy(createPrimaryMuscle = muscle, createSecondaryMuscles = secondary)

        }

    }



    fun onCreateSecondaryMuscleToggled(muscle: GymMuscleGroup) {

        _uiState.update { state ->

            if (muscle == state.createPrimaryMuscle) return@update state

            val secondary = if (muscle in state.createSecondaryMuscles) {

                state.createSecondaryMuscles - muscle

            } else {

                state.createSecondaryMuscles + muscle

            }

            state.copy(createSecondaryMuscles = secondary)

        }

    }



    fun onCreateEquipmentSelected(equipment: GymEquipment?) {

        _uiState.update { it.copy(createEquipment = equipment) }

    }



    fun saveCreate() {

        val name = _uiState.value.createName.trim()

        if (name.isEmpty()) {

            _uiState.update { it.copy(message = "Name can't be empty.") }

            return

        }

        viewModelScope.launch {

            val selection = repository.createCustomExercise(

                displayName = name,

                primaryMuscle = _uiState.value.createPrimaryMuscle,

                secondaryMuscles = _uiState.value.createSecondaryMuscles,

                equipment = _uiState.value.createEquipment,

            )

            val created = repository.getLibraryExercise(selection.exerciseId)

            _uiState.update {

                it.copy(

                    showCreate = false,

                    createName = "",

                    selectedExercise = null,

                    showDetail = false,

                    message = null,

                )

            }

            refresh()

        }

    }



    fun clearMessage() {

        _uiState.update { it.copy(message = null) }

    }

}



class ExerciseLibraryViewModelFactory(

    private val application: FlowApplication,

) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(ExerciseLibraryViewModel::class.java)) {

            return ExerciseLibraryViewModel(application) as T

        }

        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")

    }

}

