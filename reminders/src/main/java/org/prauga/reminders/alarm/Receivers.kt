// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import org.prauga.reminders.MainActivity
import org.prauga.reminders.appContainer

/** Runs [block] on the app scope while keeping the broadcast alive until it finishes. */
private fun BroadcastReceiver.work(context: Context, block: suspend () -> Unit) {
    val pending = goAsync()
    context.appContainer.applicationScope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = context.appContainer
        val snoozed = intent.getLongExtra(EXTRA_TASK_ID, -1)
        work(context) {
            if (snoozed >= 0) {
                container.repository
                    .getEntry(snoozed)
                    ?.takeIf { it.completed == null }
                    ?.let(container.notifier::notify)
                container.scheduler.clearSnooze(snoozed)
            } else {
                container.scheduler.sync()
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = context.appContainer
        val taskId =
            intent.getLongExtra(MainActivity.EXTRA_TASK_ID, -1).takeIf { it >= 0 } ?: return
        container.notifier.cancel(taskId)
        work(context) {
            when (intent.action) {
                ACTION_DONE -> {
                    container.scheduler.clearSnooze(taskId)
                    container.repository.setCompleted(taskId, done = true)
                }
                ACTION_SNOOZE -> container.scheduler.snooze(taskId)
            }
            container.scheduler.sync()
        }
    }

    companion object {
        const val ACTION_DONE = "org.prauga.reminders.action.DONE"
        const val ACTION_SNOOZE = "org.prauga.reminders.action.SNOOZE"
    }
}

/** Boot, app updates, clock and time zone changes, and exact-alarm permission changes. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        work(context) { context.appContainer.scheduler.sync() }
    }
}
