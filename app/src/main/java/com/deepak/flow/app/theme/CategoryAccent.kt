package com.deepak.flow.app.theme

import androidx.compose.ui.graphics.Color
import com.deepak.flow.core.model.Category

/**
 * Quiet, prefixed accents for categories. They stay at 6dp dots - never
 * fill cards or chips - so Flow remains monochrome with one small colour cue.
 */
object CategoryAccent {
    val Palette: List<Color> = listOf(
        Color(0xFF7D9B8A), // sage
        Color(0xFFC49A78), // clay
        Color(0xFFC4B48A), // sand
        Color(0xFF7A9BB8), // steel
        Color(0xFFA394B8), // dusk
        Color(0xFF6EB3C9), // mist
        Color(0xFFC48E96), // rose
        Color(0xFF8AA8A0), // sea
    )

    val Names: List<String> = listOf(
        "Sage",
        "Clay",
        "Sand",
        "Steel",
        "Dusk",
        "Mist",
        "Rose",
        "Sea",
    )

    const val DefaultCustomIndex = 5

    fun forCategory(
        category: Category,
        customName: String? = null,
        paletteIndex: Int? = null,
    ): Color = when (category) {
        Category.HEALTH -> Palette[0]
        Category.FITNESS -> Palette[1]
        Category.STUDY -> Palette[2]
        Category.WORK -> Palette[3]
        Category.PERSONAL -> Palette[4]
        Category.CUSTOM -> Palette[
            paletteIndex?.coerceIn(0, Palette.lastIndex) ?: stableIndex(customName),
        ]
    }

    fun stableIndex(name: String?): Int {
        if (name.isNullOrBlank()) return DefaultCustomIndex
        return name.trim().lowercase().hashCode().and(0x7fffffff) % Palette.size
    }
}
