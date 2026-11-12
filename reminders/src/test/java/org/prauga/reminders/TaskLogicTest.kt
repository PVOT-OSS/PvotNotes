// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.prauga.notes.core.model.Alarm
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.model.EntryTime
import org.prauga.reminders.alarm.Trigger
import org.prauga.reminders.alarm.dueBetween
import org.prauga.reminders.alarm.nextAfter
import org.prauga.reminders.alarm.triggers
import org.prauga.reminders.list.DayWords
import org.prauga.reminders.list.Priority
import org.prauga.reminders.list.Section
import org.prauga.reminders.list.dueLabel
import org.prauga.reminders.list.groupTasks

class TaskLogicTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = LocalDateTime.of(2026, 11, 13, 12, 0).atZone(zone).toInstant()
    private val today = LocalDate.of(2026, 11, 13)

    private fun task(id: Long, title: String, due: EntryTime? = null, priority: Int? = null) =
        Entry(
            id = id,
            uid = "u$id",
            component = Component.VTODO,
            collectionId = 1,
            summary = title,
            due = due,
            priority = priority,
            created = Instant.EPOCH.plusSeconds(id),
            lastModified = Instant.EPOCH,
        )

    private fun at(h: Int, m: Int = 0, day: LocalDate = today) =
        EntryTime.DateTime(day.atTime(h, m), zone)

    @Test
    fun groupsIntoSectionsInOrder() {
        val tasks =
            listOf(
                task(1, "later today", at(18)),
                task(2, "this morning", at(9)),
                task(3, "yesterday", EntryTime.Date(today.minusDays(1))),
                task(4, "all day today", EntryTime.Date(today)),
                task(5, "next week", EntryTime.Date(today.plusDays(7))),
                task(6, "someday"),
                task(7, "done", at(8)).copy(completed = now),
                task(8, "urgent someday", priority = 1),
            )
        val grouped = groupTasks(tasks, now, zone).map { (s, l) -> s to l.map { it.summary } }
        assertEquals(
            listOf(
                Section.OVERDUE to listOf("yesterday", "this morning"),
                Section.TODAY to listOf("later today", "all day today"),
                Section.UPCOMING to listOf("next week"),
                Section.NO_DATE to listOf("urgent someday", "someday"),
                Section.COMPLETED to listOf("done"),
            ),
            grouped,
        )
    }

    @Test
    fun prioritiesMapToICalendarValues() {
        assertEquals(Priority.HIGH, Priority.of(1))
        assertEquals(Priority.HIGH, Priority.of(4))
        assertEquals(Priority.MEDIUM, Priority.of(5))
        assertEquals(Priority.LOW, Priority.of(9))
        assertEquals(Priority.NONE, Priority.of(0))
        assertEquals(Priority.NONE, Priority.of(null))
    }

    @Test
    fun labelsDueDates() {
        val words = DayWords("Today", "Tomorrow", "Yesterday")
        val label = { due: EntryTime -> dueLabel(due, today, zone, words, Locale.UK) }
        assertEquals("Today, 18:00", label(at(18)))
        assertEquals("Tomorrow", label(EntryTime.Date(today.plusDays(1))))
        assertEquals("Yesterday", label(EntryTime.Date(today.minusDays(1))))
        assertEquals("Monday", label(EntryTime.Date(LocalDate.of(2026, 11, 16))))
        assertEquals("Fri 25 Dec", label(EntryTime.Date(LocalDate.of(2026, 12, 25))))
        assertEquals("1 Jan 2027", label(EntryTime.Date(LocalDate.of(2027, 1, 1))))
    }

    @Test
    fun computesAndPicksAlarmTriggers() {
        val tasks =
            listOf(
                task(1, "timed", at(18)).copy(alarms = listOf(Alarm(0), Alarm(60))),
                task(2, "all day", EntryTime.Date(today.plusDays(1)))
                    .copy(alarms = listOf(Alarm(0))),
                task(3, "done", at(15)).copy(alarms = listOf(Alarm(0)), completed = now),
                task(4, "no date").copy(alarms = listOf(Alarm(0))),
            )
        val all = triggers(tasks, zone)
        val instant = { h: Int, day: LocalDate -> day.atTime(h, 0).atZone(zone).toInstant() }
        assertEquals(
            listOf(
                Trigger(1, instant(17, today), Instant.EPOCH),
                Trigger(1, instant(18, today), Instant.EPOCH),
                Trigger(2, instant(9, today.plusDays(1)), Instant.EPOCH),
            ),
            all,
        )
        assertEquals(Trigger(1, instant(17, today)), nextAfter(all, now))
        assertEquals(
            listOf(Trigger(1, instant(17, today))),
            dueBetween(all, now, instant(17, today)),
        )
        assertEquals(
            listOf(1L, 2L),
            dueBetween(all, now, instant(10, today.plusDays(1))).map { it.taskId },
        )
        assertNull(nextAfter(all, instant(10, today.plusDays(1))))

        val editedLate =
            triggers(listOf(tasks[0].copy(lastModified = instant(17, today).plusSeconds(60))), zone)
        assertEquals(listOf(1L), dueBetween(editedLate, now, instant(19, today)).map { it.taskId })
        assertEquals(emptyList<Trigger>(), dueBetween(editedLate, now, instant(17, today)))
    }
}
