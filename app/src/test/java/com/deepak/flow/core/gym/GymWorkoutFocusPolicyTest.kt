package com.deepak.flow.core.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GymWorkoutFocusPolicyTest {

    private fun exercise(id: Long, skipped: Boolean = false) = GymWorkoutExercise(
        id = id,
        workoutId = 1L,
        name = "Exercise $id",
        sortOrder = id.toInt(),
        skipped = skipped,
    )

    @Test
    fun focusedEditableExercise_usesCurrentLabel() {
        val open = exercise(1L)
        assertEquals(
            "Current",
            GymWorkoutFocusPolicy.exerciseStatusLabel(
                exercise = open,
                isFocused = true,
                isEditable = true,
                isActivelyEditing = true,
            ),
        )
        assertEquals(
            "Current",
            GymWorkoutFocusPolicy.exerciseStatusLabel(
                exercise = open,
                isFocused = true,
                isEditable = true,
                isActivelyEditing = false,
            ),
        )
    }

    @Test
    fun unfocusedExercise_hasNoStatusLabel() {
        assertNull(
            GymWorkoutFocusPolicy.exerciseStatusLabel(
                exercise = exercise(1L),
                isFocused = false,
                isEditable = true,
                isActivelyEditing = false,
            ),
        )
    }

    @Test
    fun switchActionVisibleOnOtherExercises() {
        val target = exercise(2L)
        assertTrue(
            GymWorkoutFocusPolicy.shouldOfferSwitchExercise(
                exercise = target,
                isFocused = false,
                hasFocusedExerciseInSession = true,
                isResting = false,
            ),
        )
        assertFalse(
            GymWorkoutFocusPolicy.shouldOfferSwitchExercise(
                exercise = target,
                isFocused = true,
                hasFocusedExerciseInSession = true,
                isResting = false,
            ),
        )
        assertFalse(
            GymWorkoutFocusPolicy.shouldOfferSwitchExercise(
                exercise = target,
                isFocused = false,
                hasFocusedExerciseInSession = true,
                isResting = true,
            ),
        )
    }
}
