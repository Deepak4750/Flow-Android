package com.deepak.flow

import android.app.Application
import androidx.room.Room
import com.deepak.flow.core.backup.KeepDataStore
import com.deepak.flow.core.database.FlowDatabase
import com.deepak.flow.core.model.UserProfile
import com.deepak.flow.core.notification.ActiveWorkoutNotificationController
import com.deepak.flow.core.notification.NotificationChannelManager
import com.deepak.flow.core.notification.NotificationScheduler
import com.deepak.flow.core.repository.GymWorkoutRepository
import com.deepak.flow.core.repository.GymWorkoutRepositoryImpl
import com.deepak.flow.core.repository.HistoryRepository
import com.deepak.flow.core.repository.HistoryRepositoryImpl
import com.deepak.flow.core.repository.ProfileRepository
import com.deepak.flow.core.repository.ProfileRepositoryImpl
import com.deepak.flow.core.repository.ReminderRepository
import com.deepak.flow.core.repository.ReminderRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FlowApplication : Application() {

    companion object {
        private const val PREFS_NAME = "flow_app_state"
        private const val KEY_RESET_ROUNDS_V164 = "reset_rounds_v164"
    }

    lateinit var database: FlowDatabase
        private set

    lateinit var notificationScheduler: NotificationScheduler
        private set

    lateinit var reminderRepository: ReminderRepository
        private set

    lateinit var profileRepository: ProfileRepository
        private set

    lateinit var historyRepository: HistoryRepository
        private set

    lateinit var gymWorkoutRepository: GymWorkoutRepository
        private set

    lateinit var activeWorkoutNotificationController: ActiveWorkoutNotificationController
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationChannelManager.createChannel(this)
        KeepDataStore.restoreIfNeeded(this)

        // Tasks are user data. Never drop tables for a released schema.
        // Versions 1-2 existed only during development and were never shipped,
        // so those can still be rebuilt if a stale debug database turns up.
        database = Room.databaseBuilder(
            applicationContext,
            FlowDatabase::class.java,
            KeepDataStore.DATABASE_NAME,
        ).fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)
            .addMigrations(
                FlowDatabase.MIGRATION_3_4,
                FlowDatabase.MIGRATION_4_5,
                FlowDatabase.MIGRATION_5_6,
                FlowDatabase.MIGRATION_6_7,
                FlowDatabase.MIGRATION_7_8,
                FlowDatabase.MIGRATION_8_9,
                FlowDatabase.MIGRATION_9_10,
                FlowDatabase.MIGRATION_10_11,
                FlowDatabase.MIGRATION_11_12,
                FlowDatabase.MIGRATION_12_13,
                FlowDatabase.MIGRATION_13_14,
                FlowDatabase.MIGRATION_14_15,
                FlowDatabase.MIGRATION_15_16,
                FlowDatabase.MIGRATION_16_17,
                FlowDatabase.MIGRATION_17_18,
                FlowDatabase.MIGRATION_18_19,
                FlowDatabase.MIGRATION_19_20,
                FlowDatabase.MIGRATION_20_21,
                FlowDatabase.MIGRATION_21_22,
                FlowDatabase.MIGRATION_22_23,
                FlowDatabase.MIGRATION_23_24,
                FlowDatabase.MIGRATION_24_25,
                FlowDatabase.MIGRATION_25_26,
                FlowDatabase.MIGRATION_26_27,
                FlowDatabase.MIGRATION_27_28,
                FlowDatabase.MIGRATION_28_29,
            )
            .build()

        notificationScheduler = NotificationScheduler(this)
        reminderRepository = ReminderRepositoryImpl(
            dao = database.reminderDao(),
            completionDao = database.reminderCompletionDao(),
            notificationScheduler = notificationScheduler,
            onDataChanged = { com.deepak.flow.core.widget.FlowWidgets.refresh(this) },
        )
        profileRepository = ProfileRepositoryImpl(
            dao = database.userProfileDao(),
            waterDayDao = database.waterDayDao(),
        )
        gymWorkoutRepository = GymWorkoutRepositoryImpl(
            dao = database.gymWorkoutDao(),
            routineDao = database.gymRoutineDao(),
            profileDao = database.userProfileDao(),
        )
        historyRepository = HistoryRepositoryImpl(
            historyDao = database.historyDao(),
            waterDayDao = database.waterDayDao(),
            profileDao = database.userProfileDao(),
            gymWorkoutDao = database.gymWorkoutDao(),
            gymWorkoutRepository = gymWorkoutRepository,
        )
        activeWorkoutNotificationController = ActiveWorkoutNotificationController(
            appContext = this,
            repository = gymWorkoutRepository,
            scope = applicationScope,
        )
        activeWorkoutNotificationController.start()
        com.deepak.flow.core.widget.FlowWidgets.refresh(this)
        applicationScope.launch {
            notificationScheduler.syncWaterReminder(profileRepository.getProfile())
            resetRoundsCompletedOnceIfNeeded()
        }
        observeKeepDataCopy()
    }

    private suspend fun resetRoundsCompletedOnceIfNeeded() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_RESET_ROUNDS_V164, false)) return
        gymWorkoutRepository.resetAllRoundsCompleted()
        prefs.edit().putBoolean(KEY_RESET_ROUNDS_V164, true).apply()
    }

    private fun observeKeepDataCopy() {
        applicationScope.launch {
            combine(
                profileRepository.observeProfile(),
                reminderRepository.observeReminders(),
            ) { profile, _ -> profile }
                .collect { profile ->
                    KeepDataStore.sync(
                        context = this@FlowApplication,
                        database = database,
                        keepEnabled = profile?.keepDataOnUninstall
                            ?: UserProfile.DEFAULT_KEEP_DATA_ON_UNINSTALL,
                        onboardingCompleted = profile?.onboardingCompleted == true,
                    )
                }
        }
    }
}
