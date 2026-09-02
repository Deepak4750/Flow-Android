package com.deepak.flow.core.gym

/**
 * Built-in exercise catalogue backed by [GymBuiltinExerciseEntries].
 * Identity is stable [GymBuiltinExercise.id]; search and filters operate on catalogue metadata.
 */
object GymBuiltinExerciseCatalog {
    private val exercises: List<GymBuiltinExercise> = GymBuiltinExerciseEntries.all()

    private val byId: Map<String, GymBuiltinExercise> = exercises.associateBy { it.id }

    private val byNormalizedKey: Map<String, GymBuiltinExercise> = buildMap {
        exercises.forEach { exercise ->
            exercise.searchKeys.forEach { key ->
                if (!containsKey(key)) {
                    put(key, exercise)
                }
            }
        }
    }

    fun all(): List<GymBuiltinExercise> = exercises

    fun count(): Int = exercises.size

    fun byId(id: String): GymBuiltinExercise? = byId[id]

    /** Exact normalized alias match only. Returns null for ambiguous or unknown names. */
    fun resolveExact(rawName: String): GymBuiltinExercise? {
        val key = GymExerciseNormalizer.normalizeKey(rawName)
        if (key.isEmpty()) return null
        return byNormalizedKey[key]
    }

    fun search(
        query: String,
        muscleFilter: GymMuscleGroup? = null,
        equipmentFilter: GymEquipment? = null,
        limit: Int = GymExerciseNameCatalog.MAX_SUGGESTIONS,
    ): List<GymBuiltinExercise> {
        val key = GymExerciseNormalizer.normalizeKey(query)
        val filtered = exercises.filter { exercise ->
            matchesMuscleFilter(exercise, muscleFilter) &&
                matchesEquipmentFilter(exercise, equipmentFilter)
        }
        if (key.isEmpty()) {
            return filtered.take(limit)
        }
        return filtered
            .mapNotNull { exercise ->
                val rank = searchRank(exercise, key) ?: return@mapNotNull null
                exercise to rank
            }
            .sortedWith(compareBy<Pair<GymBuiltinExercise, Int>> { it.second }.thenBy { it.first.canonicalName })
            .map { it.first }
            .take(limit)
    }

    fun filterByMuscle(
        muscle: GymMuscleGroup,
        limit: Int = GymExerciseNameCatalog.MAX_SUGGESTIONS,
    ): List<GymBuiltinExercise> =
        exercises
            .filter { exercise -> matchesMuscleFilter(exercise, muscle) }
            .sortedBy { it.canonicalName }
            .take(limit)

    fun filterByEquipment(
        equipment: GymEquipment,
        limit: Int = GymExerciseNameCatalog.MAX_SUGGESTIONS,
    ): List<GymBuiltinExercise> =
        exercises
            .filter { exercise -> matchesEquipmentFilter(exercise, equipment) }
            .sortedBy { it.canonicalName }
            .take(limit)

    fun browse(
        query: String = "",
        muscleFilter: GymMuscleGroup? = null,
        equipmentFilter: GymEquipment? = null,
        limit: Int = 50,
    ): List<GymBuiltinExercise> = search(query, muscleFilter, equipmentFilter, limit)

    private fun matchesMuscleFilter(exercise: GymBuiltinExercise, muscle: GymMuscleGroup?): Boolean {
        if (muscle == null) return true
        return exercise.primaryMuscle == muscle || muscle in exercise.secondaryMuscles
    }

    private fun matchesEquipmentFilter(exercise: GymBuiltinExercise, equipment: GymEquipment?): Boolean {
        if (equipment == null) return true
        return exercise.equipment == equipment
    }

    private fun searchRank(exercise: GymBuiltinExercise, key: String): Int? {
        val canonicalKey = GymExerciseNormalizer.normalizeKey(exercise.canonicalName)
        when {
            canonicalKey == key -> return 0
            canonicalKey.startsWith(key) -> return 1
            canonicalKey.contains(key) -> return 2
        }
        exercise.aliases.forEach { alias ->
            val aliasKey = GymExerciseNormalizer.normalizeKey(alias)
            when {
                aliasKey == key -> return 0
                aliasKey.startsWith(key) -> return 1
                aliasKey.contains(key) -> return 2
            }
        }
        return null
    }
}
