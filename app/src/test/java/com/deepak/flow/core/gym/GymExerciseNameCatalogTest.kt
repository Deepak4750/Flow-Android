package com.deepak.flow.core.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GymBuiltinExerciseCatalogTest {

    @Test
    fun catalogue_hasAtLeast450Entries() {
        assertTrue(
            "Expected at least 450 catalogue entries, got ${GymBuiltinExerciseCatalog.count()}",
            GymBuiltinExerciseCatalog.count() >= 450,
        )
    }

    @Test
    fun catalogue_hasUniqueBuiltinIds() {
        val ids = GymBuiltinExerciseCatalog.all().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.startsWith("builtin:") })
    }

    @Test
    fun catalogue_hasUniqueCanonicalNames() {
        val names = GymBuiltinExerciseCatalog.all().map { it.canonicalName.lowercase() }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun resolveExact_mapsKnownAliasesToSameExercise() {
        val dbPress = GymBuiltinExerciseCatalog.resolveExact("DB Chest Press")
        val chestPressDb = GymBuiltinExerciseCatalog.resolveExact("Chest Press DB")
        assertNotNull(dbPress)
        assertNotNull(chestPressDb)
        assertEquals(dbPress?.id, chestPressDb?.id)
        assertEquals("Dumbbell Chest Press", dbPress?.canonicalName)
    }

    @Test
    fun resolveExact_mapsRdlAliases() {
        assertEquals(
            GymBuiltinExerciseCatalog.resolveExact("Romanian Deadlift")?.id,
            GymBuiltinExerciseCatalog.resolveExact("RDL")?.id,
        )
    }

    @Test
    fun resolveExact_isCaseInsensitiveAndNormalizesSpacing() {
        assertEquals(
            GymBuiltinExerciseCatalog.resolveExact("db chest press")?.id,
            GymBuiltinExerciseCatalog.resolveExact("  DB   Chest   Press  ")?.id,
        )
    }

    @Test
    fun resolveExact_doesNotMergeAmbiguousChestPress() {
        assertNull(GymBuiltinExerciseCatalog.resolveExact("Chest Press"))
    }

    @Test
    fun resolveExact_doesNotMergeRomanianAndStiffLegDeadlift() {
        val romanian = GymBuiltinExerciseCatalog.resolveExact("Romanian Deadlift")
        val stiffLeg = GymBuiltinExerciseCatalog.resolveExact("Stiff Leg Deadlift")
        assertNotNull(romanian)
        assertNotNull(stiffLeg)
        assertNotEquals(romanian?.id, stiffLeg?.id)
    }

    @Test
    fun search_findsByAliasFragment() {
        val hits = GymBuiltinExerciseCatalog.search("rdl")
        assertTrue(hits.any { it.canonicalName == "Romanian Deadlift" })
    }

    @Test
    fun search_findsDumbbellChestPressFromDbChestQuery() {
        val hits = GymBuiltinExerciseCatalog.search("DB chest")
        assertTrue(hits.any { it.id == "builtin:dumbbell_chest_press" })
    }

    @Test
    fun filterByMuscle_returnsChestExercises() {
        val hits = GymBuiltinExerciseCatalog.filterByMuscle(GymMuscleGroup.CHEST, limit = 100)
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { ex ->
            ex.primaryMuscle == GymMuscleGroup.CHEST || GymMuscleGroup.CHEST in ex.secondaryMuscles
        })
    }

    @Test
    fun filterByEquipment_returnsBarbellExercises() {
        val hits = GymBuiltinExerciseCatalog.filterByEquipment(GymEquipment.BARBELL, limit = 100)
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.equipment == GymEquipment.BARBELL })
    }

    @Test
    fun search_withMuscleFilter_doesNotReturnOtherMuscles() {
        val hits = GymBuiltinExerciseCatalog.search("press", muscleFilter = GymMuscleGroup.CHEST, limit = 50)
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { ex ->
            ex.primaryMuscle == GymMuscleGroup.CHEST || GymMuscleGroup.CHEST in ex.secondaryMuscles
        })
    }

    @Test
    fun everyExercise_hasPrimaryMuscleAndEquipment() {
        GymBuiltinExerciseCatalog.all().forEach { exercise ->
            assertNotNull("${exercise.id} missing primary muscle", exercise.primaryMuscle)
            assertNotNull("${exercise.id} missing equipment", exercise.equipment)
        }
    }
}

class GymExerciseIdentityTest {

    @Test
    fun resolveFromName_mapsAliasToBuiltinCanonicalName() {
        val selection = GymExerciseIdentity.resolveFromName(
            rawName = "DB Chest Press",
            existingCustomByKey = emptyMap(),
        )
        assertEquals("builtin:dumbbell_chest_press", selection.exerciseId)
        assertEquals("Dumbbell Chest Press", selection.displayName)
        assertFalse(selection.isCustom)
    }

    @Test
    fun resolveFromName_createsStableCustomIdentity() {
        val first = GymExerciseIdentity.resolveFromName(
            rawName = "Deepak's Cable Frankenstein Press",
            existingCustomByKey = emptyMap(),
        )
        val second = GymExerciseIdentity.resolveFromName(
            rawName = "Deepak's Cable Frankenstein Press",
            existingCustomByKey = mapOf(
                GymExerciseNormalizer.normalizeKey(first.displayName) to
                    GymCustomExerciseRecord(
                        id = first.exerciseId,
                        displayName = first.displayName,
                        normalizedKey = GymExerciseNormalizer.normalizeKey(first.displayName),
                        createdAtEpochMilli = 1L,
                    ),
            ),
        )
        assertTrue(first.isCustom)
        assertEquals(first.exerciseId, second.exerciseId)
        assertEquals("Deepak's Cable Frankenstein Press", second.displayName)
    }

