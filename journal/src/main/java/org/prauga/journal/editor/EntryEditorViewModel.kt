// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal.editor

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.prauga.journal.AttachmentStore
import org.prauga.journal.EntryEditor
import org.prauga.journal.feed.moment
import org.prauga.notes.core.data.EntryRepository
import org.prauga.notes.core.model.Attachment
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.model.EntryTime
import org.prauga.notes.core.ui.markdown.MarkdownParser

@OptIn(FlowPreview::class)
internal class EntryEditorViewModel(
    private val repository: EntryRepository,
    private val store: AttachmentStore,
    key: EntryEditor,
    defaultCollectionName: String,
    private val applicationScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val form = MutableStateFlow<EntryEditorState?>(null)
    val state: StateFlow<UiState<EntryEditorState>> =
        form
            .map<EntryEditorState?, UiState<EntryEditorState>> { current ->
                current?.let { UiState.Success(it) } ?: UiState.Loading
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val closed = Channel<EntryEditorEffect>(Channel.BUFFERED)
    val effects: Flow<EntryEditorEffect> = closed.receiveAsFlow()

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
                        val day = key.date?.let(::dateOrNull)
                        val start =
                            if (day == null || day == now.toLocalDate()) now else day.atTime(12, 0)
                        val shared = key.image?.let { store.import(it.toUri()) }
                        repository
                            .newEntry(
                                Component.VJOURNAL,
                                repository.defaultCollection(defaultCollectionName).id,
                            )
                            .copy(
                                summary = key.title,
                                description = key.text,
                                start = EntryTime.DateTime(start, zone),
                                attachments = listOfNotNull(shared),
                            )
                    }
            entry = loaded
            form.value =
                EntryEditorState(
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

    fun onEvent(event: EntryEditorEvent) {
        when (event) {
            is EntryEditorEvent.SetTitle -> edit { it.copy(title = event.title) }
            is EntryEditorEvent.SetBody -> edit { it.copy(body = event.body) }
            is EntryEditorEvent.ToggleTask ->
                edit { it.copy(body = MarkdownParser.toggleTask(it.body, event.line)) }
            is EntryEditorEvent.SetDate ->
                edit { it.copy(moment = event.date.atTime(it.moment.toLocalTime())) }
            is EntryEditorEvent.SetTime ->
                edit { it.copy(moment = it.moment.toLocalDate().atTime(event.time)) }
            is EntryEditorEvent.SetEditing ->
                form.update { it?.copy(editing = event.editing, focusBody = event.editing) }
            is EntryEditorEvent.AddCategory -> addCategory(event.category)
            is EntryEditorEvent.RemoveCategory ->
                edit { it.copy(categories = it.categories - event.category) }
            is EntryEditorEvent.AddPhotos -> addPhotos(event.uris)
            is EntryEditorEvent.RemovePhoto -> removePhoto(event.attachment)
            EntryEditorEvent.Delete -> delete()
        }
    }

    private fun addCategory(category: String) {
        val name = category.trim().removePrefix("#").trim()
        if (name.isEmpty()) return
        edit { state ->
            if (name in state.categories) state
            else state.copy(categories = state.categories + name)
        }
    }

    private fun addPhotos(uris: List<Uri>) {
        viewModelScope.launch {
            val imported = uris.mapNotNull { store.import(it) }
            edit { it.copy(attachments = it.attachments + imported) }
        }
    }

    private fun removePhoto(attachment: Attachment) {
        edit { it.copy(attachments = it.attachments - attachment) }
        viewModelScope.launch {
            save()
            store.delete(listOf(attachment))
        }
    }

    private fun delete() {
        deleted = true
        val current = entry
        val photos = form.value?.attachments.orEmpty() + current?.attachments.orEmpty()
        applicationScope.launch {
            saveLock.withLock {
                if (current != null && current.id != 0L) repository.delete(current.id)
            }
            store.delete(photos.distinct())
        }
        closed.trySend(EntryEditorEffect.Closed)
    }

    override fun onCleared() {
        if (!deleted) applicationScope.launch { save() }
    }

    private fun edit(transform: (EntryEditorState) -> EntryEditorState) {
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
            form.update { it?.copy(lastModified = stored.lastModified, saved = true) }
        }

    private fun dateOrNull(iso: String): LocalDate? =
        try {
            LocalDate.parse(iso)
        } catch (_: DateTimeParseException) {
            null
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
