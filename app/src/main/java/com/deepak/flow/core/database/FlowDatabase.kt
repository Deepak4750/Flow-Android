package com.deepak.flow.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ReminderEntity::class,
        ReminderOccurrenceDeliveryEntity::class,
        UserProfileEntity::class,
        ReminderDayCompletionEntity::class,
        WaterDayEntity::class,
        GymWorkoutEntity::class,
        GymWorkoutExerciseEntity::class,
        GymWorkoutSetEntity::class,
        GymRoutineEntity::class,
        GymRoutineDayEntity::class,
        GymRoutineExerciseEntity::class,
        GymCustomExerciseEntity::class,
        GymExerciseOverrideEntity::class,
    ],
    version = 36,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class FlowDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderOccurrenceDao(): ReminderOccurrenceDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun reminderCompletionDao(): ReminderCompletionDao
    abstract fun waterDayDao(): WaterDayDao
    abstract fun historyDao(): HistoryDao
    abstract fun gymWorkoutDao(): GymWorkoutDao
    abstract fun gymRoutineDao(): GymRoutineDao
    abstract fun gymCustomExerciseDao(): GymCustomExerciseDao
    abstract fun gymExerciseOverrideDao(): GymExerciseOverrideDao

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

        val MIGRATION_14_15_STATEMENTS: List<String> = listOf(
            """
            CREATE TABLE IF NOT EXISTS gym_routines (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                lastCompletedDayIndex INTEGER,
                createdAtEpochMilli INTEGER NOT NULL,
                updatedAtEpochMilli INTEGER NOT NULL
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS gym_routine_days (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                routineId INTEGER NOT NULL,
                dayIndex INTEGER NOT NULL,
                name TEXT NOT NULL
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS index_gym_routine_days_routineId ON gym_routine_days (routineId)",
            """
            CREATE TABLE IF NOT EXISTS gym_routine_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dayId INTEGER NOT NULL,
                name TEXT NOT NULL,
                trackingFields TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                setCount INTEGER NOT NULL
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS index_gym_routine_exercises_dayId ON gym_routine_exercises (dayId)",
            """
            CREATE TABLE IF NOT EXISTS gym_preferences (
                id INTEGER NOT NULL,
                activeRoutineId INTEGER,
                weightUnit TEXT NOT NULL,
                setRestSeconds INTEGER NOT NULL,
                exerciseRestSeconds INTEGER NOT NULL,
                automaticRest INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS gym_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                routineId INTEGER,
                routineName TEXT NOT NULL,
                dayIndex INTEGER NOT NULL,
                dayName TEXT NOT NULL,
                startedAtEpochMilli INTEGER NOT NULL,
                endedAtEpochMilli INTEGER,
                status TEXT NOT NULL,
                restKind TEXT NOT NULL,
                restDurationSeconds INTEGER NOT NULL,
                restEndsAtEpochMilli INTEGER,
                restExerciseId INTEGER
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS index_gym_sessions_routineId_dayIndex_status ON gym_sessions (routineId, dayIndex, status)",
            "CREATE INDEX IF NOT EXISTS index_gym_sessions_status ON gym_sessions (status)",
            """
            CREATE TABLE IF NOT EXISTS gym_session_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sessionId INTEGER NOT NULL,
                name TEXT NOT NULL,
                trackingFields TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                previousSetsJson TEXT NOT NULL
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS index_gym_session_exercises_sessionId ON gym_session_exercises (sessionId)",
            """
            CREATE TABLE IF NOT EXISTS gym_session_sets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                exerciseId INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL,
                saved INTEGER NOT NULL,
                measurementsJson TEXT NOT NULL
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS index_gym_session_sets_exerciseId ON gym_session_sets (exerciseId)",
        )

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_14_15_STATEMENTS.forEach(db::execSQL)
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS water_days (
                        dateEpochDay INTEGER NOT NULL,
                        intakeMl INTEGER NOT NULL,
                        addLog TEXT NOT NULL,
                        goalMl INTEGER,
                        PRIMARY KEY(dateEpochDay)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO water_days (dateEpochDay, intakeMl, addLog, goalMl)
                    SELECT waterIntakeEpochDay, waterIntakeMl, IFNULL(waterAddLog, ''), waterGoalMl
                    FROM user_profile
                    WHERE waterIntakeEpochDay IS NOT NULL
                      AND waterIntakeMl > 0
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_16_17_STATEMENTS: List<String> = listOf(
            "ALTER TABLE gym_routines ADD COLUMN currentDayIndex INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE gym_routines ADD COLUMN pendingSessionId INTEGER",
            "ALTER TABLE gym_routine_exercises ADD COLUMN note TEXT NOT NULL DEFAULT ''",
            """
            CREATE TABLE IF NOT EXISTS gym_exercise_notes (
                nameKey TEXT NOT NULL,
                displayName TEXT NOT NULL,
                note TEXT NOT NULL,
                trackingFields TEXT NOT NULL,
                updatedAtEpochMilli INTEGER NOT NULL,
                PRIMARY KEY(nameKey)
            )
            """.trimIndent(),
            "ALTER TABLE gym_sessions ADD COLUMN kind TEXT NOT NULL DEFAULT 'ROUTINE'",
            "ALTER TABLE gym_sessions ADD COLUMN weightUnit TEXT NOT NULL DEFAULT 'KG'",
            "ALTER TABLE gym_sessions ADD COLUMN sessionNote TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE gym_sessions ADD COLUMN currentExerciseIndex INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE gym_sessions ADD COLUMN currentSetIndex INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE gym_sessions ADD COLUMN restPhase TEXT NOT NULL DEFAULT 'NONE'",
            "ALTER TABLE gym_sessions ADD COLUMN restExerciseIndex INTEGER",
            "ALTER TABLE gym_session_exercises ADD COLUMN note TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE gym_session_sets ADD COLUMN failed INTEGER NOT NULL DEFAULT 0",
        )

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db, "gym_routines", "currentDayIndex", "INTEGER NOT NULL DEFAULT 0",
                )
                SqliteSchema.addColumnIfMissing(db, "gym_routines", "pendingSessionId", "INTEGER")
                SqliteSchema.addColumnIfMissing(
                    db, "gym_routine_exercises", "note", "TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(MIGRATION_16_17_STATEMENTS[3])
                SqliteSchema.addColumnIfMissing(
                    db, "gym_sessions", "kind", "TEXT NOT NULL DEFAULT 'ROUTINE'",
                )
                SqliteSchema.addColumnIfMissing(
                    db, "gym_sessions", "weightUnit", "TEXT NOT NULL DEFAULT 'KG'",
                )
                SqliteSchema.addColumnIfMissing(
                    db, "gym_sessions", "sessionNote", "TEXT NOT NULL DEFAULT ''",
                )
                SqliteSchema.addColumnIfMissing(
                    db, "gym_sessions", "currentExerciseIndex", "INTEGER NOT NULL DEFAULT 0",
                )
                SqliteSchema.addColumnIfMissing(
                    db, "gym_sessions", "currentSetIndex", "INTEGER NOT NULL DEFAULT 0",
                )
                SqliteSchema.addColumnIfMissing(
                    db, "gym_sessions", "restPhase", "TEXT NOT NULL DEFAULT 'NONE'",
                )
                SqliteSchema.addColumnIfMissing(db, "gym_sessions", "restExerciseIndex", "INTEGER")
                SqliteSchema.addColumnIfMissing(
                    db, "gym_session_exercises", "note", "TEXT NOT NULL DEFAULT ''",
                )
                SqliteSchema.addColumnIfMissing(
                    db, "gym_session_sets", "failed", "INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "gymEnabled",
                    spec = "INTEGER NOT NULL DEFAULT 1",
                )
            }
        }

        /**
         * Gym is removed. Drop all gym tables and strip gymEnabled from profile.
         * Reminders and water rows are left untouched.
         */
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS gym_session_sets")
                db.execSQL("DROP TABLE IF EXISTS gym_session_exercises")
                db.execSQL("DROP TABLE IF EXISTS gym_sessions")
                db.execSQL("DROP TABLE IF EXISTS gym_routine_exercises")
                db.execSQL("DROP TABLE IF EXISTS gym_routine_days")
                db.execSQL("DROP TABLE IF EXISTS gym_routines")
                db.execSQL("DROP TABLE IF EXISTS gym_exercise_notes")
                db.execSQL("DROP TABLE IF EXISTS gym_preferences")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_profile_new (
                        id INTEGER NOT NULL,
                        displayName TEXT,
                        nickname TEXT,
                        onboardingCompleted INTEGER NOT NULL,
                        snoozeEnabled INTEGER NOT NULL,
                        snoozeIntervalMinutes INTEGER NOT NULL,
                        remindersEnabled INTEGER NOT NULL,
                        waterEnabled INTEGER NOT NULL,
                        waterGoalMl INTEGER,
                        waterBottleStyleIndex INTEGER,
                        waterIntakeMl INTEGER NOT NULL,
                        waterIntakeEpochDay INTEGER,
                        waterAddLog TEXT NOT NULL,
                        waterCustomQuickAddsMl TEXT NOT NULL,
                        waterRemindersEnabled INTEGER NOT NULL,
                        waterReminderIntervalMinutes INTEGER NOT NULL,
                        waterActiveHoursEnabled INTEGER NOT NULL,
                        waterActiveHoursStartMinutes INTEGER NOT NULL,
                        waterActiveHoursEndMinutes INTEGER NOT NULL,
                        keepDataOnUninstall INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO user_profile_new (
                        id, displayName, nickname, onboardingCompleted,
                        snoozeEnabled, snoozeIntervalMinutes,
                        remindersEnabled, waterEnabled,
                        waterGoalMl, waterBottleStyleIndex,
                        waterIntakeMl, waterIntakeEpochDay, waterAddLog,
                        waterCustomQuickAddsMl,
                        waterRemindersEnabled, waterReminderIntervalMinutes,
                        waterActiveHoursEnabled, waterActiveHoursStartMinutes,
                        waterActiveHoursEndMinutes, keepDataOnUninstall
                    )
                    SELECT
                        id, displayName, nickname, onboardingCompleted,
                        snoozeEnabled, snoozeIntervalMinutes,
                        remindersEnabled, waterEnabled,
                        waterGoalMl, waterBottleStyleIndex,
                        waterIntakeMl, waterIntakeEpochDay, IFNULL(waterAddLog, ''),
                        IFNULL(waterCustomQuickAddsMl, ''),
                        waterRemindersEnabled, waterReminderIntervalMinutes,
                        waterActiveHoursEnabled, waterActiveHoursStartMinutes,
                        waterActiveHoursEndMinutes, keepDataOnUninstall
                    FROM user_profile
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE user_profile")
                db.execSQL("ALTER TABLE user_profile_new RENAME TO user_profile")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS gym_workouts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        status TEXT NOT NULL,
                        startedAtEpochMilli INTEGER NOT NULL,
                        endedAtEpochMilli INTEGER,
                        completed INTEGER NOT NULL,
                        weightUnit TEXT NOT NULL,
                        restEndsAtEpochMilli INTEGER,
                        restDurationSeconds INTEGER NOT NULL,
                        currentExerciseIndex INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_gym_workouts_status ON gym_workouts (status)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_gym_workouts_type_status ON gym_workouts (type, status)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS gym_workout_exercises (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        workoutId INTEGER NOT NULL,
                        exerciseName TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        trackingFields TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_gym_workout_exercises_workoutId ON gym_workout_exercises (workoutId)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS gym_workout_sets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        workoutExerciseId INTEGER NOT NULL,
                        setNumber INTEGER NOT NULL,
                        weight REAL,
                        weightUnit TEXT,
                        reps INTEGER,
                        durationSeconds INTEGER,
                        distance REAL,
                        speed REAL,
                        incline REAL,
                        resistance REAL,
                        rounds INTEGER,
                        failure INTEGER NOT NULL,
                        saved INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_gym_workout_sets_workoutExerciseId ON gym_workout_sets (workoutExerciseId)",
                )
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "gymEnabled",
                    spec = "INTEGER NOT NULL DEFAULT 1",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "gymWeightUnit",
                    spec = "TEXT NOT NULL DEFAULT 'KG'",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "gymSetRestSeconds",
                    spec = "INTEGER NOT NULL DEFAULT 90",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "user_profile",
                    column = "gymExerciseRestSeconds",
                    spec = "INTEGER NOT NULL DEFAULT 120",
                )
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "gym_workouts",
                    column = "currentExerciseStartedAtEpochMilli",
                    spec = "INTEGER",
                )
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "gym_workouts",
                    column = "starred",
                    spec = "INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "gym_workouts",
                    column = "title",
                    spec = "TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        val MIGRATION_24_25_STATEMENTS: List<String> = listOf(
            """
            CREATE TABLE IF NOT EXISTS gym_routines (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                currentDayIndex INTEGER NOT NULL,
                createdAtEpochMilli INTEGER NOT NULL,
                updatedAtEpochMilli INTEGER NOT NULL
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS gym_routine_days (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                routineId INTEGER NOT NULL,
                dayIndex INTEGER NOT NULL,
                name TEXT NOT NULL
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS index_gym_routine_days_routineId ON gym_routine_days (routineId)",
            """
            CREATE TABLE IF NOT EXISTS gym_routine_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dayId INTEGER NOT NULL,
                name TEXT NOT NULL,
                trackingFields TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                setCount INTEGER NOT NULL,
                note TEXT NOT NULL
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS index_gym_routine_exercises_dayId ON gym_routine_exercises (dayId)",
            "ALTER TABLE gym_workouts ADD COLUMN routineId INTEGER",
            "ALTER TABLE gym_workouts ADD COLUMN dayIndex INTEGER",
            "ALTER TABLE gym_workout_exercises ADD COLUMN plannedSetCount INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE gym_workout_exercises ADD COLUMN skipped INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE gym_workout_exercises ADD COLUMN routineExerciseId INTEGER",
        )

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_24_25_STATEMENTS.take(5).forEach(db::execSQL)
                SqliteSchema.addColumnIfMissing(db, "gym_workouts", "routineId", "INTEGER")
                SqliteSchema.addColumnIfMissing(db, "gym_workouts", "dayIndex", "INTEGER")
                SqliteSchema.addColumnIfMissing(
                    db,
                    "gym_workout_exercises",
                    "plannedSetCount",
                    "INTEGER NOT NULL DEFAULT 0",
                )
                SqliteSchema.addColumnIfMissing(
                    db,
                    "gym_workout_exercises",
                    "skipped",
                    "INTEGER NOT NULL DEFAULT 0",
                )
                SqliteSchema.addColumnIfMissing(
                    db,
                    "gym_workout_exercises",
                    "routineExerciseId",
                    "INTEGER",
                )
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db,
                    "gym_routines",
                    "repetitionCount",
                    "INTEGER NOT NULL DEFAULT 4",
                )
                SqliteSchema.addColumnIfMissing(
                    db,
                    "gym_routines",
                    "completedCycles",
                    "INTEGER NOT NULL DEFAULT 0",
                )
                SqliteSchema.addColumnIfMissing(
                    db,
                    "gym_routine_days",
                    "isRestDay",
                    "INTEGER NOT NULL DEFAULT 0",
                )
                SqliteSchema.addColumnIfMissing(
                    db,
                    "gym_routine_exercises",
                    "stableKey",
                    "TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    """
                    UPDATE gym_routine_exercises
                    SET stableKey = lower(hex(randomblob(16)))
                    WHERE stableKey = '' OR stableKey IS NULL
                    """.trimIndent(),
                )
                SqliteSchema.addColumnIfMissing(
                    db,
                    "gym_workouts",
                    "restKind",
                    "TEXT NOT NULL DEFAULT 'NONE'",
                )
                SqliteSchema.addColumnIfMissing(
                    db,
                    "gym_workout_exercises",
                    "exerciseStableKey",
                    "TEXT",
                )
                SqliteSchema.addColumnIfMissing(
                    db,
                    "user_profile",
                    "activeGymRoutineId",
                    "INTEGER",
                )
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db,
                    "gym_routines",
                    "starred",
                    "INTEGER NOT NULL DEFAULT 0",
                )
                SqliteSchema.addColumnIfMissing(
                    db,
                    "gym_routines",
                    "starredAtEpochMilli",
                    "INTEGER",
                )
            }
        }

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db,
                    "gym_routines",
                    "roundsCompleted",
                    "INTEGER NOT NULL DEFAULT 0",
                )
                SqliteSchema.addColumnIfMissing(
                    db,
                    "gym_routines",
                    "roundFourCheckpointDismissed",
                    "INTEGER NOT NULL DEFAULT 0",
                )
                if (SqliteSchema.hasColumn(db, "gym_routines", "completedCycles")) {
                    db.execSQL(
                        """
                        UPDATE gym_routines
                        SET roundsCompleted = completedCycles
                        WHERE completedCycles > 0
                          AND roundsCompleted = 0
                        """.trimIndent(),
                    )
                }
                SqliteSchema.rebuildGymRoutinesTable(db)
            }
        }

        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (SqliteSchema.gymRoutinesNeedsRebuild(db)) {
                    SqliteSchema.rebuildGymRoutinesTable(db)
                }
            }
        }

        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db,
                    "user_profile",
                    "menuTutorialStatus",
                    "TEXT NOT NULL DEFAULT 'COMPLETED'",
                )
            }
        }

        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db,
                    "user_profile",
                    "routineSwipeDeleteHintShown",
                    "INTEGER NOT NULL DEFAULT 1",
                )
                SqliteSchema.addColumnIfMissing(
                    db,
                    "user_profile",
                    "builderDaySwipeDeleteHintShown",
                    "INTEGER NOT NULL DEFAULT 1",
                )
            }
        }

        val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "reminders",
                    column = "expirationMode",
                    spec = "TEXT NOT NULL DEFAULT 'NONE'",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "reminders",
                    column = "occurrenceLimit",
                    spec = "INTEGER",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "reminders",
                    column = "occurrencesDelivered",
                    spec = "INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    """
                    UPDATE reminders
                    SET expirationMode = 'END_DATE'
                    WHERE endDateEpochDay IS NOT NULL
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reminder_occurrence_deliveries (
                        reminderId INTEGER NOT NULL,
                        scheduledAtEpochMilli INTEGER NOT NULL,
                        PRIMARY KEY(reminderId, scheduledAtEpochMilli)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "gym_workout_exercises",
                    column = "completedAtEpochMilli",
                    spec = "INTEGER",
                )
            }
        }

        val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS gym_custom_exercises (
                        id TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        normalizedKey TEXT NOT NULL,
                        createdAtEpochMilli INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_gym_custom_exercises_normalizedKey ON gym_custom_exercises (normalizedKey)",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "gym_routine_exercises",
                    column = "exerciseId",
                    spec = "TEXT NOT NULL DEFAULT ''",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "gym_workout_exercises",
                    column = "exerciseId",
                    spec = "TEXT NOT NULL DEFAULT ''",
                )
                GymExerciseMigration.backfillExerciseIds(db)
            }
        }

        val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS gym_exercise_overrides (
                        exerciseId TEXT NOT NULL,
                        displayName TEXT,
                        primaryMuscle TEXT,
                        secondaryMuscles TEXT,
                        equipment TEXT,
                        updatedAtEpochMilli INTEGER NOT NULL,
                        PRIMARY KEY(exerciseId)
                    )
                    """.trimIndent(),
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "gym_custom_exercises",
                    column = "primaryMuscle",
                    spec = "TEXT",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "gym_custom_exercises",
                    column = "secondaryMuscles",
                    spec = "TEXT",
                )
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "gym_custom_exercises",
                    column = "equipment",
                    spec = "TEXT",
                )
            }
        }

        val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SqliteSchema.addColumnIfMissing(
                    db = db,
                    table = "gym_workout_sets",
                    column = "skipped",
                    spec = "INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
    }
}
