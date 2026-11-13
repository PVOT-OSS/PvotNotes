// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.prauga.notes.core.model.EntryTime

val ALL_DAY_REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

fun EntryTime.localDate(zone: ZoneId): LocalDate =
    when (this) {
        is EntryTime.Date -> date
        is EntryTime.DateTime ->
            this.zone?.let { dateTime.atZone(it).withZoneSameInstant(zone).toLocalDate() }
                ?: dateTime.toLocalDate()
    }

/** The instant a task is due; an all-day task is due at the end of its day. */
fun EntryTime.dueInstant(zone: ZoneId): Instant =
    when (this) {
        is EntryTime.Date -> date.plusDays(1).atStartOfDay(zone).toInstant()
        is EntryTime.DateTime -> dateTime.atZone(this.zone ?: zone).toInstant()
    }

fun EntryTime.reminderBase(zone: ZoneId): Instant =
    when (this) {
        is EntryTime.Date -> date.atTime(ALL_DAY_REMINDER_TIME).atZone(zone).toInstant()
        is EntryTime.DateTime -> dateTime.atZone(this.zone ?: zone).toInstant()
    }
