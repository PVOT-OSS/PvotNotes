// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.data

data class EntryFilter(
    val collectionId: Long? = null,
    val category: String? = null,
    val query: String = "",
    val topLevelOnly: Boolean = false,
)
