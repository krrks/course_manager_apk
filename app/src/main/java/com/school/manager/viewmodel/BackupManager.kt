package com.school.manager.viewmodel

import android.content.Context
import com.google.gson.Gson
import com.school.manager.data.*
import java.io.*
import java.time.Instant
import java.util.zip.*

/**
 * ZIP backup/restore logic.
 *
 * ZIP layout:
 *   meta.json    – BackupMeta (schema v3: includes KP counts)
 *   state.json   – full AppState via Gson (includes KP chapters/sections/points)
 *   avatars/     – avatar image files (full export only)
 *
 * Filtered exports omit KP data (they are curriculum-wide, not lesson-scoped).
 */
class BackupManager(
    private val context: Context,
    private val gson: Gson,
    private val appVersion: String
) {

    // ── Export ────────────────────────────────────────────────────────────────

    fun buildFullZip(state: AppState): ByteArray? = runCatching {
        writeZip(
            meta = BackupMeta(
                exportedAt = Instant.now().toString(),
                appVersion = appVersion,
                counts     = state.toBackupCounts(),
                filter     = null
            ),
            state          = state,
            includeAvatars = true
        )
    }.getOrNull()

    fun buildFilteredZip(filteredState: AppState, filter: FilterDescription): ByteArray? =
        runCatching {
            writeZip(
                meta = BackupMeta(
                    exportedAt = Instant.now().toString(),
                    appVersion = appVersion,
                    counts     = filteredState.toBackupCounts(),
                    filter     = filter
                ),
                state          = filteredState,
                includeAvatars = false
            )
        }.getOrNull()

    private fun writeZip(meta: BackupMeta, state: AppState, includeAvatars: Boolean): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.entry("meta.json",  gson.toJson(meta).toByteArray())
            zip.entry("state.json", gson.toJson(state).toByteArray())
            if (includeAvatars) {
                File(context.filesDir, "avatars").takeIf { it.exists() }
                    ?.listFiles()
                    ?.forEach { f -> zip.entry("avatars/${f.name}", f.readBytes()) }
            }
        }
        return baos.toByteArray()
    }

    private fun ZipOutputStream.entry(name: String, data: ByteArray) {
        putNextEntry(ZipEntry(name)); write(data); closeEntry()
    }

    // ── Peek ──────────────────────────────────────────────────────────────────

    fun peekZip(bytes: ByteArray): ImportResult = runCatching {
        val metaJson = readEntry(bytes, "meta.json")
            ?: return ImportResult.Failure("ZIP 中未找到 meta.json")
        val meta = gson.fromJson(metaJson, BackupMeta::class.java)
            ?: return ImportResult.Failure("meta.json 解析失败")
        ImportResult.Success(
            counts        = meta.counts,
            schemaVersion = meta.schemaVersion,
            exportedAt    = meta.exportedAt,
            filter        = meta.filter
        )
    }.getOrElse { ImportResult.Failure("读取失败：${it.message}") }

    // ── Commit import ─────────────────────────────────────────────────────────

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
                    zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }
        parseGsonState(stateJson, gson, pathRemap)
    }.getOrNull()

    // ── Private ───────────────────────────────────────────────────────────────

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
