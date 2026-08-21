package com.deepak.flow.core.water

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.roundToInt

/** Matches [com.deepak.flow.app.theme.FlowWaterFill]. */
internal const val BottleFillArgb = 0xFF5CEEEE.toInt()

/** Matches [com.deepak.flow.app.theme.FlowWaterGlow]. */
internal const val BottleGlowArgb = 0xFF4DE8E8.toInt()

/** Matches [com.deepak.flow.app.theme.FlowWaterSurface]. */
internal const val BottleSurfaceArgb = 0xFFA8FFFF.toInt()

/** Matches [com.deepak.flow.app.theme.FlowBottleOutline]. */
internal const val BottleOutlineArgb = 0xFFD8D8D8.toInt()

private data class CachedBottleLayers(
    val bottle: Bitmap,
    val mask: Bitmap,
    val outline: Bitmap,
    val span: BottleFillSpan,
)

/** Reuses scaled bottle + mask/outline across widget taps for one style size. */
private object BottleLayerCache {
    private val lock = Any()
    private var key: String? = null
    private var layers: CachedBottleLayers? = null

    fun get(cacheKey: String): CachedBottleLayers? = synchronized(lock) {
        val hit = layers
        if (key == cacheKey && hit != null && !hit.bottle.isRecycled) hit else null
    }

    fun put(cacheKey: String, built: CachedBottleLayers): CachedBottleLayers = synchronized(lock) {
        val previous = layers
        if (key == cacheKey && previous != null && !previous.bottle.isRecycled) {
            if (built.bottle !== previous.bottle && !built.bottle.isRecycled) built.bottle.recycle()
            if (built.mask !== previous.mask && !built.mask.isRecycled) built.mask.recycle()
            if (built.outline !== previous.outline && !built.outline.isRecycled) {
                built.outline.recycle()
            }
            return previous
        }
        previous?.let { old ->
            if (!old.bottle.isRecycled) old.bottle.recycle()
            if (!old.mask.isRecycled) old.mask.recycle()
            if (!old.outline.isRecycled) old.outline.recycle()
        }
        key = cacheKey
        layers = built
        built
    }
}

/**
 * Widget path: bounds-check cache first so taps skip full PNG decode on hits.
 */
fun renderCachedBottleFrameBitmap(
    resources: android.content.res.Resources,
    @androidx.annotation.DrawableRes bottleRes: Int,
    cacheKey: String,
    maxContentHeightPx: Int,
    progress: Float,
    fillColor: Int = BottleFillArgb,
): Bitmap {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeResource(resources, bottleRes, bounds)
    val srcW = bounds.outWidth.coerceAtLeast(1)
    val srcH = bounds.outHeight.coerceAtLeast(1)
    val height = maxContentHeightPx.coerceAtMost(srcH)
    val width = (srcW * height / srcH).coerceAtLeast(1)
    val sizeKey = "$cacheKey:${width}x$height"
    BottleLayerCache.get(sizeKey)?.let { hit ->
        return paintBottleFrame(hit, progress, fillColor, recycleLayers = false)
    }
    val options = android.graphics.BitmapFactory.Options().apply { inScaled = false }
    val source = android.graphics.BitmapFactory.decodeResource(resources, bottleRes, options)
    try {
        return renderBottleFrameBitmap(
            source = source,
            progress = progress,
            fillColor = fillColor,
            maxContentHeightPx = maxContentHeightPx,
            cacheKey = cacheKey,
        )
    } finally {
        if (!source.isRecycled) source.recycle()
    }
}

/**
 * Renders the H₂O bottle frame used by the home-screen widget.
 * Matches in-app water fill + soft 360° glow around the water mass.
 *
 * @param maxContentHeightPx scales the source down before mask work so
 *   widget taps stay fast (mask/outline are O(pixels)).
 */
fun renderBottleFrameBitmap(
    source: Bitmap,
    progress: Float,
    fillColor: Int = BottleFillArgb,
    maxContentHeightPx: Int = Int.MAX_VALUE,
    cacheKey: String? = null,
): Bitmap {
    val sizeKey = cacheKey?.let { key ->
        val height = if (maxContentHeightPx == Int.MAX_VALUE) {
            source.height
        } else {
            maxContentHeightPx.coerceAtMost(source.height).coerceAtLeast(1)
        }
        val width = (source.width * height / source.height.coerceAtLeast(1)).coerceAtLeast(1)
        "$key:${width}x$height"
    }
    val layers = if (sizeKey != null) {
        BottleLayerCache.get(sizeKey) ?: run {
            val working = scaleForWidget(source, maxContentHeightPx)
            val built = decodeLayersUncached(working, ownBottle = working !== source || cacheKey != null)
            if (working !== source && working !== built.bottle) working.recycle()
            BottleLayerCache.put(sizeKey, built)
        }
    } else {
        val working = scaleForWidget(source, maxContentHeightPx)
        decodeLayersUncached(working, ownBottle = false)
    }
    return paintBottleFrame(
        layers = layers,
        progress = progress,
        fillColor = fillColor,
        recycleLayers = cacheKey == null,
    )
}

