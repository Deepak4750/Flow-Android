package com.deepak.flow.core.database

import androidx.sqlite.db.SupportSQLiteDatabase

internal object SqliteSchema {
    fun hasColumn(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex < 0) return false
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return true
            }
        }
        return false
    }

    fun addColumnIfMissing(
        db: SupportSQLiteDatabase,
        table: String,
        column: String,
        spec: String,
    ) {
        if (!hasColumn(db, table, column)) {
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $spec")
        }
    }

    /**
     * Rebuilds gym_routines to match [GymRoutineEntity], dropping legacy columns such as
     * repetitionCount and completedCycles that Room rejects after schema changes.
     */
    fun rebuildGymRoutinesTable(db: SupportSQLiteDatabase) {
        val roundsExpr = when {
            hasColumn(db, "gym_routines", "roundsCompleted") &&
                hasColumn(db, "gym_routines", "completedCycles") ->
                "COALESCE(roundsCompleted, completedCycles, 0)"
            hasColumn(db, "gym_routines", "roundsCompleted") ->
                "COALESCE(roundsCompleted, 0)"
            hasColumn(db, "gym_routines", "completedCycles") ->
                "COALESCE(completedCycles, 0)"
            else -> "0"
        }
        val checkpointExpr = if (hasColumn(db, "gym_routines", "roundFourCheckpointDismissed")) {
            "COALESCE(roundFourCheckpointDismissed, 0)"
        } else {
            "0"
        }
        val starredExpr = if (hasColumn(db, "gym_routines", "starred")) {
            "COALESCE(starred, 0)"
        } else {
            "0"
        }
        val starredAtExpr = if (hasColumn(db, "gym_routines", "starredAtEpochMilli")) {
            "starredAtEpochMilli"
        } else {
            "NULL"
        }

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS gym_routines_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                currentDayIndex INTEGER NOT NULL,
                roundsCompleted INTEGER NOT NULL,
                roundFourCheckpointDismissed INTEGER NOT NULL,
                starred INTEGER NOT NULL,
                starredAtEpochMilli INTEGER,
                createdAtEpochMilli INTEGER NOT NULL,
                updatedAtEpochMilli INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO gym_routines_new (
                id,
                name,
                currentDayIndex,
                roundsCompleted,
                roundFourCheckpointDismissed,
                starred,
                starredAtEpochMilli,
                createdAtEpochMilli,
                updatedAtEpochMilli
            )
            SELECT
                id,
                name,
                currentDayIndex,
                $roundsExpr,
                $checkpointExpr,
                $starredExpr,
                $starredAtExpr,
                createdAtEpochMilli,
                updatedAtEpochMilli
            FROM gym_routines
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE gym_routines")
        db.execSQL("ALTER TABLE gym_routines_new RENAME TO gym_routines")
    }

    fun gymRoutinesNeedsRebuild(db: SupportSQLiteDatabase): Boolean {
        return hasColumn(db, "gym_routines", "repetitionCount") ||
            hasColumn(db, "gym_routines", "completedCycles")
    }
}
