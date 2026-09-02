package com.deepak.flow.core.gym

/**
 * Built-in exercise catalogue. Identity is stable [id]; [canonicalName] is the preferred
 * display label. [aliases] are explicit alternate names only (no fuzzy matching).
 */
data class GymBuiltinExercise(
    val id: String,
    val canonicalName: String,
    val aliases: List<String> = emptyList(),
    val primaryMuscle: GymMuscleGroup,
    val secondaryMuscles: List<GymMuscleGroup> = emptyList(),
    val equipment: GymEquipment,
    val category: String? = "Strength",
) {
    val searchKeys: Set<String> by lazy {
        buildSet {
            add(GymExerciseNormalizer.normalizeKey(canonicalName))
            aliases.forEach { add(GymExerciseNormalizer.normalizeKey(it)) }
            add(GymExerciseNormalizer.normalizeKey(primaryMuscle.displayName))
            add(GymExerciseNormalizer.normalizeKey(equipment.displayName))
        }
    }

    fun aliasHintFor(query: String): String? {
        val key = GymExerciseNormalizer.normalizeKey(query)
        if (key.isEmpty()) return null
        val canonicalKey = GymExerciseNormalizer.normalizeKey(canonicalName)
        if (key == canonicalKey) return null
        val matched = aliases.filter { alias ->
            val aliasKey = GymExerciseNormalizer.normalizeKey(alias)
            aliasKey.contains(key) || key.contains(aliasKey)
        }
        if (matched.isEmpty()) return null
        return matched.take(3).joinToString(" · ")
    }

    fun metadata(): GymExerciseMetadata = GymExerciseMetadata(
        displayName = canonicalName,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = secondaryMuscles,
        equipment = equipment,
        category = category,
    )
}
