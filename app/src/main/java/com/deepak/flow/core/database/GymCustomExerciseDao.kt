package com.deepak.flow.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GymCustomExerciseDao {
    @Query("SELECT * FROM gym_custom_exercises ORDER BY displayName COLLATE NOCASE ASC")
    suspend fun getAll(): List<GymCustomExerciseEntity>

    @Query("SELECT * FROM gym_custom_exercises WHERE id = :id")
    suspend fun getById(id: String): GymCustomExerciseEntity?

    @Query("SELECT * FROM gym_custom_exercises WHERE normalizedKey = :normalizedKey LIMIT 1")
    suspend fun getByNormalizedKey(normalizedKey: String): GymCustomExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GymCustomExerciseEntity)

    @Query("DELETE FROM gym_custom_exercises WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        """
        SELECT * FROM gym_custom_exercises
        WHERE displayName LIKE '%' || :query || '%' ESCAPE '\'
           OR normalizedKey LIKE '%' || :normalizedQuery || '%' ESCAPE '\'
        ORDER BY displayName COLLATE NOCASE ASC
        LIMIT :limit
        """,
    )
    suspend fun search(
        query: String,
        normalizedQuery: String,
        limit: Int,
    ): List<GymCustomExerciseEntity>
}
