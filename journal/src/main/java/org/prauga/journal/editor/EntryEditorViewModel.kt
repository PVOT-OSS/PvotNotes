// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal.editor

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.prauga.journal.AppContainer
import org.prauga.journal.EntryEditor
import org.prauga.journal.feed.moment
import org.prauga.notes.core.model.Attachment
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.model.EntryTime
import org.prauga.notes.core.ui.markdown.MarkdownParser

data class EntryEditorUiState(
    val loading: Boolean = true,
    val moment: LocalDateTime = LocalDateTime.now(),
    val title: String = "",
    val body: String = "",
    val categories: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val lastModified: Instant? = null,
    val saved: Boolean = false,
    val editing: Boolean = true,
    val focusBody: Boolean = false,
)

@OptIn(FlowPreview::class)
class EntryEditorViewModel(
    private val container: AppContainer,
    key: EntryEditor,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val repository = container.repository
    private val store = container.attachments
    private val _uiState = MutableStateFlow(EntryEditorUiState())
    val uiState: StateFlow<EntryEditorUiState> = _uiState.asStateFlow()

    private val edits = Channel<Unit>(Channel.CONFLATED)
    private val saveLock = Mutex()
    private var entry: Entry? = null
    private var deleted = false
    private var revised = false

    init {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val existing = savedId(key)?.let { repository.getEntry(it) }
            val loaded =
                existing
                    ?: run {
                        val now = LocalDateTime.now(zone).withSecond(0).withNano(0)
                        val day = key.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        val start =
                            if (day == null || day == now.toLocalDate()) now else day.atTime(12, 0)
                        val shared = key.image?.let { store.import(it.toUri()) }
                        repository
                            .newEntry(
                                Component.VJOURNAL,
                                repository.defaultCollection(container.defaultCollectionName).id,
                            )
                            .copy(
                                summary = key.title,
                                description = key.text,
                                start = EntryTime.DateTime(start, zone),
                                attachments = listOfNotNull(shared),
                            )
                    }
            entry = loaded
            _uiState.value =
                EntryEditorUiState(
                    loading = false,
                    moment = loaded.moment(zone),
                    title = loaded.summary,
                    body = loaded.description,
                    categories = loaded.categories,
                    attachments = loaded.attachments,
                    lastModified = loaded.lastModified.takeIf { loaded.id != 0L },
                    saved = loaded.id != 0L,
                    editing = loaded.id == 0L,
                    focusBody = loaded.id == 0L,
                )
            if (loaded.id == 0L && !loaded.isBlank()) edits.send(Unit)
        }
        viewModelScope.launch { edits.receiveAsFlow().debounce(SAVE_DELAY_MS).collect { save() } }
    }

    fun setTitle(title: String) = edit { it.copy(title = title) }

    fun setBody(body: String) = edit { it.copy(body = body) }

    fun toggleTask(line: Int) = edit { it.copy(body = MarkdownParser.toggleTask(it.body, line)) }

    fun setDate(date: LocalDate) = edit { it.copy(moment = date.atTime(it.moment.toLocalTime())) }

    fun setTime(time: LocalTime) = edit { it.copy(moment = it.moment.toLocalDate().atTime(time)) }

    fun setEditing(editing: Boolean) {
        if (_uiState.value.loading) return
        _uiState.update { it.copy(editing = editing, focusBody = editing) }
    }

    fun addCategory(category: String) {
        val name = category.trim().removePrefix("#").trim()
        if (name.isEmpty()) return
        edit { state ->
            if (name in state.categories) state
            else state.copy(categories = state.categories + name)
        }
    }

    fun removeCategory(category: String) = edit { it.copy(categories = it.categories - category) }

    fun addPhotos(uris: List<Uri>) {
        viewModelScope.launch {
            val imported = uris.mapNotNull { store.import(it) }
            edit { it.copy(attachments = it.attachments + imported) }
        }
    }

    /** Removing a photo deletes its copy at once; there is no undo. */
    fun removePhoto(attachment: Attachment) {
        edit { it.copy(attachments = it.attachments - attachment) }
        viewModelScope.launch {
            save()
            store.delete(listOf(attachment))
        }
    }

    fun shareText(): String =
        with(_uiState.value) { listOf(title, body).filter { it.isNotBlank() }.joinToString("\n\n") }

    fun delete(onDeleted: () -> Unit) {
        deleted = true
        val current = entry
        val photos = _uiState.value.attachments + current?.attachments.orEmpty()
        container.applicationScope.launch {
            saveLock.withLock {
                if (current != null && current.id != 0L) repository.delete(current.id)
            }
            store.delete(photos.distinct())
        }
        onDeleted()
    }

    override fun onCleared() {
        if (!deleted) container.applicationScope.launch { save() }
    }

    private fun edit(transform: (EntryEditorUiState) -> EntryEditorUiState) {
        if (_uiState.value.loading) return
        _uiState.update(transform)
        edits.trySend(Unit)
    }

    private suspend fun save() =
        saveLock.withLock {
            val base = entry ?: return@withLock
            if (deleted) return@withLock
            val state = _uiState.value
            val updated =
                base.copy(
                    summary = state.title.trim(),
                    description = state.body,
                    categories = state.categories,
                    attachments = state.attachments,
                    start = EntryTime.DateTime(state.moment, ZoneId.systemDefault()),
                )
            if (updated.id == 0L && updated.isBlank()) return@withLock
            if (updated.id != 0L && updated == base) return@withLock
            val id = repository.save(updated, newRevision = !revised)
            revised = true
            savedStateHandle[KEY_ID] = id
            val stored = repository.getEntry(id) ?: return@withLock
            entry = stored
            _uiState.update { it.copy(lastModified = stored.lastModified, saved = true) }
        }

    private fun savedId(key: EntryEditor): Long? =
        key.id.takeIf { it != 0L } ?: savedStateHandle.get<Long>(KEY_ID)

    private fun Entry.isBlank() =
        summary.isBlank() && description.isBlank() && categories.isEmpty() && attachments.isEmpty()

    private companion object {
        const val SAVE_DELAY_MS = 500L
        const val KEY_ID = "id"
    }
}
