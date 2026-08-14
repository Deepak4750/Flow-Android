package com.deepak.flow.feature.home.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.model.Reminder
import com.deepak.flow.core.repository.ProfileRepository
import com.deepak.flow.core.repository.ReminderRepository
import com.deepak.flow.core.scheduling.SchedulingEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class HomeUiState(
    val reminders: List<Reminder> = emptyList(),
    val greeting: String = greetingForTime(LocalTime.now()),
    val userLabel: String? = null,
    val profileName: String? = null,
    val nextReminder: Reminder? = null,
    val nextReminderInstant: Instant? = null,
)

fun greetingForTime(time: LocalTime, nickname: String? = null): String {
    val base = when (time.hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    return if (!nickname.isNullOrBlank()) "$base, $nickname" else base
}

class HomeViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository: ReminderRepository =
        (application as FlowApplication).reminderRepository

    private val profileRepository: ProfileRepository =
        (application as FlowApplication).profileRepository

    private val schedulingEngine = SchedulingEngine()
    private val zoneId: ZoneId = ZoneId.systemDefault()

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeReminders(),
        profileRepository.observeProfile(),
    ) { reminders, profile ->
        val nickname = profile?.nickname?.takeIf { it.isNotBlank() }
        val userLabel = if (nickname == null) {
            profile?.displayName?.takeIf { it.isNotBlank() }
        } else {
            null
        }
        val now = Instant.now()
        val enabled = reminders.filter { it.enabled }
        val nextPair = enabled
            .mapNotNull { reminder ->
                schedulingEngine.calculateNextOccurrence(reminder, now, zoneId)?.let { instant ->
                    reminder to instant
                }
            }
            .minByOrNull { it.second }

        HomeUiState(
            reminders = reminders,
            greeting = greetingForTime(LocalTime.now(), nickname),
            userLabel = userLabel,
            profileName = profile?.displayName?.takeIf { it.isNotBlank() } ?: nickname,
            nextReminder = nextPair?.first,
            nextReminderInstant = nextPair?.second,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminder(id)
        }
    }

    fun toggleReminderEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.setReminderEnabled(id, enabled)
        }
    }
}
