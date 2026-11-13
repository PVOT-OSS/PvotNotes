// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.prauga.notes.list.NoteLayout

internal class NotesPreferences(private val prefs: SharedPreferences) {
    suspend fun layout(): NoteLayout =
        withContext(Dispatchers.IO) {
            val name = prefs.getString(KEY_LAYOUT, null)
            NoteLayout.entries.firstOrNull { it.name == name } ?: NoteLayout.GRID
        }

    suspend fun setLayout(layout: NoteLayout) =
        withContext(Dispatchers.IO) { prefs.edit { putString(KEY_LAYOUT, layout.name) } }

    private companion object {
        const val KEY_LAYOUT = "layout"
    }
}
