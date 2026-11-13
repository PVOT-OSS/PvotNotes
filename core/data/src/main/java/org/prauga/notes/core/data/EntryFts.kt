// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.data

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = EntryEntity::class)
@Entity(tableName = "entries_fts")
data class EntryFts(val summary: String, val description: String)
