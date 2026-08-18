package com.deepak.flow.core.widget

internal const val MatrixColumns = 7
internal const val MatrixRows = 7
internal const val MatrixDotCount = MatrixColumns * MatrixRows
internal const val MatrixReferenceSizePx = 330f
internal const val MatrixInsetFraction = 0.04f
internal const val MatrixDotRadiusFractionOfCell = 0.22f

internal data class DotGridMetrics(
    val insetPx: Float,
    val cellPx: Float,
    val radiusPx: Float,
    val spacingPx: Float,
)

/**
 * Glyph-style LED grid: inset so launcher rounding never clips dots,
 * cell-centered circles, diameter smaller than the gap.
 */
internal fun dotGridMetrics(
    sizePx: Int,
    columns: Int = MatrixColumns,
): DotGridMetrics {
    val inset = sizePx * MatrixInsetFraction
    if (columns <= 0) {
        return DotGridMetrics(insetPx = inset, cellPx = 0f, radiusPx = 0f, spacingPx = 0f)
    }
    val usable = (sizePx - inset * 2f).coerceAtLeast(0f)
    val cell = usable / columns
    return DotGridMetrics(
        insetPx = inset,
        cellPx = cell,
        radiusPx = cell * MatrixDotRadiusFractionOfCell,
        spacingPx = cell,
    )
}

internal fun dotCenterPx(indexAlongAxis: Int, metrics: DotGridMetrics): Float =
    metrics.insetPx + metrics.cellPx * indexAlongAxis + metrics.cellPx / 2f

internal fun isProgressMatrixCellFilled(index: Int, filledCount: Int): Boolean =
    index in 0 until filledCount
