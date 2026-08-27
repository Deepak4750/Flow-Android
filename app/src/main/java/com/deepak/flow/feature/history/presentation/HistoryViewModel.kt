package com.deepak.flow.feature.history.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.history.HistoryGraphLogic
import com.deepak.flow.core.history.HistoryGraphPeriod
import com.deepak.flow.core.history.HistorySeriesPoint
import com.deepak.flow.core.model.formatWaterLiters
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
    val todayEpochDay: Long = LocalDate.now().toEpochDay(),
)

data class HistoryGraphUiState(
    val period: HistoryGraphPeriod = HistoryGraphPeriod.DAILY,
    val windowTitle: String = "",
    val points: List<HistorySeriesPoint> = emptyList(),
    val canGoForward: Boolean = false,
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
    private val zoneId: ZoneId = ZoneId.systemDefault()

    private val mode = MutableStateFlow(HistoryMainMode.CALENDAR)
    private val yearMonth = MutableStateFlow(YearMonth.now(zoneId))
    private val graphPeriod = MutableStateFlow(HistoryGraphPeriod.DAILY)
    private val graphOffset = MutableStateFlow(0)

    private val calendarState = yearMonth.flatMapLatest { month ->
        historyRepository.observeActivityDays(month, zoneId).map { days ->
            HistoryCalendarUiState(
                yearMonth = month,
                activityDays = days,
                todayEpochDay = LocalDate.now(zoneId).toEpochDay(),
            )
        }
    }

    private val graphState = combine(graphPeriod, graphOffset) { period, offset ->
        period to offset
    }.flatMapLatest { (period, offset) ->
        val window = HistoryGraphLogic.window(
            period = period,
            anchorEnd = LocalDate.now(zoneId),
            offsetStepsBack = offset,
        )
        historyRepository.observeDaySeries(
            fromEpochDay = window.fromEpochDay,
            toEpochDay = window.toEpochDay,
            zoneId = zoneId,
        ).map { days ->
            HistoryGraphUiState(
                period = period,
                windowTitle = window.title,
                points = HistoryGraphLogic.bucket(
                    period = period,
                    days = days,
                    fromEpochDay = window.fromEpochDay,
                    toEpochDay = window.toEpochDay,
                ),
                canGoForward = offset > 0,
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
        if (graphPeriod.value != period) {
            graphPeriod.value = period
            graphOffset.value = 0
        }
    }

    fun goToPreviousMonth() {
        yearMonth.value = yearMonth.value.minusMonths(1)
    }

    fun goToNextMonth() {
        yearMonth.value = yearMonth.value.plusMonths(1)
    }

    fun goToPreviousGraphWindow() {
        graphOffset.value = graphOffset.value + 1
    }

    fun goToNextGraphWindow() {
        if (graphOffset.value > 0) {
            graphOffset.value = graphOffset.value - 1
        }
    }
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
