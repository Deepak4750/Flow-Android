package com.deepak.flow.feature.history.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.model.Reminder
import com.deepak.flow.core.model.ReminderExpirationMode
import com.deepak.flow.core.model.Schedule
import com.deepak.flow.core.model.categoryLabel
import com.deepak.flow.core.model.effectiveExpirationMode
import com.deepak.flow.core.model.isExpiredOn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HistoryExpiredReminderUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val title: String = "",
    val categoryLabel: String = "",
    val scheduleLabel: String = "",
    val timeLabel: String = "",
    val expirationLabel: String = "",
    val reason: String? = null,
    val note: String? = null,
    val isExpired: Boolean = false,
)

class HistoryExpiredReminderViewModel(
    application: Application,
    private val reminderId: Long,
) : AndroidViewModel(application) {

    private val reminderRepository = (application as FlowApplication).reminderRepository
    private val zoneId = ZoneId.systemDefault()
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    val uiState: StateFlow<HistoryExpiredReminderUiState> = reminderRepository
        .observeReminders()
        .map { reminders ->
            val reminder = reminders.firstOrNull { it.id == reminderId }
            if (reminder == null) {
                HistoryExpiredReminderUiState(isLoading = false, notFound = true)
            } else {
                reminder.toExpiredUiState(dateFormatter, zoneId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryExpiredReminderUiState(),
        )
}

class HistoryExpiredReminderViewModelFactory(
    private val application: FlowApplication,
    private val reminderId: Long,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryExpiredReminderViewModel::class.java)) {
            return HistoryExpiredReminderViewModel(application, reminderId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}

private fun Reminder.toExpiredUiState(
    dateFormatter: DateTimeFormatter,
    zoneId: ZoneId,
): HistoryExpiredReminderUiState {
    val today = LocalDate.now(zoneId)
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    return HistoryExpiredReminderUiState(
        isLoading = false,
        title = title,
        categoryLabel = categoryLabel(),
        scheduleLabel = historyScheduleSummary(this),
        timeLabel = reminderTimes.joinToString(", ") { it.format(timeFormatter) },
        expirationLabel = expirationSummary(dateFormatter),
        reason = reason?.takeIf { it.isNotBlank() },
        note = note?.takeIf { it.isNotBlank() },
        isExpired = isExpiredOn(today),
    )
}

internal fun historyScheduleSummary(reminder: Reminder): String = when (val schedule = reminder.schedule) {
    Schedule.Daily -> "Every day"
    is Schedule.Weekly -> "Every week"
    is Schedule.Monthly -> "Every month"
    is Schedule.EveryXDays -> if (schedule.intervalDays == 1) {
        "Every day"
    } else {
        "Every ${schedule.intervalDays} days"
    }
    is Schedule.EveryXHours -> if (schedule.intervalHours == 1) {
        "Every hour"
    } else {
        "Every ${schedule.intervalHours} hours"
    }
}

private fun Reminder.expirationSummary(dateFormatter: DateTimeFormatter): String = when (effectiveExpirationMode()) {
    ReminderExpirationMode.NONE -> "No end"
    ReminderExpirationMode.END_DATE -> endDate?.let { "Ended ${it.format(dateFormatter)}" } ?: "Ended"
    ReminderExpirationMode.OCCURRENCE_LIMIT -> {
        val limit = occurrenceLimit ?: 0
        "Completed $occurrencesDelivered of $limit"
    }
}
