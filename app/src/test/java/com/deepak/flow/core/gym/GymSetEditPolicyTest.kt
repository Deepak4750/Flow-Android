package com.deepak.flow.core.gym

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GymSetEditPolicyTest {

    private fun exercise(
        saved: Boolean = false,
        completedAt: Long? = null,
        skipped: Boolean = false,
    ) = GymWorkoutExercise(
        id = 1L,
        workoutId = 1L,
        name = "Bench Press",
        sortOrder = 0,
        skipped = skipped,
        completedAtEpochMilli = completedAt,
        sets = if (saved) {
            listOf(
                GymWorkoutSet(
                    id = 10L,
                    workoutExerciseId = 1L,
                    setNumber = 1,
                    saved = true,
                ),
            )
        } else {
            emptyList()
        },
    )

    @Test
    fun emptyExercise_setMeasurementsEditableWhenEditorOpen() {
        val open = exercise(saved = false)
        assertTrue(
            GymSetEditPolicy.canEditSetMeasurements(
                exercise = open,
                isResting = false,
                isFocused = true,
                setEditorVisible = true,
            ),
        )
    }

    @Test
    fun unfinishedExerciseWithSavedSet_remainsEditable() {
        val partial = exercise(saved = true)
        assertTrue(
            GymSetEditPolicy.canEditSetMeasurements(
                exercise = partial,
                isResting = false,
                isFocused = true,
                setEditorVisible = true,
            ),
        )
    }

    @Test
    fun completedExercise_isNotEditable() {
        val done = exercise(saved = true, completedAt = 1L)
        assertFalse(
            GymSetEditPolicy.canEditSetMeasurements(
                exercise = done,
                isResting = false,
                isFocused = true,
                setEditorVisible = true,
            ),
        )
    }

    @Test
    fun skippedExercise_isNotEditable() {
        val skipped = exercise(saved = false, skipped = true)
        assertFalse(
            GymSetEditPolicy.canEditSetMeasurements(
                exercise = skipped,
                isResting = false,
                isFocused = true,
                setEditorVisible = true,
            ),
        )
    }

    @Test
    fun trackingFieldsEditableOnlyWithoutSavedSets() {
        val open = exercise(saved = false)
        val saved = exercise(saved = true)
        assertTrue(GymSetEditPolicy.canEditTrackingFields(open))
        assertFalse(GymSetEditPolicy.canEditTrackingFields(saved))
    }
}
