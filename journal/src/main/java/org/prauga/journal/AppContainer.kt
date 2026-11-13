// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.prauga.journal.feed.Grouping
import org.prauga.notes.core.data.EntryDatabase
import org.prauga.notes.core.data.EntryRepository

class AppContainer(context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val defaultCollectionName: String = context.getString(R.string.default_collection)
    val repository: EntryRepository by lazy {
        EntryRepository(EntryDatabase.create(context, "journal.db"))
    }
    val attachments = AttachmentStore(context)
    private val prefs = context.getSharedPreferences("journal", Context.MODE_PRIVATE)

    var grouping: Grouping
        get() =
            prefs.getString(KEY_GROUPING, null)?.let {
                Grouping.entries.firstOrNull { grouping -> grouping.name == it }
            } ?: Grouping.DAY
        set(value) = prefs.edit { putString(KEY_GROUPING, value.name) }

    private companion object {
        const val KEY_GROUPING = "grouping"
    }
}
