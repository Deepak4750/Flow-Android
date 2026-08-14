package com.deepak.flow.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderCompletionDao {
    @Query("SELECT reminderId FROM reminder_day_completions WHERE dateEpochDay = :dateEpochDay")
    fun observeCompletedIdsForDate(dateEpochDay: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReminderDayCompletionEntity)

    @Query(
        "DELETE FROM reminder_day_completions WHERE reminderId = :reminderId AND dateEpochDay = :dateEpochDay",
    )
    suspend fun deleteForReminderOnDate(reminderId: Long, dateEpochDay: Long)

    @Query("DELETE FROM reminder_day_completions WHERE reminderId = :reminderId")
    suspend fun deleteForReminder(reminderId: Long)

    @Query("DELETE FROM reminder_day_completions")
    suspend fun deleteAll()
}
