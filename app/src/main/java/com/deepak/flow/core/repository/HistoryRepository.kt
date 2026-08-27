package com.deepak.flow.core.repository

import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.history.HistoryBounds
import com.deepak.flow.core.history.HistoryDaySummary
import com.deepak.flow.core.history.HistoryTaskCompletion
import com.deepak.flow.core.history.HistoryWaterDay
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import java.time.ZoneId

interface HistoryRepository {
    fun observeActivityDays(
        yearMonth: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Flow<Set<Long>>

    fun observeDaySummary(
        dateEpochDay: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Flow<HistoryDaySummary>

    fun observeDaySeries(
        fromEpochDay: Long,
        toEpochDay: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<HistoryDaySummary>>

    fun observeTaskCompletions(dateEpochDay: Long): Flow<List<HistoryTaskCompletion>>

    fun observeWaterDay(
        dateEpochDay: Long,
        todayEpochDay: Long,
    ): Flow<HistoryWaterDay?>

    fun observeGymWorkoutsForDay(
        dateEpochDay: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<GymWorkoutSession>>

    fun observeHistoryBounds(zoneId: ZoneId = ZoneId.systemDefault()): Flow<HistoryBounds>

    suspend fun getHistoryBounds(zoneId: ZoneId = ZoneId.systemDefault()): HistoryBounds
}
