// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.prauga.reminders.MainActivity
import org.prauga.reminders.appContainer

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
