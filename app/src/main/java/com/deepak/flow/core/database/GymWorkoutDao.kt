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
        WHERE status = :status
          AND endedAtEpochMilli IS NOT NULL
          AND endedAtEpochMilli >= :fromInclusive
          AND endedAtEpochMilli < :toExclusive
        ORDER BY endedAtEpochMilli DESC
        """,
    )
    fun observeCompletedBetween(
        status: String,
        fromInclusive: Long,
        toExclusive: Long,
    ): Flow<List<GymWorkoutEntity>>

    @Query(
        """
        SELECT * FROM gym_workouts
        WHERE status = :status
          AND endedAtEpochMilli IS NOT NULL
          AND endedAtEpochMilli >= :fromInclusive
          AND endedAtEpochMilli < :toExclusive
        ORDER BY endedAtEpochMilli DESC
        """,
    )
    suspend fun getCompletedBetween(
        status: String,
        fromInclusive: Long,
        toExclusive: Long,
    ): List<GymWorkoutEntity>

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

    @Query(
        """
        SELECT * FROM gym_workouts
        WHERE status = :status
        ORDER BY startedAtEpochMilli DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestByStatus(status: String): GymWorkoutEntity?

    @Query(
        """
        SELECT * FROM gym_workouts
        WHERE status = :status
        ORDER BY startedAtEpochMilli DESC
        LIMIT 1
        """,
    )
    fun observeLatestByStatus(status: String): Flow<GymWorkoutEntity?>

    @Query(
        """
        SELECT * FROM gym_workouts
        WHERE type = :type
          AND status = :status
          AND routineId = :routineId
          AND dayIndex = :dayIndex
        ORDER BY endedAtEpochMilli DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestCompletedRoutineDay(
        type: String,
        status: String,
        routineId: Long,
        dayIndex: Int,
    ): GymWorkoutEntity?

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

    @Query(
        """
        SELECT MIN(endedAtEpochMilli) FROM gym_workouts
        WHERE status = :status AND endedAtEpochMilli IS NOT NULL
        """,
    )
    suspend fun minCompletedEndedAt(status: String): Long?

    @Query(
        """
        SELECT DISTINCT TRIM(exerciseName) FROM gym_workout_exercises
        WHERE TRIM(exerciseName) != ''
        ORDER BY exerciseName COLLATE NOCASE ASC
        """,
    )
    suspend fun getDistinctExerciseNames(): List<String>

    @Query(
        """
        SELECT e.* FROM gym_workout_exercises e
        INNER JOIN gym_workouts w ON w.id = e.workoutId
        WHERE w.status = :status
          AND w.completed = 1
          AND w.id != :excludeWorkoutId
          AND e.exerciseId = :exerciseId
          AND TRIM(e.exerciseId) != ''
          AND e.skipped = 0
        ORDER BY w.endedAtEpochMilli DESC, e.id DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestCompletedExerciseByCanonicalId(
        status: String,
        exerciseId: String,
        excludeWorkoutId: Long,
    ): GymWorkoutExerciseEntity?

    @Transaction
    suspend fun deleteWorkoutCascade(workoutId: Long) {
        deleteSetsForWorkout(workoutId)
        deleteExercisesForWorkout(workoutId)
        deleteWorkout(workoutId)
    }
}

