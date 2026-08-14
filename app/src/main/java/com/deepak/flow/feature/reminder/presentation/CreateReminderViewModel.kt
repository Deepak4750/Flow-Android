package com.deepak.flow.feature.reminder.presentation

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.model.ActiveHours
import com.deepak.flow.core.model.Category
import com.deepak.flow.core.model.Reminder
import com.deepak.flow.core.model.Schedule
import com.deepak.flow.core.repository.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class CreateReminderUiState(
    val isEditMode: Boolean = false,
    val editingReminderId: Long? = null,
    val task: String = "",
    val category: Category = Category.PERSONAL,
    val customCategoryName: String = "",
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val weeklyDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY),
    val monthlyDay: Int = 1,
    val everyXDays: Int = 3,
    val everyXHours: Int = 4,
    val reminderTime: LocalTime = LocalTime.of(19, 0),
    val intervalAnchorIsNow: Boolean = true,
    val showAdvanced: Boolean = false,
    val activeHoursEnabled: Boolean = false,
    val activeHoursStart: LocalTime = LocalTime.of(8, 0),
    val activeHoursEnd: LocalTime = LocalTime.of(23, 0),
    val startDate: LocalDate = LocalDate.now(),
    val endDateEnabled: Boolean = false,
    val endDate: LocalDate? = null,
    val reason: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val needsNotificationPermission: Boolean = false,
    val enabled: Boolean = true,
    val hasUnsavedChanges: Boolean = false,
) {
    val canSave: Boolean
        get() = task.isNotBlank() &&
            !isSaving &&
            !isLoading &&
            (category != Category.CUSTOM || customCategoryName.isNotBlank())

    val isIntervalSchedule: Boolean
        get() = scheduleType == ScheduleType.EVERY_X_DAYS || scheduleType == ScheduleType.EVERY_X_HOURS
}

enum class ScheduleType(val displayName: String) {
    DAILY("Every day"),
    WEEKLY("Every week"),
    MONTHLY("Every month"),
    EVERY_X_DAYS("Every few days"),
    EVERY_X_HOURS("Every few hours"),
}

