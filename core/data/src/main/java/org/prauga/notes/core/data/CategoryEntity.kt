// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "categories",
    primaryKeys = ["entry_id", "name"],
    foreignKeys =
        [
            ForeignKey(
                entity = EntryEntity::class,
                parentColumns = ["id"],
                childColumns = ["entry_id"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("name")],
)
data class CategoryEntity(
    @ColumnInfo(name = "entry_id") val entryId: Long,
    val name: String,
    val position: Int,
)
