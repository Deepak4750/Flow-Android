package com.deepak.flow.core.repository

import com.deepak.flow.core.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeReminders(): Flow<List<Reminder>>
    suspend fun getReminder(id: Long): Reminder?
    suspend fun insertReminder(reminder: Reminder): Long
    suspend fun updateReminder(reminder: Reminder)
    suspend fun deleteReminder(id: Long)
    suspend fun deleteAllReminders()
    suspend fun setReminderEnabled(id: Long, enabled: Boolean)
    suspend fun rescheduleAllEnabledReminders()
    suspend fun cancelAllScheduledReminders()
    fun observeTodayCompletions(dateEpochDay: Long): Flow<Set<Long>>
    suspend fun setTodayCompletion(reminderId: Long, dateEpochDay: Long, completed: Boolean)
    suspend fun recordOccurrenceDelivery(reminderId: Long, scheduledAtEpochMilli: Long): Reminder?
}
