package com.deepak.flow.feature.water.presentation

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.deepak.flow.app.theme.FlowBottleOutline
import com.deepak.flow.app.theme.FlowMotion
import com.deepak.flow.app.theme.FlowWaterFill
import com.deepak.flow.app.theme.FlowWaterGlow
import com.deepak.flow.app.theme.FlowWaterSurface
import com.deepak.flow.core.water.bottleFillMaskPixels
import com.deepak.flow.core.water.bottleFillVerticalSpan
import com.deepak.flow.core.water.bottleOutlinePixels
import kotlin.math.max
import kotlin.math.roundToInt

/** Graphite lift so the bottle stays readable on AMOLED and LCD. */
private val BottleBodyColorFilter = ColorMatrixColorFilter(
    ColorMatrix(
        floatArrayOf(
            1.40f, 0f, 0f, 0f, 30f,
            0f, 1.40f, 0f, 0f, 30f,
            0f, 0f, 1.40f, 0f, 30f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

private val WaterLayerPaint = Paint()

@Composable
internal fun BottleFill(
    @DrawableRes bottleRes: Int,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    val layers = remember(bottleRes, resources) {
        decodeBottleLayers(resources, bottleRes)
    }
    val clamped = progress.coerceIn(0f, 1f)
    val fill by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = FlowMotion.REVEAL),
        label = "bottleFill",
    )
    val percent = (clamped * 100f).roundToInt()
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Water bottle, $percent percent"
        },
    ) {
        val dest = fittedDest(layers.bottle)
        val cavityTop = dest.top + dest.height * layers.fillTopFraction
        val cavityBottom = dest.top + dest.height * layers.fillBottomFraction
        val cavityHeight = (cavityBottom - cavityTop).coerceAtLeast(0f)
        val fillHeight = cavityHeight * fill
        val waterTop = cavityBottom - fillHeight
        val centerX = dest.left + dest.width / 2f
        val glowStrength = (0.45f + 0.55f * fill).coerceIn(0f, 1f)

        if (fill > 0.01f && fillHeight > 0.5f) {
            drawWaterHalo(
                centerX = centerX,
                waterTop = waterTop,
                waterBottom = cavityBottom,
                bottleWidth = dest.width,
                fillHeight = fillHeight,
                intensity = glowStrength,
            )
        }

        drawImage(
            image = layers.outline,
            dstOffset = dest.offset,
            dstSize = dest.size,
            colorFilter = ColorFilter.tint(FlowBottleOutline, BlendMode.SrcIn),
            filterQuality = FilterQuality.High,
        )

        if (fillHeight > 0.5f) {
            drawClippedWater(
                mask = layers.mask,
                dest = dest,
                waterTop = waterTop,
                waterBottom = cavityBottom,
                fillHeight = fillHeight,
                glowStrength = glowStrength,
            )
        }

        drawImage(
            image = layers.bottle,
            dstOffset = dest.offset,
            dstSize = dest.size,
            colorFilter = BottleBodyColorFilter,
            filterQuality = FilterQuality.High,
        )
    }
}

/**
 * Soft radial halo centered on the water mass so the glow wraps around
 * the luminous water (sides + above/below), not only under the base.
 */
private fun DrawScope.drawWaterHalo(
    centerX: Float,
    waterTop: Float,
    waterBottom: Float,
    bottleWidth: Float,
    fillHeight: Float,
    intensity: Float,
) {
    val centerY = (waterTop + waterBottom) / 2f
    val radiusX = bottleWidth * 0.72f
    val radiusY = max(fillHeight * 0.62f + bottleWidth * 0.22f, bottleWidth * 0.58f)
    if (radiusX < 1f || radiusY < 1f) return
    val center = Offset(centerX, centerY)
    scale(scaleX = 1f, scaleY = radiusY / radiusX, pivot = center) {
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to FlowWaterGlow.copy(alpha = 0.690f * intensity),
                    0.35f to FlowWaterGlow.copy(alpha = 0.449f * intensity),
                    0.62f to FlowWaterGlow.copy(alpha = 0.208f * intensity),
                    0.84f to FlowWaterGlow.copy(alpha = 0.069f * intensity),
                    1.00f to Color.Transparent,
                ),
                center = center,
                radius = radiusX,
            ),
            radius = radiusX,
            center = center,
        )
    }
}

