package com.deepak.flow.core.history

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class HistoryGraphPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
}

data class HistoryGraphWindow(
    val fromEpochDay: Long,
    val toEpochDay: Long,
    val title: String,
)

data class HistoryBounds(
    val earliestEpochDay: Long? = null,
    val latestEpochDay: Long? = null,
)

object HistoryGraphLogic {
    private val dayLabel = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    private val dayTitle = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val weekTitle = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val monthLabel = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
    private val monthTitle = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())

    fun windowForMonth(
        period: HistoryGraphPeriod,
        yearMonth: YearMonth,
    ): HistoryGraphWindow {
        val monthStart = yearMonth.atDay(1)
        val monthEnd = yearMonth.atEndOfMonth()
        return when (period) {
            HistoryGraphPeriod.DAILY -> HistoryGraphWindow(
                fromEpochDay = monthStart.toEpochDay(),
                toEpochDay = monthEnd.toEpochDay(),
                title = monthStart.format(monthTitle),
            )
            HistoryGraphPeriod.WEEKLY -> {
                val start = monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = monthEnd.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                HistoryGraphWindow(
                    fromEpochDay = start.toEpochDay(),
                    toEpochDay = end.toEpochDay(),
                    title = monthStart.format(monthTitle),
                )
            }
            HistoryGraphPeriod.MONTHLY -> HistoryGraphWindow(
                fromEpochDay = monthStart.toEpochDay(),
                toEpochDay = monthEnd.toEpochDay(),
                title = monthStart.format(monthTitle),
            )
        }
    }

    fun window(
        period: HistoryGraphPeriod,
        anchorEnd: LocalDate,
        offsetStepsBack: Int,
    ): HistoryGraphWindow {
        val clampedOffset = offsetStepsBack.coerceAtLeast(0)
        return when (period) {
            HistoryGraphPeriod.DAILY -> {
                val end = anchorEnd.minusWeeks(clampedOffset.toLong())
                val start = end.minusDays(6)
                HistoryGraphWindow(
                    fromEpochDay = start.toEpochDay(),
                    toEpochDay = end.toEpochDay(),
                    title = "${start.format(weekTitle)} - ${end.format(weekTitle)}",
                )
            }
            HistoryGraphPeriod.WEEKLY -> {
                val endWeekStart = anchorEnd
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .minusWeeks((clampedOffset * 8).toLong())
                val start = endWeekStart.minusWeeks(7)
                val end = endWeekStart.plusDays(6)
                HistoryGraphWindow(
                    fromEpochDay = start.toEpochDay(),
                    toEpochDay = end.toEpochDay(),
                    title = "${start.format(weekTitle)} - ${end.format(weekTitle)}",
                )
            }
            HistoryGraphPeriod.MONTHLY -> {
                val endMonth = YearMonth.from(anchorEnd).minusMonths((clampedOffset * 6).toLong())
                val startMonth = endMonth.minusMonths(5)
                HistoryGraphWindow(
                    fromEpochDay = startMonth.atDay(1).toEpochDay(),
                    toEpochDay = endMonth.atEndOfMonth().toEpochDay(),
                    title = "${startMonth.format(monthTitle)} - ${endMonth.format(monthTitle)}",
                )
            }
        }
    }

    fun bucket(
        period: HistoryGraphPeriod,
        days: List<HistoryDaySummary>,
        fromEpochDay: Long,
        toEpochDay: Long,
    ): List<HistorySeriesPoint> {
        val byDay = days.associateBy { it.dateEpochDay }
        return when (period) {
            HistoryGraphPeriod.DAILY -> {
                (fromEpochDay..toEpochDay).map { epochDay ->
                    val summary = byDay[epochDay]
                    val date = LocalDate.ofEpochDay(epochDay)
                    HistorySeriesPoint(
                        startEpochDay = epochDay,
                        endEpochDay = epochDay,
                        label = date.format(dayLabel),
                        taskCount = summary?.taskCount ?: 0,
                        waterIntakeMl = summary?.waterIntakeMl ?: 0,
                    )
                }
            }
            HistoryGraphPeriod.WEEKLY -> {
                var cursor = LocalDate.ofEpochDay(fromEpochDay)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = LocalDate.ofEpochDay(toEpochDay)
                buildList {
                    while (!cursor.isAfter(end)) {
                        val weekEnd = cursor.plusDays(6)
                        val range = cursor.toEpochDay()..minOf(weekEnd.toEpochDay(), toEpochDay)
                        val slice = range.mapNotNull { byDay[it] }
                        add(
                            HistorySeriesPoint(
                                startEpochDay = cursor.toEpochDay(),
                                endEpochDay = weekEnd.toEpochDay(),
                                label = cursor.format(dayTitle),
                                taskCount = slice.sumOf { it.taskCount },
                                waterIntakeMl = slice.sumOf { it.waterIntakeMl },
                            ),
                        )
                        cursor = cursor.plusWeeks(1)
                    }
                }
            }
            HistoryGraphPeriod.MONTHLY -> {
                var month = YearMonth.from(LocalDate.ofEpochDay(fromEpochDay))
                val endMonth = YearMonth.from(LocalDate.ofEpochDay(toEpochDay))
                buildList {
                    while (!month.isAfter(endMonth)) {
                        val start = month.atDay(1).toEpochDay()
                        val end = month.atEndOfMonth().toEpochDay()
                        val slice = (start..end).mapNotNull { byDay[it] }
                        add(
                            HistorySeriesPoint(
                                startEpochDay = start,
                                endEpochDay = end,
                                label = month.format(monthLabel),
                                taskCount = slice.sumOf { it.taskCount },
                                waterIntakeMl = slice.sumOf { it.waterIntakeMl },
                            ),
                        )
                        month = month.plusMonths(1)
                    }
                }
            }
        }
    }
}
