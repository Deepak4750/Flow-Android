package com.deepak.flow.core.database

import androidx.room.Entity

@Entity(
    tableName = "reminder_day_completions",
    primaryKeys = ["reminderId", "dateEpochDay"],
)
data class ReminderDayCompletionEntity(
    val reminderId: Long,
    val dateEpochDay: Long,
    val completedAtEpochMilli: Long,
)
