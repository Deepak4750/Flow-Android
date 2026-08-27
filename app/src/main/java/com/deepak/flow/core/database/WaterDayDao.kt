package com.deepak.flow.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WaterDayEntity)

    @Query("SELECT * FROM water_days WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getForDay(dateEpochDay: Long): WaterDayEntity?

    @Query("SELECT * FROM water_days WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    fun observeForDay(dateEpochDay: Long): Flow<WaterDayEntity?>

    @Query(
        """
        SELECT dateEpochDay FROM water_days
        WHERE dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay
          AND intakeMl > 0
        """,
    )
    fun observeActivityDays(fromEpochDay: Long, toEpochDay: Long): Flow<List<Long>>

    @Query(
        """
        SELECT * FROM water_days
        WHERE dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay
        ORDER BY dateEpochDay ASC
        """,
    )
    fun observeDaysInRange(fromEpochDay: Long, toEpochDay: Long): Flow<List<WaterDayEntity>>

    @Query("SELECT MIN(dateEpochDay) FROM water_days WHERE intakeMl > 0")
    suspend fun minActivityDay(): Long?
}
