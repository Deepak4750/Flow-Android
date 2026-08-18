package com.deepak.flow.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SavedCustomCategoryTest {

    @Test
    fun savingACustomName_makesItAvailableOnTheNextCreate() {
        val reminders = listOf(customReminder(name = "Gym", accent = 2))

        assertEquals(
            listOf(SavedCustomCategory(name = "Gym", accentColorIndex = 2)),
            reminders.savedCustomCategories(),
        )
    }

    @Test
    fun twoRemindersShareAName_deletingOneKeepsTheCategory() {
        val both = listOf(
            customReminder(id = 1, name = "Gym", accent = 1),
            customReminder(id = 2, name = "Gym", accent = 3),
        )
        assertEquals(listOf("Gym"), both.savedCustomCategories().map { it.name })

        val afterDeletingOne = both.filterNot { it.id == 1L }
        assertEquals(listOf("Gym"), afterDeletingOne.savedCustomCategories().map { it.name })
    }

    @Test
    fun permanentlyDeletingTheLastReminder_removesTheCategory() {
        val reminders = listOf(
            customReminder(id = 1, name = "Gym"),
            customReminder(id = 2, name = "Languages"),
        )
        val afterLastGymDeleted = reminders.filterNot { it.id == 1L }

        assertEquals(listOf("Languages"), afterLastGymDeleted.savedCustomCategories().map { it.name })
        assertEquals(emptyList<SavedCustomCategory>(), emptyList<Reminder>().savedCustomCategories())
    }

    @Test
    fun completingAReminder_doesNotRemoveTheCategory() {
        val stillPersisted = listOf(customReminder(name = "Gym"))
        val completedTodayIds = setOf(1L)

        assertTrue(stillPersisted.any { it.id in completedTodayIds })
        assertEquals(listOf("Gym"), stillPersisted.savedCustomCategories().map { it.name })
    }

    @Test
    fun builtInCategories_areUnaffected() {
        val reminders = listOf(
            reminder(
                category = Category.FITNESS,
                customCategoryName = null,
            ),
            customReminder(name = "Gym"),
        )
        val saved = reminders.savedCustomCategories()

        assertEquals(listOf("Gym"), saved.map { it.name })
        assertFalse(saved.any { it.name == Category.FITNESS.displayName })
        assertEquals(Category.FITNESS, reminders.first().category)
    }

    @Test
    fun blankAndEmptyNames_areOmitted() {
        val reminders = listOf(
            customReminder(name = "  "),
            customReminder(id = 2, name = ""),
            customReminder(id = 3, customCategoryName = null),
            customReminder(id = 4, name = " Gym "),
        )

        assertEquals(
            listOf(SavedCustomCategory(name = "Gym", accentColorIndex = 2)),
            reminders.savedCustomCategories(),
        )
    }

    @Test
    fun lastUsedAccent_winsForASharedName() {
        val reminders = listOf(
            customReminder(id = 1, name = "Gym", accent = 1),
            customReminder(id = 3, name = "Gym", accent = 6),
            customReminder(id = 2, name = "Gym", accent = 4),
        )

        assertEquals(
            listOf(SavedCustomCategory(name = "Gym", accentColorIndex = 6)),
            reminders.savedCustomCategories(),
        )
    }

    @Test
    fun namesAreSortedAndDistinct() {
        val reminders = listOf(
            customReminder(id = 1, name = "Languages"),
            customReminder(id = 2, name = "Gym"),
            customReminder(id = 3, name = "Languages"),
        )

        assertEquals(listOf("Gym", "Languages"), reminders.savedCustomCategories().map { it.name })
    }
}

private fun customReminder(
    id: Long = 1L,
    name: String? = "Gym",
    accent: Int? = 2,
    customCategoryName: String? = name,
) = reminder(
    id = id,
    category = Category.CUSTOM,
    customCategoryName = customCategoryName,
    accentColorIndex = accent,
)

private fun reminder(
    id: Long = 1L,
    category: Category = Category.PERSONAL,
    customCategoryName: String? = null,
    accentColorIndex: Int? = null,
) = Reminder(
    id = id,
    title = "Task",
    category = category,
    customCategoryName = customCategoryName,
    schedule = Schedule.Daily,
    reminderTimes = listOf(LocalTime.of(19, 0)),
    startDate = LocalDate.of(2026, 1, 1),
    accentColorIndex = accentColorIndex,
)
