// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.alarm

import java.time.Instant
import java.time.ZoneId
import org.prauga.notes.core.model.Entry
import org.prauga.reminders.reminderBase

/** A reminder of [taskId] at [at]; [armedAt] is when the task was last changed. */
data class Trigger(val taskId: Long, val at: Instant, val armedAt: Instant = Instant.EPOCH)

/** Every reminder of every open task, earliest first. */
fun triggers(tasks: List<Entry>, zone: ZoneId): List<Trigger> =
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
 * Reminders that came due in (after, upTo]: what to notify when an alarm fires. A reminder only
 * counts if its task was set up before it came due, so editing a task into the past stays quiet.
 */
fun dueBetween(triggers: List<Trigger>, after: Instant, upTo: Instant): List<Trigger> =
    triggers
        .filter { it.at > after && it.at <= upTo && it.armedAt < it.at }
        .distinctBy { it.taskId }

/** The next reminder strictly after [after]: what to schedule. */
fun nextAfter(triggers: List<Trigger>, after: Instant): Trigger? =
    triggers.firstOrNull { it.at > after }
