package com.school.manager.viewmodel

import android.content.Context
import com.google.gson.Gson
import com.school.manager.data.*
import java.io.*
import java.time.Instant
import java.util.zip.*

/**
 * All ZIP backup/restore logic lives here, away from the ViewModel.
 *
 * ZIP layout:
 *   meta.json    – BackupMeta (schema version, counts, filter description)
 *   state.json   – AppState (full or filtered subset, same Gson format)
 *   avatars/     – avatar image files (full export only)
 *
 * state.json is intentionally the plain AppState shape so that
 * parseGsonState() handles both new and legacy ZIPs without changes.
 */
class BackupManager(
    private val context: Context,
    private val gson: Gson,
    private val appVersion: String
) {

    // ── Export ────────────────────────────────────────────────────────────

    fun buildFullZip(state: AppState): ByteArray? = runCatching {
        val meta = BackupMeta(
            exportedAt = Instant.now().toString(),
            appVersion = appVersion,
            counts     = state.toBackupCounts(),
            filter     = null
        )
        writeZip(meta, state, includeAvatars = true)
    }.getOrNull()

    fun buildFilteredZip(filteredState: AppState, filter: FilterDescription): ByteArray? =
        runCatching {
            val meta = BackupMeta(
                exportedAt = Instant.now().toString(),
                appVersion = appVersion,
                counts     = filteredState.toBackupCounts(),
                filter     = filter
            )
            writeZip(meta, filteredState, includeAvatars = false)
        }.getOrNull()

    private fun writeZip(meta: BackupMeta, state: AppState, includeAvatars: Boolean): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.entry("meta.json",  gson.toJson(meta).toByteArray())
            zip.entry("state.json", gson.toJson(state).toByteArray())
            if (includeAvatars) {
                File(context.filesDir, "avatars").takeIf { it.exists() }
                    ?.listFiles()?.forEach { f ->
                        zip.entry("avatars/${f.name}", f.readBytes())
                    }
            }
        }
        return baos.toByteArray()
    }

    private fun ZipOutputStream.entry(name: String, data: ByteArray) {
        putNextEntry(ZipEntry(name)); write(data); closeEntry()
    }

    // ── Peek (preview before import) ──────────────────────────────────────

    /**
     * Reads only meta.json from the ZIP bytes.
     * Call this first to populate the import preview dialog.
     * Does NOT write anything to the database.
     */
    fun peekZip(bytes: ByteArray): ImportResult = runCatching {
        val metaJson = readEntry(bytes, "meta.json")
            ?: return ImportResult.Failure("ZIP 中未找到 meta.json，可能是旧版备份")
        val meta = gson.fromJson(metaJson, BackupMeta::class.java)
            ?: return ImportResult.Failure("meta.json 解析失败")
        ImportResult.Success(
            counts        = meta.counts,
            schemaVersion = meta.schemaVersion,
            exportedAt    = meta.exportedAt,
            filter        = meta.filter
        )
    }.getOrElse { ImportResult.Failure("读取失败：${it.message}") }

    // ── Commit import ─────────────────────────────────────────────────────

    /**
     * Parses state.json and restores avatars.
     * Returns the AppState ready to be merged into the database,
     * or null on failure.
     */
    fun extractState(bytes: ByteArray): AppState? = runCatching {
        val stateJson = readEntry(bytes, "state.json") ?: return null
        val avatarDir = File(context.filesDir, "avatars").also { it.mkdirs() }
        val pathRemap = mutableMapOf<String, String>()

        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.startsWith("avatars/")) {
                    val name = entry.name.removePrefix("avatars/")
                    val dest = File(avatarDir, name).also { it.writeBytes(zip.readBytes()) }
                    pathRemap[name] = dest.absolutePath
                } else {
                    zip.readBytes()   // consume and discard non-avatar entries
                }
                entry = zip.nextEntry
            }
        }
        parseGsonState(stateJson, gson, pathRemap)
    }.getOrNull()

    // ── Private helper ────────────────────────────────────────────────────

    private fun readEntry(bytes: ByteArray, name: String): String? {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == name) return zip.readBytes().toString(Charsets.UTF_8)
                zip.readBytes()
                entry = zip.nextEntry
            }
        }
        return null
    }
}
