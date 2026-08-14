package com.deepak.flow.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val displayName: String?,
    val nickname: String?,
    val onboardingCompleted: Boolean,
)
