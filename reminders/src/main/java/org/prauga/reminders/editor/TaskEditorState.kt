// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.editor

import java.time.Instant
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.model.EntryTime
import org.prauga.reminders.list.Priority

internal data class TaskEditorState(
    val title: String,
    val notes: String,
    val due: EntryTime?,
    val rrule: String?,
    val alarms: List<Int>,
    val priority: Priority,
    val subtasks: List<Entry> = emptyList(),
    val lastModified: Instant?,
    val saved: Boolean,
    val focusTitle: Boolean,
)
