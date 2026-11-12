// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.data

import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.prauga.notes.core.ical.TaskCompletion
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.model.EntryCollection

data class EntryFilter(
    val collectionId: Long? = null,
    val category: String? = null,
    val query: String = "",
    val topLevelOnly: Boolean = false,
)

class EntryRepository(
    private val database: EntryDatabase,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val entries = database.entryDao()
    private val collections = database.collectionDao()

    fun observeEntries(
        component: Component,
        filter: EntryFilter = EntryFilter(),
    ): Flow<List<Entry>> =
        entries
            .observe(
                component.name,
                filter.collectionId,
                filter.category,
                ftsMatch(filter.query),
                filter.topLevelOnly,
            )
            .map { list -> list.map { it.toModel() } }

    fun observeEntry(id: Long): Flow<Entry?> = entries.observeById(id).map { it?.toModel() }

    suspend fun getEntry(id: Long): Entry? = entries.getById(id)?.toModel()

    suspend fun getEntryByUid(uid: String): Entry? = entries.getByUid(uid)?.toModel()

    fun observeCategories(
        component: Component,
        collectionId: Long? = null,
    ): Flow<List<CategoryCount>> = entries.observeCategories(component.name, collectionId)

    fun observeCollections(): Flow<List<EntryCollection>> =
        collections.observeAll().map { list -> list.map { it.toModel() } }

    fun observeCollectionCounts(component: Component): Flow<List<CollectionCount>> =
        collections.observeWithCounts(component.name)

    suspend fun defaultCollection(defaultName: String): EntryCollection =
        collections.first()?.toModel()
            ?: EntryCollection(name = defaultName).let {
                it.copy(id = collections.insert(it.toEntity()))
            }

    suspend fun saveCollection(collection: EntryCollection): Long =
        if (collection.id == 0L) collections.insert(collection.toEntity())
        else collection.id.also { collections.update(collection.toEntity()) }

    suspend fun deleteCollection(id: Long) = collections.delete(id)

    fun newEntry(component: Component, collectionId: Long): Entry {
        val now = clock.instant()
        return Entry(
            uid = UUID.randomUUID().toString(),
            component = component,
            collectionId = collectionId,
            created = now,
            lastModified = now,
        )
    }

    suspend fun save(entry: Entry, newRevision: Boolean = true): Long {
        val existing = if (entry.id == 0L) null else entries.getById(entry.id)?.toModel()
        if (existing != null && existing == entry) return entry.id
        val categories = entry.categories.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val saved =
            entry.copy(
                categories = categories,
                lastModified = clock.instant(),
                sequence =
                    if (existing != null && newRevision) existing.sequence + 1 else entry.sequence,
            )
        return entries.save(
            saved.toEntity(),
            categories,
            saved.alarms.map { it.minutesBefore }.distinct().sorted(),
            saved.attachments.map {
                AttachmentEntity(
                    entryId = 0,
                    uri = it.uri,
                    mimeType = it.mimeType,
                    name = it.name,
                    position = 0,
                )
            },
        )
    }

    suspend fun delete(id: Long) {
        val entry = entries.getById(id)?.entry ?: return
        entries.deleteWithChildren(entry.id, entry.uid)
    }

    fun observeChildren(parentUid: String): Flow<List<Entry>> =
        entries.observeChildren(parentUid).map { list -> list.map { it.toModel() } }

    suspend fun openWithAlarms(component: Component): List<Entry> =
        entries.getOpenWithAlarms(component.name).map { it.toModel() }

    /** Completes or reopens a task; a repeating one moves to its next date instead. */
    suspend fun setCompleted(id: Long, done: Boolean): Entry? {
        val task = getEntry(id) ?: return null
        val updated =
            if (done) TaskCompletion.complete(task, clock.instant())
            else TaskCompletion.reopen(task)
        save(updated)
        return getEntry(id)
    }

    companion object {
        internal fun ftsMatch(query: String): String? =
            query
                .split(Regex("\\s+"))
                .map { token -> token.filter { it.isLetterOrDigit() } }
                .filter { it.isNotEmpty() }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" ") { "$it*" }
    }
}
