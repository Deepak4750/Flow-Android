package com.deepak.flow.app.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Dark only, monochrome only. No dynamic colour, no light scheme, no theme toggle.
private val FlowMonochromeScheme = darkColorScheme(
    primary = FlowWhite,
    onPrimary = FlowBlack,
    secondary = FlowTextSecondary,
    onSecondary = FlowTextPrimary,
    background = FlowBackground,
    onBackground = FlowTextPrimary,
    surface = FlowSurface,
    onSurface = FlowTextPrimary,
    surfaceVariant = FlowSurfaceRaised,
    onSurfaceVariant = FlowTextSecondary,
    surfaceContainer = FlowSurface,
    surfaceContainerHigh = FlowSurfaceRaised,
    outline = FlowBorderStrong,
    outlineVariant = FlowBorder,
    primaryContainer = FlowSurfaceRaised,
    onPrimaryContainer = FlowTextPrimary,
    scrim = FlowBlack,
    error = FlowError,
    onError = FlowBlack,
)

// Geometry stays tight and consistent: controls 4dp, surfaces 8dp, sheets 16dp.
private val FlowShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun FlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FlowMonochromeScheme,
        typography = FlowTypography,
        shapes = FlowShapes,
        content = content,
    )
}
