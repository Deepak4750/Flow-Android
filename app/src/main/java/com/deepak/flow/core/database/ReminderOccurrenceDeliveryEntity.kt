package com.deepak.flow.core.database

import androidx.room.Entity

@Entity(
    tableName = "reminder_occurrence_deliveries",
    primaryKeys = ["reminderId", "scheduledAtEpochMilli"],
)
data class ReminderOccurrenceDeliveryEntity(
    val reminderId: Long,
    val scheduledAtEpochMilli: Long,
)
