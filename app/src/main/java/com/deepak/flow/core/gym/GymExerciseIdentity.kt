package com.deepak.flow.core.gym

import java.util.UUID

data class GymExerciseSelection(
    val exerciseId: String,
    val displayName: String,
    val isCustom: Boolean,
)

data class GymExerciseSearchHit(
    val exerciseId: String,
    val displayName: String,
    val aliasHint: String? = null,
    val primaryMuscle: GymMuscleGroup? = null,
    val equipment: GymEquipment? = null,
    val isCustom: Boolean,
    val isCreateCustom: Boolean = false,
)

object GymExerciseIdentity {
    const val CUSTOM_PREFIX = "custom:"

    fun newCustomId(): String = "$CUSTOM_PREFIX${UUID.randomUUID()}"

    fun isBuiltinId(exerciseId: String): Boolean =
        exerciseId.startsWith("builtin:")

    fun isCustomId(exerciseId: String): Boolean =
        exerciseId.startsWith(CUSTOM_PREFIX)

    fun displayNameForBuiltin(exerciseId: String): String? =
        GymBuiltinExerciseCatalog.byId(exerciseId)?.canonicalName

    /**
     * Resolve a typed or stored name to a canonical identity.
     * Built-in aliases map to catalogue exercises. Unknown names become custom identities.
     */
    fun resolveFromName(
        rawName: String,
        existingCustomByKey: Map<String, GymCustomExerciseRecord>,
    ): GymExerciseSelection {
        val trimmed = rawName.trim()
        require(trimmed.isNotEmpty()) { "Name can't be empty." }
        GymBuiltinExerciseCatalog.resolveExact(trimmed)?.let { builtin ->
            return GymExerciseSelection(
                exerciseId = builtin.id,
                displayName = builtin.canonicalName,
                isCustom = false,
            )
        }
        val normalizedKey = GymExerciseNormalizer.normalizeKey(trimmed)
        existingCustomByKey[normalizedKey]?.let { existing ->
            return GymExerciseSelection(
                exerciseId = existing.id,
                displayName = existing.displayName,
                isCustom = true,
            )
        }
        return GymExerciseSelection(
            exerciseId = newCustomId(),
            displayName = trimmed,
            isCustom = true,
        )
    }

    /**
     * Resolve when [exerciseId] is already chosen (catalogue pick or prior custom).
     */
    fun resolveFromSelection(
        exerciseId: String,
        displayName: String,
        existingCustomByKey: Map<String, GymCustomExerciseRecord>,
    ): GymExerciseSelection {
        val trimmedId = exerciseId.trim()
        val trimmedName = displayName.trim()
        require(trimmedName.isNotEmpty()) { "Name can't be empty." }
        if (trimmedId.isNotEmpty()) {
            if (isBuiltinId(trimmedId)) {
                val canonical = displayNameForBuiltin(trimmedId) ?: trimmedName
                return GymExerciseSelection(
                    exerciseId = trimmedId,
                    displayName = canonical,
                    isCustom = false,
                )
            }
            if (isCustomId(trimmedId)) {
                return GymExerciseSelection(
                    exerciseId = trimmedId,
                    displayName = trimmedName,
                    isCustom = true,
                )
            }
        }
        return resolveFromName(trimmedName, existingCustomByKey)
    }
}
