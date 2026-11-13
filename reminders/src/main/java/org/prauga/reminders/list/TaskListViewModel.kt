// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.prauga.notes.core.data.EntryRepository
import org.prauga.notes.core.model.Component

@OptIn(ExperimentalCoroutinesApi::class)
internal class TaskListViewModel(
    private val repository: EntryRepository,
    private val defaultCollectionName: String,
) : ViewModel() {
    private val showCompleted = MutableStateFlow(false)
    private val attempt = MutableStateFlow(0)

    // Re-group every minute so tasks move from Today to Overdue without a database change.
    private val ticker = flow {
        while (true) {
            emit(Instant.now())
            delay(60_000)
        }
    }

    val state: StateFlow<UiState<TaskListState>> =
        attempt
            .flatMapLatest {
                tasks()
                    .map<TaskListState, UiState<TaskListState>> { UiState.Success(it) }
                    .catch { emit(UiState.Error(it.message.orEmpty(), it)) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun onEvent(event: TaskListEvent) {
        when (event) {
            is TaskListEvent.SetDone ->
                viewModelScope.launch { repository.setCompleted(event.id, event.done) }
            is TaskListEvent.QuickAdd -> quickAdd(event.text)
            TaskListEvent.ToggleCompleted -> showCompleted.update { !it }
            TaskListEvent.Retry -> attempt.update { it + 1 }
        }
    }

    private fun tasks() =
        combine(repository.observeEntries(Component.VTODO), showCompleted, ticker) {
            all,
            showCompleted,
            now ->
            val children = all.filter { it.parentUid != null }.groupBy { it.parentUid }
            val rows =
                all.filter { it.parentUid == null }
                    .associateWith { task ->
                        val subtasks = children[task.uid].orEmpty()
                        TaskRow(task, subtasks.count { it.completed != null }, subtasks.size)
                    }
            val sections =
                groupTasks(rows.keys.toList(), now, ZoneId.systemDefault()).map { (section, tasks)
                    ->
                    section to tasks.map { rows.getValue(it) }
                }
            TaskListState(sections = sections, showCompleted = showCompleted, now = now)
        }

    private fun quickAdd(text: String) {
        val title = text.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            val collection = repository.defaultCollection(defaultCollectionName)
            repository.save(
                repository.newEntry(Component.VTODO, collection.id).copy(summary = title)
            )
        }
    }
}
