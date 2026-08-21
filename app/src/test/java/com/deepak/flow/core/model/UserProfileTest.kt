package com.deepak.flow.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileTest {

    @Test
    fun remindersStayOnByDefault() {
        assertTrue(UserProfile().remindersEnabled)
        assertTrue(null.remindersFeatureEnabled())
    }

    @Test
    fun waterStartsOffUntilTurnedOn() {
        assertFalse(UserProfile().waterEnabled)
        assertFalse(null.waterFeatureEnabled())
    }

    @Test
    fun waterGoalStartsUnset() {
        assertEquals(null, UserProfile().waterGoalMl)
    }

    @Test
    fun bottleStyleStartsUnset() {
        assertEquals(null, UserProfile().waterBottleStyleIndex)
        assertEquals(0, parseWaterBottleStyleIndex(0))
        assertEquals(2, parseWaterBottleStyleIndex(2))
        assertEquals(null, parseWaterBottleStyleIndex(3))
        assertEquals(null, parseWaterBottleStyleIndex(-1))
    }

    @Test
    fun todayWaterIntakeResetsOnANewDayAndCanPassTheGoal() {
        val profile = UserProfile(
            waterGoalMl = 2000,
            waterIntakeMl = 1500,
            waterIntakeEpochDay = 10,
        )
        assertEquals(1500, profile.todayWaterIntakeMl(10))
        assertEquals(0, profile.todayWaterIntakeMl(11))
        assertEquals(
            2500,
            profile.copy(waterIntakeMl = 2500).todayWaterIntakeMl(10),
        )
        assertEquals(
            7000,
            profile.copy(waterIntakeMl = 8000).todayWaterIntakeMl(10),
        )
    }

    @Test
    fun waterAddThenUndoReversesOnlyTheLastAmount() {
        val start = UserProfile(waterGoalMl = 2000, waterIntakeEpochDay = 10)
        val afterFirst = start.withWaterAdd(250, 10)!!
        val afterSecond = start.copy(
            waterIntakeMl = afterFirst.millilitres,
            waterAddLog = encodeWaterAddLog(afterFirst.addLog),
        ).withWaterAdd(500, 10)!!
        assertEquals(750, afterSecond.millilitres)
        assertEquals(listOf(250, 500), afterSecond.addLog)
        val undone = start.copy(
            waterIntakeMl = afterSecond.millilitres,
            waterAddLog = encodeWaterAddLog(afterSecond.addLog),
        ).withWaterUndo(10)!!
        assertEquals(250, undone.millilitres)
        assertEquals(listOf(250), undone.addLog)
        assertFalse(start.canUndoWater(10))
        assertTrue(
            start.copy(
                waterIntakeMl = 250,
                waterAddLog = "250",
                waterIntakeEpochDay = 10,
            ).canUndoWater(10),
        )
        assertFalse(
            start.copy(
                waterIntakeMl = 250,
                waterAddLog = "250",
                waterIntakeEpochDay = 10,
            ).canUndoWater(11),
        )
    }

    @Test
    fun parseWaterGoalReadsLitersInRange() {
        assertEquals(2000, parseWaterGoalMl("2"))
        assertEquals(2500, parseWaterGoalMl("2.5"))
        assertEquals(2250, parseWaterGoalMl("2.25 L"))
        assertEquals(250, parseWaterGoalMl("0.25"))
        assertEquals(7000, parseWaterGoalMl("7"))
        assertEquals(null, parseWaterGoalMl(""))
        assertEquals(null, parseWaterGoalMl("0.2"))
        assertEquals(null, parseWaterGoalMl("7.01"))
        assertEquals(null, parseWaterGoalMl("8"))
        assertEquals("2.55", filterWaterGoalInput("2.555"))
        assertEquals("2.5 L", formatWaterLiters(2500))
        assertEquals("2.25 L", formatWaterLiters(2250))
        assertEquals("2 L", formatWaterLiters(2000))
        assertEquals("1.25 L of 2.5 L", "${formatWaterLiters(1250)} of ${formatWaterLiters(2500)}")
    }

    @Test
    fun waterAddCanPassTheGoalAndStopsAtSevenLiters() {
        val start = UserProfile(waterGoalMl = 2000, waterIntakeEpochDay = 10, waterIntakeMl = 1750)
        val overGoal = start.withWaterAdd(500, 10)!!
        assertEquals(2250, overGoal.millilitres)
        val atCap = start.copy(
            waterIntakeMl = 6800,
            waterAddLog = "6800",
        ).withWaterAdd(500, 10)!!
        assertEquals(7000, atCap.millilitres)
        assertEquals(200, atCap.addLog.last())
        assertEquals(null, start.copy(waterIntakeMl = 7000, waterAddLog = "7000").withWaterAdd(250, 10))
    }

    @Test
    fun keepDataOnUninstallStaysOnByDefault() {
        assertTrue(UserProfile().keepDataOnUninstall)
    }

    @Test
    fun remindersFeatureReadsStoredFlag() {
        assertFalse(UserProfile(remindersEnabled = false).remindersFeatureEnabled())
        assertTrue(UserProfile(remindersEnabled = true).remindersFeatureEnabled())
    }

    @Test
    fun drinkRemindersStayOffUntilTurnedOnAndNeedH2o() {
        assertFalse(UserProfile().waterRemindersEnabled)
        assertFalse(UserProfile().waterDrinkRemindersOn())
        assertFalse(
            UserProfile(waterEnabled = true, waterRemindersEnabled = false).waterDrinkRemindersOn(),
        )
        assertFalse(
            UserProfile(waterEnabled = false, waterRemindersEnabled = true).waterDrinkRemindersOn(),
        )
        assertTrue(
            UserProfile(waterEnabled = true, waterRemindersEnabled = true).waterDrinkRemindersOn(),
        )
    }

    @Test
    fun waterActiveHoursAlwaysApply() {
        val profile = UserProfile(
            waterActiveHoursEnabled = false,
            waterActiveHoursStartMinutes = 8 * 60,
            waterActiveHoursEndMinutes = 23 * 60,
        )
        val awake = profile.waterActiveHoursOrNull()
        assertEquals(java.time.LocalTime.of(8, 0), awake.startTime)
        assertEquals(java.time.LocalTime.of(23, 0), awake.endTime)
    }

    @Test
    fun customWaterAmountsSortBetweenBuiltins() {
        assertEquals(null, parseCustomWaterMl("9"))
        assertEquals(10, parseCustomWaterMl("10"))
        assertEquals(999, parseCustomWaterMl("999"))
        assertEquals(null, parseCustomWaterMl("1000"))
        assertEquals(
            listOf(250, 330, 500, 1000),
            waterQuickAddAmountsMl(listOf(330)),
        )
        assertEquals(
            listOf(100, 250, 500, 750, 1000),
            waterQuickAddAmountsMl(listOf(750, 100, 750)),
        )
        assertEquals("100,330", encodeWaterCustomQuickAdds(listOf(330, 100, 330)))
        assertEquals(listOf(100, 330), decodeWaterCustomQuickAdds("330,100,abc,9,1000"))
    }

    @Test
    fun customWaterButtonsCapAtThree() {
        val full = UserProfile(waterCustomQuickAddsMl = "100,200,300")
        assertEquals(listOf(100, 200, 300), full.waterCustomQuickAdds())
        assertEquals(null, full.withWaterCustomQuickAdd(400))
        assertEquals(listOf(100, 200), full.withoutWaterCustomQuickAdd(300))
        assertEquals(
            "100,200,300",
            encodeWaterCustomQuickAdds(listOf(100, 200, 300, 400)),
        )
    }

    @Test
    fun customWaterRejectsAmountsThatAlreadyHaveButtons() {
        assertEquals(true, isExistingWaterQuickAddMl(250, emptyList()))
        assertEquals(true, isExistingWaterQuickAddMl(500, emptyList()))
        assertEquals(true, isExistingWaterQuickAddMl(1000, emptyList()))
        assertEquals(true, isExistingWaterQuickAddMl(330, listOf(330)))
        assertEquals(false, isExistingWaterQuickAddMl(330, emptyList()))
        assertEquals(null, UserProfile().withWaterCustomQuickAdd(250))
        assertEquals(null, UserProfile(waterCustomQuickAddsMl = "330").withWaterCustomQuickAdd(330))
        assertEquals(listOf(330), UserProfile().withWaterCustomQuickAdd(330))
    }
}
