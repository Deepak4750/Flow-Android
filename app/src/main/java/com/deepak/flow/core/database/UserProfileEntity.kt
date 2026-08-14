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
)
