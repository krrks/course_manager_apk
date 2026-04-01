package com.school.manager.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.GsonBuilder
import com.school.manager.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.time.Instant

// ── Status ────────────────────────────────────────────────────────────────────

sealed class SyncStatus {
    object Disconnected : SyncStatus()
    data class Ready(val repoUrl: String, val lastSync: String?) : SyncStatus()
    object Syncing : SyncStatus()
    data class Conflict(val detail: String) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

private class ConflictException : Exception()

// ── ViewModel ─────────────────────────────────────────────────────────────────

class GitHubSyncViewModel(app: Application) : AndroidViewModel(app) {

    private val gson  = GsonBuilder().create()
    private val prefs: SharedPreferences by lazy { openPrefs(getApplication()) }

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Disconnected)
    val status: StateFlow<SyncStatus> = _status

    val savedUrl:      String  get() = prefs.getString(KEY_URL,       "") ?: ""
    val savedToken:    String  get() = prefs.getString(KEY_TOKEN,     "") ?: ""
    val savedLastSync: String? get() = prefs.getString(KEY_LAST_SYNC, null)

    init { restoreStatus() }

    private fun restoreStatus() {
        val url   = prefs.getString(KEY_URL,   null)
        val token = prefs.getString(KEY_TOKEN, null)
        _status.value = if (!url.isNullOrBlank() && !token.isNullOrBlank())
            SyncStatus.Ready(url, prefs.getString(KEY_LAST_SYNC, null))
        else SyncStatus.Disconnected
    }

    // ── Connect / Disconnect ──────────────────────────────────────────────────

