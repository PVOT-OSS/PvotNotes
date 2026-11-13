// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.ical

import java.time.Instant
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.model.Status

object TaskCompletion {
    /**
     * A repeating task with a date moves to its next occurrence and stays open instead of
     * completing.
     */
    fun complete(task: Entry, now: Instant): Entry {
        val rule = task.rrule
        val anchor = task.due ?: task.start
        if (rule != null && anchor != null) {
            val nextDue = task.due?.let { Recurrence.next(rule, it) }
            val nextStart = task.start?.let { Recurrence.next(rule, it) }
            if (nextDue != null || (task.due == null && nextStart != null)) {
                return task.copy(
                    due = nextDue ?: task.due,
                    start = nextStart ?: task.start,
                    status = Status.NEEDS_ACTION,
                    completed = null,
                    percentComplete = null,
                )
            }
        }
        return task.copy(status = Status.COMPLETED, completed = now, percentComplete = 100)
    }

    fun reopen(task: Entry): Entry =
        task.copy(status = Status.NEEDS_ACTION, completed = null, percentComplete = null)
}
