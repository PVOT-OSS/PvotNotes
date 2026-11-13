// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entries",
    foreignKeys =
        [
            ForeignKey(
                entity = CollectionEntity::class,
                parentColumns = ["id"],
                childColumns = ["collection_id"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices =
        [
            Index("uid", unique = true),
            Index("collection_id"),
            Index("component", "last_modified"),
            Index("parent_uid"),
        ],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val component: String,
    @ColumnInfo(name = "collection_id") val collectionId: Long,
    val summary: String,
    val description: String,
    val color: Int?,
    @Embedded(prefix = "start_") val start: TimeColumns?,
    @Embedded(prefix = "due_") val due: TimeColumns?,
    val completed: Long?,
    val status: String?,
    val classification: String?,
    val priority: Int?,
    @ColumnInfo(name = "percent_complete") val percentComplete: Int?,
    val created: Long,
    @ColumnInfo(name = "last_modified") val lastModified: Long,
    val sequence: Int,
    @ColumnInfo(name = "parent_uid", defaultValue = "NULL") val parentUid: String? = null,
    @ColumnInfo(defaultValue = "NULL") val rrule: String? = null,
)