    @Test
    fun resolveFromName_preservesAmbiguousNameAsCustom() {
        val selection = GymExerciseIdentity.resolveFromName(
            rawName = "Chest Press",
            existingCustomByKey = emptyMap(),
        )
        assertTrue(selection.isCustom)
        assertEquals("Chest Press", selection.displayName)
        assertTrue(GymExerciseIdentity.isCustomId(selection.exerciseId))
    }

    @Test
    fun differentEnteredNamesResolveToSameBuiltinId() {
        val first = GymExerciseIdentity.resolveFromName("DB Chest Press", emptyMap())
        val second = GymExerciseIdentity.resolveFromName("Chest Press DB", emptyMap())
        assertEquals(first.exerciseId, second.exerciseId)
    }
}

class GymExerciseMetadataResolverTest {

    @Test
    fun resolveBuiltin_appliesDisplayNameOverrideWithoutChangingId() {
        val builtin = GymBuiltinExerciseCatalog.byId("builtin:dumbbell_chest_press")!!
        val override = com.deepak.flow.core.database.GymExerciseOverrideEntity(
            exerciseId = builtin.id,
            displayName = "My DB Press",
            primaryMuscle = null,
            secondaryMuscles = null,
            equipment = null,
            updatedAtEpochMilli = 1L,
        )
        val metadata = GymExerciseMetadataResolver.resolveBuiltin(builtin, override)
        assertEquals("My DB Press", metadata.displayName)
        assertEquals(GymMuscleGroup.CHEST, metadata.primaryMuscle)
        assertEquals(builtin.id, builtin.id)
    }

    @Test
    fun resolveBuiltin_appliesMetadataOverrides() {
        val builtin = GymBuiltinExerciseCatalog.byId("builtin:dumbbell_chest_press")!!
        val override = com.deepak.flow.core.database.GymExerciseOverrideEntity(
            exerciseId = builtin.id,
            displayName = null,
            primaryMuscle = GymMuscleGroup.SHOULDERS.name,
            secondaryMuscles = GymMuscleGroup.TRICEPS.name,
            equipment = GymEquipment.MACHINE.name,
            updatedAtEpochMilli = 1L,
        )
        val metadata = GymExerciseMetadataResolver.resolveBuiltin(builtin, override)
        assertEquals(GymMuscleGroup.SHOULDERS, metadata.primaryMuscle)
        assertEquals(listOf(GymMuscleGroup.TRICEPS), metadata.secondaryMuscles)
        assertEquals(GymEquipment.MACHINE, metadata.equipment)
    }
}

class GymExerciseNameCatalogSearchTest {

    @Test
    fun searchExercises_offersCreateCustomWhenNoExactMatch() {
        val hits = GymExerciseNameCatalog.searchExercises(
            query = "Cable Frankenstein Press",
            customExercises = emptyList(),
            historicalNames = emptyList(),
        )
        assertTrue(hits.any { it.isCreateCustom })
    }

    @Test
    fun searchExercises_doesNotOfferCreateCustomForExactBuiltinAlias() {
        val hits = GymExerciseNameCatalog.searchExercises(
            query = "RDL",
            customExercises = emptyList(),
            historicalNames = emptyList(),
        )
        assertFalse(hits.any { it.isCreateCustom })
        assertTrue(hits.any { it.exerciseId == "builtin:romanian_deadlift" })
    }

    @Test
    fun searchExercises_includesMuscleAndEquipmentMetadata() {
        val hits = GymExerciseNameCatalog.searchExercises(
            query = "Barbell Bench Press",
            customExercises = emptyList(),
            historicalNames = emptyList(),
        )
        val hit = hits.first { it.exerciseId == "builtin:barbell_bench_press" }
        assertEquals(GymMuscleGroup.CHEST, hit.primaryMuscle)
        assertEquals(GymEquipment.BARBELL, hit.equipment)
    }

    @Test
    fun browseExercises_filtersByMuscle() {
        val hits = GymExerciseNameCatalog.browseExercises(
            muscleFilter = GymMuscleGroup.BICEPS,
            limit = 30,
        )
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.primaryMuscle == GymMuscleGroup.BICEPS || !it.isCustom })
    }

    @Test
    fun mergeNames_deduplicatesCaseInsensitively() {
        val merged = GymExerciseNameCatalog.mergeNames(
            routineNames = listOf("Barbell Bench Press", "bench press"),
            workoutNames = listOf("Barbell Bench Press", "Lat Pulldown"),
        )
        assertEquals(
            listOf("Barbell Bench Press", "bench press", "Lat Pulldown"),
            merged,
        )
    }

    @Test
    fun filterSuggestions_prefersPrefixMatches() {
        val all = listOf("Barbell Bench Press", "Incline Bench Press", "Cable Fly")
        assertEquals(
            listOf("Barbell Bench Press", "Incline Bench Press"),
            GymExerciseNameCatalog.filterSuggestions(all, "bench"),
        )
    }

    @Test
    fun selectSuggestion_replacesTypedValue() {
        assertEquals(
            "Barbell Bench Press",
            GymExerciseNameCatalog.selectSuggestion("bar", "Barbell Bench Press"),
        )
        assertEquals(
            "Leg Press",
            GymExerciseNameCatalog.selectSuggestion("leg", "Leg Press"),
        )
    }

    @Test
    fun filterSuggestions_limitsResultCount() {
        val all = (1..20).map { "Exercise $it" }
        assertEquals(8, GymExerciseNameCatalog.filterSuggestions(all, "").size)
    }
}
