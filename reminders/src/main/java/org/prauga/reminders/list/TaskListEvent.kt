// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.list

internal sealed interface TaskListEvent {
    data class SetDone(val id: Long, val done: Boolean) : TaskListEvent

    data class QuickAdd(val text: String) : TaskListEvent

    data object ToggleCompleted : TaskListEvent

    data object Retry : TaskListEvent
}
