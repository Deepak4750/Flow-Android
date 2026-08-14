package com.deepak.flow.core.model

import java.time.LocalDate
import java.time.LocalTime

data class Reminder(
    val id: Long = 0L,
    val title: String,
    val category: Category,
    val customCategoryName: String? = null,
    val schedule: Schedule,
    val reminderTimes: List<LocalTime>,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val enabled: Boolean = true,
    val activeHours: ActiveHours? = null,
    val reason: String? = null,
    val note: String? = null,
)
