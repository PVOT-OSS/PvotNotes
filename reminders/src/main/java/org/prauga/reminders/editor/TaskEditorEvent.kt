// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.editor

import java.time.LocalDate
import java.time.LocalTime
import org.prauga.reminders.list.Priority

internal sealed interface TaskEditorEvent {
    data class SetTitle(val title: String) : TaskEditorEvent

    data class SetNotes(val notes: String) : TaskEditorEvent

    data class SetDate(val date: LocalDate?) : TaskEditorEvent

    data class SetTime(val time: LocalTime?) : TaskEditorEvent

    data class SetRepeat(val rrule: String?) : TaskEditorEvent

    data class AddAlarm(val minutesBefore: Int) : TaskEditorEvent

    data class RemoveAlarm(val minutesBefore: Int) : TaskEditorEvent

    data class SetPriority(val priority: Priority) : TaskEditorEvent

    data class AddSubtask(val title: String) : TaskEditorEvent

    data class SetSubtaskDone(val id: Long, val done: Boolean) : TaskEditorEvent

    data class RemoveSubtask(val id: Long) : TaskEditorEvent

    data object Delete : TaskEditorEvent
}
