package com.deepak.flow.core.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryCalendarLogicTest {

    @Test
    fun combinedCompletionPercent_averagesOnlyActiveFeatures() {
        val both = HistoryCalendarLogic.combinedCompletionPercent(
            tasksEnabled = true,
            waterEnabled = true,
            scheduledTasks = 10,
            completedTasks = 8,
            waterIntakeMl = 500,
            waterGoalMl = 1000,
        )
        assertEquals(65f, both!!, 0.01f)

        val waterOnly = HistoryCalendarLogic.combinedCompletionPercent(
            tasksEnabled = false,
            waterEnabled = true,
            scheduledTasks = 0,
            completedTasks = 0,
            waterIntakeMl = 700,
            waterGoalMl = 1000,
        )
        assertEquals(70f, waterOnly!!, 0.01f)

        val none = HistoryCalendarLogic.combinedCompletionPercent(
            tasksEnabled = false,
            waterEnabled = false,
            scheduledTasks = 0,
            completedTasks = 0,
            waterIntakeMl = 0,
            waterGoalMl = null,
        )
        assertNull(none)
    }

    @Test
    fun dotLevel_usesOptionAThresholds() {
        assertEquals(HistoryCompletionDotLevel.RED, HistoryCalendarLogic.dotLevel(35f))
        assertEquals(HistoryCompletionDotLevel.RED, HistoryCalendarLogic.dotLevel(0f))
        assertEquals(HistoryCompletionDotLevel.YELLOW, HistoryCalendarLogic.dotLevel(35.01f))
        assertEquals(HistoryCompletionDotLevel.YELLOW, HistoryCalendarLogic.dotLevel(50f))
        assertEquals(HistoryCompletionDotLevel.BLUE, HistoryCalendarLogic.dotLevel(50.01f))
        assertEquals(HistoryCompletionDotLevel.BLUE, HistoryCalendarLogic.dotLevel(80f))
        assertEquals(HistoryCompletionDotLevel.GREEN, HistoryCalendarLogic.dotLevel(80.01f))
        assertEquals(HistoryCompletionDotLevel.GREEN, HistoryCalendarLogic.dotLevel(100f))
        assertEquals(HistoryCompletionDotLevel.NONE, HistoryCalendarLogic.dotLevel(null))
    }
}
