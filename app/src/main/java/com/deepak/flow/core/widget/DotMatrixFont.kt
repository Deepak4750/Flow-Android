package com.deepak.flow.core.widget

internal const val DotMatrixGlyphHeight = 5
private const val DefaultGap = 1

private val Glyphs: Map<Char, Array<String>> = mapOf(
    '0' to arrayOf("###", "#.#", "#.#", "#.#", "###"),
    '1' to arrayOf(".#.", "##.", ".#.", ".#.", "###"),
    '2' to arrayOf("###", "..#", "###", "#..", "###"),
    '3' to arrayOf("###", "..#", "###", "..#", "###"),
    '4' to arrayOf("#.#", "#.#", "###", "..#", "..#"),
    '5' to arrayOf("###", "#..", "###", "..#", "###"),
    '6' to arrayOf("###", "#..", "###", "#.#", "###"),
    '7' to arrayOf("###", "..#", "..#", "..#", "..#"),
    '8' to arrayOf("###", "#.#", "###", "#.#", "###"),
    '9' to arrayOf("###", "#.#", "###", "..#", "###"),
    '.' to arrayOf(".", ".", ".", ".", "#"),
    '%' to arrayOf("#.#", "#.#", ".#.", "#.#", "#.#"),
)

fun measureDotMatrixTextWidth(text: String, gap: Int = DefaultGap): Int {
    val widths = text.mapNotNull { Glyphs[it]?.first()?.length }
    if (widths.isEmpty()) return 0
    return widths.sum() + gap * (widths.size - 1)
}

fun squareGridSizeForDotMatrixText(text: String, gap: Int = DefaultGap): Int {
    val width = measureDotMatrixTextWidth(text, gap)
    if (width <= 0) return MatrixColumns
    return maxOf(width, DotMatrixGlyphHeight) + 2
}

fun isDotMatrixTextCellFilled(
    index: Int,
    text: String,
    columns: Int,
    rows: Int,
    gap: Int = DefaultGap,
): Boolean {
    if (columns <= 0 || rows <= 0) return false
    val col = index % columns
    val row = index / columns
    if (col !in 0 until columns || row !in 0 until rows) return false

    val glyphs = text.mapNotNull { Glyphs[it] }
    val contentWidth = measureDotMatrixTextWidth(text, gap)
    if (glyphs.isEmpty() || contentWidth <= 0) return false

    val originCol = (columns - contentWidth) / 2
    val originRow = (rows - DotMatrixGlyphHeight) / 2
    val localRow = row - originRow
    if (localRow !in 0 until DotMatrixGlyphHeight) return false

    var cursor = originCol
    glyphs.forEachIndexed { glyphIndex, glyph ->
        val width = glyph[0].length
        if (col in cursor until cursor + width) {
            return glyph[localRow][col - cursor] == '#'
        }
        cursor += width
        if (glyphIndex < glyphs.lastIndex) cursor += gap
    }
    return false
}
