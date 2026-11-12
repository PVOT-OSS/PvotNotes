// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.ical

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.model.EntryTime
import org.prauga.notes.core.model.Status

class TaskCompletionTest {
    private val now = Instant.parse("2026-11-13T10:00:00Z")
    private val task =
        Entry(
            uid = "t",
            component = Component.VTODO,
            collectionId = 1,
            created = now,
            lastModified = now,
        )

    @Test
    fun plainTaskIsCompleted() {
        val done = TaskCompletion.complete(task, now)
        assertEquals(Status.COMPLETED, done.status)
        assertEquals(now, done.completed)
        assertEquals(100, done.percentComplete)
        val open = TaskCompletion.reopen(done)
        assertEquals(Status.NEEDS_ACTION, open.status)
        assertNull(open.completed)
    }

    @Test
    fun repeatingTaskMovesToItsNextDate() {
        val weekly =
            task.copy(
                rrule = "FREQ=WEEKLY",
                start = EntryTime.Date(LocalDate.of(2026, 11, 12)),
                due = EntryTime.Date(LocalDate.of(2026, 11, 13)),
            )
        val next = TaskCompletion.complete(weekly, now)
        assertEquals(EntryTime.Date(LocalDate.of(2026, 11, 20)), next.due)
        assertEquals(EntryTime.Date(LocalDate.of(2026, 11, 19)), next.start)
        assertEquals(Status.NEEDS_ACTION, next.status)
        assertNull(next.completed)
    }

    @Test
    fun repeatingTaskWithoutADateIsJustCompleted() {
        assertEquals(
            Status.COMPLETED,
            TaskCompletion.complete(task.copy(rrule = "FREQ=DAILY"), now).status,
        )
    }
}