class CreateReminderViewModel(
    application: Application,
    private val editReminderId: Long? = null,
) : AndroidViewModel(application) {

    private val repository: ReminderRepository =
        (application as FlowApplication).reminderRepository

    private val _uiState = MutableStateFlow(
        CreateReminderUiState(
            isEditMode = editReminderId != null,
            editingReminderId = editReminderId,
        ),
    )
    val uiState: StateFlow<CreateReminderUiState> = _uiState.asStateFlow()

    private var editBaseline: ReminderFormSnapshot? = null

    init {
        editReminderId?.let { loadReminder(it) }
    }

    private fun publish(state: CreateReminderUiState): CreateReminderUiState {
        val baseline = editBaseline
        val hasChanges = baseline != null && ReminderFormSnapshot.from(state) != baseline
        return state.copy(hasUnsavedChanges = hasChanges)
    }

    private fun updateState(transform: (CreateReminderUiState) -> CreateReminderUiState) {
        _uiState.update { current -> publish(transform(current)) }
    }

    private fun loadReminder(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val reminder = repository.getReminder(id) ?: return@launch
            val loaded = CreateReminderUiState(
                isEditMode = true,
                editingReminderId = id,
                task = reminder.title,
                category = reminder.category,
                customCategoryName = reminder.customCategoryName.orEmpty(),
                scheduleType = reminder.schedule.toScheduleType(),
                weeklyDays = (reminder.schedule as? Schedule.Weekly)?.daysOfWeek
                    ?: setOf(DayOfWeek.MONDAY),
                monthlyDay = (reminder.schedule as? Schedule.Monthly)?.dayOfMonth ?: 1,
                everyXDays = (reminder.schedule as? Schedule.EveryXDays)?.intervalDays ?: 3,
                everyXHours = (reminder.schedule as? Schedule.EveryXHours)?.intervalHours ?: 4,
                reminderTime = reminder.reminderTimes.firstOrNull() ?: LocalTime.of(19, 0),
                intervalAnchorIsNow = when (reminder.schedule) {
                    is Schedule.EveryXDays, is Schedule.EveryXHours -> false
                    else -> true
                },
                activeHoursEnabled = reminder.activeHours != null,
                activeHoursStart = reminder.activeHours?.startTime ?: LocalTime.of(8, 0),
                activeHoursEnd = reminder.activeHours?.endTime ?: LocalTime.of(23, 0),
                startDate = reminder.startDate,
                endDateEnabled = reminder.endDate != null,
                endDate = reminder.endDate,
                reason = reminder.reason.orEmpty(),
                note = reminder.note.orEmpty(),
                enabled = reminder.enabled,
            )
            editBaseline = ReminderFormSnapshot.from(loaded)
            _uiState.value = publish(loaded.copy(isLoading = false))
        }
    }

    fun updateTask(value: String) = updateState { it.copy(task = value) }
    fun updateCategory(value: Category) = updateState { it.copy(category = value) }
    fun updateCustomCategoryName(value: String) = updateState { it.copy(customCategoryName = value) }
    fun updateScheduleType(value: ScheduleType) = updateState { state ->
        val intervalAnchorIsNow = when (value) {
            ScheduleType.EVERY_X_DAYS, ScheduleType.EVERY_X_HOURS ->
                if (state.isEditMode) state.intervalAnchorIsNow else true
            else -> state.intervalAnchorIsNow
        }
        state.copy(scheduleType = value, intervalAnchorIsNow = intervalAnchorIsNow)
    }
    fun toggleWeeklyDay(day: DayOfWeek) = updateState { state ->
        val days = state.weeklyDays.toMutableSet()
        if (day in days && days.size > 1) days.remove(day) else days.add(day)
        state.copy(weeklyDays = days)
    }

    fun setWeeklyDay(day: DayOfWeek, selected: Boolean) = updateState { state ->
        val days = state.weeklyDays.toMutableSet()
        if (selected) {
            days.add(day)
        } else if (days.size > 1) {
            days.remove(day)
        }
        state.copy(weeklyDays = days)
    }
    fun updateMonthlyDay(value: Int) = updateState {
        it.copy(monthlyDay = value.coerceIn(1, 31))
    }
    fun updateMonthlyDayInput(input: String) {
        val digits = input.filter { it.isDigit() }.take(2)
        if (digits.isEmpty()) {
            updateState { it.copy(monthlyDay = 1) }
            return
        }
        val parsed = digits.toIntOrNull()?.coerceIn(1, 31) ?: 1
        updateState { it.copy(monthlyDay = parsed) }
    }
    fun incrementMonthlyDay() = updateMonthlyDay(_uiState.value.monthlyDay + 1)
    fun decrementMonthlyDay() = updateMonthlyDay(_uiState.value.monthlyDay - 1)
    fun updateReminderTime(value: LocalTime) = updateState { it.copy(reminderTime = value) }
    fun setIntervalStartNow() = updateState { it.copy(intervalAnchorIsNow = true) }
    fun setIntervalStartCustom() = updateState { state ->
        state.copy(
            intervalAnchorIsNow = false,
            startDate = if (state.isEditMode) state.startDate else LocalDate.now(),
        )
    }
    fun setIntervalCustomStart(time: LocalTime) = updateState { state ->
        state.copy(
            intervalAnchorIsNow = false,
            reminderTime = time,
            startDate = if (!state.isEditMode) LocalDate.now() else state.startDate,
        )
    }
    fun setIntervalCustomStartDate(date: LocalDate) = updateState { state ->
        state.copy(
            intervalAnchorIsNow = false,
            startDate = date,
        )
    }
    fun updateStartDate(date: LocalDate) = updateState { it.copy(startDate = date) }
    fun setEndDateEnabled(enabled: Boolean) = updateState { state ->
        state.copy(
            endDateEnabled = enabled,
            endDate = when {
                !enabled -> null
                state.endDate != null -> state.endDate
                else -> state.startDate.plusMonths(1)
            },
        )
    }
    fun updateEndDate(date: LocalDate) = updateState { it.copy(endDate = date, endDateEnabled = true) }
    fun toggleAdvanced() = updateState { it.copy(showAdvanced = !it.showAdvanced) }
    fun setActiveHoursEnabled(enabled: Boolean) = updateState { it.copy(activeHoursEnabled = enabled) }
    fun updateActiveHoursStart(value: LocalTime) = updateState { it.copy(activeHoursStart = value) }
    fun updateActiveHoursEnd(value: LocalTime) = updateState { it.copy(activeHoursEnd = value) }
    fun updateReason(value: String) = updateState { it.copy(reason = value) }
    fun updateNote(value: String) = updateState { it.copy(note = value) }

    fun updateEveryXDays(value: Int) = updateState {
        it.copy(everyXDays = value.coerceIn(INTERVAL_DAYS_MIN, INTERVAL_DAYS_MAX))
    }
    fun updateEveryXHours(value: Int) = updateState {
        it.copy(everyXHours = value.coerceIn(INTERVAL_HOURS_MIN, INTERVAL_HOURS_MAX))
    }
    fun updateEveryXDaysInput(input: String) {
        val digits = input.filter { it.isDigit() }.take(3)
        if (digits.isEmpty()) {
            updateState { it.copy(everyXDays = INTERVAL_DAYS_MIN) }
            return
        }
        val parsed = digits.toIntOrNull()?.coerceIn(INTERVAL_DAYS_MIN, INTERVAL_DAYS_MAX) ?: INTERVAL_DAYS_MIN
        updateState { it.copy(everyXDays = parsed) }
    }
    fun updateEveryXHoursInput(input: String) {
        val digits = input.filter { it.isDigit() }.take(3)
        if (digits.isEmpty()) {
            updateState { it.copy(everyXHours = INTERVAL_HOURS_MIN) }
            return
        }
        val parsed = digits.toIntOrNull()?.coerceIn(INTERVAL_HOURS_MIN, INTERVAL_HOURS_MAX) ?: INTERVAL_HOURS_MIN
        updateState { it.copy(everyXHours = parsed) }
    }
    fun incrementEveryXDays() = updateEveryXDays(_uiState.value.everyXDays + 1)
    fun decrementEveryXDays() = updateEveryXDays(_uiState.value.everyXDays - 1)
    fun incrementEveryXHours() = updateEveryXHours(_uiState.value.everyXHours + 1)
    fun decrementEveryXHours() = updateEveryXHours(_uiState.value.everyXHours - 1)

    fun checkNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val granted = ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            _uiState.update { it.copy(needsNotificationPermission = true) }
        }
        return granted
    }

    fun onNotificationPermissionHandled() {
        _uiState.update { it.copy(needsNotificationPermission = false) }
    }

    fun saveReminder(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val schedule = when (state.scheduleType) {
                ScheduleType.DAILY -> Schedule.Daily
                ScheduleType.WEEKLY -> Schedule.Weekly(state.weeklyDays)
                ScheduleType.MONTHLY -> Schedule.Monthly(state.monthlyDay)
                ScheduleType.EVERY_X_DAYS -> Schedule.EveryXDays(state.everyXDays)
                ScheduleType.EVERY_X_HOURS -> Schedule.EveryXHours(state.everyXHours)
            }
            val zoneId = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zoneId)
            val startDate = when (state.scheduleType) {
                ScheduleType.EVERY_X_DAYS, ScheduleType.EVERY_X_HOURS ->
                    if (state.intervalAnchorIsNow) now.toLocalDate() else state.startDate
                else -> state.startDate
            }
            val times = when (state.scheduleType) {
                ScheduleType.EVERY_X_DAYS, ScheduleType.EVERY_X_HOURS -> {
                    val anchorTime = if (state.intervalAnchorIsNow) {
                        now.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
                    } else {
                        state.reminderTime
                    }
                    listOf(anchorTime)
                }
                else -> listOf(state.reminderTime)
            }
            val reminder = Reminder(
                id = state.editingReminderId ?: 0L,
                title = state.task.trim(),
                category = state.category,
                customCategoryName = if (state.category == Category.CUSTOM) {
                    state.customCategoryName.trim()
                } else {
                    null
                },
                schedule = schedule,
                reminderTimes = times,
                startDate = startDate,
                endDate = if (state.endDateEnabled) state.endDate else null,
                enabled = state.enabled,
                activeHours = if (state.activeHoursEnabled) {
                    ActiveHours(state.activeHoursStart, state.activeHoursEnd)
                } else {
                    null
                },
                reason = state.reason.takeIf { it.isNotBlank() },
                note = state.note.takeIf { it.isNotBlank() },
            )
            if (state.isEditMode && state.editingReminderId != null) {
                repository.updateReminder(reminder)
            } else {
                repository.insertReminder(reminder)
            }
            _uiState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }

    companion object {
        const val INTERVAL_DAYS_MIN = 1
        const val INTERVAL_DAYS_MAX = 365
        const val INTERVAL_HOURS_MIN = 1
        const val INTERVAL_HOURS_MAX = 168
    }
}

