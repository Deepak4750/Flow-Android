package com.deepak.flow.core.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

internal fun renderProgressMatrixBitmap(
    filledCount: Int,
    sizePx: Int,
    onColor: Int,
    offColor: Int,
    backgroundColor: Int,
    columns: Int = MatrixColumns,
    rows: Int = MatrixRows,
): Bitmap = renderDotGridBitmap(
    sizePx = sizePx,
    columns = columns,
    rows = rows,
    onColor = onColor,
    offColor = offColor,
    backgroundColor = backgroundColor,
    isOn = { index -> isProgressMatrixCellFilled(index, filledCount) },
)

internal fun renderDotMatrixTextBitmap(
    text: String,
    sizePx: Int,
    onColor: Int,
    offColor: Int,
    backgroundColor: Int,
): Bitmap {
    val columns = squareGridSizeForDotMatrixText(text)
    return renderDotGridBitmap(
        sizePx = sizePx,
        columns = columns,
        rows = columns,
        onColor = onColor,
        offColor = offColor,
        backgroundColor = backgroundColor,
        isOn = { index -> isDotMatrixTextCellFilled(index, text, columns, columns) },
    )
}

internal fun renderDotGridBitmap(
    sizePx: Int,
    columns: Int,
    rows: Int,
    onColor: Int,
    offColor: Int,
    backgroundColor: Int,
    isOn: (index: Int) -> Boolean,
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(backgroundColor)
    if (columns <= 0 || rows <= 0) return bitmap

    val metrics = dotGridMetrics(sizePx, columns)
    val offPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = offColor }
    val onPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = onColor }
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(28, Color.red(onColor), Color.green(onColor), Color.blue(onColor))
    }
    for (row in 0 until rows) {
        for (col in 0 until columns) {
            val index = row * columns + col
            val cx = dotCenterPx(col, metrics)
            val cy = dotCenterPx(row, metrics)
            if (isOn(index)) {
                canvas.drawCircle(cx, cy, metrics.radiusPx * 1.22f, haloPaint)
                canvas.drawCircle(cx, cy, metrics.radiusPx, onPaint)
            } else {
                canvas.drawCircle(cx, cy, metrics.radiusPx, offPaint)
            }
        }
    }
    return bitmap
}
