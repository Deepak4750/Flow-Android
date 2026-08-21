package com.deepak.flow.core.backup

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.deepak.flow.core.database.FlowDatabase
import java.io.File

/**
 * A copy of Flow's database in the phone's Documents folder, so tasks can
 * return after uninstall even if the system leftover is not kept.
 *
 * Nothing is uploaded. The file stays on this device.
 */
object KeepDataStore {
    const val DATABASE_NAME = "flow_database"
    const val DISPLAY_NAME = "flow-keep.db"
    private const val MIME_TYPE = "application/octet-stream"

    internal fun shouldWriteKeepCopy(
        keepEnabled: Boolean,
        onboardingCompleted: Boolean,
    ): Boolean = keepEnabled && onboardingCompleted

    fun restoreIfNeeded(context: Context) {
        runCatching {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (dbFile.exists() && dbFile.length() > 0L) return
            val bytes = readBackup(context) ?: return
            if (bytes.isEmpty()) return
            dbFile.parentFile?.mkdirs()
            File("${dbFile.path}-wal").delete()
            File("${dbFile.path}-shm").delete()
            dbFile.writeBytes(bytes)
        }
    }

    fun sync(context: Context, database: FlowDatabase, keepEnabled: Boolean, onboardingCompleted: Boolean) {
        runCatching {
            if (!shouldWriteKeepCopy(keepEnabled, onboardingCompleted)) {
                deleteBackup(context)
                return@runCatching
            }
            database.query("PRAGMA wal_checkpoint(FULL)", emptyArray()).close()
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists() || dbFile.length() <= 0L) return@runCatching
            writeBackup(context, dbFile.readBytes())
        }
    }

    private fun writeBackup(context: Context, bytes: ByteArray) {
        val resolver = context.contentResolver
        deleteBackup(context)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, DISPLAY_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOCUMENTS}/Flow",
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection(), values) ?: return
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    private fun readBackup(context: Context): ByteArray? {
        val resolver = context.contentResolver
        resolver.query(
            collection(),
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(DISPLAY_NAME),
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            val uri = ContentUris.withAppendedId(collection(), id)
            return resolver.openInputStream(uri)?.use { it.readBytes() }
        }
        return null
    }

    private fun deleteBackup(context: Context) {
        context.contentResolver.delete(
            collection(),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(DISPLAY_NAME),
        )
    }

    private fun collection() = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
}
