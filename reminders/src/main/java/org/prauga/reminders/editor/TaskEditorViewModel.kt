// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
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

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal class TaskEditorViewModel(
    private val repository: EntryRepository,
    key: TaskEditor,
    defaultCollectionName: String,
    private val applicationScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val form = MutableStateFlow<TaskEditorState?>(null)
    val state: StateFlow<UiState<TaskEditorState>> =
        form
            .map<TaskEditorState?, UiState<TaskEditorState>> { current ->
                current?.let { UiState.Success(it) } ?: UiState.Loading
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val closed = Channel<TaskEditorEffect>(Channel.BUFFERED)
    val effects: Flow<TaskEditorEffect> = closed.receiveAsFlow()

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
            form.value =
                TaskEditorState(
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
            uid.value = loaded.uid
            if (loaded.id == 0L && !loaded.isBlank()) edits.send(Unit)
        }
        viewModelScope.launch {
            uid.filterNotNull()
                .flatMapLatest { repository.observeChildren(it) }
                .collect { children -> form.update { it?.copy(subtasks = children) } }
        }
        viewModelScope.launch { edits.receiveAsFlow().debounce(SAVE_DELAY_MS).collect { save() } }
    }

    fun onEvent(event: TaskEditorEvent) {
        when (event) {
            is TaskEditorEvent.SetTitle -> edit { it.copy(title = event.title) }
            is TaskEditorEvent.SetNotes -> edit { it.copy(notes = event.notes) }
            is TaskEditorEvent.SetDate -> edit { it.withDate(event.date) }
            is TaskEditorEvent.SetTime -> edit { it.withTime(event.time) }
            is TaskEditorEvent.SetRepeat -> edit { it.copy(rrule = event.rrule) }
            is TaskEditorEvent.AddAlarm ->
                edit { it.copy(alarms = (it.alarms + event.minutesBefore).distinct().sorted()) }
            is TaskEditorEvent.RemoveAlarm ->
                edit { it.copy(alarms = it.alarms - event.minutesBefore) }
            is TaskEditorEvent.SetPriority -> edit { it.copy(priority = event.priority) }
            is TaskEditorEvent.AddSubtask -> addSubtask(event.title)
            is TaskEditorEvent.SetSubtaskDone ->
                viewModelScope.launch { repository.setCompleted(event.id, event.done) }
            is TaskEditorEvent.RemoveSubtask ->
                viewModelScope.launch { repository.delete(event.id) }
            TaskEditorEvent.Delete -> delete()
        }
    }

    private fun addSubtask(title: String) {
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

    private fun delete() {
        deleted = true
        val id = entry?.id ?: 0L
        applicationScope.launch { saveLock.withLock { if (id != 0L) repository.delete(id) } }
        closed.trySend(TaskEditorEffect.Closed)
    }

    override fun onCleared() {
        if (!deleted) applicationScope.launch { save() }
    }

    private fun edit(transform: (TaskEditorState) -> TaskEditorState) {
        if (form.value == null) return
        form.update { it?.let(transform) }
        edits.trySend(Unit)
    }

    private suspend fun save(force: Boolean = false) =
        saveLock.withLock {
            val base = entry ?: return@withLock
            if (deleted) return@withLock
            val state = form.value ?: return@withLock
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
            form.update { it?.copy(lastModified = stored.lastModified, saved = true) }
        }

    private fun TaskEditorState.withDate(date: LocalDate?): TaskEditorState =
        when {
            date == null -> copy(due = null, rrule = null, alarms = emptyList())
            due is EntryTime.DateTime ->
                copy(
                    due =
                        EntryTime.DateTime(
                            date.atTime(due.dateTime.toLocalTime()),
                            ZoneId.systemDefault(),
                        )
                )
            else -> copy(due = EntryTime.Date(date))
        }

    // Setting a time on a task with no reminders adds one at the due time.
    private fun TaskEditorState.withTime(time: LocalTime?): TaskEditorState {
        val date =
            when (val current = due) {
                is EntryTime.Date -> current.date
                is EntryTime.DateTime -> current.dateTime.toLocalDate()
                null -> LocalDate.now()
            }
        return if (time == null) {
            copy(due = EntryTime.Date(date))
        } else {
            copy(
                due = EntryTime.DateTime(date.atTime(time), ZoneId.systemDefault()),
                alarms = alarms.ifEmpty { listOf(0) },
            )
        }
    }

    private fun savedId(key: TaskEditor): Long? =
        key.id.takeIf { it != 0L } ?: savedStateHandle.get<Long>(KEY_ID)

    private fun Entry.isBlank() = summary.isBlank() && description.isBlank() && due == null

    private companion object {
        const val SAVE_DELAY_MS = 500L
        const val KEY_ID = "id"
    }
}
