// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.prauga.notes.core.data.EntryDatabase
import org.prauga.notes.core.data.EntryRepository

internal class AppContainer(context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val defaultCollectionName: String = context.getString(R.string.default_collection)
    val repository: EntryRepository by lazy {
        EntryRepository(EntryDatabase.create(context, "journal.db"))
    }
    val attachments = AttachmentStore(context.filesDir, context.contentResolver)
    val preferences =
        JournalPreferences(context.getSharedPreferences("journal", Context.MODE_PRIVATE))
}
