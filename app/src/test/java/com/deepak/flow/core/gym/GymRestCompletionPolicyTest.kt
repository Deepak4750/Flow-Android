package com.deepak.flow.core.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GymRestCompletionPolicyTest {

    private fun exercise(
        id: Long,
        sortOrder: Int,
        completed: Boolean = false,
        skipped: Boolean = false,
    ) = GymWorkoutExercise(
        id = id,
        workoutId = 1L,
        name = "Exercise $id",
        sortOrder = sortOrder,
        skipped = skipped,
        completedAtEpochMilli = if (completed) 1L else null,
    )

    @Test
    fun nextExerciseIndexAfterExerciseRest_selectsNextIncompleteAfterCurrent() {
        val exercises = listOf(
            exercise(id = 1L, sortOrder = 0, completed = true),
            exercise(id = 2L, sortOrder = 1, completed = true),
            exercise(id = 3L, sortOrder = 2),
            exercise(id = 4L, sortOrder = 3),
        )
        assertEquals(2, GymRestCompletionPolicy.nextExerciseIndexAfterExerciseRest(exercises, fromIndex = 1))
    }

    @Test
    fun nextExerciseIndexAfterExerciseRest_wrapsToEarlierIncomplete() {
        val exercises = listOf(
            exercise(id = 1L, sortOrder = 0),
            exercise(id = 2L, sortOrder = 1, completed = true),
            exercise(id = 3L, sortOrder = 2, completed = true),
        )
        assertEquals(0, GymRestCompletionPolicy.nextExerciseIndexAfterExerciseRest(exercises, fromIndex = 2))
    }

    @Test
    fun nextExerciseIndexAfterExerciseRest_selectsFirstIncomplete() {
        val exercises = listOf(
            exercise(id = 1L, sortOrder = 0, completed = true),
            exercise(id = 2L, sortOrder = 1),
            exercise(id = 3L, sortOrder = 2),
        )
        assertEquals(1, GymRestCompletionPolicy.nextExerciseIndexAfterExerciseRest(exercises, fromIndex = 0))
    }

    @Test
    fun nextExerciseIndexAfterExerciseRest_returnsNullWhenAllResolved() {
        val exercises = listOf(
            exercise(id = 1L, sortOrder = 0, completed = true),
            exercise(id = 2L, sortOrder = 1, skipped = true),
        )
        assertNull(GymRestCompletionPolicy.nextExerciseIndexAfterExerciseRest(exercises, fromIndex = 0))
    }

    @Test
    fun shouldOpenSetEditorAfterSetRest_whenExerciseEditable() {
        val open = exercise(id = 1L, sortOrder = 0)
        assertTrue(GymRestCompletionPolicy.shouldOpenSetEditorAfterSetRest(open))
    }

    @Test
    fun shouldOpenSetEditorAfterSetRest_falseWhenCompleted() {
        val done = exercise(id = 1L, sortOrder = 0, completed = true)
        assertFalse(GymRestCompletionPolicy.shouldOpenSetEditorAfterSetRest(done))
    }
}
