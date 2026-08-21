package com.deepak.flow.core.widget

import com.deepak.flow.core.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaterWidgetSnapshotTest {

    @Test
    fun staysUnreadyUntilWaterGoalAndBottleAreSet() {
        assertFalse(waterWidgetSnapshotFor(null, todayEpochDay = 10).ready)
        assertFalse(waterWidgetSnapshotFor(UserProfile(waterEnabled = true), 10).ready)
        assertFalse(
            waterWidgetSnapshotFor(
                UserProfile(waterEnabled = true, waterGoalMl = 2000),
                todayEpochDay = 10,
            ).ready,
        )
        val ready = waterWidgetSnapshotFor(
            UserProfile(
                waterEnabled = true,
                waterGoalMl = 2000,
                waterBottleStyleIndex = 1,
                waterIntakeMl = 250,
                waterIntakeEpochDay = 10,
            ),
            todayEpochDay = 10,
        )
        assertTrue(ready.ready)
        assertEquals(250, ready.millilitres)
        assertEquals(2000, ready.goalMl)
        assertEquals(1, ready.styleIndex)
    }

    @Test
    fun intakeResetsWhenTheDayChanges() {
        val snapshot = waterWidgetSnapshotFor(
            UserProfile(
                waterEnabled = true,
                waterGoalMl = 2000,
                waterBottleStyleIndex = 0,
                waterIntakeMl = 750,
                waterIntakeEpochDay = 10,
            ),
            todayEpochDay = 11,
        )
        assertTrue(snapshot.ready)
        assertEquals(0, snapshot.millilitres)
    }
}
