package com.deepak.flow.core.gym

import com.deepak.flow.core.database.GymCustomExerciseEntity
import com.deepak.flow.core.database.GymExerciseOverrideEntity

/**
 * Resolves effective exercise metadata by layering user overrides on catalogue defaults.
 * Built-in catalogue data is immutable; overrides are stored separately.
 */
object GymExerciseMetadataResolver {
    fun resolveBuiltin(
        exercise: GymBuiltinExercise,
        override: GymExerciseOverrideEntity?,
    ): GymExerciseMetadata {
        if (override == null) return exercise.metadata()
        return GymExerciseMetadata(
            displayName = override.displayName?.trim()?.takeIf { it.isNotEmpty() } ?: exercise.canonicalName,
            primaryMuscle = GymMuscleGroup.fromStored(override.primaryMuscle) ?: exercise.primaryMuscle,
            secondaryMuscles = override.secondaryMuscles?.let(GymExerciseMetadataCodec::decodeMuscles)
                ?.takeIf { it.isNotEmpty() }
                ?: exercise.secondaryMuscles,
            equipment = GymExerciseMetadataCodec.decodeEquipment(override.equipment)
                ?: exercise.equipment,
            category = exercise.category,
        )
    }

    fun resolveCustom(entity: GymCustomExerciseEntity): GymExerciseMetadata =
        GymExerciseMetadata(
            displayName = entity.displayName,
            primaryMuscle = GymMuscleGroup.fromStored(entity.primaryMuscle),
            secondaryMuscles = GymExerciseMetadataCodec.decodeMuscles(entity.secondaryMuscles),
            equipment = GymExerciseMetadataCodec.decodeEquipment(entity.equipment),
            category = "Strength",
        )

    fun displayNameForBuiltin(
        exercise: GymBuiltinExercise,
        override: GymExerciseOverrideEntity?,
    ): String = resolveBuiltin(exercise, override).displayName
}
