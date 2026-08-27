package com.deepak.flow.feature.history.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.history.HistoryDaySummary
import com.deepak.flow.core.history.HistoryTaskCompletion
import com.deepak.flow.core.history.HistoryWaterDay
import com.deepak.flow.core.model.formatWaterLiters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HistoryDayUiState(
    val dateEpochDay: Long,
    val dateLabel: String,
    val tasksSubtitle: String,
    val waterSubtitle: String,
)

data class HistoryTasksUiState(
    val dateLabel: String,
    val completions: List<HistoryTaskCompletionItem> = emptyList(),
)

data class HistoryTaskCompletionItem(
    val title: String,
    val timeLabel: String,
)

data class HistoryWaterUiState(
    val dateLabel: String,
    val hasIntake: Boolean = false,
    val totalLabel: String = "",
    val goalLabel: String? = null,
    val addAmounts: List<String> = emptyList(),
)

class HistoryDayViewModel(
    application: Application,
    private val dateEpochDay: Long,
) : AndroidViewModel(application) {

    private val historyRepository = (application as FlowApplication).historyRepository
    private val zoneId = ZoneId.systemDefault()
    private val dateLabel = formatHistoryDate(dateEpochDay, zoneId)

    val uiState: StateFlow<HistoryDayUiState> = historyRepository
        .observeDaySummary(dateEpochDay, zoneId)
        .map { summary -> summary.toDayUiState(dateLabel) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryDaySummary(
                dateEpochDay = dateEpochDay,
                taskCount = 0,
                waterIntakeMl = 0,
            ).toDayUiState(dateLabel),
        )
}

class HistoryTasksViewModel(
    application: Application,
    dateEpochDay: Long,
) : AndroidViewModel(application) {

    private val historyRepository = (application as FlowApplication).historyRepository
    private val zoneId = ZoneId.systemDefault()
    private val dateLabel = formatHistoryDate(dateEpochDay, zoneId)
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    val uiState: StateFlow<HistoryTasksUiState> = historyRepository
        .observeTaskCompletions(dateEpochDay)
        .map { completions ->
            HistoryTasksUiState(
                dateLabel = dateLabel,
                completions = completions.map { it.toItem(zoneId, timeFormatter) },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryTasksUiState(dateLabel = dateLabel),
        )
}

class HistoryWaterViewModel(
    application: Application,
    dateEpochDay: Long,
) : AndroidViewModel(application) {

    private val historyRepository = (application as FlowApplication).historyRepository
    private val zoneId = ZoneId.systemDefault()
    private val dateLabel = formatHistoryDate(dateEpochDay, zoneId)
    private val today = LocalDate.now(zoneId).toEpochDay()

    val uiState: StateFlow<HistoryWaterUiState> = historyRepository
        .observeWaterDay(dateEpochDay, today)
        .map { day -> day.toWaterUiState(dateLabel) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryWaterUiState(dateLabel = dateLabel),
        )
}

class HistoryDayViewModelFactory(
    private val application: FlowApplication,
    private val dateEpochDay: Long,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HistoryDayViewModel::class.java) ->
                HistoryDayViewModel(application, dateEpochDay) as T
            modelClass.isAssignableFrom(HistoryTasksViewModel::class.java) ->
                HistoryTasksViewModel(application, dateEpochDay) as T
            modelClass.isAssignableFrom(HistoryWaterViewModel::class.java) ->
                HistoryWaterViewModel(application, dateEpochDay) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}

private fun HistoryDaySummary.toDayUiState(dateLabel: String) = HistoryDayUiState(
    dateEpochDay = dateEpochDay,
    dateLabel = dateLabel,
    tasksSubtitle = when (taskCount) {
        0 -> "Nothing completed"
        1 -> "1 completed"
        else -> "$taskCount completed"
    },
    waterSubtitle = if (waterIntakeMl > 0) formatWaterLiters(waterIntakeMl) else "No water logged",
)

private fun HistoryTaskCompletion.toItem(
    zoneId: ZoneId,
    timeFormatter: DateTimeFormatter,
) = HistoryTaskCompletionItem(
    title = title,
    timeLabel = Instant.ofEpochMilli(completedAtEpochMilli)
        .atZone(zoneId)
        .toLocalTime()
        .format(timeFormatter),
)

private fun HistoryWaterDay?.toWaterUiState(dateLabel: String): HistoryWaterUiState {
    if (this == null || !hasIntake) {
        return HistoryWaterUiState(dateLabel = dateLabel)
    }
    return HistoryWaterUiState(
        dateLabel = dateLabel,
        hasIntake = true,
        totalLabel = formatWaterLiters(intakeMl),
        goalLabel = goalMl?.takeIf { it > 0 }?.let { "Goal ${formatWaterLiters(it)}" },
        addAmounts = addLog.map { "+${formatWaterLiters(it)}" },
    )
}

fun formatHistoryDate(dateEpochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
    val date = LocalDate.ofEpochDay(dateEpochDay)
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    return date.format(formatter)
}
