// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import org.prauga.reminders.editor.TaskEditorScreen
import org.prauga.reminders.editor.TaskEditorViewModel
import org.prauga.reminders.list.TaskListScreen
import org.prauga.reminders.list.TaskListViewModel

@Serializable internal data object TaskList : NavKey

@Serializable
internal data class TaskEditor(val id: Long = 0, val title: String = "", val text: String = "") :
    NavKey

@Composable
internal fun RemindersNavigation(request: TaskEditor?, onRequestHandled: () -> Unit) {
    val backStack = rememberNavBackStack(TaskList)
    val container = LocalContext.current.appContainer

    LaunchedEffect(request) {
        if (request != null) {
            backStack.add(request)
            onRequestHandled()
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<TaskList> {
                    TaskListScreen(
                        viewModel =
                            viewModel {
                                TaskListViewModel(
                                    container.repository,
                                    container.defaultCollectionName,
                                )
                            },
                        onOpenTask = { backStack.add(TaskEditor(id = it)) },
                        onNewTask = { backStack.add(TaskEditor()) },
                    )
                }
                entry<TaskEditor> { key ->
                    TaskEditorScreen(
                        viewModel =
                            viewModel {
                                TaskEditorViewModel(
                                    container.repository,
                                    key,
                                    container.defaultCollectionName,
                                    container.applicationScope,
                                    createSavedStateHandle(),
                                )
                            },
                        canNotify = container.notifier::canNotify,
                        canScheduleExact = container.scheduler::canScheduleExact,
                        onClose = { backStack.removeLastOrNull() },
                    )
                }
            },
    )
}
