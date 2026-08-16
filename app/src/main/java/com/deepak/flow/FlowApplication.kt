package com.deepak.flow

import android.app.Application
import androidx.room.Room
import com.deepak.flow.core.database.FlowDatabase
import com.deepak.flow.core.notification.NotificationChannelManager
import com.deepak.flow.core.notification.NotificationScheduler
import com.deepak.flow.core.repository.ProfileRepository
import com.deepak.flow.core.repository.ProfileRepositoryImpl
import com.deepak.flow.core.repository.ReminderRepository
import com.deepak.flow.core.repository.ReminderRepositoryImpl

class FlowApplication : Application() {

    lateinit var database: FlowDatabase
        private set

    lateinit var notificationScheduler: NotificationScheduler
        private set

    lateinit var reminderRepository: ReminderRepository
        private set

    lateinit var profileRepository: ProfileRepository
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationChannelManager.createChannel(this)

        // Reminders are user data. Never drop tables for a released schema.
        // Versions 1–2 existed only during development and were never shipped,
        // so those can still be rebuilt if a stale debug database turns up.
        database = Room.databaseBuilder(
            applicationContext,
            FlowDatabase::class.java,
            "flow_database",
        ).fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)
            .addMigrations(
                FlowDatabase.MIGRATION_3_4,
                FlowDatabase.MIGRATION_4_5,
                FlowDatabase.MIGRATION_5_6,
                FlowDatabase.MIGRATION_6_7,
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
        )
    }
}
