package com.deepak.flow.core.history

data class HistoryTaskCompletion(
    val reminderId: Long,
    val title: String,
    val completedAtEpochMilli: Long,
)

data class HistoryWaterDay(
    val dateEpochDay: Long,
    val intakeMl: Int,
    val addLog: List<Int>,
    val goalMl: Int?,
) {
    val hasIntake: Boolean get() = intakeMl > 0
}

data class HistoryDaySummary(
    val dateEpochDay: Long,
    val taskCount: Int,
    val waterIntakeMl: Int,
)

data class HistorySeriesPoint(
    val startEpochDay: Long,
    val endEpochDay: Long,
    val label: String,
    val taskCount: Int,
    val waterIntakeMl: Int,
)
