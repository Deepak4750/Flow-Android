package com.deepak.flow.core.gym

import java.util.Locale

/**
 * Merges exercise names from routine templates and past workouts into a deduped
 * suggestion list. Names are keyed case-insensitively; the most recently seen
 * casing wins.
 *
 * Search for the exercise picker is handled by [searchExercises], which combines
 * the built-in catalogue, custom exercises, and historical names.
 */
object GymExerciseNameCatalog {
    const val MAX_SUGGESTIONS = 8
    const val PICKER_LIMIT = 50

    fun mergeNames(
        routineNames: List<String>,
        workoutNames: List<String>,
    ): List<String> {
        val byKey = LinkedHashMap<String, String>()
        (workoutNames + routineNames).forEach { raw ->
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return@forEach
            val key = trimmed.lowercase(Locale.US)
            byKey[key] = trimmed
        }
        return byKey.values.sortedBy { it.lowercase(Locale.US) }
    }

    fun filterSuggestions(
        allNames: List<String>,
        query: String,
        limit: Int = MAX_SUGGESTIONS,
    ): List<String> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return allNames.take(limit)
        }
        val key = trimmedQuery.lowercase(Locale.US)
        return allNames
            .filter { name ->
                val lower = name.lowercase(Locale.US)
                lower.contains(key)
            }
            .sortedWith(
                compareBy<String> { name ->
                    val lower = name.lowercase(Locale.US)
                    when {
                        lower == key -> 0
                        lower.startsWith(key) -> 1
                        else -> 2
                    }
                }.thenBy { it.lowercase(Locale.US) },
            )
            .take(limit)
    }

    fun selectSuggestion(current: String, suggestion: String): String = suggestion.trim()

    fun searchExercises(
        query: String,
        customExercises: List<GymCustomExerciseRecord> = emptyList(),
        historicalNames: List<String> = emptyList(),
        overridesById: Map<String, GymExerciseOverrideRecord> = emptyMap(),
        muscleFilter: GymMuscleGroup? = null,
        equipmentFilter: GymEquipment? = null,
        limit: Int = MAX_SUGGESTIONS,
    ): List<GymExerciseSearchHit> {
        val trimmedQuery = query.trim()
        val results = LinkedHashMap<String, GymExerciseSearchHit>()

        GymBuiltinExerciseCatalog.search(trimmedQuery, muscleFilter, equipmentFilter, limit).forEach { builtin ->
            val override = overridesById[builtin.id]
            val metadata = GymExerciseMetadataResolver.resolveBuiltin(
                exercise = builtin,
                override = override?.toEntity(),
            )
            results.putIfAbsent(
                builtin.id,
                GymExerciseSearchHit(
                    exerciseId = builtin.id,
                    displayName = metadata.displayName,
                    aliasHint = builtin.aliasHintFor(trimmedQuery),
                    primaryMuscle = metadata.primaryMuscle,
                    equipment = metadata.equipment,
                    isCustom = false,
                ),
            )
        }

        val normalizedQuery = GymExerciseNormalizer.normalizeKey(trimmedQuery)
        customExercises
            .filter { custom ->
                val metadata = custom.toMetadata()
                matchesMuscleFilter(metadata, muscleFilter) &&
                    matchesEquipmentFilter(metadata, equipmentFilter) &&
                    (
                        normalizedQuery.isEmpty() ||
                            custom.displayName.lowercase(Locale.US).contains(normalizedQuery) ||
                            custom.normalizedKey.contains(normalizedQuery)
                        )
            }
            .sortedBy { it.displayName.lowercase(Locale.US) }
            .take(limit)
            .forEach { custom ->
                val metadata = custom.toMetadata()
                results.putIfAbsent(
                    custom.id,
                    GymExerciseSearchHit(
                        exerciseId = custom.id,
                        displayName = metadata.displayName,
                        primaryMuscle = metadata.primaryMuscle,
                        equipment = metadata.equipment,
                        isCustom = true,
                    ),
                )
            }

        filterSuggestions(historicalNames, trimmedQuery, limit).forEach { name ->
            val resolved = GymBuiltinExerciseCatalog.resolveExact(name)
            if (resolved != null) {
                val override = overridesById[resolved.id]
                val metadata = GymExerciseMetadataResolver.resolveBuiltin(
                    exercise = resolved,
                    override = override?.toEntity(),
                )
                results.putIfAbsent(
                    resolved.id,
                    GymExerciseSearchHit(
                        exerciseId = resolved.id,
                        displayName = metadata.displayName,
                        aliasHint = resolved.aliasHintFor(name),
                        primaryMuscle = metadata.primaryMuscle,
                        equipment = metadata.equipment,
                        isCustom = false,
                    ),
                )
            } else {
                val key = "historical:${GymExerciseNormalizer.normalizeKey(name)}"
                if (!results.containsKey(key)) {
                    results[key] = GymExerciseSearchHit(
                        exerciseId = "",
                        displayName = name,
                        isCustom = true,
                    )
                }
            }
        }

        val hits = results.values.take(limit).toMutableList()
        if (trimmedQuery.isNotEmpty() && shouldOfferCreateCustom(trimmedQuery, hits)) {
            hits.add(
                GymExerciseSearchHit(
                    exerciseId = "",
                    displayName = trimmedQuery,
                    isCustom = true,
                    isCreateCustom = true,
                ),
            )
        }
        return hits
    }

    fun browseExercises(
        query: String = "",
        customExercises: List<GymCustomExerciseRecord> = emptyList(),
        overridesById: Map<String, GymExerciseOverrideRecord> = emptyMap(),
        muscleFilter: GymMuscleGroup? = null,
        equipmentFilter: GymEquipment? = null,
        limit: Int = PICKER_LIMIT,
    ): List<GymExerciseSearchHit> = searchExercises(
        query = query,
        customExercises = customExercises,
        historicalNames = emptyList(),
        overridesById = overridesById,
        muscleFilter = muscleFilter,
        equipmentFilter = equipmentFilter,
        limit = limit,
    )

    private fun shouldOfferCreateCustom(
        query: String,
        hits: List<GymExerciseSearchHit>,
    ): Boolean {
        val normalized = GymExerciseNormalizer.normalizeKey(query)
        if (normalized.isEmpty()) return false
        if (GymBuiltinExerciseCatalog.resolveExact(query) != null) return false
        val exactHit = hits.any { hit ->
            !hit.isCreateCustom &&
                GymExerciseNormalizer.normalizeKey(hit.displayName) == normalized
        }
        return !exactHit
    }

    private fun matchesMuscleFilter(metadata: GymExerciseMetadata, muscle: GymMuscleGroup?): Boolean {
        if (muscle == null) return true
        return metadata.primaryMuscle == muscle || muscle in metadata.secondaryMuscles
    }

    private fun matchesEquipmentFilter(metadata: GymExerciseMetadata, equipment: GymEquipment?): Boolean {
        if (equipment == null) return true
        return metadata.equipment == equipment
    }

    private fun GymCustomExerciseRecord.toMetadata(): GymExerciseMetadata =
        GymExerciseMetadata(
            displayName = displayName,
            primaryMuscle = primaryMuscle,
            secondaryMuscles = secondaryMuscles,
            equipment = equipment,
        )
}
