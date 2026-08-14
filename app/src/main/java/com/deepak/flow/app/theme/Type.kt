package com.deepak.flow.app.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Two families only: a light-weight sans for statements, a mono for labels and
// metadata. Tight negative tracking on headlines, wide tracking on uppercase labels.
private val FlowSans = FontFamily.SansSerif
private val FlowMono = FontFamily.Monospace

/*
 * Role to style mapping. A role means the same thing on every screen, so it must
 * look the same on every screen. Pick by role, never by eye.
 *
 *   Screen hero (greeting, tagline, opening statement)
 *       headlineLarge / FlowTextPrimary        — via FlowScreenTitle
 *   Prominent item (the next-up reminder, the drawer identity)
 *       headlineMedium / FlowTextPrimary
 *   List item title, text field input, dialog title
 *       titleLarge / FlowTextPrimary
 *   Drawer navigation item
 *       titleMedium / FlowTextPrimary when selected, FlowTextSecondary otherwise
 *   Screen header title, section label, field label
 *       labelLarge mono uppercase / FlowTextTertiary
 *   Button and text-action label
 *       labelLarge mono uppercase / foreground of the control
 *   Inline metadata, chip label
 *       labelMedium mono uppercase / FlowTextTertiary
 *   Chosen value (selector row, info row, option row, stepper unit)
 *       bodyLarge / FlowTextPrimary, FlowTextSecondary when not chosen
 *   Supporting prose, helper text, list metadata line
 *       bodyMedium / FlowTextSecondary
 *   Placeholder, disabled content
 *       host style / FlowTextDisabled
 *
 * The colour is part of the role. FlowAccent is not in this table: it marks what
 * happens next and the selected option in a sheet, and appears nowhere else.
 */

val FlowTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FlowSans,
        fontWeight = FontWeight.Light,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.2).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FlowSans,
        fontWeight = FontWeight.Light,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-1).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FlowSans,
        fontWeight = FontWeight.Light,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.75).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FlowSans,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FlowSans,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FlowSans,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.15).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FlowSans,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FlowSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FlowSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FlowSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FlowSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FlowSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
    ),
    // Form labels are read, not decorative: 12sp with slightly tightened
    // tracking stays disciplined while remaining comfortably legible.
    labelLarge = TextStyle(
        fontFamily = FlowMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FlowMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FlowMono,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.sp,
    ),
)
