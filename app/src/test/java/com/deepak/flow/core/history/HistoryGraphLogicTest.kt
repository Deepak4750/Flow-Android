package com.deepak.flow.core.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HistoryGraphLogicTest {

    @Test
    fun windowForMonth_dailyUsesWholeMonth() {
        val window = HistoryGraphLogic.windowForMonth(
            period = HistoryGraphPeriod.DAILY,
            yearMonth = java.time.YearMonth.of(2026, 8),
        )
        assertEquals(java.time.LocalDate.of(2026, 8, 1).toEpochDay(), window.fromEpochDay)
        assertEquals(java.time.LocalDate.of(2026, 8, 31).toEpochDay(), window.toEpochDay)
        assertEquals("Aug 2026", window.title)
    }

    @Test
    fun dailyWindow_isSevenDaysEndingOnAnchor() {
        val window = HistoryGraphLogic.window(
            period = HistoryGraphPeriod.DAILY,
            anchorEnd = LocalDate.of(2026, 8, 25),
            offsetStepsBack = 0,
        )
        assertEquals(LocalDate.of(2026, 8, 19).toEpochDay(), window.fromEpochDay)
        assertEquals(LocalDate.of(2026, 8, 25).toEpochDay(), window.toEpochDay)
    }

    @Test
    fun dailyBucket_fillsMissingDaysWithZeros() {
        val from = LocalDate.of(2026, 8, 19).toEpochDay()
        val to = LocalDate.of(2026, 8, 25).toEpochDay()
        val points = HistoryGraphLogic.bucket(
            period = HistoryGraphPeriod.DAILY,
            days = listOf(
                HistoryDaySummary(
                    dateEpochDay = LocalDate.of(2026, 8, 20).toEpochDay(),
                    taskCount = 2,
                    waterIntakeMl = 500,
                ),
            ),
            fromEpochDay = from,
            toEpochDay = to,
        )
        assertEquals(7, points.size)
        assertEquals(2, points[1].taskCount)
        assertEquals(0, points[0].taskCount)
        assertTrue(points.all { it.label.isNotBlank() })
    }

    @Test
    fun weeklyBucket_sumsDaysInWeek() {
        val monday = LocalDate.of(2026, 8, 17)
        val from = monday.toEpochDay()
        val to = monday.plusDays(13).toEpochDay()
        val points = HistoryGraphLogic.bucket(
            period = HistoryGraphPeriod.WEEKLY,
            days = listOf(
                HistoryDaySummary(monday.toEpochDay(), 1, 250),
                HistoryDaySummary(monday.plusDays(1).toEpochDay(), 2, 250),
                HistoryDaySummary(monday.plusWeeks(1).toEpochDay(), 4, 1000),
            ),
            fromEpochDay = from,
            toEpochDay = to,
        )
        assertEquals(2, points.size)
        assertEquals(3, points[0].taskCount)
        assertEquals(500, points[0].waterIntakeMl)
        assertEquals(4, points[1].taskCount)
    }
}
