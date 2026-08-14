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

        // No blanket destructive fallback: reminders are user data now, so a future schema
        // bump must ship a real Migration rather than silently dropping the tables. Only
        // versions 1 and 2 stay destructive — they existed solely during initial
        // development, were never released, and their schemas were never exported, so no
        // migration could be written for them; this keeps a stale dev database recoverable
        // instead of crashing on open.
        database = Room.databaseBuilder(
            applicationContext,
            FlowDatabase::class.java,
            "flow_database",
        ).fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)
            .addMigrations(FlowDatabase.MIGRATION_3_4)
            .build()

        notificationScheduler = NotificationScheduler(this)
        reminderRepository = ReminderRepositoryImpl(
            dao = database.reminderDao(),
            completionDao = database.reminderCompletionDao(),
            notificationScheduler = notificationScheduler,
        )
        profileRepository = ProfileRepositoryImpl(
            dao = database.userProfileDao(),
        )
    }
}
