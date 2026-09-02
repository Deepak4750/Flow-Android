package com.deepak.flow.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.deepak.flow.core.model.ContextualHints
import com.deepak.flow.core.model.MenuTutorialStatus
import com.deepak.flow.core.model.SnoozeSettings
import com.deepak.flow.core.model.UserProfile

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val displayName: String?,
    val nickname: String?,
    val onboardingCompleted: Boolean,
    val snoozeEnabled: Boolean = SnoozeSettings.DEFAULT_ENABLED,
    val snoozeIntervalMinutes: Int = SnoozeSettings.DEFAULT_INTERVAL_MINUTES,
    val remindersEnabled: Boolean = UserProfile.DEFAULT_REMINDERS_ENABLED,
    val waterEnabled: Boolean = UserProfile.DEFAULT_WATER_ENABLED,
    val gymEnabled: Boolean = UserProfile.DEFAULT_GYM_ENABLED,
    val gymWeightUnit: String = "KG",
    val gymSetRestSeconds: Int = 90,
    val gymExerciseRestSeconds: Int = 120,
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
    val activeGymRoutineId: Long? = null,
    val menuTutorialStatus: String = MenuTutorialStatus.DEFAULT.name,
    val routineSwipeDeleteHintShown: Boolean = ContextualHints.ROUTINE_SWIPE_DELETE_HINT_SHOWN_DEFAULT,
    val builderDaySwipeDeleteHintShown: Boolean = ContextualHints.BUILDER_DAY_SWIPE_DELETE_HINT_SHOWN_DEFAULT,
)
