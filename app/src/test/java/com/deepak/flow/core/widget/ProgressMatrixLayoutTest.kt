package com.deepak.flow.core.widget

import com.deepak.flow.core.model.DailyProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressMatrixLayoutTest {

    @Test
    fun grid_isSevenBySeven() {
        assertEquals(7, MatrixColumns)
        assertEquals(7, MatrixRows)
        assertEquals(49, MatrixDotCount)
    }

    @Test
    fun filledCount_mapsPercentToDotUnits() {
        assertEquals(0, matrixFilledCount(DailyProgress(0, 0)))
        assertEquals(0, matrixFilledCount(DailyProgress(10, 0)))
        assertEquals(24, matrixFilledCount(DailyProgress(2, 1)))
        assertEquals(49, matrixFilledCount(DailyProgress(4, 4)))
    }

    @Test
    fun fillOrder_isLeftToRightThenTopToBottom() {
        val filledCount = 8
        (0 until MatrixDotCount).forEach { index ->
            if (index < filledCount) {
                assertTrue(isProgressMatrixCellFilled(index, filledCount))
            } else {
                assertFalse(isProgressMatrixCellFilled(index, filledCount))
            }
        }
        assertTrue(isProgressMatrixCellFilled(0, 1))
        assertFalse(isProgressMatrixCellFilled(7, 1))
        assertTrue(isProgressMatrixCellFilled(7, 8))
    }

    @Test
    fun metrics_useThinInsetAndVisibleGaps() {
        val size = MatrixReferenceSizePx.toInt()
        val metrics = dotGridMetrics(size)
        val first = dotCenterPx(0, metrics)
        val last = dotCenterPx(MatrixColumns - 1, metrics)
        val edge = first - metrics.radiusPx
        val opposite = size - (last + metrics.radiusPx)
        assertTrue(edge in 12f..28f)
        assertEquals(edge, opposite, 0.5f)
        assertTrue(metrics.spacingPx - 2f * metrics.radiusPx > metrics.radiusPx)
    }

    @Test
    fun metrics_scaleUniformlyWithCanvas() {
        val half = dotGridMetrics(165)
        val full = dotGridMetrics(330)
        assertEquals(full.radiusPx / 2f, half.radiusPx, 0.01f)
        assertEquals(full.spacingPx / 2f, half.spacingPx, 0.01f)
        assertEquals(full.insetPx / 2f, half.insetPx, 0.01f)
    }
}
