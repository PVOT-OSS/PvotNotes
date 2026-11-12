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
        ORDER BY last_modified DESC
        """
    )
    fun observe(
        component: String,
        collectionId: Long?,
        category: String?,
        match: String?,
    ): Flow<List<EntryWithCategories>>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    fun observeById(id: Long): Flow<EntryWithCategories?>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): EntryWithCategories?

    @Transaction
    @Query("SELECT * FROM entries WHERE uid = :uid")
    suspend fun getByUid(uid: String): EntryWithCategories?

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

    @Transaction
    suspend fun save(entry: EntryEntity, categories: List<String>): Long {
        val id =
            if (entry.id == 0L) {
                insert(entry)
            } else {
                update(entry)
                entry.id
            }
        deleteCategories(id)
        insertCategories(categories.mapIndexed { index, name -> CategoryEntity(id, name, index) })
        return id
    }
}
