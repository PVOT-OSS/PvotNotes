// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.list

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.prauga.notes.core.model.EntryTime
import org.prauga.reminders.localDate

internal fun dueLabel(
    due: EntryTime,
    today: LocalDate,
    zone: ZoneId,
    words: DayWords,
    locale: Locale,
): String {
    val date = due.localDate(zone)
    val day =
        when {
            date == today -> words.today
            date == today.plusDays(1) -> words.tomorrow
            date == today.minusDays(1) -> words.yesterday
            date > today && date < today.plusDays(7) ->
                DateTimeFormatter.ofPattern("EEEE", locale).format(date)
            date.year == today.year -> DateTimeFormatter.ofPattern("EEE d MMM", locale).format(date)
            else -> DateTimeFormatter.ofPattern("d MMM yyyy", locale).format(date)
        }
    if (due !is EntryTime.DateTime) return day
    val time =
        due.zone?.let { due.dateTime.atZone(it).withZoneSameInstant(zone).toLocalTime() }
            ?: due.dateTime.toLocalTime()
    return "$day, " +
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(time)
}
