package com.deepak.flow.app.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.deepak.flow.app.theme.FlowMotion
import com.deepak.flow.app.theme.FlowSurfaceRaised

/** Shared drag/reorder pickup visuals for Flow lists. Gesture logic stays in each screen. */
object FlowDragReorder {
    const val draggedZIndex = 10f
    const val draggedScale = 1.01f
    val draggedElevation = 6.dp
}

/**
 * Wraps a reorder list item with shared pickup, float, displacement, and settle visuals.
 *
 * While dragging, an opaque [FlowSurfaceRaised] layer fills the item bounds behind its
 * content so underlying rows cannot bleed through. Non-dragged siblings animate smoothly
 * when [displacementY] changes.
 */
@Composable
fun FlowDragReorderItem(
    isDragging: Boolean,
    displacementY: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val animatedDisplacement by animateFloatAsState(
        targetValue = displacementY,
        animationSpec = tween(FlowMotion.STANDARD),
        label = "flowDragReorderDisplacement",
    )
    val translationY = if (isDragging) displacementY else animatedDisplacement

    val scale by animateFloatAsState(
        targetValue = if (isDragging) FlowDragReorder.draggedScale else 1f,
        animationSpec = tween(FlowMotion.STANDARD),
        label = "flowDragReorderScale",
    )
    val elevation by animateDpAsState(
        targetValue = if (isDragging) FlowDragReorder.draggedElevation else 0.dp,
        animationSpec = tween(FlowMotion.STANDARD),
        label = "flowDragReorderElevation",
    )
    val surfaceAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(FlowMotion.STANDARD),
        label = "flowDragReorderSurfaceAlpha",
    )
    val shape = MaterialTheme.shapes.medium

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) FlowDragReorder.draggedZIndex else 0f)
            .graphicsLayer {
                this.translationY = translationY
                scaleX = scale
                scaleY = scale
                clip = false
            },
    ) {
        // Always composed so pickup does not rebuild the tree mid-gesture.
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = surfaceAlpha }
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    clip = false,
                )
                .clip(shape)
                .background(FlowSurfaceRaised, shape),
        )
        content()
    }
}
