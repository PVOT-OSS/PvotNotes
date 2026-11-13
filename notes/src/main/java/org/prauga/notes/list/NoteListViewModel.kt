// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.prauga.notes.NotesPreferences
import org.prauga.notes.core.data.EntryFilter
import org.prauga.notes.core.data.EntryRepository
import org.prauga.notes.core.model.Component

@OptIn(ExperimentalCoroutinesApi::class)
internal class NoteListViewModel(
    private val repository: EntryRepository,
    private val preferences: NotesPreferences,
    private val defaultCollectionName: String,
) : ViewModel() {
    private val controls = MutableStateFlow(ListControls())
    private val attempt = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            val layout = preferences.layout()
            controls.update { it.copy(layout = layout) }
        }
    }

    val state: StateFlow<UiState<NoteListState>> =
        attempt
            .flatMapLatest {
                notes()
                    .map<NoteListState, UiState<NoteListState>> { UiState.Success(it) }
                    .catch { emit(UiState.Error(it.message.orEmpty(), it)) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun onEvent(event: NoteListEvent) {
        when (event) {
            is NoteListEvent.QuickAdd -> quickAdd(event.text)
            is NoteListEvent.SetQuery -> controls.update { it.copy(query = event.query) }
            is NoteListEvent.SetSearching ->
                controls.update {
                    it.copy(
                        searching = event.searching,
                        query = if (event.searching) it.query else "",
                    )
                }
            is NoteListEvent.SelectCollection ->
                controls.update { it.copy(collection = event.collectionId) }
            is NoteListEvent.SelectCategory ->
                controls.update { it.copy(category = event.category) }
            NoteListEvent.ToggleLayout -> toggleLayout()
            NoteListEvent.Retry -> attempt.update { it + 1 }
        }
    }

    private fun notes() =
        repository.observeCollectionCounts(Component.VJOURNAL).let { collections ->
            // A selected collection or tag can disappear; filtering on it would hide every note.
            val selectedCollection =
                combine(controls, collections) { controls, all ->
                        controls.collection?.takeIf { id -> all.any { it.id == id } }
                    }
                    .distinctUntilChanged()
            val categories =
                selectedCollection.flatMapLatest {
                    repository.observeCategories(Component.VJOURNAL, it)
                }
            val effective =
                combine(controls, collections, categories) { controls, all, tags ->
                    controls.dropMissing(all.map { it.id }.toSet(), tags.map { it.name }.toSet())
                }
            combine(
                effective,
                effective
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
            ) { controls, notes, tags, all ->
                NoteListState(
                    notes = notes,
                    categories = tags.map { it.name },
                    selectedCategory = controls.category,
                    collections = all,
                    selectedCollection = controls.collection,
                    query = controls.query,
                    searching = controls.searching,
                    layout = controls.layout,
                )
            }
        }

    private fun quickAdd(text: String) {
        val title = text.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            val selected = (state.value as? UiState.Success)?.data?.selectedCollection
            val collectionId = selected ?: repository.defaultCollection(defaultCollectionName).id
            repository.save(
                repository.newEntry(Component.VJOURNAL, collectionId).copy(summary = title)
            )
        }
    }

    private fun toggleLayout() {
        val layout =
            if (controls.value.layout == NoteLayout.GRID) NoteLayout.LIST else NoteLayout.GRID
        controls.update { it.copy(layout = layout) }
        viewModelScope.launch { preferences.setLayout(layout) }
    }
}
