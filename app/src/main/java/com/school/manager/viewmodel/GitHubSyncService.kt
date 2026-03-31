package com.school.manager.viewmodel

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.net.HttpURLConnection
import java.net.URL

internal data class GitHubMeta(
    val syncedAt: String = "",
    val appVersion: String = "",
    val dataHash: String = "",
    val lessonCount: Int = 0,
    val studentCount: Int = 0,
    val kpCount: Int = 0
)

/**
 * Low-level GitHub Contents API wrapper.
 * All relative paths resolve under [FOLDER] in the target repo.
 * Zero new dependencies — uses HttpURLConnection + Gson already in the project.
 */
internal class GitHubSyncService(repoUrl: String, private val token: String, private val gson: Gson) {

    val owner: String
    val repo: String

    init {
        val cleaned = repoUrl
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
            .removeSuffix(".git").trim('/')
        val parts = cleaned.split("/")
        require(parts.size >= 2) { "Expected: https://github.com/owner/repo" }
        owner = parts[0]
        repo  = parts[1]
    }

    companion object { const val FOLDER = "userdata_sync" }

    // ── Connection helpers ────────────────────────────────────────────────────

    private fun conn(relPath: String, method: String): HttpURLConnection =
        (URL("https://api.github.com/repos/$owner/$repo/contents/$FOLDER/$relPath")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/vnd.github.v3+json")
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 20_000; readTimeout = 30_000
        }

    private fun repoConn(): HttpURLConnection =
        (URL("https://api.github.com/repos/$owner/$repo")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/vnd.github.v3+json")
            connectTimeout = 10_000; readTimeout = 10_000
        }

    // ── Public API ────────────────────────────────────────────────────────────

    /** null = success, else human-readable error */
    fun testConnection(): String? = runCatching {
        val code = repoConn().responseCode
        when (code) {
            200  -> null
            401  -> "Authentication failed — check your PAT"
            403  -> "Access denied — token needs Contents read/write scope"
            404  -> "Repo not found: $owner/$repo"
            else -> "GitHub returned HTTP $code"
        }
    }.getOrElse { "Network error: ${it.message}" }

    /** Returns (sha, rawBytes) or null if path doesn't exist / on error */
    fun getFile(relPath: String): Pair<String, ByteArray>? = runCatching {
        val c = conn(relPath, "GET")
        if (c.responseCode != 200) return null
        val obj  = gson.fromJson(c.inputStream.bufferedReader().readText(), JsonObject::class.java)
        val sha  = obj["sha"].asString
        val data = Base64.decode(obj["content"].asString.replace("\n", ""), Base64.DEFAULT)
        sha to data
    }.getOrNull()

    /** Cheaper than getFile — only fetches SHA. Null if not found. */
    fun getFileSha(relPath: String): String? = runCatching {
        val c = conn(relPath, "GET")
        if (c.responseCode != 200) return null
        gson.fromJson(c.inputStream.bufferedReader().readText(), JsonObject::class.java)["sha"].asString
    }.getOrNull()

    /** Create (sha=null) or update (sha=existing SHA) a file. Returns true on success. */
    fun putFile(relPath: String, bytes: ByteArray, sha: String?, message: String): Boolean =
        runCatching {
            val c = conn(relPath, "PUT").also { it.doOutput = true }
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val body = buildString {
                append("""{"message":${gson.toJson(message)},"content":"$b64"""")
                if (sha != null) append(""","sha":"$sha"""")
                append("}")
            }
            c.outputStream.bufferedWriter().use { it.write(body) }
            c.responseCode in 200..201
        }.getOrElse { false }

    /** List files in a sub-directory under FOLDER. Returns (name, sha) pairs. */
    fun listDir(subdir: String): List<Pair<String, String>> = runCatching {
        val c = conn(subdir, "GET")
        if (c.responseCode != 200) return emptyList()
        gson.fromJson(c.inputStream.bufferedReader().readText(), JsonArray::class.java)
            .mapNotNull {
                val o = it.asJsonObject
                (o["name"]?.asString ?: return@mapNotNull null) to
                (o["sha"]?.asString  ?: return@mapNotNull null)
            }
    }.getOrElse { emptyList() }
}
