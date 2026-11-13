// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.editor

internal sealed interface NoteEditorEvent {
    data class SetTitle(val title: String) : NoteEditorEvent

    data class SetBody(val body: String) : NoteEditorEvent

    data class AddCategory(val category: String) : NoteEditorEvent

    data class RemoveCategory(val category: String) : NoteEditorEvent

    data class ToggleTask(val line: Int) : NoteEditorEvent

    data class SetEditing(val editing: Boolean) : NoteEditorEvent

    data class SetCollection(val collectionId: Long) : NoteEditorEvent

    data class SetColor(val color: Int?) : NoteEditorEvent

    data object Delete : NoteEditorEvent
}
