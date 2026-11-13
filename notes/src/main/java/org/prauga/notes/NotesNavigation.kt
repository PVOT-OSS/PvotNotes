// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes

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
import org.prauga.notes.collections.CollectionsScreen
import org.prauga.notes.collections.CollectionsViewModel
import org.prauga.notes.editor.NoteEditorScreen
import org.prauga.notes.editor.NoteEditorViewModel
import org.prauga.notes.list.NoteListScreen
import org.prauga.notes.list.NoteListViewModel

@Serializable internal data object NoteList : NavKey

@Serializable
internal data class NoteEditor(
    val id: Long = 0,
    val title: String = "",
    val text: String = "",
    val collectionId: Long = 0,
) : NavKey

@Serializable internal data object Collections : NavKey

@Composable
internal fun NotesNavigation(request: NoteEditor?, onRequestHandled: () -> Unit) {
    val backStack = rememberNavBackStack(NoteList)
    val container = LocalContext.current.appContainer
    val repository = container.repository

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
                entry<NoteList> {
                    NoteListScreen(
                        viewModel =
                            viewModel {
                                NoteListViewModel(
                                    repository,
                                    container.preferences,
                                    container.defaultCollectionName,
                                )
                            },
                        onOpenNote = { backStack.add(NoteEditor(id = it)) },
                        onNewNote = { backStack.add(NoteEditor(collectionId = it ?: 0)) },
                        onOpenCollections = { backStack.add(Collections) },
                    )
                }
                entry<Collections> {
                    CollectionsScreen(
                        viewModel = viewModel { CollectionsViewModel(repository) },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<NoteEditor> { key ->
                    NoteEditorScreen(
                        viewModel =
                            viewModel {
                                NoteEditorViewModel(
                                    repository,
                                    key,
                                    container.defaultCollectionName,
                                    container.applicationScope,
                                    createSavedStateHandle(),
                                )
                            },
                        onClose = { backStack.removeLastOrNull() },
                    )
                }
            },
    )
}
