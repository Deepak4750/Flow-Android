package com.deepak.flow.core.repository

import com.deepak.flow.core.database.ReminderDao
import com.deepak.flow.core.database.ReminderEntity
import com.deepak.flow.core.model.ActiveHours
import com.deepak.flow.core.model.Reminder
import com.deepak.flow.core.model.Schedule
import com.deepak.flow.core.notification.NotificationScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate

class ReminderRepositoryImpl(
    private val dao: ReminderDao,
    private val notificationScheduler: NotificationScheduler,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    },
) : ReminderRepository {

    override fun observeReminders(): Flow<List<Reminder>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain(json) } }

    override suspend fun getReminder(id: Long): Reminder? =
        dao.getById(id)?.toDomain(json)

    override suspend fun insertReminder(reminder: Reminder): Long {
        val id = dao.insert(reminder.toEntity(json))
        val saved = reminder.copy(id = id)
        if (saved.enabled) {
            notificationScheduler.scheduleNextOccurrence(saved)
        }
        return id
    }

    override suspend fun updateReminder(reminder: Reminder) {
        dao.update(reminder.toEntity(json))
        notificationScheduler.cancelReminder(reminder.id)
        if (reminder.enabled) {
            notificationScheduler.scheduleNextOccurrence(reminder)
        }
    }

    override suspend fun deleteReminder(id: Long) {
        notificationScheduler.cancelReminder(id)
        dao.deleteById(id)
    }

    override suspend fun deleteAllReminders() {
        // Alarms are keyed by reminder id, so every pending one must be cancelled
        // before the rows disappear.
        dao.getAllIds().forEach { notificationScheduler.cancelReminder(it) }
        dao.deleteAll()
    }

    override suspend fun setReminderEnabled(id: Long, enabled: Boolean) {
        val existing = dao.getById(id) ?: return
        val updated = existing.copy(enabled = enabled)
        dao.update(updated)
        if (enabled) {
            notificationScheduler.scheduleNextOccurrence(updated.toDomain(json))
        } else {
            notificationScheduler.cancelReminder(id)
        }
    }

    override suspend fun rescheduleAllEnabledReminders() {
        notificationScheduler.cancelAll()
        dao.getEnabled().forEach { entity ->
            notificationScheduler.scheduleNextOccurrence(entity.toDomain(json))
        }
    }
}

private fun ReminderEntity.toDomain(json: Json): Reminder = Reminder(
    id = id,
    title = title,
    category = category,
    customCategoryName = customCategoryName,
    schedule = json.decodeFromString(Schedule.serializer(), scheduleJson),
    reminderTimes = json.decodeFromString(ListSerializer(com.deepak.flow.core.model.LocalTimeSerializer), reminderTimesJson),
    startDate = LocalDate.ofEpochDay(startDateEpochDay),
    endDate = endDateEpochDay?.let { LocalDate.ofEpochDay(it) },
    enabled = enabled,
    activeHours = activeHoursJson?.let { json.decodeFromString(ActiveHours.serializer(), it) },
    reason = reason,
    note = note,
)

private fun Reminder.toEntity(json: Json): ReminderEntity = ReminderEntity(
    id = id,
    title = title,
    category = category,
    customCategoryName = customCategoryName,
    scheduleJson = json.encodeToString(Schedule.serializer(), schedule),
    reminderTimesJson = json.encodeToString(ListSerializer(com.deepak.flow.core.model.LocalTimeSerializer), reminderTimes),
    startDateEpochDay = startDate.toEpochDay(),
    endDateEpochDay = endDate?.toEpochDay(),
    enabled = enabled,
    activeHoursJson = activeHours?.let { json.encodeToString(ActiveHours.serializer(), it) },
    reason = reason,
    note = note,
)
