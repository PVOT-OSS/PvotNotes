// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.alarm

import android.content.BroadcastReceiver
import android.content.Context
import kotlinx.coroutines.launch
import org.prauga.reminders.appContainer

internal fun BroadcastReceiver.work(context: Context, block: suspend () -> Unit) {
    val pending = goAsync()
    context.appContainer.applicationScope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}
