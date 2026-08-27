package com.deepak.flow.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GymWorkoutDao {
    @Query("SELECT * FROM gym_workouts WHERE id = :id")
    suspend fun getWorkout(id: Long): GymWorkoutEntity?

    @Query("SELECT * FROM gym_workouts WHERE id = :id")
    fun observeWorkout(id: Long): Flow<GymWorkoutEntity?>

    @Query(
        """
        SELECT * FROM gym_workouts
        WHERE type = :type AND status = :status
        ORDER BY startedAtEpochMilli DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestByTypeAndStatus(type: String, status: String): GymWorkoutEntity?

    @Query(
        """
        SELECT * FROM gym_workouts
        WHERE type = :type AND status = :status
        ORDER BY startedAtEpochMilli DESC
        LIMIT 1
        """,
    )
    fun observeLatestByTypeAndStatus(type: String, status: String): Flow<GymWorkoutEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWorkout(entity: GymWorkoutEntity): Long

    @Update
    suspend fun updateWorkout(entity: GymWorkoutEntity)

    @Query("DELETE FROM gym_workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    @Query("SELECT * FROM gym_workout_exercises WHERE workoutId = :workoutId ORDER BY sortOrder ASC")
    suspend fun getExercises(workoutId: Long): List<GymWorkoutExerciseEntity>

    @Query("SELECT * FROM gym_workout_exercises WHERE workoutId = :workoutId ORDER BY sortOrder ASC")
    fun observeExercises(workoutId: Long): Flow<List<GymWorkoutExerciseEntity>>

    @Query("SELECT * FROM gym_workout_exercises WHERE id = :id")
    suspend fun getExercise(id: Long): GymWorkoutExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercise(entity: GymWorkoutExerciseEntity): Long

    @Update
    suspend fun updateExercise(entity: GymWorkoutExerciseEntity)

    @Query("DELETE FROM gym_workout_exercises WHERE id = :id")
    suspend fun deleteExercise(id: Long)

    @Query("SELECT * FROM gym_workout_sets WHERE workoutExerciseId = :exerciseId ORDER BY setNumber ASC")
    suspend fun getSets(exerciseId: Long): List<GymWorkoutSetEntity>

    @Query("SELECT * FROM gym_workout_sets WHERE workoutExerciseId = :exerciseId ORDER BY setNumber ASC")
    fun observeSets(exerciseId: Long): Flow<List<GymWorkoutSetEntity>>

    @Query(
        """
        SELECT s.* FROM gym_workout_sets s
        INNER JOIN gym_workout_exercises e ON e.id = s.workoutExerciseId
        WHERE e.workoutId = :workoutId
        ORDER BY e.sortOrder ASC, s.setNumber ASC
        """,
    )
    fun observeSetsForWorkout(workoutId: Long): Flow<List<GymWorkoutSetEntity>>

    @Query("SELECT * FROM gym_workout_sets WHERE id = :id")
    suspend fun getSet(id: Long): GymWorkoutSetEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSet(entity: GymWorkoutSetEntity): Long

    @Update
    suspend fun updateSet(entity: GymWorkoutSetEntity)

    @Query("DELETE FROM gym_workout_sets WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query(
        """
        DELETE FROM gym_workout_sets WHERE workoutExerciseId IN (
            SELECT id FROM gym_workout_exercises WHERE workoutId = :workoutId
        )
        """,
    )
    suspend fun deleteSetsForWorkout(workoutId: Long)

    @Query("DELETE FROM gym_workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteExercisesForWorkout(workoutId: Long)

    @Transaction
    suspend fun deleteWorkoutCascade(workoutId: Long) {
        deleteSetsForWorkout(workoutId)
        deleteExercisesForWorkout(workoutId)
        deleteWorkout(workoutId)
    }
}

