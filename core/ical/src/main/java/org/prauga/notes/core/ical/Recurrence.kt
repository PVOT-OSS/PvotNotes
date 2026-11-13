// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.ical

import java.time.LocalDate
import java.time.LocalDateTime
import net.fortuna.ical4j.model.Recur
import org.prauga.notes.core.model.EntryTime

object Recurrence {
    /** The first occurrence of [rrule] after [time], counting [time] as the series start. */
    fun next(rrule: String, time: EntryTime): EntryTime? =
        runCatching {
                when (time) {
                    is EntryTime.Date -> {
                        val recur = Recur<LocalDate>(rrule)
                        recur
                            .getDates(time.date, time.date.plusDays(1), time.date.plusYears(100), 1)
                            .firstOrNull()
                            ?.let { EntryTime.Date(it) }
                    }
                    is EntryTime.DateTime -> {
                        val recur = Recur<LocalDateTime>(rrule)
                        recur
                            .getDates(
                                time.dateTime,
                                time.dateTime.plusSeconds(1),
                                time.dateTime.plusYears(100),
                                1,
                            )
                            .firstOrNull()
                            ?.let { EntryTime.DateTime(it, time.zone) }
                    }
                }
            }
            .getOrNull()

    /** Signed minutes of an RFC 5545 duration (`-PT15M` is -15); positive is after. */
    fun durationMinutes(value: String): Int? {
        val match = DURATION.matchEntire(value.trim()) ?: return null
        val (sign, weeks, days, hours, minutes, seconds) = match.destructured
        val total =
            (weeks.toLongOrNull() ?: 0) * 7 * 24 * 60 +
                (days.toLongOrNull() ?: 0) * 24 * 60 +
                (hours.toLongOrNull() ?: 0) * 60 +
                (minutes.toLongOrNull() ?: 0) +
                (seconds.toLongOrNull() ?: 0) / 60
        return (if (sign == "-") -total else total).toInt()
    }

    fun duration(minutes: Int): String {
        val sign = if (minutes < 0) "-" else ""
        val m = kotlin.math.abs(minutes)
        return when {
            m == 0 -> "PT0S"
            m % (7 * 24 * 60) == 0 -> "${sign}P${m / (7 * 24 * 60)}W"
            m % (24 * 60) == 0 -> "${sign}P${m / (24 * 60)}D"
            m % 60 == 0 -> "${sign}PT${m / 60}H"
            else -> "${sign}PT${m}M"
        }
    }

    private val DURATION =
        Regex("([+-]?)P(?:(\\d+)W)?(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?")
}
