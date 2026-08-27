package com.deepak.flow.feature.history.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.history.HistoryCalendarLogic
import com.deepak.flow.core.history.HistoryCompletionDotLevel
import com.deepak.flow.core.history.HistoryGraphLogic
import com.deepak.flow.core.history.HistoryGraphPeriod
import com.deepak.flow.core.history.HistorySeriesPoint
import com.deepak.flow.core.model.formatWaterLiters
import com.deepak.flow.core.scheduling.SchedulingEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class HistoryMainMode {
    CALENDAR,
    GRAPHS,
}

data class HistoryCalendarUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val activityDays: Set<Long> = emptySet(),
    val completionDots: Map<Long, HistoryCompletionDotLevel> = emptyMap(),
    val todayEpochDay: Long = LocalDate.now().toEpochDay(),
    val earliestEpochDay: Long? = null,
    val latestEpochDay: Long? = null,
    val canGoPreviousMonth: Boolean = false,
    val canGoNextMonth: Boolean = false,
)

data class HistoryGraphUiState(
    val period: HistoryGraphPeriod = HistoryGraphPeriod.DAILY,
    val yearMonth: YearMonth = YearMonth.now(),
    val windowTitle: String = "",
    val points: List<HistorySeriesPoint> = emptyList(),
    val canGoForward: Boolean = false,
    val canGoBack: Boolean = false,
)

