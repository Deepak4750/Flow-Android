package com.deepak.flow.feature.gym.presentation

import com.deepak.flow.core.gym.GymRoutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineCatalogViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun requestDeleteShowsConfirmation() = runTest(dispatcher) {
        val store = FakeRoutineCatalogStore(
            listOf(routine(id = 2L, name = "Push")),
        )
        val viewModel = RoutineCatalogViewModel(store)
        collect(viewModel)
        viewModel.requestDeleteRoutine(2L)
        advanceUntilIdle()
        assertEquals(2L, viewModel.uiState.value.confirmDeleteRoutineId)
        assertEquals("Push", viewModel.uiState.value.confirmDeleteRoutineName)
        assertNull(viewModel.uiState.value.deleteBlockedMessage)
    }

    @Test
    fun confirmDeleteRemovesRoutine() = runTest(dispatcher) {
        val store = FakeRoutineCatalogStore(
            listOf(routine(id = 2L, name = "Push")),
        )
        val viewModel = RoutineCatalogViewModel(store)
        collect(viewModel)
        viewModel.requestDeleteRoutine(2L)
        advanceUntilIdle()
        viewModel.confirmDeleteRoutine()
        advanceUntilIdle()
        assertEquals(listOf(2L), store.deleted)
        assertNull(viewModel.uiState.value.confirmDeleteRoutineId)
        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun requestDeleteIsBlockedDuringActiveWorkout() = runTest(dispatcher) {
        val store = FakeRoutineCatalogStore(
            listOf(routine(id = 2L, name = "Push")),
        )
        store.activeWorkoutRoutineIds += 2L
        val viewModel = RoutineCatalogViewModel(store)
        collect(viewModel)
        viewModel.requestDeleteRoutine(2L)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.confirmDeleteRoutineId)
        assertEquals(
            "Finish or discard the active workout before deleting this routine.",
            viewModel.uiState.value.deleteBlockedMessage,
        )
        assertTrue(store.deleted.isEmpty())
    }

    @Test
    fun confirmDeleteFromEditIsBlockedDuringActiveWorkout() = runTest(dispatcher) {
        val store = FakeRoutineCatalogStore(
            listOf(routine(id = 9L, name = "Pull")),
        )
        val viewModel = RoutineCatalogViewModel(store)
        collect(viewModel)
        viewModel.requestDeleteRoutine(9L)
        advanceUntilIdle()
        store.activeWorkoutRoutineIds += 9L
        viewModel.confirmDeleteRoutine()
        advanceUntilIdle()
        assertTrue(store.deleted.isEmpty())
        assertNull(viewModel.uiState.value.confirmDeleteRoutineId)
        assertEquals(
            "Finish or discard the active workout before deleting this routine.",
            viewModel.uiState.value.deleteBlockedMessage,
        )
    }

    private fun TestScope.collect(viewModel: RoutineCatalogViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }
}

private fun routine(id: Long, name: String) = GymRoutine(id = id, name = name)

private class FakeRoutineCatalogStore(
    initial: List<GymRoutine>,
) : RoutineCatalogStore {
    private val routines = MutableStateFlow(initial)
    val deleted = mutableListOf<Long>()
    val activeWorkoutRoutineIds = mutableSetOf<Long>()

    override fun observeRoutines(): Flow<List<GymRoutine>> = routines.asStateFlow()

    override suspend fun getRoutine(routineId: Long): GymRoutine? =
        routines.value.find { it.id == routineId }

    override suspend fun setRoutineStarred(routineId: Long, starred: Boolean) {
        routines.value = routines.value.map { routine ->
            if (routine.id == routineId) routine.copy(starred = starred) else routine
        }
    }

    override suspend fun isRoutineInActiveWorkout(routineId: Long): Boolean =
        routineId in activeWorkoutRoutineIds

    override suspend fun deleteRoutine(routineId: Long) {
        deleted += routineId
        routines.value = routines.value.filter { it.id != routineId }
    }
}
