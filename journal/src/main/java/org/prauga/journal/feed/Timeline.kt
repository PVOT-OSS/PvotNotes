// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal.feed

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.model.EntryTime

internal fun Entry.moment(zone: ZoneId): LocalDateTime =
    when (val time = start) {
        is EntryTime.Date -> time.date.atStartOfDay()
        is EntryTime.DateTime ->
            time.zone?.let { time.dateTime.atZone(it).withZoneSameInstant(zone).toLocalDateTime() }
                ?: time.dateTime
        null -> LocalDateTime.ofInstant(created, zone)
    }

internal fun firstDayOfWeek(locale: Locale): DayOfWeek = WeekFields.of(locale).firstDayOfWeek

internal fun weekStart(date: LocalDate, locale: Locale): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek(locale)))

internal fun periodOf(date: LocalDate, grouping: Grouping, locale: Locale): Period =
    when (grouping) {
        Grouping.DAY -> Period(grouping, date)
        Grouping.WEEK -> Period(grouping, weekStart(date, locale))
        Grouping.MONTH -> Period(grouping, date.withDayOfMonth(1))
    }

internal fun timeline(
    entries: List<Entry>,
    grouping: Grouping,
    zone: ZoneId,
    locale: Locale,
): List<Pair<Period, List<Entry>>> =
    entries
        .sortedByDescending { it.moment(zone) }
        .groupBy { periodOf(it.moment(zone).toLocalDate(), grouping, locale) }
        .toList()

internal fun weekDays(date: LocalDate, locale: Locale): List<LocalDate> {
    val first = weekStart(date, locale)
    return (0L..6L).map { first.plusDays(it) }
}
