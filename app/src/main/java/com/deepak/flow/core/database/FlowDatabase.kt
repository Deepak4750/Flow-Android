package com.deepak.flow.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ReminderEntity::class, UserProfileEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(DatabaseConverters::class)
abstract class FlowDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun userProfileDao(): UserProfileDao
}
