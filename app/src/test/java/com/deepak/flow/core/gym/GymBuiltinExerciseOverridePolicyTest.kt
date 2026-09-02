package com.deepak.flow.core.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GymBuiltinExerciseOverridePolicyTest {

    private val builtin = GymBuiltinExerciseCatalog.byId("builtin:dumbbell_chest_press")!!

    @Test
    fun buildOverrideEntity_storesOnlyDisplayNameDelta() {
        val entity = GymBuiltinExerciseOverridePolicy.buildOverrideEntity(
            builtin = builtin,
            displayName = "My DB Chest Press",
            primaryMuscle = builtin.primaryMuscle,
            secondaryMuscles = builtin.secondaryMuscles,
            equipment = builtin.equipment,
            nowEpochMilli = 1L,
        )
        assertNotNull(entity)
        assertEquals("My DB Chest Press", entity?.displayName)
        assertNull(entity?.primaryMuscle)
        assertNull(entity?.secondaryMuscles)
        assertNull(entity?.equipment)
        assertEquals(builtin.id, entity?.exerciseId)
    }

    @Test
    fun buildOverrideEntity_returnsNullWhenMatchingCatalogue() {
        val entity = GymBuiltinExerciseOverridePolicy.buildOverrideEntity(
            builtin = builtin,
            displayName = builtin.canonicalName,
            primaryMuscle = builtin.primaryMuscle,
            secondaryMuscles = builtin.secondaryMuscles,
            equipment = builtin.equipment,
            nowEpochMilli = 1L,
        )
        assertNull(entity)
    }

    @Test
    fun hasUserOverride_isFalseForNullOverride() {
        assertFalse(GymBuiltinExerciseOverridePolicy.hasUserOverride(null, builtin))
    }

    @Test
    fun hasUserOverride_isTrueWhenDisplayNameDiffers() {
        val entity = GymBuiltinExerciseOverridePolicy.buildOverrideEntity(
            builtin = builtin,
            displayName = "My DB Chest Press",
            primaryMuscle = builtin.primaryMuscle,
            secondaryMuscles = builtin.secondaryMuscles,
            equipment = builtin.equipment,
            nowEpochMilli = 1L,
        )!!
        assertTrue(GymBuiltinExerciseOverridePolicy.hasUserOverride(entity, builtin))
    }

    @Test
    fun resetOverride_restoresCatalogueDefaults() {
        val override = GymBuiltinExerciseOverridePolicy.buildOverrideEntity(
            builtin = builtin,
            displayName = "My DB Chest Press",
            primaryMuscle = GymMuscleGroup.SHOULDERS,
            secondaryMuscles = listOf(GymMuscleGroup.TRICEPS),
            equipment = GymEquipment.MACHINE,
            nowEpochMilli = 1L,
        )!!
        val resolved = GymExerciseMetadataResolver.resolveBuiltin(builtin, override)
        assertEquals("My DB Chest Press", resolved.displayName)

        val afterReset = GymExerciseMetadataResolver.resolveBuiltin(builtin, override = null)
        assertEquals(builtin.canonicalName, afterReset.displayName)
        assertEquals(builtin.primaryMuscle, afterReset.primaryMuscle)
        assertEquals(builtin.secondaryMuscles, afterReset.secondaryMuscles)
        assertEquals(builtin.equipment, afterReset.equipment)
    }

    @Test
    fun builtinIdentity_unchangedAfterOverride() {
        val override = GymBuiltinExerciseOverridePolicy.buildOverrideEntity(
            builtin = builtin,
            displayName = "My DB Chest Press",
            primaryMuscle = builtin.primaryMuscle,
            secondaryMuscles = builtin.secondaryMuscles,
            equipment = builtin.equipment,
            nowEpochMilli = 1L,
        )!!
        val exercise = GymExerciseLibrary.getExercise(
            exerciseId = builtin.id,
            customExercises = emptyList(),
            overridesById = mapOf(
                builtin.id to GymExerciseOverrideRecord(
                    exerciseId = builtin.id,
                    displayName = override.displayName,
                    updatedAtEpochMilli = 1L,
                ),
            ),
        )
        assertNotNull(exercise)
        assertEquals(builtin.id, exercise?.exerciseId)
        assertEquals("My DB Chest Press", exercise?.displayName)
        assertEquals(builtin.canonicalName, exercise?.catalogueDefaults?.displayName)
    }
}

class GymExerciseLibraryMetadataTest {

