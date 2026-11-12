// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.prauga.notes.core.data.EntryRepository
import org.prauga.notes.core.model.Alarm
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.model.EntryTime
import org.prauga.reminders.TaskEditor
import org.prauga.reminders.list.Priority

data class TaskEditorUiState(
    val loading: Boolean = true,
    val title: String = "",
    val notes: String = "",
    val due: EntryTime? = null,
    val rrule: String? = null,
    val alarms: List<Int> = emptyList(),
    val priority: Priority = Priority.NONE,
    val subtasks: List<Entry> = emptyList(),
    val lastModified: Instant? = null,
    val saved: Boolean = false,
    val focusTitle: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TaskEditorViewModel(
    private val repository: EntryRepository,
    key: TaskEditor,
    defaultCollectionName: String,
    private val applicationScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskEditorUiState())
    val uiState: StateFlow<TaskEditorUiState> = _uiState.asStateFlow()

    private val edits = Channel<Unit>(Channel.CONFLATED)
    private val saveLock = Mutex()
    private val uid = MutableStateFlow<String?>(null)
    private var entry: Entry? = null
    private var deleted = false
    private var revised = false

    init {
        viewModelScope.launch {
            val loaded =
                (savedId(key)?.let { repository.getEntry(it) })
                    ?: repository
                        .newEntry(
                            Component.VTODO,
                            repository.defaultCollection(defaultCollectionName).id,
                        )
                        .copy(summary = key.title, description = key.text)
            entry = loaded
            uid.value = loaded.uid
            _uiState.value =
                TaskEditorUiState(
                    loading = false,
                    title = loaded.summary,
                    notes = loaded.description,
                    due = loaded.due,
                    rrule = loaded.rrule,
                    alarms = loaded.alarms.map { it.minutesBefore },
                    priority = Priority.of(loaded.priority),
                    lastModified = loaded.lastModified.takeIf { loaded.id != 0L },
                    saved = loaded.id != 0L,
                    focusTitle = loaded.id == 0L && loaded.summary.isBlank(),
                )
            if (loaded.id == 0L && !loaded.isBlank()) edits.send(Unit)
        }
        viewModelScope.launch {
            uid.filterNotNull()
                .flatMapLatest { repository.observeChildren(it) }
                .collect { children -> _uiState.update { it.copy(subtasks = children) } }
        }
        viewModelScope.launch { edits.receiveAsFlow().debounce(SAVE_DELAY_MS).collect { save() } }
    }

    fun setTitle(title: String) = edit { it.copy(title = title) }

    fun setNotes(notes: String) = edit { it.copy(notes = notes) }

    fun setDate(date: LocalDate?) = edit { state ->
        when {
            date == null -> state.copy(due = null, rrule = null, alarms = emptyList())
            state.due is EntryTime.DateTime ->
                state.copy(
                    due =
                        EntryTime.DateTime(
                            date.atTime((state.due as EntryTime.DateTime).dateTime.toLocalTime()),
                            ZoneId.systemDefault(),
                        )
                )
            else -> state.copy(due = EntryTime.Date(date))
        }
    }

    /** Setting a time on a task with no reminders adds one at the due time. */
    fun setTime(time: LocalTime?) = edit { state ->
        val date =
            state.due?.let { due ->
                if (due is EntryTime.Date) due.date
                else (due as EntryTime.DateTime).dateTime.toLocalDate()
            } ?: LocalDate.now()
        if (time == null) {
            state.copy(due = EntryTime.Date(date))
        } else {
            state.copy(
                due = EntryTime.DateTime(date.atTime(time), ZoneId.systemDefault()),
                alarms = state.alarms.ifEmpty { listOf(0) },
            )
        }
    }

    fun setRepeat(rrule: String?) = edit { it.copy(rrule = rrule) }

    fun addAlarm(minutesBefore: Int) = edit {
        it.copy(alarms = (it.alarms + minutesBefore).distinct().sorted())
    }

    fun removeAlarm(minutesBefore: Int) = edit { it.copy(alarms = it.alarms - minutesBefore) }

    fun setPriority(priority: Priority) = edit { it.copy(priority = priority) }

    fun addSubtask(title: String) {
        val text = title.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            save(force = true)
            val parent = entry ?: return@launch
            repository.save(
                repository
                    .newEntry(Component.VTODO, parent.collectionId)
                    .copy(summary = text, parentUid = parent.uid)
            )
        }
    }

    fun setSubtaskDone(id: Long, done: Boolean) {
        viewModelScope.launch { repository.setCompleted(id, done) }
    }

    fun removeSubtask(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun delete(onDeleted: () -> Unit) {
        deleted = true
        val id = entry?.id ?: 0L
        applicationScope.launch { saveLock.withLock { if (id != 0L) repository.delete(id) } }
        onDeleted()
    }

    override fun onCleared() {
        if (!deleted) applicationScope.launch { save() }
    }

    private fun edit(transform: (TaskEditorUiState) -> TaskEditorUiState) {
        if (_uiState.value.loading) return
        _uiState.update(transform)
        edits.trySend(Unit)
    }

    private suspend fun save(force: Boolean = false) =
        saveLock.withLock {
            val base = entry ?: return@withLock
            if (deleted) return@withLock
            val state = _uiState.value
            val updated =
                base.copy(
                    summary = state.title.trim(),
                    description = state.notes,
                    due = state.due,
                    rrule = state.rrule.takeIf { state.due != null },
                    alarms = if (state.due == null) emptyList() else state.alarms.map(::Alarm),
                    priority = state.priority.value,
                )
            if (updated.id == 0L && updated.isBlank() && !force) return@withLock
            if (updated.id != 0L && updated == base) return@withLock
            val id = repository.save(updated, newRevision = !revised)
            revised = true
            savedStateHandle[KEY_ID] = id
            val stored = repository.getEntry(id) ?: return@withLock
            entry = stored
            _uiState.update { it.copy(lastModified = stored.lastModified, saved = true) }
        }

    private fun savedId(key: TaskEditor): Long? =
        key.id.takeIf { it != 0L } ?: savedStateHandle.get<Long>(KEY_ID)

    private fun Entry.isBlank() = summary.isBlank() && description.isBlank() && due == null

    private companion object {
        const val SAVE_DELAY_MS = 500L
        const val KEY_ID = "id"
    }
}