private fun paintBottleFrame(
    layers: CachedBottleLayers,
    progress: Float,
    fillColor: Int,
    recycleLayers: Boolean,
): Bitmap {
    val width = layers.bottle.width
    val height = layers.bottle.height
    val span = layers.span
    val padX = max(1, (width * 0.22f).roundToInt())
    val padTop = max(1, (height * 0.08f).roundToInt())
    val padBottom = max(1, (width * 0.22f).roundToInt())
    val out = Bitmap.createBitmap(
        width + padX * 2,
        height + padTop + padBottom,
        Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(out)
    val ox = padX.toFloat()
    val oy = padTop.toFloat()
    val clamped = progress.coerceIn(0f, 1f)
    val glowStrength = (0.45f + 0.55f * clamped).coerceIn(0f, 1f)
    val cavityTop = span.top.toFloat()
    val cavityBottom = span.bottomExclusive.toFloat()
    val fillHeight = (cavityBottom - cavityTop) * clamped
    val waterTop = cavityBottom - fillHeight
    val centerX = ox + width / 2f
    val waterCenterY = oy + (waterTop + cavityBottom) / 2f

    if (clamped > 0.01f && fillHeight > 0.5f) {
        val bloomRadiusX = width * 0.72f
        val bloomRadiusY = max(fillHeight * 0.62f + width * 0.22f, width * 0.58f)
        drawWaterHalo(
            canvas = canvas,
            centerX = centerX,
            centerY = waterCenterY,
            radiusX = bloomRadiusX,
            radiusY = bloomRadiusY,
            intensity = glowStrength,
        )
    }

    val outlinePaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = PorterDuffColorFilter(BottleOutlineArgb, PorterDuff.Mode.SRC_IN)
    }
    canvas.drawBitmap(layers.outline, ox, oy, outlinePaint)

    if (fillHeight > 0.5f) {
        drawClippedWater(
            canvas = canvas,
            mask = layers.mask,
            originX = ox,
            originY = oy,
            width = width,
            height = height,
            waterTop = waterTop,
            waterBottom = cavityBottom,
            fillHeight = fillHeight,
            fillColor = fillColor,
            glowStrength = glowStrength,
        )
    }

    val bodyPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    1.40f, 0f, 0f, 0f, 30f,
                    0f, 1.40f, 0f, 0f, 30f,
                    0f, 0f, 1.40f, 0f, 30f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
    }
    canvas.drawBitmap(layers.bottle, ox, oy, bodyPaint)

    if (recycleLayers) {
        // Uncached path: layers borrow the caller's source; only free mask/outline.
        layers.mask.recycle()
        layers.outline.recycle()
    }
    return out
}

private fun scaleForWidget(source: Bitmap, maxContentHeightPx: Int): Bitmap {
    if (maxContentHeightPx == Int.MAX_VALUE || source.height <= maxContentHeightPx) return source
    val width = source.width * maxContentHeightPx / source.height
    return Bitmap.createScaledBitmap(source, width.coerceAtLeast(1), maxContentHeightPx, true)
}

private fun decodeLayersUncached(
    source: Bitmap,
    ownBottle: Boolean,
): CachedBottleLayers {
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
    val bottle = if (ownBottle) {
        if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        source
    }
    return CachedBottleLayers(bottle, mask, outline, span)
}

/** Soft radial halo centered on the water mass so glow wraps 360°. */
private fun drawWaterHalo(
    canvas: Canvas,
    centerX: Float,
    centerY: Float,
    radiusX: Float,
    radiusY: Float,
    intensity: Float,
) {
    if (radiusX < 1f || radiusY < 1f) return
    val save = canvas.save()
    canvas.scale(1f, radiusY / radiusX, centerX, centerY)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            centerX,
            centerY,
            radiusX,
            intArrayOf(
                withAlpha(BottleGlowArgb, (0.690f * intensity * 255f).roundToInt()),
                withAlpha(BottleGlowArgb, (0.449f * intensity * 255f).roundToInt()),
                withAlpha(BottleGlowArgb, (0.208f * intensity * 255f).roundToInt()),
                withAlpha(BottleGlowArgb, (0.069f * intensity * 255f).roundToInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.35f, 0.62f, 0.84f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawCircle(centerX, centerY, radiusX, paint)
    canvas.restoreToCount(save)
}

private fun drawClippedWater(
    canvas: Canvas,
    mask: Bitmap,
    originX: Float,
    originY: Float,
    width: Int,
    height: Int,
    waterTop: Float,
    waterBottom: Float,
    fillHeight: Float,
    fillColor: Int,
    glowStrength: Float,
) {
    val left = originX
    val top = originY + waterTop
    val right = originX + width
    val bottom = originY + waterBottom
    val layerBounds = RectF(left, top, right, bottom)
    val save = canvas.saveLayer(layerBounds, Paint())
    try {
        val fillPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = PorterDuffColorFilter(fillColor, PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(mask, originX, originY, fillPaint)

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                top,
                0f,
                bottom,
                intArrayOf(
                    withAlpha(BottleGlowArgb, (0.225f * glowStrength * 255f).roundToInt()),
                    withAlpha(BottleGlowArgb, (0.25f * glowStrength * 255f).roundToInt()),
                    withAlpha(BottleGlowArgb, (0.225f * glowStrength * 255f).roundToInt()),
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(left, top, right, bottom, glowPaint)

        val band = (fillHeight * 0.18f).coerceIn(8f, 28f)
        val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                top,
                0f,
                top + band,
                intArrayOf(
                    withAlpha(BottleSurfaceArgb, (0.24f * 255f).roundToInt()),
                    withAlpha(BottleGlowArgb, (0.125f * 255f).roundToInt()),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(left, top, right, top + band, surfacePaint)

        val clipPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(mask, originX, originY, clipPaint)
    } finally {
        canvas.restoreToCount(save)
    }
}

private fun withAlpha(argb: Int, alpha: Int): Int {
    val a = alpha.coerceIn(0, 255)
    return (a shl 24) or (argb and 0x00FFFFFF)
}
