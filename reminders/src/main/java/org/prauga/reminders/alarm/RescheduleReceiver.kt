// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.prauga.reminders.appContainer

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        work(context) { context.appContainer.scheduler.sync() }
    }
}
