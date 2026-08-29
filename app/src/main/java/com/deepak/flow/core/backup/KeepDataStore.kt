package com.deepak.flow.core.backup

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.os.Process
import android.provider.MediaStore
import com.deepak.flow.core.database.FlowDatabase
import java.io.File

/**
 * A copy of Flow's database in the phone's Documents folder, so tasks can
 * return after uninstall even if the system leftover is not kept.
 *
 * Nothing is uploaded. The file stays on this device.
 *
 * Backups are scoped to the Android user profile that wrote them so a cloned
 * Flow instance on another profile does not inherit the original user's data.
 */
object KeepDataStore {
    const val DATABASE_NAME = "flow_database"
    const val DISPLAY_NAME = "flow-keep.db"
    const val META_DISPLAY_NAME = "flow-keep.meta"
    private const val MIME_TYPE = "application/octet-stream"
    private const val META_MIME_TYPE = "application/json"

    internal fun shouldWriteKeepCopy(
        keepEnabled: Boolean,
        onboardingCompleted: Boolean,
    ): Boolean = keepEnabled && onboardingCompleted

    private const val PER_USER_RANGE = 100_000

    internal fun currentAndroidUserId(): Int = Process.myUid() / PER_USER_RANGE

    internal fun shouldRestoreBackup(metaOwnerUserId: Int, currentUserId: Int): Boolean =
        metaOwnerUserId == currentUserId

    fun restoreIfNeeded(context: Context) {
        runCatching {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (dbFile.exists() && dbFile.length() > 0L) return
            val meta = readBackupMeta(context)
            if (meta != null && !shouldRestoreBackup(meta.ownerUserId, currentAndroidUserId())) return
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
            writeBackup(context, dbFile.readBytes(), currentAndroidUserId())
        }
    }

    internal data class KeepBackupMeta(
        val ownerUserId: Int,
    )

    private fun writeBackup(context: Context, bytes: ByteArray, ownerUserId: Int) {
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
        writeBackupMeta(context, ownerUserId)
    }

    private fun writeBackupMeta(context: Context, ownerUserId: Int) {
        val resolver = context.contentResolver
        deleteBackupMeta(context)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, META_DISPLAY_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, META_MIME_TYPE)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOCUMENTS}/Flow",
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection(), values) ?: return
        val json = """{"ownerUserId":$ownerUserId}"""
        resolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
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

    private fun readBackupMeta(context: Context): KeepBackupMeta? {
        val resolver = context.contentResolver
        resolver.query(
            collection(),
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(META_DISPLAY_NAME),
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            val uri = ContentUris.withAppendedId(collection(), id)
            val json = resolver.openInputStream(uri)?.use { it.readBytes()?.decodeToString() } ?: return null
            val ownerUserId = """"ownerUserId"\s*:\s*(\d+)""".toRegex()
                .find(json)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: return null
            return KeepBackupMeta(ownerUserId = ownerUserId)
        }
        return null
    }

    private fun deleteBackup(context: Context) {
        context.contentResolver.delete(
            collection(),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(DISPLAY_NAME),
        )
        deleteBackupMeta(context)
    }

    private fun deleteBackupMeta(context: Context) {
        context.contentResolver.delete(
            collection(),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(META_DISPLAY_NAME),
        )
    }

    private fun collection() = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
}