/**
 * Water, inner illumination, and surface highlight, all clipped to the
 * bottle interior mask. A rect clip alone would let the surface band
 * leak through the transparent padding of the PNG.
 */
private fun DrawScope.drawClippedWater(
    mask: ImageBitmap,
    dest: FittedDest,
    waterTop: Float,
    waterBottom: Float,
    fillHeight: Float,
    glowStrength: Float,
) {
    val left = dest.left
    val right = dest.left + dest.width
    clipRect(
        left = left,
        top = waterTop,
        right = right,
        bottom = waterBottom,
    ) {
        drawContext.canvas.saveLayer(
            Rect(left, waterTop, right, waterBottom),
            WaterLayerPaint,
        )
        try {
            drawImage(
                image = mask,
                dstOffset = dest.offset,
                dstSize = dest.size,
                colorFilter = ColorFilter.tint(FlowWaterFill, BlendMode.SrcIn),
                filterQuality = FilterQuality.High,
            )
            // Soft, even luminosity through the water mass (not bottom-heavy).
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        FlowWaterGlow.copy(alpha = 0.225f * glowStrength),
                        FlowWaterGlow.copy(alpha = 0.25f * glowStrength),
                        FlowWaterGlow.copy(alpha = 0.225f * glowStrength),
                    ),
                    startY = waterTop,
                    endY = waterBottom,
                ),
                topLeft = Offset(left, waterTop),
                size = Size(dest.width, fillHeight),
            )
            val band = (fillHeight * 0.18f).coerceIn(8f, 28f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        FlowWaterSurface.copy(alpha = 0.24f),
                        FlowWaterGlow.copy(alpha = 0.125f),
                        Color.Transparent,
                    ),
                    startY = waterTop,
                    endY = waterTop + band,
                ),
                topLeft = Offset(left, waterTop),
                size = Size(dest.width, band),
            )
            drawImage(
                image = mask,
                dstOffset = dest.offset,
                dstSize = dest.size,
                blendMode = BlendMode.DstIn,
                filterQuality = FilterQuality.High,
            )
        } finally {
            drawContext.canvas.restore()
        }
    }
}

private data class BottleLayers(
    val bottle: ImageBitmap,
    val mask: ImageBitmap,
    val outline: ImageBitmap,
    val fillTopFraction: Float,
    val fillBottomFraction: Float,
)

private data class FittedDest(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val offset: IntOffset get() = IntOffset(left.roundToInt(), top.roundToInt())
    val size: IntSize get() = IntSize(width.roundToInt(), height.roundToInt())
}

/**
 * Fit the bottle inside the canvas with a modest floor reserve so the
 * ambient bloom can sit under the base instead of being clipped into a
 * hard horizontal edge at the canvas bottom.
 */
private fun DrawScope.fittedDest(image: ImageBitmap): FittedDest {
    val bottomReserve = size.height * 0.14f
    val sideReserve = size.width * 0.12f
    val topReserve = size.height * 0.04f
    val availWidth = (size.width - sideReserve * 2f).coerceAtLeast(1f)
    val availHeight = (size.height - bottomReserve - topReserve).coerceAtLeast(1f)
    val scale = minOf(availWidth / image.width, availHeight / image.height)
    val width = image.width * scale
    val height = image.height * scale
    return FittedDest(
        left = (size.width - width) / 2f,
        top = topReserve + ((availHeight - height) / 2f).coerceAtLeast(0f),
        width = width,
        height = height,
    )
}

private fun decodeBottleLayers(
    resources: Resources,
    @DrawableRes bottleRes: Int,
): BottleLayers {
    val source = BitmapFactory.decodeResource(resources, bottleRes)
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    val maskPixels = bottleFillMaskPixels(pixels, width, height)
    val outlinePixels = bottleOutlinePixels(pixels, width, height)
    val span = bottleFillVerticalSpan(maskPixels, width, height)
    val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    mask.setPixels(maskPixels, 0, width, 0, 0, width, height)
    val outline = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    outline.setPixels(outlinePixels, 0, width, 0, 0, width, height)
    val imageHeight = height.coerceAtLeast(1).toFloat()
    return BottleLayers(
        bottle = source.asImageBitmap(),
        mask = mask.asImageBitmap(),
        outline = outline.asImageBitmap(),
        fillTopFraction = span.top / imageHeight,
        fillBottomFraction = span.bottomExclusive / imageHeight,
    )
}
