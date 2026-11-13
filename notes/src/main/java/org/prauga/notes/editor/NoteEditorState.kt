// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.editor

import java.time.Instant
import org.prauga.notes.core.model.EntryCollection

internal data class NoteEditorState(
    val title: String,
    val body: String,
    val categories: List<String>,
    val color: Int?,
    val collectionId: Long,
    val collections: List<EntryCollection>,
    val lastModified: Instant?,
    val saved: Boolean,
    val focusTitle: Boolean,
    val editing: Boolean,
)
