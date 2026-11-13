// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.list

import org.prauga.notes.core.data.CollectionCount
import org.prauga.notes.core.model.Entry

internal data class NoteListState(
    val notes: List<Entry>,
    val categories: List<String>,
    val selectedCategory: String?,
    val collections: List<CollectionCount>,
    val selectedCollection: Long?,
    val query: String,
    val searching: Boolean,
    val layout: NoteLayout,
)
