// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Transaction
    @Query(
        """
        SELECT * FROM entries
        WHERE component = :component
          AND (:collectionId IS NULL OR collection_id = :collectionId)
          AND (:category IS NULL OR id IN (SELECT entry_id FROM categories WHERE name = :category))
          AND (:match IS NULL OR rowid IN (SELECT rowid FROM entries_fts WHERE entries_fts MATCH :match))
          AND (:topLevelOnly = 0 OR parent_uid IS NULL)
        ORDER BY last_modified DESC
        """
    )
    fun observe(
        component: String,
        collectionId: Long?,
        category: String?,
        match: String?,
        topLevelOnly: Boolean,
    ): Flow<List<EntryWithRelations>>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    fun observeById(id: Long): Flow<EntryWithRelations?>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): EntryWithRelations?

    @Transaction
    @Query("SELECT * FROM entries WHERE uid = :uid")
    suspend fun getByUid(uid: String): EntryWithRelations?

    @Query(
        """
        SELECT categories.name AS name, COUNT(*) AS count FROM categories
        JOIN entries ON entries.id = categories.entry_id
        WHERE entries.component = :component
          AND (:collectionId IS NULL OR entries.collection_id = :collectionId)
        GROUP BY categories.name ORDER BY categories.name COLLATE NOCASE
        """
    )
    fun observeCategories(component: String, collectionId: Long?): Flow<List<CategoryCount>>

    @Insert suspend fun insert(entry: EntryEntity): Long

    @Update suspend fun update(entry: EntryEntity)

    @Insert suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE entry_id = :entryId")
    suspend fun deleteCategories(entryId: Long)

    @Query("DELETE FROM entries WHERE id = :id") suspend fun delete(id: Long)

    @Insert suspend fun insertAlarms(alarms: List<AlarmEntity>)

    @Query("DELETE FROM alarms WHERE entry_id = :entryId") suspend fun deleteAlarms(entryId: Long)

    @Insert suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Query("DELETE FROM attachments WHERE entry_id = :entryId")
    suspend fun deleteAttachments(entryId: Long)

    @Query("DELETE FROM entries WHERE parent_uid = :parentUid")
    suspend fun deleteChildren(parentUid: String)

    @Transaction
    @Query("SELECT * FROM entries WHERE parent_uid = :parentUid ORDER BY created")
    fun observeChildren(parentUid: String): Flow<List<EntryWithRelations>>

    @Transaction
    @Query(
        """
        SELECT * FROM entries
        WHERE component = :component AND completed IS NULL
          AND id IN (SELECT entry_id FROM alarms)
        """
    )
    suspend fun getOpenWithAlarms(component: String): List<EntryWithRelations>

    @Transaction
    suspend fun save(
        entry: EntryEntity,
        categories: List<String>,
        alarms: List<Int>,
        attachments: List<AttachmentEntity>,
    ): Long {
        val id =
            if (entry.id == 0L) {
                insert(entry)
            } else {
                update(entry)
                entry.id
            }
        deleteCategories(id)
        insertCategories(categories.mapIndexed { index, name -> CategoryEntity(id, name, index) })
        deleteAlarms(id)
        insertAlarms(alarms.map { AlarmEntity(entryId = id, minutesBefore = it) })
        deleteAttachments(id)
        insertAttachments(
            attachments.mapIndexed { index, it -> it.copy(id = 0, entryId = id, position = index) }
        )
        return id
    }

    @Transaction
    suspend fun deleteWithChildren(id: Long, uid: String) {
        deleteChildren(uid)
        delete(id)
    }
}
