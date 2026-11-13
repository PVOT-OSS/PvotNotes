// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.notes.core.model.Alarm
import org.prauga.notes.core.model.Attachment
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.EntryCollection
import org.prauga.notes.core.model.EntryTime
import org.prauga.notes.core.model.Status

@RunWith(AndroidJUnit4::class)
class EntryRepositoryTest {
    private lateinit var database: EntryDatabase
    private lateinit var repository: EntryRepository

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    EntryDatabase::class.java,
                )
                .build()
        repository = EntryRepository(database)
    }

    @After fun tearDown() = database.close()

    @Test
    fun savesAndReadsBackAnEntry() = runTest {
        val collection = repository.defaultCollection("Notes")
        val start =
            EntryTime.DateTime(LocalDateTime.of(2026, 11, 3, 21, 30), ZoneId.of("Asia/Kolkata"))
        val entry =
            repository
                .newEntry(Component.VJOURNAL, collection.id)
                .copy(
                    summary = "Title",
                    description = "Body",
                    categories = listOf("b", "a", " b ", ""),
                    start = start,
                )
        val id = repository.save(entry)
        val saved = checkNotNull(repository.getEntry(id))
        assertEquals(listOf("b", "a"), saved.categories)
        assertEquals(start, saved.start)
        assertEquals(0, saved.sequence)

        repository.save(saved.copy(summary = "Renamed"))
        assertEquals(1, checkNotNull(repository.getEntry(id)).sequence)
        assertEquals("Renamed", checkNotNull(repository.getEntryByUid(entry.uid)).summary)

        repository.save(
            checkNotNull(repository.getEntry(id)).copy(summary = "Autosaved"),
            newRevision = false,
        )
        assertEquals(1, checkNotNull(repository.getEntry(id)).sequence)
    }

    @Test
    fun filtersByComponentCategoryAndText() = runTest {
        val collection = repository.defaultCollection("Notes")
        fun note(summary: String, description: String, vararg categories: String) =
            repository
                .newEntry(Component.VJOURNAL, collection.id)
                .copy(
                    summary = summary,
                    description = description,
                    categories = categories.toList(),
                )
        repository.save(note("Groceries", "milk and eggs", "home"))
        repository.save(note("Standup", "discussed release", "work"))
        repository.save(
            repository.newEntry(Component.VTODO, collection.id).copy(summary = "Groceries task")
        )

        suspend fun summaries(filter: EntryFilter) =
            repository
                .observeEntries(Component.VJOURNAL, filter)
                .first()
                .map { it.summary }
                .sorted()

        assertEquals(listOf("Groceries", "Standup"), summaries(EntryFilter()))
        assertEquals(listOf("Standup"), summaries(EntryFilter(category = "work")))
        assertEquals(listOf("Groceries"), summaries(EntryFilter(query = "MIL")))
        assertEquals(listOf("Standup"), summaries(EntryFilter(query = "rel stand")))
        assertEquals(listOf("Groceries", "Standup"), summaries(EntryFilter(query = "\"*")))
        assertEquals(
            listOf(CategoryCount("home", 1), CategoryCount("work", 1)),
            repository.observeCategories(Component.VJOURNAL).first(),
        )
    }

    @Test
    fun deletingCollectionDeletesItsEntries() = runTest {
        val collection = repository.defaultCollection("Notes")
        val id =
            repository.save(
                repository.newEntry(Component.VJOURNAL, collection.id).copy(summary = "x")
            )
        repository.deleteCollection(collection.id)
        assertNull(repository.getEntry(id))
    }

    @Test
    fun countsEntriesPerCollection() = runTest {
        val first = repository.defaultCollection("Notes")
        val second = repository.saveCollection(EntryCollection(name = "Work"))
        repository.save(repository.newEntry(Component.VJOURNAL, first.id).copy(summary = "a"))
        repository.save(repository.newEntry(Component.VJOURNAL, second).copy(summary = "b"))
        repository.save(repository.newEntry(Component.VJOURNAL, second).copy(summary = "c"))
        repository.save(repository.newEntry(Component.VTODO, second).copy(summary = "task"))
        assertEquals(
            listOf(
                CollectionCount(first.id, "Notes", null, 1),
                CollectionCount(second, "Work", null, 2),
            ),
            repository.observeCollectionCounts(Component.VJOURNAL).first(),
        )
        assertEquals(
            listOf("b", "c"),
            repository
                .observeEntries(Component.VJOURNAL, EntryFilter(collectionId = second))
                .first()
                .map { it.summary }
                .sorted(),
        )
    }

    @Test
    fun keepsSubtasksAlarmsAndAttachments() = runTest {
        val collection = repository.defaultCollection("Tasks")
        val parent =
            repository
                .newEntry(Component.VTODO, collection.id)
                .copy(summary = "Trip", alarms = listOf(Alarm(60), Alarm(0), Alarm(60)))
        val parentId = repository.save(parent)
        repository.save(
            repository
                .newEntry(Component.VTODO, collection.id)
                .copy(summary = "Pack", parentUid = parent.uid)
        )
        val journal =
            repository
                .newEntry(Component.VJOURNAL, collection.id)
                .copy(
                    attachments =
                        listOf(
                            Attachment("attachments/a.jpg", "image/jpeg"),
                            Attachment("attachments/b.jpg"),
                        )
                )
        val journalId = repository.save(journal)

        assertEquals(
            listOf(Alarm(0), Alarm(60)),
            checkNotNull(repository.getEntry(parentId)).alarms,
        )
        assertEquals(journal.attachments, checkNotNull(repository.getEntry(journalId)).attachments)
        assertEquals(
            listOf("Pack"),
            repository.observeChildren(parent.uid).first().map { it.summary },
        )
        assertEquals(
            listOf("Trip"),
            repository
                .observeEntries(Component.VTODO, EntryFilter(topLevelOnly = true))
                .first()
                .map { it.summary },
        )
        assertEquals(listOf("Trip"), repository.openWithAlarms(Component.VTODO).map { it.summary })

        repository.delete(parentId)
        assertEquals(
            emptyList<String>(),
            repository.observeEntries(Component.VTODO).first().map { it.summary },
        )
    }

    @Test
    fun completingARepeatingTaskMovesItOn() = runTest {
        val collection = repository.defaultCollection("Tasks")
        val id =
            repository.save(
                repository
                    .newEntry(Component.VTODO, collection.id)
                    .copy(
                        summary = "Bins",
                        rrule = "FREQ=WEEKLY",
                        due = EntryTime.Date(LocalDate.of(2026, 11, 13)),
                    )
            )
        val next = checkNotNull(repository.setCompleted(id, done = true))
        assertEquals(EntryTime.Date(LocalDate.of(2026, 11, 20)), next.due)
        assertNull(next.completed)
        repository.save(next.copy(rrule = null))
        assertEquals(
            Status.COMPLETED,
            checkNotNull(repository.setCompleted(id, done = true)).status,
        )
    }
}
