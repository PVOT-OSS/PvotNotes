// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal.editor

import android.net.Uri
import java.time.LocalDate
import java.time.LocalTime
import org.prauga.notes.core.model.Attachment

internal sealed interface EntryEditorEvent {
    data class SetTitle(val title: String) : EntryEditorEvent

    data class SetBody(val body: String) : EntryEditorEvent

    data class ToggleTask(val line: Int) : EntryEditorEvent

    data class SetDate(val date: LocalDate) : EntryEditorEvent

    data class SetTime(val time: LocalTime) : EntryEditorEvent

    data class SetEditing(val editing: Boolean) : EntryEditorEvent

    data class AddCategory(val category: String) : EntryEditorEvent

    data class RemoveCategory(val category: String) : EntryEditorEvent

    data class AddPhotos(val uris: List<Uri>) : EntryEditorEvent

    data class RemovePhoto(val attachment: Attachment) : EntryEditorEvent

    data object Delete : EntryEditorEvent
}
