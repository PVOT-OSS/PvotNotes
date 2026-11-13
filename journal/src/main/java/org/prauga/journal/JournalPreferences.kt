// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.prauga.journal.feed.Grouping

internal class JournalPreferences(private val prefs: SharedPreferences) {
    suspend fun grouping(): Grouping =
        withContext(Dispatchers.IO) {
            val name = prefs.getString(KEY_GROUPING, null)
            Grouping.entries.firstOrNull { it.name == name } ?: Grouping.DAY
        }

    suspend fun setGrouping(grouping: Grouping) =
        withContext(Dispatchers.IO) { prefs.edit { putString(KEY_GROUPING, grouping.name) } }

    private companion object {
        const val KEY_GROUPING = "grouping"
    }
}
