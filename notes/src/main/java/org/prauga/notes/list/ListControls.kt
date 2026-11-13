// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.list

internal data class ListControls(
    val query: String = "",
    val searching: Boolean = false,
    val category: String? = null,
    val collection: Long? = null,
    val layout: NoteLayout = NoteLayout.GRID,
)

internal fun ListControls.dropMissing(
    collectionIds: Set<Long>,
    categories: Set<String>,
): ListControls =
    copy(
        collection = collection?.takeIf { it in collectionIds },
        category = category?.takeIf { it in categories },
    )
