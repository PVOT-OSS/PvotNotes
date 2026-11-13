// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.list

internal sealed interface NoteListEvent {
    data class QuickAdd(val text: String) : NoteListEvent

    data class SetQuery(val query: String) : NoteListEvent

    data class SetSearching(val searching: Boolean) : NoteListEvent

    data class SelectCollection(val collectionId: Long?) : NoteListEvent

    data class SelectCategory(val category: String?) : NoteListEvent

    data object ToggleLayout : NoteListEvent

    data object Retry : NoteListEvent
}
