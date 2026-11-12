// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.prauga.notes.core.data.EntryRepository
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.Entry

data class TaskRow(val task: Entry, val subtasksDone: Int, val subtasksTotal: Int)

data class TaskListUiState(
    val sections: List<Pair<Section, List<TaskRow>>> = emptyList(),
    val showCompleted: Boolean = false,
    val now: Instant = Instant.now(),
    val loading: Boolean = true,
)

class TaskListViewModel(
    private val repository: EntryRepository,
    private val defaultCollectionName: String,
) : ViewModel() {
    private val showCompleted = MutableStateFlow(false)

    // Re-group every minute so tasks move from Today to Overdue without a database change.
    private val ticker = flow {
        while (true) {
            emit(Instant.now())
            delay(60_000)
        }
    }

    val uiState: StateFlow<TaskListUiState> =
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
                    groupTasks(rows.keys.toList(), now, ZoneId.systemDefault()).map {
                        (section, tasks) ->
                        section to tasks.map { rows.getValue(it) }
                    }
                TaskListUiState(
                    sections = sections,
                    showCompleted = showCompleted,
                    now = now,
                    loading = false,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    fun setDone(id: Long, done: Boolean) {
        viewModelScope.launch { repository.setCompleted(id, done) }
    }

    fun toggleCompleted() = showCompleted.update { !it }

    fun quickAdd(text: String) {
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
