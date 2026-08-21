package com.deepak.flow.core.water

import kotlin.math.roundToInt

/**
 * Newer bottle PNGs have a transparent interior. Water is the largest
 * enclosed transparent region, so it sits inside the glass up to the
 * cap and skips handle holes. Opaque artwork is never painted over.
 *
 * Older fully opaque PNGs still use the luma scanline path.
 */
internal const val BottleWallLuma = 24
internal const val BottleFillInset = 2
internal const val BottleLidGapRows = 8
internal const val BottleTransparentAlpha = 16
internal const val BottleOutlineThickness = 7

internal fun luma(argb: Int): Int {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return r + g + b
}

internal fun isBottleArtwork(argb: Int, wallLuma: Int = BottleWallLuma): Boolean =
    luma(argb) > wallLuma

private data class BottleRowWalls(val first: Int, val last: Int)

/**
 * Returns an ARGB buffer: white opaque where water may sit, otherwise 0.
 */
fun bottleFillMaskPixels(
    pixels: IntArray,
    width: Int,
    height: Int,
    wallLuma: Int = BottleWallLuma,
    minGap: Int = 3,
    inset: Int = BottleFillInset,
    lidGapRows: Int = BottleLidGapRows,
): IntArray {
    require(pixels.size == width * height)
    if (hasTransparentPixels(pixels)) {
        return bottleAlphaInteriorMask(pixels, width, height)
    }
    val mask = IntArray(width * height)
    val walls = arrayOfNulls<BottleRowWalls>(height)
    for (y in 0 until height) {
        val row = y * width
        var firstWall = -1
        var lastWall = -1
        for (x in 0 until width) {
            if (isBottleArtwork(pixels[row + x], wallLuma)) {
                if (firstWall < 0) firstWall = x
                lastWall = x
            }
        }
        if (firstWall < 0 || lastWall - firstWall < minGap) continue
        walls[y] = BottleRowWalls(firstWall, lastWall)
    }
    val fillTop = fillTopRow(walls, pixels, width, wallLuma, minGap, inset, lidGapRows)
    for (y in fillTop until height) {
        val rowWalls = walls[y] ?: continue
        fillMaskRow(
            mask = mask,
            pixels = pixels,
            width = width,
            y = y,
            firstWall = rowWalls.first,
            lastWall = rowWalls.last,
            wallLuma = wallLuma,
            inset = inset,
            minGap = minGap,
        )
    }
    bridgeMaskGaps(mask, pixels, walls, width, wallLuma, inset, minGap, fillTop)
    return mask
}

/**
 * Inclusive top row and exclusive bottom row of fillable water,
 * from the bottom of the cap down to the base.
 */
data class BottleFillSpan(val top: Int, val bottomExclusive: Int) {
    val height: Int get() = (bottomExclusive - top).coerceAtLeast(0)
}

fun bottleFillVerticalSpan(
    mask: IntArray,
    width: Int,
    height: Int,
): BottleFillSpan {
    require(mask.size == width * height)
    var top = -1
    var bottom = 0
    for (y in 0 until height) {
        if (rowHasFill(mask, width, y)) {
            if (top < 0) top = y
            bottom = y + 1
        }
    }
    return if (top < 0) BottleFillSpan(0, 0) else BottleFillSpan(top, bottom)
}

private fun hasTransparentPixels(pixels: IntArray): Boolean {
    for (pixel in pixels) {
        if ((pixel ushr 24) < BottleTransparentAlpha) return true
    }
    return false
}

private fun isTransparent(argb: Int): Boolean =
    (argb ushr 24) < BottleTransparentAlpha

/**
 * Floods transparent pixels from the image edge, then fills the largest
 * remaining transparent blob - the bottle cavity, not the background
 * or a handle hole.
 */
private fun bottleAlphaInteriorMask(
    pixels: IntArray,
    width: Int,
    height: Int,
): IntArray {
    val exterior = floodTransparentFromEdges(pixels, width, height)
    val visited = BooleanArray(width * height)
    var bestStart = -1
    var bestCount = 0
    for (index in pixels.indices) {
        if (visited[index] || exterior[index] || !isTransparent(pixels[index])) continue
        val count = floodComponent(index, pixels, exterior, visited, width, height)
        if (count > bestCount) {
            bestCount = count
            bestStart = index
        }
    }
    if (bestStart < 0) return IntArray(width * height)
    val filled = BooleanArray(width * height)
    floodComponent(bestStart, pixels, exterior, filled, width, height)
    val mask = IntArray(width * height)
    val white = 0xFFFFFFFF.toInt()
    val edge = 0xA0FFFFFF.toInt()
    for (index in filled.indices) {
        if (!filled[index]) continue
        mask[index] = if (nextToOpaque(pixels, width, height, index)) edge else white
    }
    return mask
}