private fun Schedule.toScheduleType(): ScheduleType = when (this) {
    Schedule.Daily -> ScheduleType.DAILY
    is Schedule.Weekly -> ScheduleType.WEEKLY
    is Schedule.Monthly -> ScheduleType.MONTHLY
    is Schedule.EveryXDays -> ScheduleType.EVERY_X_DAYS
    is Schedule.EveryXHours -> ScheduleType.EVERY_X_HOURS
}

private data class ReminderFormSnapshot(
    val task: String,
    val category: Category,
    val customCategoryName: String,
    val scheduleType: ScheduleType,
    val weeklyDays: Set<DayOfWeek>,
    val monthlyDay: Int,
    val everyXDays: Int,
    val everyXHours: Int,
    val reminderTime: LocalTime,
    val intervalAnchorIsNow: Boolean,
    val activeHoursEnabled: Boolean,
    val activeHoursStart: LocalTime,
    val activeHoursEnd: LocalTime,
    val startDate: LocalDate,
    val endDateEnabled: Boolean,
    val endDate: LocalDate?,
    val reason: String,
    val note: String,
    val enabled: Boolean,
) {
    companion object {
        fun from(state: CreateReminderUiState) = ReminderFormSnapshot(
            task = state.task.trim(),
            category = state.category,
            customCategoryName = state.customCategoryName.trim(),
            scheduleType = state.scheduleType,
            weeklyDays = state.weeklyDays,
            monthlyDay = state.monthlyDay,
            everyXDays = state.everyXDays,
            everyXHours = state.everyXHours,
            reminderTime = state.reminderTime,
            intervalAnchorIsNow = state.intervalAnchorIsNow,
            activeHoursEnabled = state.activeHoursEnabled,
            activeHoursStart = state.activeHoursStart,
            activeHoursEnd = state.activeHoursEnd,
            startDate = state.startDate,
            endDateEnabled = state.endDateEnabled,
            endDate = state.endDate,
            reason = state.reason.trim(),
            note = state.note.trim(),
            enabled = state.enabled,
        )
    }
}