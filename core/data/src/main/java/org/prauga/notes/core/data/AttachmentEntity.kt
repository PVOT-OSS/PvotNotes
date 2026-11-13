// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys =
        [
            ForeignKey(
                entity = EntryEntity::class,
                parentColumns = ["id"],
                childColumns = ["entry_id"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("entry_id")],
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "entry_id") val entryId: Long,
    val uri: String,
    @ColumnInfo(name = "mime_type") val mimeType: String?,
    val name: String?,
    val position: Int,
)
