package com.school.manager.util

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Copies an image from a content URI (gallery pick) into the app's
 * private files directory so the path stays valid after app restart.
 * Returns the absolute file path, or null on error.
 */
fun copyImageToAppStorage(context: Context, uri: Uri, entityId: Long): String? {
    return try {
        val dir = File(context.filesDir, "avatars").also { it.mkdirs() }
        val dest = File(dir, "avatar_$entityId.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        dest.absolutePath
    } catch (_: Exception) { null }
}