/**
 * White ring on the outside of the bottle only. Interior water and
 * handle holes that do not reach the image edge stay clear.
 *
 * Exterior pixels get Euclidean distance to the silhouette, then a
 * short smooth falloff so the stroke is not a stair-stepped 0/1 ring.
 */
fun bottleOutlinePixels(
    pixels: IntArray,
    width: Int,
    height: Int,
    thickness: Int = BottleOutlineThickness,
): IntArray {
    require(pixels.size == width * height)
    if (thickness <= 0 || !hasTransparentPixels(pixels)) {
        return IntArray(width * height)
    }
    val exterior = floodTransparentFromEdges(pixels, width, height)
    val outline = IntArray(width * height)
    val radius = thickness.toFloat()
    val search = kotlin.math.ceil(radius + 0.5f).toInt()
    for (index in pixels.indices) {
        if (!exterior[index]) continue
        val x = index % width
        val y = index / width
        var nearestSq = Float.POSITIVE_INFINITY
        val x0 = (x - search).coerceAtLeast(0)
        val x1 = (x + search).coerceAtMost(width - 1)
        val y0 = (y - search).coerceAtLeast(0)
        val y1 = (y + search).coerceAtMost(height - 1)
        for (ny in y0..y1) {
            val row = ny * width
            val dy = ny - y
            val dy2 = dy * dy
            for (nx in x0..x1) {
                if (isTransparent(pixels[row + nx])) continue
                val dx = nx - x
                val d2 = (dx * dx + dy2).toFloat()
                if (d2 < nearestSq) nearestSq = d2
            }
        }
        if (nearestSq == Float.POSITIVE_INFINITY) continue
        val alpha = outlineCoverageAlpha(kotlin.math.sqrt(nearestSq), radius)
        if (alpha > 0) {
            outline[index] = (alpha shl 24) or 0x00FFFFFF
        }
    }
    return outline
}

/** Coverage of a stroke of [radius] px with a ~1.5 px anti-aliased outer rim. */
internal fun outlineCoverageAlpha(
    distance: Float,
    radius: Float,
    aaPx: Float = 1.5f,
): Int {
    val outer = radius + 0.5f
    if (distance >= outer) return 0
    val inner = (radius - aaPx).coerceAtLeast(0f)
    if (distance <= inner) return 255
    val t = (outer - distance) / (outer - inner)
    val smooth = t * t * (3f - 2f * t)
    return (smooth * 255f + 0.5f).toInt().coerceIn(0, 255)
}

private fun nextToOpaque(
    pixels: IntArray,
    width: Int,
    height: Int,
    index: Int,
): Boolean {
    val x = index % width
    val y = index / width
    if (x > 0 && !isTransparent(pixels[index - 1])) return true
    if (x + 1 < width && !isTransparent(pixels[index + 1])) return true
    if (y > 0 && !isTransparent(pixels[index - width])) return true
    if (y + 1 < height && !isTransparent(pixels[index + width])) return true
    return false
}

private fun floodTransparentFromEdges(
    pixels: IntArray,
    width: Int,
    height: Int,
): BooleanArray {
    val seen = BooleanArray(width * height)
    val stack = ArrayDeque<Int>()
    fun tryPush(index: Int) {
        if (index !in seen.indices || seen[index]) return
        if (!isTransparent(pixels[index])) return
        seen[index] = true
        stack.addLast(index)
    }
    for (x in 0 until width) {
        tryPush(x)
        tryPush((height - 1) * width + x)
    }
    for (y in 1 until height - 1) {
        tryPush(y * width)
        tryPush(y * width + width - 1)
    }
    while (stack.isNotEmpty()) {
        val index = stack.removeLast()
        val x = index % width
        val y = index / width
        if (x > 0) tryPush(index - 1)
        if (x + 1 < width) tryPush(index + 1)
        if (y > 0) tryPush(index - width)
        if (y + 1 < height) tryPush(index + width)
    }
    return seen
}

