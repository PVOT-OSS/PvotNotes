// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.ical

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.notes.core.model.Alarm
import org.prauga.notes.core.model.Attachment
import org.prauga.notes.core.model.Classification
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.model.EntryTime
import org.prauga.notes.core.model.Status

class ICalMapperTest {
    private val created = Instant.parse("2026-11-03T10:15:30Z")
    private val modified = Instant.parse("2026-11-03T11:00:00Z")

    private fun entry(component: Component) =
        Entry(
            uid = "uid-1",
            component = component,
            collectionId = 1,
            created = created,
            lastModified = modified,
        )

    private fun roundTrip(entry: Entry): Entry =
        ICalMapper.fromICalendar(ICalMapper.toICalendar(listOf(entry)), 1).single()

    @Test
    fun noteRoundTrips() {
        val note =
            entry(Component.VJOURNAL)
                .copy(
                    summary = "Groceries",
                    description = "- milk\n- eggs, bread; butter",
                    categories = listOf("home", "shopping"),
                    classification = Classification.PRIVATE,
                    color = 0xFFFF6347.toInt(),
                    sequence = 2,
                )
        assertEquals(note, roundTrip(note))
    }

    @Test
    fun journalWithZonedStartRoundTrips() {
        val zone = ZoneId.of("Asia/Kolkata")
        val journal =
            entry(Component.VJOURNAL)
                .copy(
                    summary = "Day",
                    start = EntryTime.DateTime(LocalDateTime.of(2026, 11, 3, 21, 30), zone),
                    status = Status.FINAL,
                )
        val text = ICalMapper.toICalendar(listOf(journal))
        assertTrue(text, text.contains("DTSTART;TZID=Asia/Kolkata:20261103T213000"))
        assertTrue(text, text.contains("BEGIN:VTIMEZONE\r\nTZID:Asia/Kolkata"))
        assertEquals(journal, roundTrip(journal))
    }

    @Test
    fun allDayAndFloatingAndUtcTimesRoundTrip() {
        listOf(
                EntryTime.Date(LocalDate.of(2026, 11, 4)),
                EntryTime.DateTime(LocalDateTime.of(2026, 11, 4, 9, 0), null),
                EntryTime.DateTime(LocalDateTime.of(2026, 11, 4, 9, 0), ZoneOffset.UTC),
            )
            .forEach { time ->
                val journal = entry(Component.VJOURNAL).copy(start = time)
                assertEquals(journal, roundTrip(journal))
            }
    }

    @Test
    fun taskRoundTrips() {
        val task =
            entry(Component.VTODO)
                .copy(
                    summary = "Pay rent",
                    due = EntryTime.Date(LocalDate.of(2026, 12, 1)),
                    completed = Instant.parse("2026-11-30T08:00:00Z"),
                    status = Status.COMPLETED,
                    priority = 1,
                    percentComplete = 100,
                )
        assertEquals(task, roundTrip(task))
    }

    @Test
    fun readsForeignCalendarAndSkipsUnknownComponents() {
        val text =
            """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Example//Other//EN
            BEGIN:VEVENT
            UID:event-1
            DTSTAMP:20261103T100000Z
            DTSTART:20261103T100000Z
            END:VEVENT
            BEGIN:VJOURNAL
            UID:journal-1
            DTSTAMP:20261103T100000Z
            SUMMARY:Meeting minutes
            CATEGORIES:work,meetings
            CATEGORIES:q4
            X-UNKNOWN:kept later
            END:VJOURNAL
            END:VCALENDAR
            """
                .trimIndent()
                .replace("\n", "\r\n") + "\r\n"
        val entry = ICalMapper.fromICalendar(text, 7).single()
        assertEquals("journal-1", entry.uid)
        assertEquals("Meeting minutes", entry.summary)
        assertEquals(listOf("work", "meetings", "q4"), entry.categories)
        assertEquals(7L, entry.collectionId)
        assertEquals(Instant.parse("2026-11-03T10:00:00Z"), entry.created)
    }

    @Test
    fun writesColourAsNearestCss3NameAndReadsNames() {
        val note = entry(Component.VJOURNAL).copy(summary = "x", color = 0xFFFE6448.toInt())
        val text = ICalMapper.toICalendar(listOf(note))
        assertTrue(text, text.contains("COLOR:tomato"))
        assertEquals(0xFFFF6347.toInt(), ICalMapper.fromICalendar(text, 1).single().color)

        val foreign = text.replace("COLOR:tomato", "COLOR:Not-A-Colour")
        assertEquals(null, ICalMapper.fromICalendar(foreign, 1).single().color)
    }

    @Test
    fun taskWithRepeatParentAndAlarmsRoundTrips() {
        val task =
            entry(Component.VTODO)
                .copy(
                    summary = "Water plants",
                    due =
                        EntryTime.DateTime(
                            LocalDateTime.of(2026, 11, 14, 18, 0),
                            ZoneId.of("Asia/Kolkata"),
                        ),
                    rrule = "FREQ=WEEKLY;BYDAY=SA",
                    parentUid = "parent-1",
                    alarms = listOf(Alarm(0), Alarm(15), Alarm(24 * 60)),
                )
        val text = ICalMapper.toICalendar(listOf(task))
        assertTrue(text, text.contains("RRULE:FREQ=WEEKLY;BYDAY=SA"))
        assertTrue(text, text.contains("RELATED-TO:parent-1"))
        assertTrue(text, text.contains("TRIGGER;RELATED=END:-PT15M"))
        assertTrue(text, text.contains("BEGIN:VALARM"))
        assertEquals(task, roundTrip(task))
    }

    @Test
    fun journalAttachmentsRoundTrip() {
        val journal =
            entry(Component.VJOURNAL)
                .copy(
                    attachments =
                        listOf(Attachment("attachments/a.jpg"), Attachment("attachments/b.png"))
                )
        assertEquals(journal, roundTrip(journal))
    }
}
