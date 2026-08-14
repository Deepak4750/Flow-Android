package com.deepak.flow.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ReminderEntity::class,
        UserProfileEntity::class,
        ReminderDayCompletionEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class FlowDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun reminderCompletionDao(): ReminderCompletionDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reminder_day_completions (
                        reminderId INTEGER NOT NULL,
                        dateEpochDay INTEGER NOT NULL,
                        completedAtEpochMilli INTEGER NOT NULL,
                        PRIMARY KEY(reminderId, dateEpochDay)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
