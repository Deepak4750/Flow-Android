package com.deepak.flow.core.gym

/**
 * Immutable catalogue defaults for a built-in exercise row.
 */
data class GymLibraryCatalogueDefaults(
    val displayName: String,
    val primaryMuscle: GymMuscleGroup?,
    val secondaryMuscles: List<GymMuscleGroup> = emptyList(),
    val equipment: GymEquipment?,
)

/**
 * Unified exercise row for the Exercise Library and browse flows.
 * Built-in entries use catalogue defaults layered with user overrides.
 */
data class GymLibraryExercise(
    val exerciseId: String,
    val displayName: String,
    val canonicalName: String,
    val aliases: List<String> = emptyList(),
    val primaryMuscle: GymMuscleGroup?,
    val secondaryMuscles: List<GymMuscleGroup> = emptyList(),
    val equipment: GymEquipment?,
    val category: String? = null,
    val isCustom: Boolean,
    val catalogueDefaults: GymLibraryCatalogueDefaults? = null,
    val hasUserOverride: Boolean = false,
)

object GymExerciseLibrary {
    const val LIBRARY_LIMIT = 600

    fun listExercises(
        query: String = "",
        customExercises: List<GymCustomExerciseRecord> = emptyList(),
        overridesById: Map<String, GymExerciseOverrideRecord> = emptyMap(),
        muscleFilter: GymMuscleGroup? = null,
        equipmentFilter: GymEquipment? = null,
        sourceFilter: GymLibrarySourceFilter = GymLibrarySourceFilter.ALL,
        includeCustom: Boolean = true,
        limit: Int = LIBRARY_LIMIT,
    ): List<GymLibraryExercise> {
        val results = LinkedHashMap<String, GymLibraryExercise>()
        val trimmedQuery = query.trim()
        val normalizedQuery = GymExerciseNormalizer.normalizeKey(trimmedQuery)

        val builtins = if (normalizedQuery.isEmpty()) {
            GymBuiltinExerciseCatalog.all()
        } else {
            GymBuiltinExerciseCatalog.search(trimmedQuery, limit = limit)
        }

        builtins.forEach { builtin ->
            val override = overridesById[builtin.id]
            val metadata = GymExerciseMetadataResolver.resolveBuiltin(
                exercise = builtin,
                override = override?.toEntity(),
            )
            if (!matchesMuscle(metadata.primaryMuscle, metadata.secondaryMuscles, muscleFilter)) return@forEach
            if (!matchesEquipment(metadata.equipment, equipmentFilter)) return@forEach
            if (
                normalizedQuery.isNotEmpty() &&
                !matchesQuery(builtin, normalizedQuery) &&
                !matchesResolvedName(metadata, normalizedQuery)
            ) {
                return@forEach
            }
            val catalogue = builtin.metadata()
            results[builtin.id] = GymLibraryExercise(
                exerciseId = builtin.id,
                displayName = metadata.displayName,
                canonicalName = builtin.canonicalName,
                aliases = builtin.aliases,
                primaryMuscle = metadata.primaryMuscle,
                secondaryMuscles = metadata.secondaryMuscles,
                equipment = metadata.equipment,
                category = metadata.category,
                isCustom = false,
                catalogueDefaults = GymLibraryCatalogueDefaults(
                    displayName = catalogue.displayName,
                    primaryMuscle = catalogue.primaryMuscle,
                    secondaryMuscles = catalogue.secondaryMuscles,
                    equipment = catalogue.equipment,
                ),
                hasUserOverride = GymBuiltinExerciseOverridePolicy.hasUserOverride(
                    override = override?.toEntity(),
                    builtin = builtin,
                ),
            )
        }

        if (includeCustom) {
            customExercises
                .filter { custom ->
                    val metadata = custom.toMetadata()
                    matchesMuscle(metadata.primaryMuscle, metadata.secondaryMuscles, muscleFilter) &&
                        matchesEquipment(metadata.equipment, equipmentFilter) &&
                        (
                            normalizedQuery.isEmpty() ||
                                GymExerciseNormalizer.normalizeKey(custom.displayName).contains(normalizedQuery) ||
                                custom.normalizedKey.contains(normalizedQuery)
                            )
                }
                .sortedBy { it.displayName.lowercase() }
                .forEach { custom ->
                    val metadata = custom.toMetadata()
                    results.putIfAbsent(
                        custom.id,
                        GymLibraryExercise(
                            exerciseId = custom.id,
                            displayName = metadata.displayName,
                            canonicalName = custom.displayName,
                            aliases = emptyList(),
                            primaryMuscle = metadata.primaryMuscle,
                            secondaryMuscles = metadata.secondaryMuscles,
                            equipment = metadata.equipment,
                            category = metadata.category,
                            isCustom = true,
                        ),
                    )
                }
        }

        return results.values
            .filter { exercise ->
                when (sourceFilter) {
                    GymLibrarySourceFilter.ALL -> true
                    GymLibrarySourceFilter.BUILTIN -> !exercise.isCustom
                    GymLibrarySourceFilter.CUSTOM -> exercise.isCustom
                }
            }
            .sortedBy { it.displayName.lowercase() }
            .take(limit)
    }

