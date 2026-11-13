// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes

import android.content.Context
import androidx.core.content.edit
import org.prauga.notes.list.NoteLayout

class NotesPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("notes", Context.MODE_PRIVATE)

    var layout: NoteLayout
        get() =
            prefs.getString(KEY_LAYOUT, null)?.let {
                NoteLayout.entries.firstOrNull { layout -> layout.name == it }
            } ?: NoteLayout.GRID
        set(value) = prefs.edit { putString(KEY_LAYOUT, value.name) }

    private companion object {
        const val KEY_LAYOUT = "layout"
    }
}
