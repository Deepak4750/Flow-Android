package com.deepak.flow.app.theme

import com.deepak.flow.core.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CategoryAccentTest {

    @Test
    fun builtInCategories_haveStablePrefixedColours() {
        assertEquals(CategoryAccent.Palette[0], CategoryAccent.forCategory(Category.HEALTH))
        assertEquals(CategoryAccent.Palette[1], CategoryAccent.forCategory(Category.FITNESS))
        assertEquals(CategoryAccent.Palette[3], CategoryAccent.forCategory(Category.WORK))
        assertEquals(CategoryAccent.Palette[4], CategoryAccent.forCategory(Category.PERSONAL))
    }

    @Test
    fun customCategory_usesChosenPaletteIndex() {
        val colour = CategoryAccent.forCategory(
            category = Category.CUSTOM,
            customName = "Side project",
            paletteIndex = 6,
        )
        assertEquals(CategoryAccent.Palette[6], colour)
    }

    @Test
    fun customCategory_hashesNameWhenIndexMissing() {
        val first = CategoryAccent.forCategory(Category.CUSTOM, customName = "Side project")
        val second = CategoryAccent.forCategory(Category.CUSTOM, customName = "Side project")
        val other = CategoryAccent.forCategory(Category.CUSTOM, customName = "Languages")
        assertEquals(first, second)
        assertNotEquals(first, other)
    }

    @Test
    fun stableIndex_isWithinPalette() {
        val index = CategoryAccent.stableIndex("Side project")
        assertEquals(true, index in CategoryAccent.Palette.indices)
        assertEquals(CategoryAccent.DefaultCustomIndex, CategoryAccent.stableIndex("  "))
    }
}
