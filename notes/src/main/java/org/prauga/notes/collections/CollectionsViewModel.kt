// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.prauga.notes.core.data.CollectionCount
import org.prauga.notes.core.data.EntryRepository
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.EntryCollection

@OptIn(ExperimentalCoroutinesApi::class)
internal class CollectionsViewModel(private val repository: EntryRepository) : ViewModel() {
    private val attempt = MutableStateFlow(0)

    val state: StateFlow<UiState<List<CollectionCount>>> =
        attempt
            .flatMapLatest {
                repository
                    .observeCollectionCounts(Component.VJOURNAL)
                    .map<List<CollectionCount>, UiState<List<CollectionCount>>> {
                        UiState.Success(it)
                    }
                    .catch { emit(UiState.Error(it.message.orEmpty(), it)) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun onEvent(event: CollectionsEvent) {
        when (event) {
            is CollectionsEvent.Save -> save(event.id, event.name, event.color)
            is CollectionsEvent.Delete -> delete(event.id)
            CollectionsEvent.Retry -> attempt.update { it + 1 }
        }
    }

    private fun save(id: Long, name: String, color: Int?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.saveCollection(EntryCollection(id = id, name = trimmed, color = color))
        }
    }

    private fun delete(id: Long) {
        val collections = (state.value as? UiState.Success)?.data ?: return
        if (collections.size <= 1) return
        viewModelScope.launch { repository.deleteCollection(id) }
    }
}
