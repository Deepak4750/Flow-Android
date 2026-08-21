package com.deepak.flow.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.deepak.flow.core.model.SnoozeSettings

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val displayName: String?,
    val nickname: String?,
    val onboardingCompleted: Boolean,
    val snoozeEnabled: Boolean = SnoozeSettings.DEFAULT_ENABLED,
    val snoozeIntervalMinutes: Int = SnoozeSettings.DEFAULT_INTERVAL_MINUTES,
    val remindersEnabled: Boolean = true,
    val waterEnabled: Boolean = false,
    val waterGoalMl: Int? = null,
    val waterBottleStyleIndex: Int? = null,
    val waterIntakeMl: Int = 0,
    val waterIntakeEpochDay: Long? = null,
    val waterAddLog: String = "",
    val waterCustomQuickAddsMl: String = "",
    val waterRemindersEnabled: Boolean = false,
    val waterReminderIntervalMinutes: Int = 60,
    val waterActiveHoursEnabled: Boolean = false,
    val waterActiveHoursStartMinutes: Int = 480,
    val waterActiveHoursEndMinutes: Int = 1380,
    val keepDataOnUninstall: Boolean = true,
)
