// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.Entry
import org.prauga.reminders.alarm.SnoozeAction
import org.prauga.reminders.alarm.snoozeAction

class SnoozeTest {
    private val now = Instant.parse("2026-11-14T10:00:00Z")
    private val open =
        Entry(
            id = 1,
            uid = "u1",
            component = Component.VTODO,
            collectionId = 1,
            summary = "Call",
            created = now,
            lastModified = now,
        )

    @Test
    fun `a snooze still ahead is re-armed after a reboot or force-stop`() {
        assertEquals(SnoozeAction.REARM, snoozeAction(open, now.plusSeconds(300), now))
    }

    @Test
    fun `a snooze that came due while alarms were cleared notifies`() {
        assertEquals(SnoozeAction.NOTIFY, snoozeAction(open, now.minusSeconds(60), now))
        assertEquals(SnoozeAction.NOTIFY, snoozeAction(open, now, now))
    }

    @Test
    fun `a snooze for a completed or deleted task is dropped`() {
        assertEquals(SnoozeAction.DROP, snoozeAction(open.copy(completed = now), now, now))
        assertEquals(SnoozeAction.DROP, snoozeAction(null, now.plusSeconds(300), now))
    }
}
