package com.deepak.flow.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.deepak.flow.core.model.Category

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val category: Category,
    val customCategoryName: String?,
    val scheduleJson: String,
    val reminderTimesJson: String,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long?,
    val enabled: Boolean,
    val activeHoursJson: String?,
    val reason: String?,
    val note: String?,
)
