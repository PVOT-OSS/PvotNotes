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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotCardDefaults
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.components.button.PvotFab
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.textfield.PvotTextField
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.navigation.PvotTopBarAction
import org.prauga.notes.R
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.ui.markdown.MarkdownText
import org.prauga.notes.noteContainerColor

@Composable
fun NoteListScreen(
    viewModel: NoteListViewModel,
    onOpenNote: (Long) -> Unit,
    onNewNote: (Long?) -> Unit,
    onOpenCollections: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = state.searching) { viewModel.setSearching(false) }

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
                    onClick = { viewModel.setSearching(!state.searching) },
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
                    onClick = viewModel::toggleLayout,
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
        NoteListContent(state, padding, viewModel, onOpenNote)
    }
}

@Composable
private fun NoteListContent(
    state: NoteListUiState,
    padding: PaddingValues,
    viewModel: NoteListViewModel,
    onOpenNote: (Long) -> Unit,
) {
    val columns =
        if (state.layout == NoteLayout.GRID) StaggeredGridCells.Adaptive(160.dp)
        else StaggeredGridCells.Fixed(1)
    LazyVerticalStaggeredGrid(
        columns = columns,
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp,
    ) {
        if (state.searching) {
            item(span = StaggeredGridItemSpan.FullLine) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                PvotTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = stringResource(R.string.search_notes),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
        }
        if (!state.searching) {
            item(span = StaggeredGridItemSpan.FullLine) { QuickAdd(onAdd = viewModel::quickAdd) }
        }
        if (state.collections.size > 1) {
            item(span = StaggeredGridItemSpan.FullLine) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.selectedCollection == null,
                            onClick = { viewModel.selectCollection(null) },
                            label = { Text(stringResource(R.string.all_collections)) },
                            leadingIcon = {
                                Icon(
                                    painterResource(R.drawable.ic_folder),
                                    null,
                                    Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                    items(state.collections, key = { it.id }) { collection ->
                        FilterChip(
                            selected = state.selectedCollection == collection.id,
                            onClick = {
                                viewModel.selectCollection(
                                    if (state.selectedCollection == collection.id) null
                                    else collection.id
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text(stringResource(R.string.all_notes)) },
                        )
                    }
                    items(state.categories) { category ->
                        FilterChip(
                            selected = state.selectedCategory == category,
                            onClick = {
                                viewModel.selectCategory(
                                    if (state.selectedCategory == category) null else category
                                )
                            },
                            label = { Text(category) },
                        )
                    }
                }
            }
        }
        if (!state.loading && state.notes.isEmpty()) {
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
                    modifier = Modifier.padding(top = 64.dp),
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
            note.color?.let { PvotCardDefaults.colors(containerColor = noteContainerColor(it)) }
                ?: PvotCardDefaults.colors(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                Spacer(Modifier.height(6.dp))
            if (note.description.isNotBlank()) {
                MarkdownText(
                    markdown = note.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxBlocks = 6,
                )
            }
            if (note.categories.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
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
