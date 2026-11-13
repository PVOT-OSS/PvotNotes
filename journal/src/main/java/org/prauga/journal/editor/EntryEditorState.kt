// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal.editor

import java.time.Instant
import java.time.LocalDateTime
import org.prauga.notes.core.model.Attachment

internal data class EntryEditorState(
    val moment: LocalDateTime,
    val title: String,
    val body: String,
    val categories: List<String>,
    val attachments: List<Attachment>,
    val lastModified: Instant?,
    val saved: Boolean,
    val editing: Boolean,
    val focusBody: Boolean,
)
