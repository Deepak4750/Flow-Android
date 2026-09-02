package com.deepak.flow.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gym_custom_exercises",
    indices = [Index(value = ["normalizedKey"], unique = true)],
)
data class GymCustomExerciseEntity(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val normalizedKey: String,
    val createdAtEpochMilli: Long,
    val primaryMuscle: String? = null,
    val secondaryMuscles: String? = null,
    val equipment: String? = null,
)
