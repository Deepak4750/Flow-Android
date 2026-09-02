package com.deepak.flow.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import com.deepak.flow.core.gym.GymBuiltinExerciseCatalog
import com.deepak.flow.core.gym.GymExerciseIdentity
import com.deepak.flow.core.gym.GymExerciseNormalizer

internal object GymExerciseMigration {
    fun backfillExerciseIds(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        val customByKey = mutableMapOf<String, String>()
        val names = linkedSetOf<String>()

        db.query("SELECT DISTINCT TRIM(name) AS n FROM gym_routine_exercises WHERE TRIM(name) != ''").use { cursor ->
            val index = cursor.getColumnIndexOrThrow("n")
            while (cursor.moveToNext()) {
                names.add(cursor.getString(index))
            }
        }
        db.query(
            "SELECT DISTINCT TRIM(exerciseName) AS n FROM gym_workout_exercises WHERE TRIM(exerciseName) != ''",
        ).use { cursor ->
            val index = cursor.getColumnIndexOrThrow("n")
            while (cursor.moveToNext()) {
                names.add(cursor.getString(index))
            }
        }

        names.forEach { rawName ->
            val trimmed = rawName.trim()
            if (trimmed.isEmpty()) return@forEach
            val builtin = GymBuiltinExerciseCatalog.resolveExact(trimmed)
            val exerciseId = if (builtin != null) {
                builtin.id
            } else {
                val normalizedKey = GymExerciseNormalizer.normalizeKey(trimmed)
                customByKey.getOrPut(normalizedKey) {
                    val id = GymExerciseIdentity.newCustomId()
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO gym_custom_exercises (id, displayName, normalizedKey, createdAtEpochMilli)
                        VALUES (?, ?, ?, ?)
                        """.trimIndent(),
                        arrayOf(id, trimmed, normalizedKey, now),
                    )
                    id
                }
            }
            db.execSQL(
                """
                UPDATE gym_routine_exercises
                SET exerciseId = ?
                WHERE TRIM(name) = ?
                  AND (exerciseId = '' OR exerciseId IS NULL)
                """.trimIndent(),
                arrayOf(exerciseId, trimmed),
            )
            db.execSQL(
                """
                UPDATE gym_workout_exercises
                SET exerciseId = ?
                WHERE TRIM(exerciseName) = ?
                  AND (exerciseId = '' OR exerciseId IS NULL)
                """.trimIndent(),
                arrayOf(exerciseId, trimmed),
            )
        }
    }
}
