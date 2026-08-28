package com.deepak.flow.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "gym_routines")
data class GymRoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val currentDayIndex: Int = 0,
    val roundsCompleted: Int = 0,
    val roundFourCheckpointDismissed: Boolean = false,
    val starred: Boolean = false,
    val starredAtEpochMilli: Long? = null,
    val createdAtEpochMilli: Long,
    val updatedAtEpochMilli: Long,
)

@Entity(
    tableName = "gym_routine_days",
    indices = [Index(value = ["routineId"])],
)
data class GymRoutineDayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val routineId: Long,
    val dayIndex: Int,
    val name: String,
    val isRestDay: Boolean = false,
)

@Entity(
    tableName = "gym_routine_exercises",
    indices = [Index(value = ["dayId"])],
)
data class GymRoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dayId: Long,
    val stableKey: String,
    val name: String,
    val trackingFields: String = "",
    val sortOrder: Int,
    val setCount: Int,
    val note: String = "",
)
