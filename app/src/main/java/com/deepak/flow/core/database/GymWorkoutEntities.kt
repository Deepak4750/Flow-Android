package com.deepak.flow.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gym_workouts",
    indices = [Index(value = ["status"]), Index(value = ["type", "status"])],
)
data class GymWorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String,
    val status: String,
    val startedAtEpochMilli: Long,
    val endedAtEpochMilli: Long? = null,
    val completed: Boolean = false,
    val weightUnit: String = "KG",
    val restEndsAtEpochMilli: Long? = null,
    val restDurationSeconds: Int = 90,
    val currentExerciseIndex: Int = 0,
    /** When the current exercise became current. Null until the first exercise exists. */
    val currentExerciseStartedAtEpochMilli: Long? = null,
    val starred: Boolean = false,
    val title: String = "",
    val routineId: Long? = null,
    val dayIndex: Int? = null,
    val restKind: String = "NONE",
)

@Entity(
    tableName = "gym_workout_exercises",
    indices = [Index(value = ["workoutId"])],
)
data class GymWorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val workoutId: Long,
    /** Canonical exercise identity (built-in or custom). */
    val exerciseId: String = "",
    val exerciseName: String,
    val sortOrder: Int,
    val note: String = "",
    val trackingFields: String = "",
    val plannedSetCount: Int = 0,
    val skipped: Boolean = false,
    val completedAtEpochMilli: Long? = null,
    val routineExerciseId: Long? = null,
    val exerciseStableKey: String? = null,
)

@Entity(
    tableName = "gym_workout_sets",
    indices = [Index(value = ["workoutExerciseId"])],
)
data class GymWorkoutSetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val workoutExerciseId: Long,
    val setNumber: Int,
    val weight: Double? = null,
    val weightUnit: String? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val distance: Double? = null,
    val speed: Double? = null,
    val incline: Double? = null,
    val resistance: Double? = null,
    val rounds: Int? = null,
    val failure: Boolean = false,
    val saved: Boolean = false,
    val skipped: Boolean = false,
)

