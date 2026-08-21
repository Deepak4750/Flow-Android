package com.deepak.flow.core.water

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BottleFillMaskTest {

    private val black = 0xFF000000.toInt()
    private val wall = 0xFF808080.toInt()

    @Test
    fun fillsInteriorBetweenOuterWallsAndLeavesOutsideEmpty() {
        val width = 7
        val height = 5
        val row = intArrayOf(
            black, wall, black, black, black, wall, black,
        )
        val pixels = IntArray(width * height) { black }
        for (x in 1..5) {
            pixels[0 * width + x] = wall
            pixels[2 * width + x] = wall
        }
        for (x in 0 until width) pixels[1 * width + x] = row[x]

        val mask = bottleFillMaskPixels(pixels, width, height, minGap = 1, inset = 1)
        assertEquals(0, mask[1 * width + 0])
        assertEquals(0, mask[1 * width + 1])
        assertNotEquals(0, mask[1 * width + 3])
        assertEquals(0, mask[1 * width + 5])
        assertEquals(0, mask[1 * width + 6])
    }

    @Test
    fun fillsAroundInteriorGripDots() {
        val width = 9
        val height = 1
        val pixels = intArrayOf(
            wall, black, black, wall, black, black, black, wall, black,
        )
        val mask = bottleFillMaskPixels(pixels, width, height, minGap = 1, inset = 1)
        assertEquals(0, mask[0])
        assertEquals(0, mask[3])
        assertNotEquals(0, mask[4])
        assertNotEquals(0, mask[6])
        assertEquals(0, mask[7])
    }

    @Test
    fun doesNotPaintTheCapAndFillsUpToItsBottom() {
        val width = 5
        val height = 8
        val pixels = IntArray(width * height) { black }
        for (y in 1..2) {
            for (x in 1..3) pixels[y * width + x] = wall
        }
        pixels[5 * width + 1] = wall
        pixels[5 * width + 3] = wall
        pixels[6 * width + 1] = wall
        pixels[6 * width + 3] = wall
        val mask = bottleFillMaskPixels(
            pixels,
            width,
            height,
            minGap = 1,
            inset = 0,
            lidGapRows = 2,
        )
        assertEquals(0, mask[1 * width + 2])
        assertEquals(0, mask[2 * width + 2])
        assertNotEquals(0, mask[3 * width + 2])
        assertNotEquals(0, mask[4 * width + 2])
        assertNotEquals(0, mask[5 * width + 2])
        assertNotEquals(0, mask[6 * width + 2])
        assertEquals(0, mask[7 * width + 2])
        val span = bottleFillVerticalSpan(mask, width, height)
        assertEquals(3, span.top)
        assertEquals(7, span.bottomExclusive)
    }

    @Test
    fun alphaFillUsesTheEnclosedInteriorAndIgnoresTheBackground() {
        val clear = 0x00000000
        val wall = 0xFF808080.toInt()
        val width = 5
        val height = 5
        val pixels = IntArray(width * height) { clear }
        for (x in 1..3) {
            pixels[1 * width + x] = wall
            pixels[3 * width + x] = wall
        }
        pixels[2 * width + 1] = wall
        pixels[2 * width + 3] = wall
        val mask = bottleFillMaskPixels(pixels, width, height)
        assertEquals(0, mask[0])
        assertEquals(0, mask[2 * width + 1])
        assertNotEquals(0, mask[2 * width + 2])
        assertEquals(0, mask[4 * width + 2])
    }

    @Test
    fun alphaFillKeepsTheLargerCavityAndSkipsAHandleHole() {
        val clear = 0x00000000
        val wall = 0xFF808080.toInt()
        val width = 9
        val height = 5
        val pixels = IntArray(width * height) { wall }
        for (x in 0 until width) {
            pixels[x] = clear
            pixels[4 * width + x] = clear
        }
        for (y in 0 until height) {
            pixels[y * width] = clear
            pixels[y * width + 8] = clear
        }
        pixels[2 * width + 2] = clear
        pixels[2 * width + 4] = clear
        pixels[2 * width + 5] = clear
        pixels[2 * width + 6] = clear
        val mask = bottleFillMaskPixels(pixels, width, height)
        assertEquals(0, mask[0])
        assertEquals(0, mask[2 * width + 2])
        assertNotEquals(0, mask[2 * width + 4])
        assertNotEquals(0, mask[2 * width + 6])
    }

    @Test
    fun outlinePaintsTheOuterSilhouetteNotTheInterior() {
        val clear = 0x00000000
        val wall = 0xFF808080.toInt()
        val width = 7
        val height = 7
        val pixels = IntArray(width * height) { clear }
        for (x in 2..4) {
            pixels[2 * width + x] = wall
            pixels[4 * width + x] = wall
        }
        pixels[3 * width + 2] = wall
        pixels[3 * width + 4] = wall
        val outline = bottleOutlinePixels(pixels, width, height, thickness = 1)
        assertEquals(0, outline[3 * width + 3])
        assertNotEquals(0, outline[3 * width + 1])
        assertEquals(0, outline[0])
        val adjacentAlpha = outline[3 * width + 1] ushr 24
        assertTrue(adjacentAlpha in 1..255)
    }

    @Test
    fun outlineFalloffUsesPartialAlphaInsteadOfAHardRing() {
        assertEquals(255, outlineCoverageAlpha(0.4f, radius = 3f))
        val mid = outlineCoverageAlpha(2.5f, radius = 3f)
        val outer = outlineCoverageAlpha(3.2f, radius = 3f)
        assertTrue(mid in 1..254)
        assertTrue(outer in 1 until mid)
        assertEquals(0, outlineCoverageAlpha(3.5f, radius = 3f))
    }
}
