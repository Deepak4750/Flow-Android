package com.deepak.flow.core.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GymRestUiPolicyTest {

    private val openExercise = GymWorkoutExercise(
        id = 1L,
        workoutId = 1L,
        name = "Bench",
        sortOrder = 0,
    )

    @Test
    fun isRestActive_whenTimerRemaining() {
        val now = 1_000L
        val ends = now + 42_000L
        assertTrue(GymRestUiPolicy.isRestActive(ends, now))
    }

    @Test
    fun isRestActive_whenSuppressedLocally() {
        val now = 1_000L
        val ends = now + 42_000L
        assertFalse(GymRestUiPolicy.isRestActive(ends, now, restUiSuppressed = true))
    }

    @Test
    fun isRestActive_whenExpired() {
        val now = 100_000L
        assertFalse(GymRestUiPolicy.isRestActive(50_000L, now))
    }

    @Test
    fun setEditorHiddenWhileResting() {
        assertFalse(
            GymRestUiPolicy.shouldShowSetEditor(
                exercise = openExercise,
                isResting = true,
                isFocused = true,
                setEditorVisible = true,
            ),
        )
    }

    @Test
    fun setEditorVisibleWhenNotResting() {
        assertTrue(
            GymRestUiPolicy.shouldShowSetEditor(
                exercise = openExercise,
                isResting = false,
                isFocused = true,
                setEditorVisible = true,
            ),
        )
    }

    @Test
    fun awaitingActionsHiddenWhileResting() {
        assertFalse(
            GymRestUiPolicy.shouldShowAwaitingActions(
                isResting = true,
                isFocused = true,
                isEditable = true,
                awaitingNextAction = true,
            ),
        )
    }

    @Test
    fun selectDuringRestDoesNotOpenEditor() {
        assertFalse(GymRestUiPolicy.shouldOpenEditorAfterExerciseSelect(isResting = true))
        assertTrue(GymRestUiPolicy.shouldOpenEditorAfterExerciseSelect(isResting = false))
    }

    @Test
    fun exerciseProgressLabels() {
        val done = openExercise.copy(completedAtEpochMilli = 1L)
        assertEquals(
            "Current",
            GymRestUiPolicy.exerciseProgressLabel(
                openExercise,
                isFocused = true,
                isEditable = true,
                isActivelyEditing = true,
            ),
        )
        assertNull(
            GymRestUiPolicy.exerciseProgressLabel(
                openExercise,
                isFocused = false,
                isEditable = true,
                isActivelyEditing = false,
            ),
        )
        assertNull(
            GymRestUiPolicy.exerciseProgressLabel(
                done,
                isFocused = false,
                isEditable = false,
                isActivelyEditing = false,
            ),
        )
    }
}
