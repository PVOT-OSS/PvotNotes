// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.alarm

import java.time.Instant
import java.time.ZoneId
import org.prauga.notes.core.model.Entry
import org.prauga.reminders.reminderBase

/** Every reminder of every open task, earliest first. */
internal fun triggers(tasks: List<Entry>, zone: ZoneId): List<Trigger> =
    tasks
        .filter { it.completed == null }
        .flatMap { task ->
            val base = (task.due ?: task.start)?.reminderBase(zone) ?: return@flatMap emptyList()
            task.alarms.map {
                Trigger(task.id, base.minusSeconds(it.minutesBefore * 60L), task.lastModified)
            }
        }
        .sortedBy { it.at }

/**
 * Only reminders set up before they came due count, so editing a task into the past stays quiet.
 */
internal fun dueBetween(triggers: List<Trigger>, after: Instant, upTo: Instant): List<Trigger> =
    triggers
        .filter { it.at > after && it.at <= upTo && it.armedAt < it.at }
        .distinctBy { it.taskId }

internal fun nextAfter(triggers: List<Trigger>, after: Instant): Trigger? =
    triggers.firstOrNull { it.at > after }
