package com.deepak.flow.core.gym

import com.deepak.flow.core.database.GymExerciseOverrideEntity

/**
 * Builds user override rows for built-in exercises. Only fields that differ from the
 * immutable catalogue are stored; an empty diff means no override row is needed.
 */
object GymBuiltinExerciseOverridePolicy {
    fun buildOverrideEntity(
        builtin: GymBuiltinExercise,
        displayName: String?,
        primaryMuscle: GymMuscleGroup?,
        secondaryMuscles: List<GymMuscleGroup>,
        equipment: GymEquipment?,
        nowEpochMilli: Long,
    ): GymExerciseOverrideEntity? {
        val trimmedName = displayName?.trim()?.takeIf { it.isNotEmpty() }
        val nameOverride = trimmedName?.takeIf { it != builtin.canonicalName }
        val muscleOverride = primaryMuscle?.takeIf { it != builtin.primaryMuscle }
        val secondaryOverride = secondaryMuscles.takeIf { it != builtin.secondaryMuscles }
        val equipmentOverride = equipment?.takeIf { it != builtin.equipment }

        if (
            nameOverride == null &&
            muscleOverride == null &&
            secondaryOverride == null &&
            equipmentOverride == null
        ) {
            return null
        }

        return GymExerciseOverrideEntity(
            exerciseId = builtin.id,
            displayName = nameOverride,
            primaryMuscle = muscleOverride?.name,
            secondaryMuscles = GymExerciseMetadataCodec.encodeMuscles(secondaryOverride.orEmpty()),
            equipment = GymExerciseMetadataCodec.encodeEquipment(equipmentOverride),
            updatedAtEpochMilli = nowEpochMilli,
        )
    }

    fun hasUserOverride(
        override: GymExerciseOverrideEntity?,
        builtin: GymBuiltinExercise,
    ): Boolean {
        if (override == null) return false
        val resolved = GymExerciseMetadataResolver.resolveBuiltin(builtin, override)
        val catalogue = builtin.metadata()
        return resolved.displayName != catalogue.displayName ||
            resolved.primaryMuscle != catalogue.primaryMuscle ||
            resolved.secondaryMuscles != catalogue.secondaryMuscles ||
            resolved.equipment != catalogue.equipment
    }
}
