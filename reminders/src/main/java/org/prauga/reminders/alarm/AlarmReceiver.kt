// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.prauga.reminders.appContainer

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
