// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.ical

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.prauga.notes.core.model.EntryTime

class RecurrenceTest {
    @Test
    fun nextDailyDate() {
        assertEquals(
            EntryTime.Date(LocalDate.of(2026, 11, 14)),
            Recurrence.next("FREQ=DAILY", EntryTime.Date(LocalDate.of(2026, 11, 13))),
        )
    }

    @Test
    fun nextWeekdaySkipsTheWeekend() {
        val friday =
            EntryTime.DateTime(LocalDateTime.of(2026, 11, 13, 9, 0), ZoneId.of("Asia/Kolkata"))
        assertEquals(
            EntryTime.DateTime(LocalDateTime.of(2026, 11, 16, 9, 0), ZoneId.of("Asia/Kolkata")),
            Recurrence.next("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", friday),
        )
    }

    @Test
    fun nextMonthlyAndYearly() {
        val start = EntryTime.Date(LocalDate.of(2026, 1, 31))
        assertEquals(
            EntryTime.Date(LocalDate.of(2026, 3, 31)),
            Recurrence.next("FREQ=MONTHLY", start),
        )
        assertEquals(
            EntryTime.Date(LocalDate.of(2027, 1, 31)),
            Recurrence.next("FREQ=YEARLY", start),
        )
    }

    @Test
    fun badRuleHasNoNextOccurrence() {
        assertNull(Recurrence.next("NOT A RULE", EntryTime.Date(LocalDate.of(2026, 1, 1))))
    }

    @Test
    fun durationsBothWays() {
        assertEquals(-15, Recurrence.durationMinutes("-PT15M"))
        assertEquals(-1440, Recurrence.durationMinutes("-P1D"))
        assertEquals(-10080, Recurrence.durationMinutes("-P1W"))
        assertEquals(90, Recurrence.durationMinutes("PT1H30M"))
        assertNull(Recurrence.durationMinutes("soon"))
        listOf(0, -15, -60, -1440, -10080, -90).forEach {
            assertEquals(it, Recurrence.durationMinutes(Recurrence.duration(it)))
        }
    }
}
