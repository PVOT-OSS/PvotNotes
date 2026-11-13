// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.prauga.notes.core.model.Attachment

/** An attachment's uri is its path relative to the app's files directory. */
class AttachmentStore(private val context: Context) {
    private val dir = File(context.filesDir, DIR)

    suspend fun import(uri: Uri): Attachment? =
        withContext(Dispatchers.IO) {
            runCatching {
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val extension =
                        MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
                    dir.mkdirs()
                    val name = "${UUID.randomUUID()}.$extension"
                    context.contentResolver.openInputStream(uri)!!.use { input ->
                        File(dir, name).outputStream().use { input.copyTo(it) }
                    }
                    Attachment(uri = "$DIR/$name", mimeType = mime)
                }
                .getOrNull()
        }

    fun file(attachment: Attachment): File? =
        attachment.uri
            .takeIf { it.startsWith("$DIR/") && !it.contains("..") }
            ?.let { File(context.filesDir, it) }

    suspend fun delete(attachments: List<Attachment>) =
        withContext(Dispatchers.IO) { attachments.forEach { file(it)?.delete() } }

    private companion object {
        const val DIR = "attachments"
    }
}
