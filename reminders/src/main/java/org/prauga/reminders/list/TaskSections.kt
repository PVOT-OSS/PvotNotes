// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.list

import java.time.Instant
import java.time.ZoneId
import org.prauga.notes.core.model.Entry
import org.prauga.reminders.dueInstant
import org.prauga.reminders.localDate

internal fun sectionOf(task: Entry, now: Instant, zone: ZoneId): Section {
    if (task.completed != null) return Section.COMPLETED
    val due = task.due ?: return Section.NO_DATE
    val today = now.atZone(zone).toLocalDate()
    return when {
        due.dueInstant(zone) <= now -> Section.OVERDUE
        due.localDate(zone) == today -> Section.TODAY
        else -> Section.UPCOMING
    }
}

internal fun groupTasks(
    tasks: List<Entry>,
    now: Instant,
    zone: ZoneId,
): List<Pair<Section, List<Entry>>> =
    tasks
        .groupBy { sectionOf(it, now, zone) }
        .toSortedMap()
        .map { (section, list) ->
            section to
                when (section) {
                    Section.COMPLETED -> list.sortedByDescending { it.completed }
                    Section.NO_DATE ->
                        list.sortedWith(compareBy({ priorityRank(it) }, { it.created }))
                    else ->
                        list.sortedWith(
                            compareBy({ it.due?.dueInstant(zone) }, { priorityRank(it) })
                        )
                }
        }

private fun priorityRank(task: Entry): Int = task.priority?.takeIf { it > 0 } ?: 10
