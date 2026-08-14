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
}
