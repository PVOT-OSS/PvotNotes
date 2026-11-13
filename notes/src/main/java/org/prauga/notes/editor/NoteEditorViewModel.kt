// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.prauga.notes.NoteEditor
import org.prauga.notes.core.data.EntryRepository
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.model.EntryCollection
import org.prauga.notes.core.ui.markdown.MarkdownParser

@OptIn(FlowPreview::class)
internal class NoteEditorViewModel(
    private val repository: EntryRepository,
    key: NoteEditor,
    defaultCollectionName: String,
    private val applicationScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val form = MutableStateFlow<NoteEditorState?>(null)
    val state: StateFlow<UiState<NoteEditorState>> =
        form
            .map<NoteEditorState?, UiState<NoteEditorState>> { current ->
                current?.let { UiState.Success(it) } ?: UiState.Loading
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val closed = Channel<NoteEditorEffect>(Channel.BUFFERED)
    val effects: Flow<NoteEditorEffect> = closed.receiveAsFlow()

    private val edits = Channel<Unit>(Channel.CONFLATED)
    private val saveLock = Mutex()
    private var entry: Entry? = null
    private var collections: List<EntryCollection> = emptyList()
    private var deleted = false
    private var revised = false

    init {
        viewModelScope.launch {
            val loaded =
                (savedId(key)?.let { repository.getEntry(it) })
                    ?: repository
                        .newEntry(
                            Component.VJOURNAL,
                            initialCollection(key.collectionId, defaultCollectionName),
                        )
                        .copy(summary = key.title, description = key.text)
            entry = loaded
            form.value =
                NoteEditorState(
                    title = loaded.summary,
                    body = loaded.description,
                    categories = loaded.categories,
                    color = loaded.color,
                    collectionId = loaded.collectionId,
                    collections = collections,
                    lastModified = loaded.lastModified.takeIf { loaded.id != 0L },
                    saved = loaded.id != 0L,
                    focusTitle = loaded.id == 0L && loaded.isBlank(),
                    editing = loaded.id == 0L,
                )
            if (loaded.id == 0L && !loaded.isBlank()) edits.send(Unit)
        }
        viewModelScope.launch {
            repository.observeCollections().collect { list ->
                collections = list
                form.update { it?.copy(collections = list) }
            }
        }
        viewModelScope.launch { edits.receiveAsFlow().debounce(SAVE_DELAY_MS).collect { save() } }
    }

    private fun setTitle(title: String) = edit { it.copy(title = title) }

    private fun setBody(body: String) = edit { it.copy(body = body) }

    private fun addCategory(category: String) {
        val name = category.trim().removePrefix("#").trim()
        if (name.isEmpty()) return
        edit { state ->
            if (state.categories.contains(name)) state
            else state.copy(categories = state.categories + name)
        }
    }

    private fun toggleTask(line: Int) = edit {
        it.copy(body = MarkdownParser.toggleTask(it.body, line))
    }

    private fun setEditing(editing: Boolean) {
        if (form.value == null) return
        form.update { it?.copy(editing = editing, focusTitle = false) }
    }

    private fun setCollection(id: Long) = edit { it.copy(collectionId = id) }

    private fun setColor(color: Int?) = edit { it.copy(color = color) }

    private fun removeCategory(category: String) = edit {
        it.copy(categories = it.categories - category)
    }

    private fun delete() {
        deleted = true
        val id = entry?.id ?: 0L
        applicationScope.launch { saveLock.withLock { if (id != 0L) repository.delete(id) } }
        closed.trySend(NoteEditorEffect.Closed)
    }

    fun onEvent(event: NoteEditorEvent) {
        when (event) {
            is NoteEditorEvent.SetTitle -> setTitle(event.title)
            is NoteEditorEvent.SetBody -> setBody(event.body)
            is NoteEditorEvent.AddCategory -> addCategory(event.category)
            is NoteEditorEvent.RemoveCategory -> removeCategory(event.category)
            is NoteEditorEvent.ToggleTask -> toggleTask(event.line)
            is NoteEditorEvent.SetEditing -> setEditing(event.editing)
            is NoteEditorEvent.SetCollection -> setCollection(event.collectionId)
            is NoteEditorEvent.SetColor -> setColor(event.color)
            NoteEditorEvent.Delete -> delete()
        }
    }

    override fun onCleared() {
        if (!deleted) applicationScope.launch { save() }
    }

    private fun edit(transform: (NoteEditorState) -> NoteEditorState) {
        if (form.value == null) return
        form.update { it?.let(transform) }
        edits.trySend(Unit)
    }

    private suspend fun save() =
        saveLock.withLock {
            val base = entry ?: return@withLock
            if (deleted) return@withLock
            val state = form.value ?: return@withLock
            val updated =
                base.copy(
                    summary = state.title.trim(),
                    description = state.body,
                    categories = state.categories,
                    color = state.color,
                    collectionId = state.collectionId,
                )
            if (updated.id == 0L && updated.isBlank()) return@withLock
            if (updated.id != 0L && updated == base) return@withLock
            val id = repository.save(updated, newRevision = !revised)
            savedStateHandle[KEY_ID] = id
            revised = true
            val stored = repository.getEntry(id) ?: return@withLock
            entry = stored
            form.update { it?.copy(lastModified = stored.lastModified, saved = true) }
        }

    private suspend fun initialCollection(requested: Long, defaultName: String): Long {
        val all = repository.observeCollections().first()
        return all.firstOrNull { it.id == requested }?.id
            ?: repository.defaultCollection(defaultName).id
    }

    private fun savedId(key: NoteEditor): Long? =
        key.id.takeIf { it != 0L } ?: savedStateHandle.get<Long>(KEY_ID)

    private fun Entry.isBlank() = summary.isBlank() && description.isBlank() && categories.isEmpty()

    private companion object {
        const val SAVE_DELAY_MS = 500L
        const val KEY_ID = "id"
    }
}
