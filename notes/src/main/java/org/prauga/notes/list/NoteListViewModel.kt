// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.prauga.notes.NotesPreferences
import org.prauga.notes.core.data.CollectionCount
import org.prauga.notes.core.data.EntryFilter
import org.prauga.notes.core.data.EntryRepository
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.Entry

enum class NoteLayout {
    GRID,
    LIST,
}

data class NoteListUiState(
    val notes: List<Entry> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val collections: List<CollectionCount> = emptyList(),
    val selectedCollection: Long? = null,
    val query: String = "",
    val searching: Boolean = false,
    val layout: NoteLayout = NoteLayout.GRID,
    val loading: Boolean = true,
)

internal data class ListControls(
    val query: String = "",
    val searching: Boolean = false,
    val category: String? = null,
    val collection: Long? = null,
    val layout: NoteLayout = NoteLayout.GRID,
)

internal fun ListControls.dropMissing(
    collectionIds: Set<Long>,
    categories: Set<String>,
): ListControls =
    copy(
        collection = collection?.takeIf { it in collectionIds },
        category = category?.takeIf { it in categories },
    )

@OptIn(ExperimentalCoroutinesApi::class)
class NoteListViewModel(
    private val repository: EntryRepository,
    private val preferences: NotesPreferences,
    private val defaultCollectionName: String,
) : ViewModel() {
    private val controls = MutableStateFlow(ListControls(layout = preferences.layout))

    private val collections = repository.observeCollectionCounts(Component.VJOURNAL)

    // A selected collection or tag can disappear (deleted collection, last note untagged): drop it,
    // or the query keeps filtering on something the UI no longer shows. Tags follow the collection.
    private val selectedCollection =
        combine(controls, collections) { controls, collections ->
                controls.collection?.takeIf { id -> collections.any { it.id == id } }
            }
            .distinctUntilChanged()

    private val categories =
        selectedCollection.flatMapLatest { repository.observeCategories(Component.VJOURNAL, it) }

    private val effectiveControls =
        combine(controls, collections, categories) { controls, collections, categories ->
            controls.dropMissing(
                collections.map { it.id }.toSet(),
                categories.map { it.name }.toSet(),
            )
        }

    val uiState: StateFlow<NoteListUiState> =
        combine(
                effectiveControls,
                effectiveControls
                    .map {
                        EntryFilter(
                            collectionId = it.collection,
                            category = it.category,
                            query = it.query,
                        )
                    }
                    .distinctUntilChanged()
                    .flatMapLatest { repository.observeEntries(Component.VJOURNAL, it) },
                categories,
                collections,
            ) { controls, notes, categories, collections ->
                NoteListUiState(
                    notes = notes,
                    categories = categories.map { it.name },
                    selectedCategory = controls.category,
                    collections = collections,
                    selectedCollection = controls.collection,
                    query = controls.query,
                    searching = controls.searching,
                    layout = controls.layout,
                    loading = false,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoteListUiState())

    fun quickAdd(text: String) {
        val title = text.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            val collectionId =
                uiState.value.selectedCollection
                    ?: repository.defaultCollection(defaultCollectionName).id
            repository.save(
                repository.newEntry(Component.VJOURNAL, collectionId).copy(summary = title)
            )
        }
    }

    fun setQuery(query: String) = controls.update { it.copy(query = query) }

    fun setSearching(searching: Boolean) =
        controls.update { it.copy(searching = searching, query = if (searching) it.query else "") }

    fun selectCollection(collection: Long?) = controls.update { it.copy(collection = collection) }

    fun selectCategory(category: String?) = controls.update { it.copy(category = category) }

    fun toggleLayout() =
        controls.update {
            val layout = if (it.layout == NoteLayout.GRID) NoteLayout.LIST else NoteLayout.GRID
            preferences.layout = layout
            it.copy(layout = layout)
        }
}
