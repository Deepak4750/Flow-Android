package com.deepak.flow.core.history

enum class HistoryCompletionDotLevel {
    NONE,
    RED,
    NEUTRAL,
    YELLOW,
    BLUE,
    GREEN,
}

object HistoryCalendarLogic {
    fun combinedCompletionPercent(
        tasksEnabled: Boolean,
        waterEnabled: Boolean,
        scheduledTasks: Int,
        completedTasks: Int,
        waterIntakeMl: Int,
        waterGoalMl: Int?,
    ): Float? {
        val parts = mutableListOf<Float>()
        if (tasksEnabled && scheduledTasks > 0) {
            parts.add(completedTasks.toFloat() / scheduledTasks.toFloat() * 100f)
        }
        if (waterEnabled && waterGoalMl != null && waterGoalMl > 0) {
            parts.add(waterIntakeMl.toFloat() / waterGoalMl.toFloat() * 100f)
        }
        if (parts.isEmpty()) return null
        return parts.average().toFloat()
    }

    fun dotLevel(percent: Float?): HistoryCompletionDotLevel {
        if (percent == null) return HistoryCompletionDotLevel.NONE
        return when {
            percent <= 35f -> HistoryCompletionDotLevel.RED
            percent <= 50f -> HistoryCompletionDotLevel.YELLOW
            percent <= 80f -> HistoryCompletionDotLevel.BLUE
            else -> HistoryCompletionDotLevel.GREEN
        }
    }
}
