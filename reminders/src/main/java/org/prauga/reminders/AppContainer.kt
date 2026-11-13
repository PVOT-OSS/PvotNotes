// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.prauga.notes.core.data.EntryDatabase
import org.prauga.notes.core.data.EntryRepository
import org.prauga.reminders.alarm.ReminderNotifier
import org.prauga.reminders.alarm.ReminderScheduler

internal class AppContainer(context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val defaultCollectionName: String = context.getString(R.string.default_collection)
    val repository: EntryRepository by lazy {
        EntryRepository(EntryDatabase.create(context, "reminders.db"))
    }
    val notifier: ReminderNotifier by lazy { ReminderNotifier(context) }
    val scheduler: ReminderScheduler by lazy { ReminderScheduler(context, repository, notifier) }
}
