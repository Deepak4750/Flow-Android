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

        database = Room.databaseBuilder(
            applicationContext,
            FlowDatabase::class.java,
            "flow_database",
        ).fallbackToDestructiveMigration(dropAllTables = true)
            .build()

        notificationScheduler = NotificationScheduler(this)
        reminderRepository = ReminderRepositoryImpl(
            dao = database.reminderDao(),
            notificationScheduler = notificationScheduler,
        )
        profileRepository = ProfileRepositoryImpl(
            dao = database.userProfileDao(),
        )
    }
}
