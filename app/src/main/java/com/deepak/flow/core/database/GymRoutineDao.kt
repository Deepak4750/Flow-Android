package com.deepak.flow.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GymRoutineDao {
    @Query("SELECT * FROM gym_routines ORDER BY updatedAtEpochMilli DESC")
    fun observeRoutines(): Flow<List<GymRoutineEntity>>

    @Query("SELECT * FROM gym_routines ORDER BY updatedAtEpochMilli DESC")
    suspend fun getRoutines(): List<GymRoutineEntity>

    @Query("SELECT * FROM gym_routines WHERE id = :id")
    suspend fun getRoutine(id: Long): GymRoutineEntity?

    @Query("SELECT * FROM gym_routines WHERE id = :id")
    fun observeRoutine(id: Long): Flow<GymRoutineEntity?>

    @Query("SELECT * FROM gym_routines ORDER BY updatedAtEpochMilli DESC LIMIT 1")
    suspend fun getPrimaryRoutine(): GymRoutineEntity?

    @Query("SELECT * FROM gym_routines ORDER BY updatedAtEpochMilli DESC LIMIT 1")
    fun observePrimaryRoutine(): Flow<GymRoutineEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRoutine(entity: GymRoutineEntity): Long

    @Update
    suspend fun updateRoutine(entity: GymRoutineEntity)

    @Query(
        """
        UPDATE gym_routines
        SET roundsCompleted = 0, roundFourCheckpointDismissed = 0
        """,
    )
    suspend fun resetAllRoundsCompleted()

    @Query("DELETE FROM gym_routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long)

    @Query("SELECT * FROM gym_routine_days WHERE routineId = :routineId ORDER BY dayIndex ASC")
    suspend fun getDays(routineId: Long): List<GymRoutineDayEntity>

    @Query("SELECT * FROM gym_routine_days WHERE routineId = :routineId ORDER BY dayIndex ASC")
    fun observeDays(routineId: Long): Flow<List<GymRoutineDayEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDay(entity: GymRoutineDayEntity): Long

    @Update
    suspend fun updateDay(entity: GymRoutineDayEntity)

    @Query("DELETE FROM gym_routine_days WHERE id = :id")
    suspend fun deleteDay(id: Long)

    @Query("DELETE FROM gym_routine_days WHERE routineId = :routineId")
    suspend fun deleteDaysForRoutine(routineId: Long)

    @Query("SELECT * FROM gym_routine_exercises WHERE dayId = :dayId ORDER BY sortOrder ASC")
    suspend fun getExercises(dayId: Long): List<GymRoutineExerciseEntity>

    @Query("SELECT * FROM gym_routine_exercises WHERE id = :id")
    suspend fun getExercise(id: Long): GymRoutineExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercise(entity: GymRoutineExerciseEntity): Long

    @Update
    suspend fun updateExercise(entity: GymRoutineExerciseEntity)

    @Query("DELETE FROM gym_routine_exercises WHERE id = :id")
    suspend fun deleteExercise(id: Long)

    @Query(
        """
        DELETE FROM gym_routine_exercises WHERE dayId IN (
            SELECT id FROM gym_routine_days WHERE routineId = :routineId
        )
        """,
    )
    suspend fun deleteExercisesForRoutine(routineId: Long)

    @Query("DELETE FROM gym_routine_exercises WHERE dayId = :dayId")
    suspend fun deleteExercisesForDay(dayId: Long)

    @Query(
        """
        SELECT DISTINCT TRIM(name) FROM gym_routine_exercises
        WHERE TRIM(name) != ''
        ORDER BY name COLLATE NOCASE ASC
        """,
    )
    suspend fun getDistinctExerciseNames(): List<String>

    @Transaction
    suspend fun deleteRoutineCascade(routineId: Long) {
        val days = getDays(routineId)
        days.forEach { deleteExercisesForDay(it.id) }
        deleteDaysForRoutine(routineId)
        deleteRoutine(routineId)
    }
}