    fun getExercise(
        exerciseId: String,
        customExercises: List<GymCustomExerciseRecord>,
        overridesById: Map<String, GymExerciseOverrideRecord>,
    ): GymLibraryExercise? {
        if (GymExerciseIdentity.isBuiltinId(exerciseId)) {
            val builtin = GymBuiltinExerciseCatalog.byId(exerciseId) ?: return null
            val override = overridesById[exerciseId]
            val metadata = GymExerciseMetadataResolver.resolveBuiltin(
                exercise = builtin,
                override = override?.toEntity(),
            )
            val catalogue = builtin.metadata()
            return GymLibraryExercise(
                exerciseId = builtin.id,
                displayName = metadata.displayName,
                canonicalName = builtin.canonicalName,
                aliases = builtin.aliases,
                primaryMuscle = metadata.primaryMuscle,
                secondaryMuscles = metadata.secondaryMuscles,
                equipment = metadata.equipment,
                category = metadata.category,
                isCustom = false,
                catalogueDefaults = GymLibraryCatalogueDefaults(
                    displayName = catalogue.displayName,
                    primaryMuscle = catalogue.primaryMuscle,
                    secondaryMuscles = catalogue.secondaryMuscles,
                    equipment = catalogue.equipment,
                ),
                hasUserOverride = GymBuiltinExerciseOverridePolicy.hasUserOverride(
                    override = override?.toEntity(),
                    builtin = builtin,
                ),
            )
        }
        val custom = customExercises.firstOrNull { it.id == exerciseId } ?: return null
        val metadata = custom.toMetadata()
        return GymLibraryExercise(
            exerciseId = custom.id,
            displayName = metadata.displayName,
            canonicalName = custom.displayName,
            aliases = emptyList(),
            primaryMuscle = metadata.primaryMuscle,
            secondaryMuscles = metadata.secondaryMuscles,
            equipment = metadata.equipment,
            category = metadata.category,
            isCustom = true,
        )
    }

    private fun matchesMuscle(
        primary: GymMuscleGroup?,
        secondary: List<GymMuscleGroup>,
        filter: GymMuscleGroup?,
    ): Boolean {
        if (filter == null) return true
        return primary == filter || filter in secondary
    }

    private fun matchesEquipment(equipment: GymEquipment?, filter: GymEquipment?): Boolean {
        if (filter == null) return true
        return equipment == filter
    }

    private fun matchesQuery(builtin: GymBuiltinExercise, normalizedQuery: String): Boolean {
        if (GymExerciseNormalizer.normalizeKey(builtin.canonicalName).contains(normalizedQuery)) return true
        return builtin.aliases.any { alias ->
            GymExerciseNormalizer.normalizeKey(alias).contains(normalizedQuery)
        }
    }

    private fun matchesResolvedName(metadata: GymExerciseMetadata, normalizedQuery: String): Boolean =
        GymExerciseNormalizer.normalizeKey(metadata.displayName).contains(normalizedQuery)

    private fun GymCustomExerciseRecord.toMetadata() = GymExerciseMetadata(
        displayName = displayName,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = secondaryMuscles,
        equipment = equipment,
    )

    private fun GymExerciseOverrideRecord.toEntity() =
        com.deepak.flow.core.database.GymExerciseOverrideEntity(
            exerciseId = exerciseId,
            displayName = displayName,
            primaryMuscle = primaryMuscle?.name,
            secondaryMuscles = GymExerciseMetadataCodec.encodeMuscles(secondaryMuscles),
            equipment = GymExerciseMetadataCodec.encodeEquipment(equipment),
            updatedAtEpochMilli = updatedAtEpochMilli,
        )
}
