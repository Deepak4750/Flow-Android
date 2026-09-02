package com.deepak.flow.core.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GymCustomExercisePolicyTest {

    private fun customLibraryExercise(id: String = "custom:test-delete") = GymLibraryExercise(
        exerciseId = id,
        displayName = "Test Custom",
        canonicalName = "Test Custom",
        aliases = emptyList(),
        primaryMuscle = GymMuscleGroup.CHEST,
        secondaryMuscles = emptyList(),
        equipment = GymEquipment.DUMBBELL,
        category = "Strength",
        isCustom = true,
        catalogueDefaults = null,
        hasUserOverride = false,
    )

    private fun builtinLibraryExercise() = GymExerciseLibrary.getExercise(
        exerciseId = "builtin:dumbbell_chest_press",
        customExercises = emptyList(),
        overridesById = emptyMap(),
    )!!

    @Test
    fun canDeleteFromLibrary_allowsCustomOnly() {
        assertTrue(GymCustomExercisePolicy.canDeleteFromLibrary("custom:abc"))
        assertFalse(GymCustomExercisePolicy.canDeleteFromLibrary("builtin:dumbbell_chest_press"))
    }

    @Test
    fun canEditInLibrary_customOnly() {
        assertTrue(GymCustomExercisePolicy.canEditInLibrary(customLibraryExercise()))
        assertFalse(GymCustomExercisePolicy.canEditInLibrary(builtinLibraryExercise()))
    }

    @Test
    fun deletingCustomExercise_removesLibraryEntryButPreservesWorkoutSnapshot() {
        val customId = "custom:history-test"
        val custom = GymCustomExerciseRecord(
            id = customId,
            displayName = "Tempo Curl",
            normalizedKey = "tempo curl",
            createdAtEpochMilli = 1L,
        )
        val workoutExercise = GymWorkoutExercise(
            id = 42L,
            workoutId = 7L,
            name = "Tempo Curl",
            sortOrder = 0,
            exerciseId = customId,
        )
        val beforeDelete = GymExerciseLibrary.getExercise(
            exerciseId = customId,
            customExercises = listOf(custom),
            overridesById = emptyMap(),
        )
        assertTrue(beforeDelete?.isCustom == true)

        val afterDelete = GymExerciseLibrary.getExercise(
            exerciseId = customId,
            customExercises = emptyList(),
            overridesById = emptyMap(),
        )
        assertNull(afterDelete)
        assertEquals("Tempo Curl", workoutExercise.name)
        assertEquals(customId, workoutExercise.exerciseId)
    }
}
