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
    version = 14,
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

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "reminders",
                    column = "accentColorIndex",
                    spec = "INTEGER",
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "remindersEnabled",
                    spec = "INTEGER NOT NULL DEFAULT 1",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "waterEnabled",
                    spec = "INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "keepDataOnUninstall",
                    spec = "INTEGER NOT NULL DEFAULT 1",
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "waterGoalMl",
                    spec = "INTEGER",
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "waterBottleStyleIndex",
                    spec = "INTEGER",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "waterIntakeMl",
                    spec = "INTEGER NOT NULL DEFAULT 0",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "waterIntakeEpochDay",
                    spec = "INTEGER",
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "waterAddLog",
                    spec = "TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "waterRemindersEnabled",
                    spec = "INTEGER NOT NULL DEFAULT 0",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "waterReminderIntervalMinutes",
                    spec = "INTEGER NOT NULL DEFAULT 60",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "waterActiveHoursEnabled",
                    spec = "INTEGER NOT NULL DEFAULT 0",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "waterActiveHoursStartMinutes",
                    spec = "INTEGER NOT NULL DEFAULT 480",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "waterActiveHoursEndMinutes",
                    spec = "INTEGER NOT NULL DEFAULT 1380",
                )
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "waterCustomQuickAddsMl",
                    spec = "TEXT NOT NULL DEFAULT ''",
                )
            }
        }
    }
}
