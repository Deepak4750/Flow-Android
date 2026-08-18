package com.deepak.flow.feature.reminder.presentation

import com.deepak.flow.app.theme.CategoryAccent
import com.deepak.flow.core.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateReminderCategoryTest {

    @Test
    fun tappingASavedCustom_selectsNameAndLastAccent() {
        val selected = CreateReminderUiState().selectingSavedCustom(
            name = "Gym",
            accentColorIndex = 6,
        )

        assertEquals(Category.CUSTOM, selected.category)
        assertEquals("Gym", selected.customCategoryName)
        assertEquals(6, selected.accentColorIndex)
    }

    @Test
    fun tappingASavedCustom_usesStableAccentWhenIndexMissing() {
        val selected = CreateReminderUiState(
            accentColorIndex = 0,
        ).selectingSavedCustom(
            name = "Gym",
            accentColorIndex = null,
        )

        assertEquals(Category.CUSTOM, selected.category)
        assertEquals("Gym", selected.customCategoryName)
        assertEquals(CategoryAccent.stableIndex("Gym"), selected.accentColorIndex)
    }
}
