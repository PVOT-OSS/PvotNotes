// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.data

import androidx.room.Embedded
import androidx.room.Relation

data class EntryWithRelations(
    @Embedded val entry: EntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entry_id") val categories: List<CategoryEntity>,
    @Relation(parentColumn = "id", entityColumn = "entry_id") val alarms: List<AlarmEntity>,
    @Relation(parentColumn = "id", entityColumn = "entry_id")
    val attachments: List<AttachmentEntity>,
)
