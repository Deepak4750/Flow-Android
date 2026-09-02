package com.deepak.flow.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GymExerciseOverrideDao {
    @Query("SELECT * FROM gym_exercise_overrides")
    suspend fun getAll(): List<GymExerciseOverrideEntity>

    @Query("SELECT * FROM gym_exercise_overrides WHERE exerciseId = :exerciseId")
    suspend fun getByExerciseId(exerciseId: String): GymExerciseOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GymExerciseOverrideEntity)

    @Query("DELETE FROM gym_exercise_overrides WHERE exerciseId = :exerciseId")
    suspend fun delete(exerciseId: String)
}
