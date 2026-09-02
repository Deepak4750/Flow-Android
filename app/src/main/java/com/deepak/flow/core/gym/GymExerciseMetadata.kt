package com.deepak.flow.core.gym

data class GymExerciseMetadata(
    val displayName: String,
    val primaryMuscle: GymMuscleGroup?,
    val secondaryMuscles: List<GymMuscleGroup> = emptyList(),
    val equipment: GymEquipment?,
    val category: String? = "Strength",
)
