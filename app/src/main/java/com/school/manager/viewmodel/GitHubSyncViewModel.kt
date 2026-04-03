package com.school.manager.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.GsonBuilder
import com.school.manager.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow; import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch; import kotlinx.coroutines.withContext
import java.io.File; import java.security.MessageDigest; import java.time.Instant

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

    fun disconnect() { prefs.edit().clear().apply(); _status.value = SyncStatus.Disconnected }

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
                is ConflictException -> SyncStatus.Conflict("远端数据自上次同步后已被修改，本地也有修改，存在冲突。")
                else                 -> SyncStatus.Error(e.message ?: "Push failed")
            }
        }
    }

    fun pushForce(appState: AppState, context: Context, appVersion: String) =
        push(appState, context, appVersion, force = true)

    // ── Pull ──────────────────────────────────────────────────────────────────

    fun pull(context: Context, onResult: (AppState?, KpSyncData?) -> Unit) {
        val url   = prefs.getString(KEY_URL,   null) ?: return
        val token = prefs.getString(KEY_TOKEN, null) ?: return
        viewModelScope.launch {
            _status.value = SyncStatus.Syncing
            var result: Pair<AppState, KpSyncData?>? = null
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

        // Build state payload (KP data kept separate)
        val stateNoKp = appState.stripAvatarPaths().copy(
            kpChapters = emptyList(), kpSections = emptyList(), knowledgePoints = emptyList()
        )
        val stateBytes = gson.toJson(stateNoKp).toByteArray(Charsets.UTF_8)
        val stateHash  = sha256(stateBytes)

        // Fetch remote meta once (used for conflict check and as reference state hash)
        val remoteMetaBytes = svc.getFile("meta.json")?.second
        val remoteMeta = remoteMetaBytes?.let { bytes ->
            runCatching { gson.fromJson(bytes.toString(Charsets.UTF_8), GitHubMeta::class.java) }.getOrNull()
        }

        // Conflict detection
        if (!force) {
            val lastHash = prefs.getString(KEY_LAST_HASH, null)
            if (remoteMeta != null && lastHash != null && remoteMeta.dataHash != lastHash && stateHash != lastHash)
                throw ConflictException()
        }

        // Upload state.json only if changed
        val lastStateHash = prefs.getString(KEY_LAST_HASH, null)
        val stateChanged  = stateHash != lastStateHash
        if (stateChanged || force) {
            if (!svc.putFile("state.json", stateBytes, svc.getFileSha("state.json"), "sync: update state"))
                throw Exception("Failed to upload state.json — check repo write permissions")
        }

        // Upload kp_data.json (full hierarchy: all chapters, sections, points)
        var kpDataHash = prefs.getString(KEY_KP_DATA_HASH, "") ?: ""
        if (appState.kpChapters.isNotEmpty() || appState.knowledgePoints.isNotEmpty()) {
            val kpBytes = gson.toJson(GsonKpData(
                chapters = appState.kpChapters.map { GsonKpChapter(it.id, it.grade, it.no, it.name) },
                sections = appState.kpSections.map { GsonKpSection(it.id, it.chapterId, it.no, it.name) },
                points   = appState.knowledgePoints.map { GsonKnowledgePoint(it.id, it.sectionId, it.no, it.title, it.content, it.isCustom) }
            )).toByteArray(Charsets.UTF_8)
            val kpHash = sha256(kpBytes)
            if (kpHash != prefs.getString(KEY_KP_DATA_HASH, null) || force) {
                svc.putFile("kp_data.json", kpBytes, svc.getFileSha("kp_data.json"), "sync: kp data")
                prefs.edit().putString(KEY_KP_DATA_HASH, kpHash).apply()
                kpDataHash = kpHash
            }
        }

        // Upload avatars (skip files already on remote)
        val avatarDir = File(context.filesDir, "avatars")
        if (avatarDir.exists()) {
            val remoteShas = svc.listDir("avatars").associate { (n, s) -> n to s }
            avatarDir.listFiles()?.forEach { f ->
                if (!remoteShas.containsKey(f.name) || force)
                    svc.putFile("avatars/${f.name}", f.readBytes(), remoteShas[f.name], "sync: avatar")
            }
        }

        // Push meta.json last — preserves remote dataHash when state upload was skipped
        val effectiveHash = if (stateChanged || force) stateHash else remoteMeta?.dataHash?.takeIf { it.isNotBlank() } ?: stateHash
        svc.putFile("meta.json", gson.toJson(GitHubMeta(
            syncedAt = Instant.now().toString(), appVersion = appVersion, dataHash = effectiveHash,
            lessonCount = appState.lessons.size, studentCount = appState.students.size,
            customKpCount = appState.knowledgePoints.count { it.isCustom }, kpDataHash = kpDataHash
        )).toByteArray(), svc.getFileSha("meta.json"), "sync: meta")
        prefs.edit().putString(KEY_LAST_HASH, effectiveHash).apply()
    }

    // ── Core pull ─────────────────────────────────────────────────────────────

    private fun executePull(url: String, token: String, context: Context): Pair<AppState, KpSyncData?> {
        val svc = GitHubSyncService(url, token, gson)

        val (_, stateBytes) = svc.getFile("state.json")
            ?: throw Exception("远端未找到数据，请先从本设备推送")

        // Download avatars
        val avatarDir = File(context.filesDir, "avatars").also { it.mkdirs() }
        val pathRemap = mutableMapOf<String, String>()
        svc.listDir("avatars").forEach { (name, _) ->
            val (_, bytes) = svc.getFile("avatars/$name") ?: return@forEach
            val dest = File(avatarDir, name).also { it.writeBytes(bytes) }
            pathRemap[name] = dest.absolutePath
        }

        val state = parseGsonState(stateBytes.toString(Charsets.UTF_8), gson, pathRemap)
            ?: throw Exception("远端数据解析失败，文件可能已损坏")

        val remoteMeta = runCatching {
            svc.getFile("meta.json")?.second?.let {
                gson.fromJson(it.toString(Charsets.UTF_8), GitHubMeta::class.java)
            }
        }.getOrNull()

        var kpSyncData: KpSyncData? = null

        // ── New format: kp_data.json (all KPs with chapter/section hierarchy) ──
        val remoteKpDataHash = remoteMeta?.kpDataHash?.takeIf { it.isNotBlank() }
        if (remoteKpDataHash != null) {
            val localKpDataHash = prefs.getString(KEY_KP_DATA_HASH, null)
            if (remoteKpDataHash != localKpDataHash) {
                val bytes = svc.getFile("kp_data.json")?.second
                if (bytes != null) {
                    kpSyncData = parseKpData(bytes, gson)
                    prefs.edit().putString(KEY_KP_DATA_HASH, remoteKpDataHash).apply()
                }
            }
        } else {
            // Legacy: per-grade kp_custom_{grade}.json
            val gradeHashes = remoteMeta?.gradeKpHashes ?: emptyMap()
            if (gradeHashes.isNotEmpty()) {
                if (gradeHashes.any { (g, h) -> prefs.getString("$KEY_KP_HASH_PREFIX$g", null) != h }) {
                    val allKps = mutableListOf<KnowledgePoint>()
                    gradeHashes.forEach { (g, h) ->
                        svc.getFile("kp_custom_$g.json")?.second?.let { parseCustomKps(it, gson)?.let { kps -> allKps.addAll(kps) } }
                        prefs.edit().putString("$KEY_KP_HASH_PREFIX$g", h).apply()
                    }
                    kpSyncData = KpSyncData(emptyList(), emptyList(), allKps)
                }
            } else if (remoteMeta?.kpCustomHash?.isNotBlank() == true) {
                // Oldest legacy: single kp_custom.json
                val localLegacy = prefs.getString(KEY_KP_HASH_LEGACY, null)
                if (remoteMeta.kpCustomHash != localLegacy) {
                    svc.getFile("kp_custom.json")?.second?.let { bytes ->
                        parseCustomKps(bytes, gson)?.let { kpSyncData = KpSyncData(emptyList(), emptyList(), it) }
                        prefs.edit().putString(KEY_KP_HASH_LEGACY, remoteMeta.kpCustomHash).apply()
                    }
                }
            }
        }

        prefs.edit().putString(KEY_LAST_HASH, sha256(stateBytes)).apply()
        return state to kpSyncData
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun now() = Instant.now().toString().take(19).replace("T", " ")

    private fun openPrefs(context: Context): SharedPreferences = try {
        val mk = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "gh_sync_prefs", mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) { context.getSharedPreferences("gh_sync_prefs_plain", Context.MODE_PRIVATE) }

    companion object {
        private const val KEY_URL            = "repo_url"
        private const val KEY_TOKEN          = "token"
        private const val KEY_LAST_SYNC      = "last_sync"
        private const val KEY_LAST_HASH      = "last_hash"
        private const val KEY_KP_DATA_HASH   = "kp_data_hash"
        private const val KEY_KP_HASH_LEGACY = "kp_custom_hash"
        private const val KEY_KP_HASH_PREFIX = "kp_hash_grade_"
    }
}

private fun AppState.stripAvatarPaths(): AppState = copy(
    teachers = teachers.map { t -> t.copy(avatarUri = t.avatarUri?.let { File(it).name }) },
    students = students.map { s -> s.copy(avatarUri = s.avatarUri?.let { File(it).name }) }
)