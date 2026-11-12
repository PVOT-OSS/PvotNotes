// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.prauga.journal.feed.Grouping
import org.prauga.journal.feed.Period
import org.prauga.journal.feed.moment
import org.prauga.journal.feed.timeline
import org.prauga.journal.feed.weekDays
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.model.EntryTime

class TimelineTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    private fun entry(id: Long, at: LocalDateTime) =
        Entry(
            id = id,
            uid = "u$id",
            component = Component.VJOURNAL,
            collectionId = 1,
            summary = "e$id",
            start = EntryTime.DateTime(at, zone),
            created = Instant.EPOCH,
            lastModified = Instant.EPOCH,
        )

    private val entries =
        listOf(
            entry(1, LocalDateTime.of(2026, 11, 13, 9, 0)),
            entry(2, LocalDateTime.of(2026, 11, 13, 21, 30)),
            entry(3, LocalDateTime.of(2026, 11, 9, 8, 0)),
            entry(4, LocalDateTime.of(2026, 10, 31, 23, 0)),
        )

    private fun titles(grouping: Grouping, locale: Locale) =
        timeline(entries, grouping, zone, locale).map { (period, list) ->
            period to list.map { it.summary }
        }

    @Test
    fun groupsByDayNewestFirst() {
        assertEquals(
            listOf(
                Period(Grouping.DAY, LocalDate.of(2026, 11, 13)) to listOf("e2", "e1"),
                Period(Grouping.DAY, LocalDate.of(2026, 11, 9)) to listOf("e3"),
                Period(Grouping.DAY, LocalDate.of(2026, 10, 31)) to listOf("e4"),
            ),
            titles(Grouping.DAY, Locale.UK),
        )
    }

    @Test
    fun weeksFollowTheLocale() {
        assertEquals(
            listOf(
                Period(Grouping.WEEK, LocalDate.of(2026, 11, 9)) to listOf("e2", "e1", "e3"),
                Period(Grouping.WEEK, LocalDate.of(2026, 10, 26)) to listOf("e4"),
            ),
            titles(Grouping.WEEK, Locale.UK),
        )
        assertEquals(
            listOf(
                Period(Grouping.WEEK, LocalDate.of(2026, 11, 8)) to listOf("e2", "e1", "e3"),
                Period(Grouping.WEEK, LocalDate.of(2026, 10, 25)) to listOf("e4"),
            ),
            titles(Grouping.WEEK, Locale.US),
        )
        assertEquals(
            LocalDate.of(2026, 11, 9),
            weekDays(LocalDate.of(2026, 11, 13), Locale.UK).first(),
        )
        assertEquals(
            LocalDate.of(2026, 11, 15),
            weekDays(LocalDate.of(2026, 11, 13), Locale.UK).last(),
        )
    }

    @Test
    fun groupsByMonth() {
        assertEquals(
            listOf(
                Period(Grouping.MONTH, LocalDate.of(2026, 11, 1)) to listOf("e2", "e1", "e3"),
                Period(Grouping.MONTH, LocalDate.of(2026, 10, 1)) to listOf("e4"),
            ),
            titles(Grouping.MONTH, Locale.UK),
        )
    }

    @Test
    fun convertsToTheViewersZone() {
        val utcLate =
            entries[3].copy(
                start = EntryTime.DateTime(LocalDateTime.of(2026, 10, 31, 20, 0), ZoneOffset.UTC)
            )
        assertEquals(LocalDateTime.of(2026, 11, 1, 1, 30), utcLate.moment(zone))
        val undated = entries[0].copy(start = null, created = Instant.parse("2026-11-12T20:00:00Z"))
        assertEquals(LocalDateTime.of(2026, 11, 13, 1, 30), undated.moment(zone))
    }
}
