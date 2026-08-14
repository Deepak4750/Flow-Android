package com.deepak.flow.app.theme

import androidx.compose.ui.unit.dp

object FlowSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val screenHorizontal = 20.dp

    // Every screen opens the same way: this inset, the navigation row, this inset
    // again, then the screen's label. Keeps the first line of content at the same
    // height whether or not the screen has a back button.
    val screenTop = md
}

object FlowSizes {
    val touchTarget = 48.dp
    val iconSm = 18.dp
    val iconMd = 22.dp
    val iconLg = 24.dp
    val fab = 56.dp
    val hairline = 1.dp
    val switchTrackWidth = 44.dp
    val switchTrackHeight = 24.dp
    val switchThumb = 16.dp

    // A glyph sits centred in its touch target, so the target's edge is not the
    // glyph's edge. Shifting an icon button by this much puts the glyph itself on
    // the screen's left edge, in line with the text below it.
    val iconActionOpticalInset = (touchTarget - iconMd) / 2

    // Chips read as compact pills but still need a full touch target, so the
    // tappable area is grown around this height rather than the height raised.
    val chipHeight = 36.dp

    // The single accent mark: what happens next, and the chosen option in a sheet.
    val accentDot = 6.dp

    // Bottom inset that keeps the last list row clear of the floating action button.
    val fabClearance = 96.dp

    val drawerWidth = 300.dp
}

object FlowMotion {
    const val FAST = 120
    const val STANDARD = 160
    const val REVEAL = 220
}
