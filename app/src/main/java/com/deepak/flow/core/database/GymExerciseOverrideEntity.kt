package com.deepak.flow.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User-specific metadata overrides for built-in catalogue exercises.
 * The canonical [exerciseId] never changes; only display/metadata may differ per user.
 */
@Entity(tableName = "gym_exercise_overrides")
data class GymExerciseOverrideEntity(
    @PrimaryKey
    val exerciseId: String,
    val displayName: String? = null,
    val primaryMuscle: String? = null,
    val secondaryMuscles: String? = null,
    val equipment: String? = null,
    val updatedAtEpochMilli: Long,
)
