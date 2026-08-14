package com.deepak.flow.app.theme

import androidx.compose.ui.graphics.Color

// Flow is monochrome by design. Greys carry all hierarchy; the accent is reserved
// for a single semantic role (what happens next) and nothing decorative.
val FlowBlack = Color(0xFF000000)
val FlowBackground = Color(0xFF000000)
val FlowSurface = Color(0xFF0A0A0A)
val FlowSurfaceRaised = Color(0xFF141414)

// Dividers stay quiet; interactive outlines clear the 3:1 non-text contrast floor.
val FlowBorder = Color(0xFF2A2A2A)
val FlowBorderStrong = Color(0xFF5A5A5A)

val FlowWhite = Color(0xFFFFFFFF)
val FlowPressed = Color(0xFFE4E4E4)

// Text ramp. Contrast ratios against the #000000 background:
// primary 21:1, secondary 12.6:1, tertiary 9.4:1, disabled 4.9:1.
// Every step clears WCAG AA (4.5:1) so no text in Flow is decorative-only.
val FlowTextPrimary = Color(0xFFFFFFFF)
val FlowTextSecondary = Color(0xFFC8C8C8)
val FlowTextTertiary = Color(0xFFADADAD)
val FlowTextDisabled = Color(0xFF7A7A7A)

val FlowAccent = Color(0xFF4FC3F7)
val FlowError = Color(0xFFE57373)
