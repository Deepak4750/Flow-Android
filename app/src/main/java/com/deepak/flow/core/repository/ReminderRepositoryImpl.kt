package com.deepak.flow.core.repository

import com.deepak.flow.core.database.ReminderDao
import com.deepak.flow.core.database.ReminderCompletionDao
import com.deepak.flow.core.database.ReminderDayCompletionEntity
import com.deepak.flow.core.database.ReminderEntity
import com.deepak.flow.core.database.ReminderOccurrenceDao
import com.deepak.flow.core.database.ReminderOccurrenceDeliveryEntity
import com.deepak.flow.core.model.ActiveHours
import com.deepak.flow.core.model.Reminder
import com.deepak.flow.core.model.ReminderExpirationMode
import com.deepak.flow.core.model.effectiveExpirationMode
import com.deepak.flow.core.model.isExpiredOn
import com.deepak.flow.core.model.Schedule
import com.deepak.flow.core.notification.NotificationScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate

class ReminderRepositoryImpl(
    private val dao: ReminderDao,
    private val completionDao: ReminderCompletionDao,
    private val occurrenceDao: ReminderOccurrenceDao,
    private val notificationScheduler: NotificationScheduler,
    private val onDataChanged: () -> Unit = {},
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
        if (saved.enabled && !saved.isExpiredOn(LocalDate.now())) {
            notificationScheduler.scheduleNextOccurrence(saved)
        }
        onDataChanged()
        return id
    }

    override suspend fun updateReminder(reminder: Reminder) {
        dao.update(reminder.toEntity(json))
        notificationScheduler.cancelReminder(reminder.id)
        if (reminder.enabled && !reminder.isExpiredOn(LocalDate.now())) {
            notificationScheduler.scheduleNextOccurrence(reminder)
        }
        onDataChanged()
    }

    override suspend fun deleteReminder(id: Long) {
        notificationScheduler.cancelReminder(id)
        completionDao.deleteForReminder(id)
        dao.deleteById(id)
        onDataChanged()
    }

    override suspend fun deleteAllReminders() {
        cancelAllScheduledReminders()
        completionDao.deleteAll()
        dao.deleteAll()
        onDataChanged()
    }

    override suspend fun setReminderEnabled(id: Long, enabled: Boolean) {
        val existing = dao.getById(id) ?: return
        val updated = existing.copy(enabled = enabled)
        dao.update(updated)
        if (enabled && !updated.toDomain(json).isExpiredOn(LocalDate.now())) {
            notificationScheduler.scheduleNextOccurrence(updated.toDomain(json))
        } else {
            notificationScheduler.cancelReminder(id)
        }
        onDataChanged()
    }

    override suspend fun rescheduleAllEnabledReminders() {
        cancelAllScheduledReminders()
        val today = LocalDate.now()
        dao.getEnabled().forEach { entity ->
            val reminder = entity.toDomain(json)
            if (!reminder.isExpiredOn(today)) {
                notificationScheduler.scheduleNextOccurrence(reminder)
            }
        }
    }

    override suspend fun cancelAllScheduledReminders() {
        dao.getAllIds().forEach { notificationScheduler.cancelReminder(it) }
    }

    override fun observeTodayCompletions(dateEpochDay: Long): Flow<Set<Long>> =
        completionDao.observeCompletedIdsForDate(dateEpochDay).map { it.toSet() }

    override suspend fun setTodayCompletion(reminderId: Long, dateEpochDay: Long, completed: Boolean) {
        if (completed) {
            completionDao.insert(
                ReminderDayCompletionEntity(
                    reminderId = reminderId,
                    dateEpochDay = dateEpochDay,
                    completedAtEpochMilli = System.currentTimeMillis(),
                ),
            )
        } else {
            completionDao.deleteForReminderOnDate(reminderId, dateEpochDay)
        }
        onDataChanged()
    }

    override suspend fun recordOccurrenceDelivery(
        reminderId: Long,
        scheduledAtEpochMilli: Long,
    ): Reminder? {
        val rowId = occurrenceDao.insert(
            ReminderOccurrenceDeliveryEntity(
                reminderId = reminderId,
                scheduledAtEpochMilli = scheduledAtEpochMilli,
            ),
        )
        if (rowId == -1L) return null

        val entity = dao.getById(reminderId) ?: return null
        val updatedEntity = entity.copy(occurrencesDelivered = entity.occurrencesDelivered + 1)
        dao.update(updatedEntity)
        val reminder = updatedEntity.toDomain(json)
        if (reminder.isExpiredOn(LocalDate.now())) {
            notificationScheduler.cancelReminder(reminderId)
        }
        onDataChanged()
        return reminder
    }
}

private fun ReminderEntity.toDomain(json: Json): Reminder = Reminder(
    id = id,
    title = title,
    category = category,
    customCategoryName = customCategoryName,
    schedule = json.decodeFromString(Schedule.serializer(), scheduleJson),
    reminderTimes = json.decodeFromString(
        ListSerializer(com.deepak.flow.core.model.LocalTimeSerializer),
        reminderTimesJson,
    ),
    startDate = LocalDate.ofEpochDay(startDateEpochDay),
    expirationMode = runCatching {
        ReminderExpirationMode.valueOf(expirationMode)
    }.getOrDefault(ReminderExpirationMode.NONE),
    endDate = endDateEpochDay?.let { LocalDate.ofEpochDay(it) },
    occurrenceLimit = occurrenceLimit,
    occurrencesDelivered = occurrencesDelivered,
    enabled = enabled,
    activeHours = activeHoursJson?.let { json.decodeFromString(ActiveHours.serializer(), it) },
    reason = reason,
    note = note,
    accentColorIndex = accentColorIndex,
)

private fun Reminder.toEntity(json: Json): ReminderEntity {
    val mode = effectiveExpirationMode()
    return ReminderEntity(
        id = id,
        title = title,
        category = category,
        customCategoryName = customCategoryName,
        scheduleJson = json.encodeToString(Schedule.serializer(), schedule),
        reminderTimesJson = json.encodeToString(
            ListSerializer(com.deepak.flow.core.model.LocalTimeSerializer),
            reminderTimes,
        ),
        startDateEpochDay = startDate.toEpochDay(),
        expirationMode = mode.name,
        endDateEpochDay = if (mode == ReminderExpirationMode.END_DATE) endDate?.toEpochDay() else null,
        occurrenceLimit = if (mode == ReminderExpirationMode.OCCURRENCE_LIMIT) occurrenceLimit else null,
        occurrencesDelivered = occurrencesDelivered,
        enabled = enabled,
        activeHoursJson = activeHours?.let { json.encodeToString(ActiveHours.serializer(), it) },
        reason = reason,
        note = note,
        accentColorIndex = accentColorIndex,
    )
}
