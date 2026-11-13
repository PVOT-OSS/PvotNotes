// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.prauga.notes.core.model.Attachment
import timber.log.Timber

/** An attachment's uri is its path relative to the app's files directory. */
internal class AttachmentStore(private val filesDir: File, private val resolver: ContentResolver) {
    private val dir = File(filesDir, DIR)

    suspend fun import(uri: Uri): Attachment? =
        withContext(Dispatchers.IO) {
            val mime = resolver.getType(uri) ?: "image/jpeg"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
            val name = "${UUID.randomUUID()}.$extension"
            val target = File(dir, name)
            try {
                dir.mkdirs()
                val input = resolver.openInputStream(uri) ?: return@withContext null
                input.use { stream -> target.outputStream().use { stream.copyTo(it) } }
                Attachment(uri = "$DIR/$name", mimeType = mime)
            } catch (e: IOException) {
                Timber.w(e, "Couldn't copy a picked image")
                target.delete()
                null
            } catch (e: SecurityException) {
                Timber.w(e, "No access to a picked image")
                target.delete()
                null
            }
        }

    fun file(attachment: Attachment): File? =
        attachment.uri
            .takeIf { it.startsWith("$DIR/") && !it.contains("..") }
            ?.let { File(filesDir, it) }

    suspend fun delete(attachments: List<Attachment>) =
        withContext(Dispatchers.IO) { attachments.forEach { file(it)?.delete() } }

    private companion object {
        const val DIR = "attachments"
    }
}