    @Test
    fun listExercises_sourceFilterCustom_returnsOnlyCustom() {
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
            sourceFilter = GymLibrarySourceFilter.CUSTOM,
        )
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.isCustom })
        assertTrue(results.any { it.exerciseId == custom.id })
    }

    @Test
    fun listExercises_sourceFilterBuiltin_excludesCustom() {
        val custom = GymCustomExerciseRecord(
            id = "custom:test-y-raise",
            displayName = "Single Arm Cable Y Raise",
            normalizedKey = "single arm cable y raise",
            createdAtEpochMilli = 1L,
        )
        val results = GymExerciseLibrary.listExercises(
            customExercises = listOf(custom),
            sourceFilter = GymLibrarySourceFilter.BUILTIN,
            limit = 20,
        )
        assertTrue(results.isNotEmpty())
        assertTrue(results.none { it.isCustom })
    }

    @Test
    fun customExercise_renamePreservesCustomId() {
        val originalId = "custom:rename-test"
        val custom = GymCustomExerciseRecord(
            id = originalId,
            displayName = "Cable Y Raise",
            normalizedKey = "cable y raise",
            createdAtEpochMilli = 1L,
            primaryMuscle = GymMuscleGroup.SHOULDERS,
            equipment = GymEquipment.CABLE,
        )
        val renamed = custom.copy(
            displayName = "Single Arm Cable Y Raise",
            normalizedKey = GymExerciseNormalizer.normalizeKey("Single Arm Cable Y Raise"),
            secondaryMuscles = listOf(GymMuscleGroup.TRAPS),
        )
        val exercise = GymExerciseLibrary.getExercise(
            exerciseId = originalId,
            customExercises = listOf(renamed),
            overridesById = emptyMap(),
        )
        assertNotNull(exercise)
        assertEquals(originalId, exercise?.exerciseId)
        assertEquals("Single Arm Cable Y Raise", exercise?.displayName)
        assertEquals(listOf(GymMuscleGroup.TRAPS), exercise?.secondaryMuscles)
    }

    @Test
    fun createCustomExercise_duplicateNormalizedNameResolvesExisting() {
        val first = GymExerciseIdentity.resolveFromName(
            rawName = "Single Arm Cable Y Raise",
            existingCustomByKey = emptyMap(),
        )
        val existing = GymCustomExerciseRecord(
            id = first.exerciseId,
            displayName = "Single Arm Cable Y Raise",
            normalizedKey = GymExerciseNormalizer.normalizeKey("Single Arm Cable Y Raise"),
            createdAtEpochMilli = 1L,
            primaryMuscle = GymMuscleGroup.SHOULDERS,
            equipment = GymEquipment.CABLE,
        )
        val second = GymExerciseIdentity.resolveFromName(
            rawName = "single arm cable y raise",
            existingCustomByKey = mapOf(existing.normalizedKey to existing),
        )
        assertEquals(first.exerciseId, second.exerciseId)
        assertTrue(second.isCustom)
    }

    @Test
    fun listExercises_muscleFilterUsesResolvedMetadataFromOverride() {
        val override = GymExerciseOverrideRecord(
            exerciseId = "builtin:dumbbell_chest_press",
            displayName = "My DB Press",
            primaryMuscle = GymMuscleGroup.SHOULDERS,
            updatedAtEpochMilli = 1L,
        )
        val shoulderResults = GymExerciseLibrary.listExercises(
            customExercises = emptyList(),
            overridesById = mapOf(override.exerciseId to override),
            muscleFilter = GymMuscleGroup.SHOULDERS,
            limit = 600,
        )
        assertTrue(shoulderResults.any { it.exerciseId == "builtin:dumbbell_chest_press" })
        assertEquals("My DB Press", shoulderResults.first { it.exerciseId == "builtin:dumbbell_chest_press" }.displayName)

        val chestResults = GymExerciseLibrary.listExercises(
            customExercises = emptyList(),
            overridesById = mapOf(override.exerciseId to override),
            muscleFilter = GymMuscleGroup.CHEST,
            limit = 600,
        )
        assertFalse(chestResults.any { it.exerciseId == "builtin:dumbbell_chest_press" })
    }

    @Test
    fun listExercises_searchStillFindsBuiltinByCatalogueName() {
        val results = GymExerciseLibrary.listExercises(query = "dumbbell chest", limit = 20)
        assertTrue(results.any { it.exerciseId == "builtin:dumbbell_chest_press" })
    }
}
