package com.deepak.flow.core.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DotMatrixFontTest {

    @Test
    fun measureWidth_countsGlyphsAndGaps() {
        // 4, 0, % are each 3 columns with 1-column gaps: 3+1+3+1+3 = 11
        assertEquals(11, measureDotMatrixTextWidth("40%"))
        // 3, 3, ., 3, % → 3+1+3+1+1+1+3+1+3 = 17
        assertEquals(17, measureDotMatrixTextWidth("33.3%"))
        assertEquals(3, measureDotMatrixTextWidth("1"))
        assertEquals(0, measureDotMatrixTextWidth(""))
    }

    @Test
    fun digitOne_lightsExpectedCellsOnTightGrid() {
        val columns = 3
        val rows = 5
        val lit = litCells("1", columns, rows)
        assertEquals(
            setOf(1, 3, 4, 7, 10, 12, 13, 14),
            lit,
        )
    }

    @Test
    fun percentPage_centersGlyphsAndLeavesMarginOff() {
        val columns = 13
        val rows = 8
        val lit = litCells("40%", columns, rows)
        assertTrue(lit.isNotEmpty())
        // Top-left margin cell stays off.
        assertFalse(0 in lit)
        // A center-band cell belonging to the first glyph should be on.
        val originCol = (columns - measureDotMatrixTextWidth("40%")) / 2
        val originRow = (rows - DotMatrixGlyphHeight) / 2
        val firstOnIndex = originRow * columns + originCol
        assertTrue(firstOnIndex in lit)
    }

    private fun litCells(text: String, columns: Int, rows: Int): Set<Int> =
        (0 until columns * rows).filter { index ->
            isDotMatrixTextCellFilled(index, text, columns, rows)
        }.toSet()
}
