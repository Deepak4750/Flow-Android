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
    version = 6,
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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "snoozeIntervalMinutes",
                    spec = "INTEGER NOT NULL DEFAULT 10",
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "snoozeEnabled",
                    spec = "INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
    }
}
