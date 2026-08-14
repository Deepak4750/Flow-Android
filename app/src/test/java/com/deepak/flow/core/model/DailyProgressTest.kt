package com.deepak.flow.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyProgressTest {

    @Test
    fun ratio_isZeroWhenNoTasks() {
        val progress = DailyProgress(totalTasks = 0, completedTasks = 0)
        assertEquals(0f, progress.ratio)
        assertFalse(progress.hasTasksToday)
    }

    @Test
    fun ratio_calculatesCompletedOverTotal() {
        val progress = DailyProgress(totalTasks = 4, completedTasks = 1)
        assertEquals(0.25f, progress.ratio)
        assertTrue(progress.hasTasksToday)
    }

    @Test
    fun formatPercent_roundsToOneDecimalOrWhole() {
        assertEquals("0%", formatDailyProgressPercent(0f))
        assertEquals("100%", formatDailyProgressPercent(1f))
        assertEquals("33.3%", formatDailyProgressPercent(1f / 3f))
        assertEquals("50%", formatDailyProgressPercent(0.5f))
    }

    @Test
    fun dotMatrix_fillsEachColumnBottomToTopThenMovesRight() {
        val columns = 4
        val rows = 3
        val filledCount = 4
        // First column is full (bottom, mid, top). Next column has only its bottom dot.
        val filled = setOf(8, 4, 0, 9)
        (0 until columns * rows).forEach { index ->
            if (index in filled) {
                assertTrue(isDotMatrixCellFilled(index, filledCount, columns, rows))
            } else {
                assertFalse(isDotMatrixCellFilled(index, filledCount, columns, rows))
            }
        }
    }
}