private fun floodComponent(
    start: Int,
    pixels: IntArray,
    exterior: BooleanArray,
    visited: BooleanArray,
    width: Int,
    height: Int,
): Int {
    if (visited[start]) return 0
    val stack = ArrayDeque<Int>()
    visited[start] = true
    stack.addLast(start)
    var count = 0
    while (stack.isNotEmpty()) {
        val index = stack.removeLast()
        count++
        val x = index % width
        val y = index / width
        fun tryPush(next: Int) {
            if (next !in visited.indices || visited[next]) return
            if (exterior[next] || !isTransparent(pixels[next])) return
            visited[next] = true
            stack.addLast(next)
        }
        if (x > 0) tryPush(index - 1)
        if (x + 1 < width) tryPush(index + 1)
        if (y > 0) tryPush(index - width)
        if (y + 1 < height) tryPush(index + width)
    }
    return count
}

private fun fillTopRow(
    walls: Array<BottleRowWalls?>,
    pixels: IntArray,
    width: Int,
    wallLuma: Int,
    minGap: Int,
    inset: Int,
    lidGapRows: Int,
): Int {
    val firstArt = walls.indices.firstOrNull { walls[it] != null } ?: return 0
    val maxHole = (lidGapRows - 1).coerceAtLeast(0)
    val lidEnd = artworkRunEnd(walls, firstArt, maxHole)
    val nextArt = ((lidEnd + 1) until walls.size).firstOrNull { walls[it] != null }
    if (nextArt != null && nextArt - lidEnd > maxHole + 1) {
        return lidEnd + 1
    }
    for (y in firstArt until walls.size) {
        val rowWalls = walls[y] ?: continue
        if (interiorEmptyCount(pixels, width, y, rowWalls, wallLuma, inset) >= minGap) {
            return y
        }
    }
    return firstArt
}

private fun artworkRunEnd(
    walls: Array<BottleRowWalls?>,
    start: Int,
    maxHole: Int,
): Int {
    var end = start
    var hole = 0
    for (y in start + 1 until walls.size) {
        if (walls[y] != null) {
            end = y
            hole = 0
        } else {
            hole++
            if (hole > maxHole) break
        }
    }
    return end
}

private fun bridgeMaskGaps(
    mask: IntArray,
    pixels: IntArray,
    walls: Array<BottleRowWalls?>,
    width: Int,
    wallLuma: Int,
    inset: Int,
    minGap: Int,
    fillTop: Int,
) {
    val lastY = walls.indices.lastOrNull { walls[it] != null } ?: return
    if (fillTop > lastY) return
    for (y in fillTop..lastY) {
        if (walls[y] != null) continue
        var above = y - 1
        while (above >= 0 && walls[above] == null) above--
        var below = y + 1
        while (below <= lastY && walls[below] == null) below++
        if (above < 0 || below > lastY) continue
        val t = (y - above).toFloat() / (below - above)
        fillMaskRow(
            mask = mask,
            pixels = pixels,
            width = width,
            y = y,
            firstWall = lerp(walls[above]!!.first, walls[below]!!.first, t),
            lastWall = lerp(walls[above]!!.last, walls[below]!!.last, t),
            wallLuma = wallLuma,
            inset = inset,
            minGap = minGap,
        )
    }
}

private fun fillMaskRow(
    mask: IntArray,
    pixels: IntArray,
    width: Int,
    y: Int,
    firstWall: Int,
    lastWall: Int,
    wallLuma: Int,
    inset: Int,
    minGap: Int,
) {
    val start = firstWall + inset
    val end = lastWall - inset
    if (end - start < minGap) return
    val row = y * width
    val filled = 0xFFFFFFFF.toInt()
    val edge = 0xA0FFFFFF.toInt()
    for (x in start..end) {
        if (isBottleArtwork(pixels[row + x], wallLuma)) continue
        val nextToWall =
            (x > 0 && isBottleArtwork(pixels[row + x - 1], wallLuma)) ||
                (x + 1 < width && isBottleArtwork(pixels[row + x + 1], wallLuma))
        mask[row + x] = if (nextToWall) edge else filled
    }
}

private fun interiorEmptyCount(
    pixels: IntArray,
    width: Int,
    y: Int,
    walls: BottleRowWalls,
    wallLuma: Int,
    inset: Int,
): Int {
    val start = walls.first + inset
    val end = walls.last - inset
    if (end < start) return 0
    val row = y * width
    var empty = 0
    for (x in start..end) {
        if (!isBottleArtwork(pixels[row + x], wallLuma)) empty++
    }
    return empty
}

private fun lerp(start: Int, end: Int, t: Float): Int =
    start + ((end - start) * t).roundToInt()

private fun rowHasFill(mask: IntArray, width: Int, y: Int): Boolean {
    val row = y * width
    for (x in 0 until width) {
        if (mask[row + x] != 0) return true
    }
    return false
}
