package com.deepak.flow.core.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GymExerciseLibraryTest {

    @Test
    fun listExercises_includesBuiltInAndCustom() {
        val custom = GymCustomExerciseRecord(
            id = "custom:test-y-raise",
            displayName = "Single Arm Cable Y Raise",
            normalizedKey = "single arm cable y raise",
            createdAtEpochMilli = 1L,
            primaryMuscle = GymMuscleGroup.SHOULDERS,
            equipment = GymEquipment.CABLE,
        )
        val results = GymExerciseLibrary.listExercises(
            customExercises = listOf(custom),
        )
        assertTrue(results.any { it.exerciseId == "custom:test-y-raise" })
        assertTrue(results.any { it.exerciseId.startsWith("builtin:") })
    }

    @Test
    fun listExercises_filtersByMuscleFromMetadata() {
        val results = GymExerciseLibrary.listExercises(
            muscleFilter = GymMuscleGroup.BICEPS,
            limit = 100,
        )
        assertTrue(results.isNotEmpty())
        assertTrue(
            results.all { item ->
                item.primaryMuscle == GymMuscleGroup.BICEPS ||
                    GymMuscleGroup.BICEPS in item.secondaryMuscles
            },
        )
    }

    @Test
    fun listExercises_filtersByEquipmentFromMetadata() {
        val results = GymExerciseLibrary.listExercises(
            equipmentFilter = GymEquipment.LANDMINE,
            limit = 50,
        )
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.equipment == GymEquipment.LANDMINE })
    }

    @Test
    fun getExercise_appliesBuiltinOverrideDisplayName() {
        val builtin = GymBuiltinExerciseCatalog.byId("builtin:dumbbell_chest_press")!!
        val override = GymExerciseOverrideRecord(
            exerciseId = builtin.id,
            displayName = "My DB Press",
            updatedAtEpochMilli = 1L,
        )
        val exercise = GymExerciseLibrary.getExercise(
            exerciseId = builtin.id,
            customExercises = emptyList(),
            overridesById = mapOf(builtin.id to override),
        )
        assertNotNull(exercise)
        assertEquals("My DB Press", exercise?.displayName)
        assertEquals(builtin.id, exercise?.exerciseId)
    }

    @Test
    fun customExercise_normalizedSearchFindsExistingCustom() {
        val first = GymExerciseIdentity.resolveFromName(
            rawName = "Single Arm Cable Y Raise",
            existingCustomByKey = emptyMap(),
        )
        val existing = GymCustomExerciseRecord(
            id = first.exerciseId,
            displayName = "Single Arm Cable Y Raise",
            normalizedKey = GymExerciseNormalizer.normalizeKey("Single Arm Cable Y Raise"),
            createdAtEpochMilli = 1L,
        )
        val second = GymExerciseIdentity.resolveFromName(
            rawName = "  single arm cable y raise  ",
            existingCustomByKey = mapOf(existing.normalizedKey to existing),
        )
        assertEquals(first.exerciseId, second.exerciseId)
        assertFalse(second.isCustom == false)
    }
}