    fun connect(url: String, token: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _status.value = SyncStatus.Syncing
            val err = withContext(Dispatchers.IO) {
                GitHubSyncService(url.trim(), token.trim(), gson).testConnection()
            }
            if (err == null) {
                prefs.edit().putString(KEY_URL, url.trim()).putString(KEY_TOKEN, token.trim()).apply()
                _status.value = SyncStatus.Ready(url.trim(), prefs.getString(KEY_LAST_SYNC, null))
            } else {
                _status.value = SyncStatus.Disconnected
            }
            onResult(err)
        }
    }

    fun disconnect() {
        prefs.edit().clear().apply()
        _status.value = SyncStatus.Disconnected
    }

    /** Resets status to Ready after dismissing error/conflict dialogs. */
    fun restoreReady() {
        val url = prefs.getString(KEY_URL, null) ?: return
        _status.value = SyncStatus.Ready(url, prefs.getString(KEY_LAST_SYNC, null))
    }

    // ── Push ──────────────────────────────────────────────────────────────────

    fun push(appState: AppState, context: Context, appVersion: String, force: Boolean = false) {
        val url   = prefs.getString(KEY_URL,   null) ?: return
        val token = prefs.getString(KEY_TOKEN, null) ?: return
        viewModelScope.launch {
            _status.value = SyncStatus.Syncing
            var pushError: Throwable? = null
            withContext(Dispatchers.IO) {
                try { executePush(url, token, appState, context, appVersion, force) }
                catch (e: Exception) { pushError = e }
            }
            _status.value = when (val e = pushError) {
                null                 -> {
                    val now = now()
                    prefs.edit().putString(KEY_LAST_SYNC, now).apply()
                    SyncStatus.Ready(url, now)
                }
                is ConflictException -> SyncStatus.Conflict(
                    "远端数据自上次同步后已被修改，本地也有修改，存在冲突。"
                )
                else -> SyncStatus.Error(e.message ?: "Push failed")
            }
        }
    }

    fun pushForce(appState: AppState, context: Context, appVersion: String) =
        push(appState, context, appVersion, force = true)

    // ── Pull ──────────────────────────────────────────────────────────────────

    /**
     * Pulls state.json (always) and kp_custom.json (only when remote hash differs).
     * [onResult] receives the pulled AppState and optionally a list of custom KPs
     * that should be merged into the local DB. Built-in KPs are never touched.
     */
    fun pull(
        context: Context,
        onResult: (AppState?, List<KnowledgePoint>?) -> Unit
    ) {
        val url   = prefs.getString(KEY_URL,   null) ?: return
        val token = prefs.getString(KEY_TOKEN, null) ?: return
        viewModelScope.launch {
            _status.value = SyncStatus.Syncing
            var result: Pair<AppState, List<KnowledgePoint>?>? = null
            var pullError: String? = null
            withContext(Dispatchers.IO) {
                try { result = executePull(url, token, context) }
                catch (e: Exception) { pullError = e.message ?: "Pull failed" }
            }
            if (pullError != null) {
                _status.value = SyncStatus.Error(pullError!!)
                onResult(null, null)
            } else {
                val now = now()
                prefs.edit().putString(KEY_LAST_SYNC, now).apply()
                _status.value = SyncStatus.Ready(url, now)
                onResult(result?.first, result?.second)
            }
        }
    }

    // ── Core push ─────────────────────────────────────────────────────────────

    private fun executePush(
        url: String, token: String,
        appState: AppState, context: Context,
        appVersion: String, force: Boolean
    ) {
        val svc = GitHubSyncService(url, token, gson)

        // State payload: avatar paths stripped, all KP omitted
        val stateNoKp = appState.stripAvatarPaths().copy(
            kpChapters      = emptyList(),
            kpSections      = emptyList(),
            knowledgePoints = emptyList()
        )
        val stateBytes = gson.toJson(stateNoKp).toByteArray(Charsets.UTF_8)
        val stateHash  = sha256(stateBytes)

        // Custom KPs: serialized separately, pushed only when changed
        val customKps     = appState.knowledgePoints.filter { it.isCustom }
        val kpBytes       = serializeCustomKps(customKps)
        val kpHash        = sha256(kpBytes)
        val lastKpHash    = prefs.getString(KEY_KP_HASH, null)
        val kpChanged     = kpHash != lastKpHash

        // Conflict detection on main state
        if (!force) {
            val remoteMetaBytes = svc.getFile("meta.json")?.second
            if (remoteMetaBytes != null) {
                val remoteMeta = runCatching {
                    gson.fromJson(remoteMetaBytes.toString(Charsets.UTF_8), GitHubMeta::class.java)
                }.getOrNull()
                val lastHash = prefs.getString(KEY_LAST_HASH, null)
                if (remoteMeta != null && lastHash != null) {
                    val remoteChanged = remoteMeta.dataHash != lastHash
                    val localChanged  = stateHash != lastHash
                    if (remoteChanged && localChanged) throw ConflictException()
                }
            }
        }

        // Always push state.json
        if (!svc.putFile("state.json", stateBytes, svc.getFileSha("state.json"), "sync: update data"))
            throw Exception("Failed to upload state.json — check repo write permissions")

        // Push kp_custom.json only when changed (or force)
        if (kpChanged || force) {
            svc.putFile("kp_custom.json", kpBytes, svc.getFileSha("kp_custom.json"), "sync: custom kps")
            prefs.edit().putString(KEY_KP_HASH, kpHash).apply()
        }

        // Push avatars
        val avatarDir = File(context.filesDir, "avatars")
        if (avatarDir.exists()) {
            val remoteShAs = svc.listDir("avatars").associate { (n, s) -> n to s }
            avatarDir.listFiles()?.forEach { f ->
                svc.putFile("avatars/${f.name}", f.readBytes(), remoteShAs[f.name], "sync: avatar")
            }
        }

        // Push meta.json last — acts as "commit complete" marker
        val meta = GitHubMeta(
            syncedAt      = Instant.now().toString(),
            appVersion    = appVersion,
            dataHash      = stateHash,
            lessonCount   = appState.lessons.size,
            studentCount  = appState.students.size,
            kpCustomHash  = kpHash,
            customKpCount = customKps.size
        )
        svc.putFile("meta.json", gson.toJson(meta).toByteArray(), svc.getFileSha("meta.json"), "sync: meta")
        prefs.edit().putString(KEY_LAST_HASH, stateHash).apply()
    }

    // ── Core pull ─────────────────────────────────────────────────────────────

    private fun executePull(
        url: String, token: String, context: Context
    ): Pair<AppState, List<KnowledgePoint>?> {
        val svc = GitHubSyncService(url, token, gson)

        // Download state.json (no KP data inside)
        val (_, stateBytes) = svc.getFile("state.json")
            ?: throw Exception("远端未找到数据，请先从本设备推送")

        // Download avatars and build path remap
        val avatarDir = File(context.filesDir, "avatars").also { it.mkdirs() }
        val pathRemap = mutableMapOf<String, String>()
        svc.listDir("avatars").forEach { (name, _) ->
            val (_, bytes) = svc.getFile("avatars/$name") ?: return@forEach
            val dest = File(avatarDir, name).also { it.writeBytes(bytes) }
            pathRemap[name] = dest.absolutePath
        }

        val state = parseGsonState(stateBytes.toString(Charsets.UTF_8), gson, pathRemap)
            ?: throw Exception("远端数据解析失败，文件可能已损坏")

        // Check remote meta for kp_custom.json hash
        val remoteMeta = runCatching {
            svc.getFile("meta.json")?.second?.let {
                gson.fromJson(it.toString(Charsets.UTF_8), GitHubMeta::class.java)
            }
        }.getOrNull()

        val localKpHash = prefs.getString(KEY_KP_HASH, null)
        var customKps: List<KnowledgePoint>? = null

        // Download kp_custom.json only when remote hash differs from local
        if (remoteMeta?.kpCustomHash?.isNotBlank() == true &&
            remoteMeta.kpCustomHash != localKpHash) {
            val kpFileBytes = svc.getFile("kp_custom.json")?.second
            if (kpFileBytes != null) {
                customKps = parseCustomKps(kpFileBytes, gson)
                prefs.edit().putString(KEY_KP_HASH, remoteMeta.kpCustomHash).apply()
            }
        }

        prefs.edit().putString(KEY_LAST_HASH, sha256(stateBytes)).apply()
        return state to customKps
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    private fun serializeCustomKps(points: List<KnowledgePoint>): ByteArray {
        val gsonPoints = points.map {
            GsonKnowledgePoint(it.id, it.sectionId, it.no, it.title, it.content, true)
        }
        return gson.toJson(GsonCustomKps(gsonPoints)).toByteArray(Charsets.UTF_8)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun now() = Instant.now().toString().take(19).replace("T", " ")

    private fun openPrefs(context: Context): SharedPreferences = try {
        EncryptedSharedPreferences.create(
            "gh_sync_prefs",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        context.getSharedPreferences("gh_sync_prefs_plain", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_URL       = "repo_url"
        private const val KEY_TOKEN     = "token"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_LAST_HASH = "last_hash"
        private const val KEY_KP_HASH   = "kp_custom_hash"   // hash of kp_custom.json
    }
}

// ── Private extension — strips absolute avatar paths to bare filenames ────────

private fun AppState.stripAvatarPaths(): AppState = copy(
    teachers = teachers.map { t -> t.copy(avatarUri = t.avatarUri?.let { File(it).name }) },
    students = students.map { s -> s.copy(avatarUri = s.avatarUri?.let { File(it).name }) }
)
