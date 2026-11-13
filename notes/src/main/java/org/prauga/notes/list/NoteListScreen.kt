// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.coreui.PvotErrorContent
import com.prauga.pvot.coreui.PvotLoadingContent
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotCardDefaults
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.components.button.PvotFab
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.textfield.PvotTextField
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.navigation.PvotTopBarAction
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.notes.R
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.ui.AppDimens
import org.prauga.notes.core.ui.entryContainerColor
import org.prauga.notes.core.ui.markdown.MarkdownText

@Composable
internal fun NoteListScreen(
    viewModel: NoteListViewModel,
    onOpenNote: (Long) -> Unit,
    onNewNote: (Long?) -> Unit,
    onOpenCollections: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        UiState.Loading -> PvotLoadingContent()
        is UiState.Error ->
            PvotErrorContent(
                title = stringResource(R.string.error_title),
                message = stringResource(R.string.error_message),
                onRetry = { viewModel.onEvent(NoteListEvent.Retry) },
            )
        is UiState.Success ->
            NoteListScaffold(
                current.data,
                viewModel::onEvent,
                onOpenNote,
                onNewNote,
                onOpenCollections,
            )
    }
}

@Composable
private fun NoteListScaffold(
    state: NoteListState,
    onEvent: (NoteListEvent) -> Unit,
    onOpenNote: (Long) -> Unit,
    onNewNote: (Long?) -> Unit,
    onOpenCollections: () -> Unit,
) {
    BackHandler(enabled = state.searching) { onEvent(NoteListEvent.SetSearching(false)) }

    PvotScreenScaffold(
        topBar = {
            PvotTopBar(title = stringResource(R.string.app_name)) {
                PvotTopBarAction(
                    icon = painterResource(R.drawable.ic_folder),
                    contentDescription = stringResource(R.string.collections),
                    onClick = onOpenCollections,
                )
                PvotTopBarAction(
                    icon = painterResource(R.drawable.ic_search),
                    contentDescription = stringResource(R.string.search),
                    onClick = { onEvent(NoteListEvent.SetSearching(!state.searching)) },
                )
                PvotTopBarAction(
                    icon =
                        painterResource(
                            if (state.layout == NoteLayout.GRID) R.drawable.ic_view_list
                            else R.drawable.ic_grid_view
                        ),
                    contentDescription =
                        stringResource(
                            if (state.layout == NoteLayout.GRID) R.string.show_as_list
                            else R.string.show_as_grid
                        ),
                    onClick = { onEvent(NoteListEvent.ToggleLayout) },
                )
            }
        },
        floatingAction = {
            PvotFab(
                icon = painterResource(R.drawable.ic_add),
                onClick = { onNewNote(state.selectedCollection) },
                contentDescription = stringResource(R.string.new_note),
                text = stringResource(R.string.new_note),
            )
        },
    ) { padding ->
        NoteListContent(state, padding, onEvent, onOpenNote)
    }
}

@Composable
private fun NoteListContent(
    state: NoteListState,
    padding: PaddingValues,
    onEvent: (NoteListEvent) -> Unit,
    onOpenNote: (Long) -> Unit,
) {
    val columns =
        if (state.layout == NoteLayout.GRID) StaggeredGridCells.Adaptive(AppDimens.GridCell)
        else StaggeredGridCells.Fixed(1)
    LazyVerticalStaggeredGrid(
        columns = columns,
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = PvotSpacing.L,
                end = PvotSpacing.L,
                top = padding.calculateTopPadding() + PvotSpacing.S,
                bottom = padding.calculateBottomPadding() + AppDimens.FabClearance,
            ),
        horizontalArrangement = Arrangement.spacedBy(PvotSpacing.M),
        verticalItemSpacing = PvotSpacing.M,
    ) {
        if (state.searching) {
            item(span = StaggeredGridItemSpan.FullLine) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                PvotTextField(
                    value = state.query,
                    onValueChange = { onEvent(NoteListEvent.SetQuery(it)) },
                    placeholder = stringResource(R.string.search_notes),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
        }
        if (!state.searching) {
            item(span = StaggeredGridItemSpan.FullLine) {
                QuickAdd(onAdd = { onEvent(NoteListEvent.QuickAdd(it)) })
            }
        }
        if (state.collections.size > 1) {
            item(span = StaggeredGridItemSpan.FullLine) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S)) {
                    item {
                        FilterChip(
                            selected = state.selectedCollection == null,
                            onClick = { onEvent(NoteListEvent.SelectCollection(null)) },
                            label = { Text(stringResource(R.string.all_collections)) },
                            leadingIcon = {
                                Icon(
                                    painterResource(R.drawable.ic_folder),
                                    null,
                                    Modifier.size(AppDimens.IconS),
                                )
                            },
                        )
                    }
                    items(state.collections, key = { it.id }) { collection ->
                        FilterChip(
                            selected = state.selectedCollection == collection.id,
                            onClick = {
                                onEvent(
                                    NoteListEvent.SelectCollection(
                                        if (state.selectedCollection == collection.id) null
                                        else collection.id
                                    )
                                )
                            },
                            label = { Text(collection.name) },
                        )
                    }
                }
            }
        }
        if (state.categories.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S)) {
                    item {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { onEvent(NoteListEvent.SelectCategory(null)) },
                            label = { Text(stringResource(R.string.all_notes)) },
                        )
                    }
                    items(state.categories) { category ->
                        FilterChip(
                            selected = state.selectedCategory == category,
                            onClick = {
                                onEvent(
                                    NoteListEvent.SelectCategory(
                                        if (state.selectedCategory == category) null else category
                                    )
                                )
                            },
                            label = { Text(category) },
                        )
                    }
                }
            }
        }
        if (state.notes.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                PvotEmptyContent(
                    message =
                        stringResource(
                            when {
                                state.query.isNotBlank() || state.selectedCategory != null ->
                                    R.string.no_matching_notes
                                state.selectedCollection != null -> R.string.no_notes_in_collection
                                else -> R.string.no_notes
                            }
                        ),
                    modifier = Modifier.padding(top = AppDimens.EmptyStateTop),
                )
            }
        }
        items(state.notes, key = { it.id }) { note ->
            NoteCard(note, onClick = { onOpenNote(note.id) })
        }
    }
}

@Composable
private fun NoteCard(note: Entry, onClick: () -> Unit) {
    PvotCard(
        onClick = onClick,
        colors =
            note.color?.let { PvotCardDefaults.colors(containerColor = entryContainerColor(it)) }
                ?: PvotCardDefaults.colors(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(PvotSpacing.L)) {
            if (note.summary.isNotBlank()) {
                Text(
                    text = note.summary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (note.summary.isNotBlank() && note.description.isNotBlank())
                Spacer(Modifier.height(PvotSpacing.S))
            if (note.description.isNotBlank()) {
                MarkdownText(
                    markdown = note.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxBlocks = 6,
                )
            }
            if (note.categories.isNotEmpty()) {
                Spacer(Modifier.height(PvotSpacing.S))
                Text(
                    text = note.categories.joinToString("  ") { "#$it" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QuickAdd(onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val submit = {
        onAdd(text)
        text = ""
    }
    PvotTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = stringResource(R.string.quick_note),
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        trailingIcon =
            if (text.isNotBlank()) {
                {
                    PvotIconButton(
                        icon = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.add_note),
                        onClick = submit,
                    )
                }
            } else {
                null
            },
        modifier = Modifier.fillMaxWidth(),
    )
}
