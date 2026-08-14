package com.deepak.flow.core.model

import java.util.Locale
import kotlin.math.roundToInt

data class DailyProgress(
    val totalTasks: Int,
    val completedTasks: Int,
) {
    val ratio: Float
        get() = if (totalTasks <= 0) 0f else completedTasks.toFloat() / totalTasks.toFloat()

    val hasTasksToday: Boolean
        get() = totalTasks > 0
}

/**
 * Whether the cell at [index] (left-to-right, top-to-bottom) should be filled.
 * Each column fills from the bottom up; progress then moves to the next column.
 */
fun isDotMatrixCellFilled(
    index: Int,
    filledCount: Int,
    columns: Int,
    rows: Int,
): Boolean {
    if (filledCount <= 0 || columns <= 0 || rows <= 0) return false
    val col = index % columns
    val rowFromTop = index / columns
    val rowFromBottom = rows - 1 - rowFromTop
    val fillOrder = col * rows + rowFromBottom
    return fillOrder < filledCount
}

fun formatDailyProgressPercent(ratio: Float): String {
    if (ratio <= 0f) return "0%"
    if (ratio >= 1f) return "100%"
    val percent = ratio * 100f
    val roundedOneDecimal = (percent * 10f).roundToInt() / 10f
    return if (roundedOneDecimal == roundedOneDecimal.toLong().toFloat()) {
        "${roundedOneDecimal.toLong()}%"
    } else {
        String.format(Locale.US, "%.1f%%", roundedOneDecimal)
    }
}
