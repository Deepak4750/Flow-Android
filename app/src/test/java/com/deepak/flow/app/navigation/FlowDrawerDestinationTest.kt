package com.deepak.flow.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowDrawerDestinationTest {

    @Test
    fun drawerOrder_isHomeThenFeaturesThenGymThenHistoryAboveSettings() {
        assertEquals(
            listOf(
                FlowDrawerDestination.HOME,
                FlowDrawerDestination.REMINDERS,
                FlowDrawerDestination.WATER,
                FlowDrawerDestination.GYM,
                FlowDrawerDestination.HISTORY,
                FlowDrawerDestination.SETTINGS,
                FlowDrawerDestination.ABOUT,
            ),
            FlowDrawerDestination.entries,
        )
    }

    @Test
    fun waterTrackingSitsDirectlyBelowReminders() {
        val destinations = FlowDrawerDestination.entries
        val remindersIndex = destinations.indexOf(FlowDrawerDestination.REMINDERS)
        assertEquals(FlowDrawerDestination.WATER, destinations[remindersIndex + 1])
        assertEquals("Tasks", FlowDrawerDestination.REMINDERS.label)
        assertEquals("H₂O", FlowDrawerDestination.WATER.label)
    }

    @Test
    fun gymSitsDirectlyBelowWaterTracking() {
        val destinations = FlowDrawerDestination.entries
        val waterIndex = destinations.indexOf(FlowDrawerDestination.WATER)
        assertEquals(FlowDrawerDestination.GYM, destinations[waterIndex + 1])
        assertEquals("Gym", FlowDrawerDestination.GYM.label)
    }

    @Test
    fun historySitsDirectlyBelowGym() {
        val destinations = FlowDrawerDestination.entries
        val gymIndex = destinations.indexOf(FlowDrawerDestination.GYM)
        assertEquals(FlowDrawerDestination.HISTORY, destinations[gymIndex + 1])
    }

    @Test
    fun historySitsDirectlyAboveSettings() {
        val destinations = FlowDrawerDestination.entries
        val settingsIndex = destinations.indexOf(FlowDrawerDestination.SETTINGS)
        assertEquals(FlowDrawerDestination.HISTORY, destinations[settingsIndex - 1])
    }

    @Test
    fun tasksWaterAndGymHaveFeatureToggles() {
        assertEquals(
            listOf(
                FlowDrawerDestination.REMINDERS,
                FlowDrawerDestination.WATER,
                FlowDrawerDestination.GYM,
            ),
            FlowDrawerDestination.entries.filter { it.isFeature },
        )
    }

    @Test
    fun featureTogglesFollowStoredFlags() {
        assertTrue(
            FlowDrawerDestination.REMINDERS.isEnabled(
                remindersEnabled = true,
                waterEnabled = false,
                gymEnabled = true,
            ),
        )
        assertFalse(
            FlowDrawerDestination.REMINDERS.isEnabled(
                remindersEnabled = false,
                waterEnabled = true,
                gymEnabled = true,
            ),
        )
        assertTrue(
            FlowDrawerDestination.WATER.isEnabled(
                remindersEnabled = false,
                waterEnabled = true,
                gymEnabled = true,
            ),
        )
        assertFalse(
            FlowDrawerDestination.WATER.isEnabled(
                remindersEnabled = true,
                waterEnabled = false,
                gymEnabled = true,
            ),
        )
        assertTrue(
            FlowDrawerDestination.GYM.isEnabled(
                remindersEnabled = false,
                waterEnabled = false,
                gymEnabled = true,
            ),
        )
        assertFalse(
            FlowDrawerDestination.GYM.isEnabled(
                remindersEnabled = true,
                waterEnabled = true,
                gymEnabled = false,
            ),
        )
        assertTrue(
            FlowDrawerDestination.HOME.isEnabled(
                remindersEnabled = false,
                waterEnabled = false,
                gymEnabled = false,
            ),
        )
    }

    @Test
    fun featureOffDoesNotBlockOpeningTheDestination() {
        assertFalse(
            FlowDrawerDestination.REMINDERS.isEnabled(
                remindersEnabled = false,
                waterEnabled = true,
                gymEnabled = true,
            ),
        )
        assertFalse(
            FlowDrawerDestination.WATER.isEnabled(
                remindersEnabled = true,
                waterEnabled = false,
                gymEnabled = true,
            ),
        )
        assertFalse(
            FlowDrawerDestination.GYM.isEnabled(
                remindersEnabled = true,
                waterEnabled = true,
                gymEnabled = false,
            ),
        )
        assertTrue(FlowDrawerDestination.REMINDERS.canNavigate())
        assertTrue(FlowDrawerDestination.WATER.canNavigate())
        assertTrue(FlowDrawerDestination.GYM.canNavigate())
        assertTrue(FlowDrawerDestination.HOME.canNavigate())
    }

    @Test
    fun switchValueExistsOnTasksWaterAndGym() {
        assertEquals(
            true,
            FlowDrawerDestination.REMINDERS.featureChecked(
                remindersEnabled = true,
                waterEnabled = false,
                gymEnabled = true,
            ),
        )
        assertEquals(
            false,
            FlowDrawerDestination.WATER.featureChecked(
                remindersEnabled = true,
                waterEnabled = false,
                gymEnabled = true,
            ),
        )
        assertEquals(
            true,
            FlowDrawerDestination.WATER.featureChecked(
                remindersEnabled = false,
                waterEnabled = true,
                gymEnabled = false,
            ),
        )
        assertEquals(
            true,
            FlowDrawerDestination.GYM.featureChecked(
                remindersEnabled = true,
                waterEnabled = true,
                gymEnabled = true,
            ),
        )
        assertEquals(
            false,
            FlowDrawerDestination.GYM.featureChecked(
                remindersEnabled = true,
                waterEnabled = true,
                gymEnabled = false,
            ),
        )
        assertNull(
            FlowDrawerDestination.HISTORY.featureChecked(
                remindersEnabled = true,
                waterEnabled = true,
                gymEnabled = true,
            ),
        )
        assertNull(
            FlowDrawerDestination.SETTINGS.featureChecked(
                remindersEnabled = true,
                waterEnabled = true,
                gymEnabled = true,
            ),
        )
    }
}
