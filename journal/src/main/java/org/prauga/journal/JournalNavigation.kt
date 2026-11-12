// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal

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
import org.prauga.journal.editor.EntryEditorScreen
import org.prauga.journal.editor.EntryEditorViewModel
import org.prauga.journal.feed.FeedScreen
import org.prauga.journal.feed.FeedViewModel

@Serializable data object Feed : NavKey

/** [date] (ISO) is the day a new entry is for; [image] a shared image's content URI. */
@Serializable
data class EntryEditor(
    val id: Long = 0,
    val title: String = "",
    val text: String = "",
    val date: String? = null,
    val image: String? = null,
) : NavKey

@Composable
fun JournalNavigation(request: EntryEditor?, onRequestHandled: () -> Unit) {
    val backStack = rememberNavBackStack(Feed)
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
                entry<Feed> {
                    FeedScreen(
                        viewModel = viewModel { FeedViewModel(container) },
                        attachments = container.attachments,
                        onOpenEntry = { backStack.add(EntryEditor(id = it)) },
                        onNewEntry = { day -> backStack.add(EntryEditor(date = day?.toString())) },
                    )
                }
                entry<EntryEditor> { key ->
                    EntryEditorScreen(
                        viewModel =
                            viewModel {
                                EntryEditorViewModel(container, key, createSavedStateHandle())
                            },
                        attachments = container.attachments,
                        onClose = { backStack.removeLastOrNull() },
                    )
                }
            },
    )
}
