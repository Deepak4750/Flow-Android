package com.deepak.flow.core.gym

import com.deepak.flow.core.database.GymCustomExerciseEntity
import com.deepak.flow.core.database.GymExerciseOverrideEntity

data class GymCustomExerciseRecord(
    val id: String,
    val displayName: String,
    val normalizedKey: String,
    val createdAtEpochMilli: Long,
    val primaryMuscle: GymMuscleGroup? = null,
    val secondaryMuscles: List<GymMuscleGroup> = emptyList(),
    val equipment: GymEquipment? = null,
)

data class GymExerciseOverrideRecord(
    val exerciseId: String,
    val displayName: String? = null,
    val primaryMuscle: GymMuscleGroup? = null,
    val secondaryMuscles: List<GymMuscleGroup> = emptyList(),
    val equipment: GymEquipment? = null,
    val updatedAtEpochMilli: Long,
)

fun GymCustomExerciseEntity.toRecord(): GymCustomExerciseRecord =
    GymCustomExerciseRecord(
        id = id,
        displayName = displayName,
        normalizedKey = normalizedKey,
        createdAtEpochMilli = createdAtEpochMilli,
        primaryMuscle = GymMuscleGroup.fromStored(primaryMuscle),
        secondaryMuscles = GymExerciseMetadataCodec.decodeMuscles(secondaryMuscles),
        equipment = GymExerciseMetadataCodec.decodeEquipment(equipment),
    )

fun GymExerciseOverrideEntity.toRecord(): GymExerciseOverrideRecord =
    GymExerciseOverrideRecord(
        exerciseId = exerciseId,
        displayName = displayName,
        primaryMuscle = GymMuscleGroup.fromStored(primaryMuscle),
        secondaryMuscles = GymExerciseMetadataCodec.decodeMuscles(secondaryMuscles),
        equipment = GymExerciseMetadataCodec.decodeEquipment(equipment),
        updatedAtEpochMilli = updatedAtEpochMilli,
    )

fun GymExerciseOverrideRecord.toEntity(): GymExerciseOverrideEntity =
    GymExerciseOverrideEntity(
        exerciseId = exerciseId,
        displayName = displayName,
        primaryMuscle = primaryMuscle?.name,
        secondaryMuscles = GymExerciseMetadataCodec.encodeMuscles(secondaryMuscles),
        equipment = GymExerciseMetadataCodec.encodeEquipment(equipment),
        updatedAtEpochMilli = updatedAtEpochMilli,
    )

fun GymCustomExerciseRecord.toEntity(): GymCustomExerciseEntity =
    GymCustomExerciseEntity(
        id = id,
        displayName = displayName,
        normalizedKey = normalizedKey,
        createdAtEpochMilli = createdAtEpochMilli,
        primaryMuscle = primaryMuscle?.name,
        secondaryMuscles = GymExerciseMetadataCodec.encodeMuscles(secondaryMuscles),
        equipment = GymExerciseMetadataCodec.encodeEquipment(equipment),
    )
