package com.deepak.flow.core.database

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class HistoryTaskCompletionRow(
    val reminderId: Long,
    val dateEpochDay: Long,
    val completedAtEpochMilli: Long,
    val title: String?,
)

data class HistoryTaskCountRow(
    val dateEpochDay: Long,
    val completionCount: Int,
)

@Dao
interface HistoryDao {
    @Query(
        """
        SELECT
            c.reminderId AS reminderId,
            c.dateEpochDay AS dateEpochDay,
            c.completedAtEpochMilli AS completedAtEpochMilli,
            r.title AS title
        FROM reminder_day_completions c
        LEFT JOIN reminders r ON r.id = c.reminderId
        WHERE c.dateEpochDay = :dateEpochDay
        ORDER BY c.completedAtEpochMilli ASC
        """,
    )
    fun observeTaskCompletions(dateEpochDay: Long): Flow<List<HistoryTaskCompletionRow>>

    @Query(
        """
        SELECT COUNT(*) FROM reminder_day_completions
        WHERE dateEpochDay = :dateEpochDay
        """,
    )
    fun observeTaskCompletionCount(dateEpochDay: Long): Flow<Int>

    @Query(
        """
        SELECT DISTINCT dateEpochDay FROM reminder_day_completions
        WHERE dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay
        """,
    )
    fun observeTaskActivityDays(fromEpochDay: Long, toEpochDay: Long): Flow<List<Long>>

    @Query(
        """
        SELECT dateEpochDay AS dateEpochDay, COUNT(*) AS completionCount
        FROM reminder_day_completions
        WHERE dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY dateEpochDay
        ORDER BY dateEpochDay ASC
        """,
    )
    fun observeTaskCountsInRange(fromEpochDay: Long, toEpochDay: Long): Flow<List<HistoryTaskCountRow>>
}