data class HistoryUiState(
    val mode: HistoryMainMode = HistoryMainMode.CALENDAR,
    val calendar: HistoryCalendarUiState = HistoryCalendarUiState(),
    val graphs: HistoryGraphUiState = HistoryGraphUiState(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val historyRepository = (application as FlowApplication).historyRepository
    private val reminderRepository = (application as FlowApplication).reminderRepository
    private val profileRepository = (application as FlowApplication).profileRepository
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val schedulingEngine = SchedulingEngine()

    private val mode = MutableStateFlow(HistoryMainMode.CALENDAR)
    private val yearMonth = MutableStateFlow(YearMonth.now(zoneId))
    private val graphPeriod = MutableStateFlow(HistoryGraphPeriod.DAILY)

    private val boundsState = historyRepository.observeHistoryBounds(zoneId)

    private val calendarState = combine(
        yearMonth,
        boundsState,
    ) { month, bounds ->
        month to bounds
    }.flatMapLatest { (month, bounds) ->
        val earliest = bounds.earliestEpochDay
        val latest = bounds.latestEpochDay
        combine(
            historyRepository.observeActivityDays(month, zoneId),
            historyRepository.observeDaySeries(
                fromEpochDay = month.atDay(1).toEpochDay(),
                toEpochDay = month.atEndOfMonth().toEpochDay(),
                zoneId = zoneId,
            ),
            reminderRepository.observeReminders(),
            profileRepository.observeProfile(),
        ) { activityDays, daySummaries, reminders, profile ->
            val tasksEnabled = profile?.remindersEnabled != false
            val waterEnabled = profile?.waterEnabled != false
            val waterGoal = profile?.waterGoalMl
            val summaryByDay = daySummaries.associateBy { it.dateEpochDay }
            val completionDots = buildMap {
                var day = month.atDay(1)
                val end = month.atEndOfMonth()
                while (!day.isAfter(end)) {
                    val epochDay = day.toEpochDay()
                    val summary = summaryByDay[epochDay]
                    val scheduled = if (tasksEnabled) {
                        reminders.count { reminder ->
                            reminder.enabled &&
                                schedulingEngine.isScheduledOnDate(reminder, day, zoneId)
                        }
                    } else {
                        0
                    }
                    val completed = summary?.taskCount ?: 0
                    val waterMl = summary?.waterIntakeMl ?: 0
                    val percent = HistoryCalendarLogic.combinedCompletionPercent(
                        tasksEnabled = tasksEnabled,
                        waterEnabled = waterEnabled,
                        scheduledTasks = scheduled,
                        completedTasks = completed,
                        waterIntakeMl = waterMl,
                        waterGoalMl = waterGoal,
                    )
                    put(epochDay, HistoryCalendarLogic.dotLevel(percent))
                    day = day.plusDays(1)
                }
            }
            val today = LocalDate.now(zoneId)
            val todayMonth = YearMonth.from(today)
            val earliestMonth = earliest?.let { YearMonth.from(LocalDate.ofEpochDay(it)) }
            val latestMonth = latest?.let { YearMonth.from(LocalDate.ofEpochDay(it)) }
            val maxMonth = when {
                latestMonth == null -> todayMonth
                latestMonth.isBefore(todayMonth) -> todayMonth
                else -> latestMonth
            }
            HistoryCalendarUiState(
                yearMonth = month,
                activityDays = activityDays,
                completionDots = completionDots,
                todayEpochDay = today.toEpochDay(),
                earliestEpochDay = earliest,
                latestEpochDay = latest,
                canGoPreviousMonth = earliestMonth != null && month.isAfter(earliestMonth),
                canGoNextMonth = month.isBefore(maxMonth),
            )
        }
    }

    private val graphState = combine(
        yearMonth,
        graphPeriod,
        boundsState,
    ) { month, period, bounds ->
        Triple(month, period, bounds)
    }.flatMapLatest { (month, period, bounds) ->
        val window = HistoryGraphLogic.windowForMonth(period, month)
        val earliest = bounds.earliestEpochDay
        val latest = bounds.latestEpochDay
        historyRepository.observeDaySeries(
            fromEpochDay = window.fromEpochDay,
            toEpochDay = window.toEpochDay,
            zoneId = zoneId,
        ).map { days ->
            val today = LocalDate.now(zoneId)
            val todayMonth = YearMonth.from(today)
            val earliestMonth = earliest?.let { YearMonth.from(LocalDate.ofEpochDay(it)) }
            val latestMonth = latest?.let { YearMonth.from(LocalDate.ofEpochDay(it)) }
            val maxMonth = when {
                latestMonth == null -> todayMonth
                latestMonth.isBefore(todayMonth) -> todayMonth
                else -> latestMonth
            }
            HistoryGraphUiState(
                period = period,
                yearMonth = month,
                windowTitle = window.title,
                points = HistoryGraphLogic.bucket(
                    period = period,
                    days = days,
                    fromEpochDay = window.fromEpochDay,
                    toEpochDay = window.toEpochDay,
                ),
                canGoForward = month.isBefore(maxMonth),
                canGoBack = earliestMonth != null && month.isAfter(earliestMonth),
            )
        }
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        mode,
        calendarState,
        graphState,
    ) { selectedMode, calendar, graphs ->
        HistoryUiState(
            mode = selectedMode,
            calendar = calendar,
            graphs = graphs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    fun setMode(value: HistoryMainMode) {
        mode.value = value
    }

    fun setGraphPeriod(period: HistoryGraphPeriod) {
        graphPeriod.value = period
    }

    fun goToPreviousMonth() {
        if (uiState.value.calendar.canGoPreviousMonth) {
            yearMonth.value = yearMonth.value.minusMonths(1)
        }
    }

    fun goToNextMonth() {
        if (uiState.value.calendar.canGoNextMonth) {
            yearMonth.value = yearMonth.value.plusMonths(1)
        }
    }

    fun goToPreviousGraphWindow() = goToPreviousMonth()

    fun goToNextGraphWindow() = goToNextMonth()
}

fun HistorySeriesPoint.waterBarValue(): Float = waterIntakeMl.toFloat()

fun HistorySeriesPoint.tasksBarValue(): Float = taskCount.toFloat()

fun formatHistoryGraphWaterTotal(points: List<HistorySeriesPoint>): String {
    val total = points.sumOf { it.waterIntakeMl }
    return if (total > 0) formatWaterLiters(total) else "0 L"
}

fun formatHistoryGraphTaskTotal(points: List<HistorySeriesPoint>): String {
    val total = points.sumOf { it.taskCount }
    return when (total) {
        0 -> "0"
        1 -> "1 task"
        else -> "$total tasks"
    }
}

fun formatHistoryGraphWaterPoint(point: HistorySeriesPoint): String =
    if (point.waterIntakeMl > 0) formatWaterLiters(point.waterIntakeMl) else "0 L"

fun formatHistoryGraphTaskPoint(point: HistorySeriesPoint): String = when (point.taskCount) {
    0 -> "0 tasks"
    1 -> "1 task"
    else -> "${point.taskCount} tasks"
}
