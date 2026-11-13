// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotErrorContent
import com.prauga.pvot.coreui.PvotLoadingContent
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotDialogActionStyle
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.components.button.PvotFab
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotListRow
import com.prauga.pvot.designsystem.components.textfield.PvotTextField
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.notes.R
import org.prauga.notes.core.data.CollectionCount
import org.prauga.notes.core.ui.AppDimens
import org.prauga.notes.core.ui.EntryColorRow

private data class CollectionDraft(val id: Long = 0, val name: String = "", val color: Int? = null)

@Composable
internal fun CollectionsScreen(viewModel: CollectionsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        UiState.Loading -> PvotLoadingContent()
        is UiState.Error ->
            PvotErrorContent(
                title = stringResource(R.string.error_title),
                message = stringResource(R.string.error_message),
                onRetry = { viewModel.onEvent(CollectionsEvent.Retry) },
            )
        is UiState.Success -> CollectionsContent(current.data, viewModel::onEvent, onBack)
    }
}

@Composable
private fun CollectionsContent(
    collections: List<CollectionCount>,
    onEvent: (CollectionsEvent) -> Unit,
    onBack: () -> Unit,
) {
    var draft by remember { mutableStateOf<CollectionDraft?>(null) }
    var deleting by remember { mutableStateOf<CollectionCount?>(null) }

    PvotScreenScaffold(
        topBar = { PvotTopBar(title = stringResource(R.string.collections), onBack = onBack) },
        floatingAction = {
            PvotFab(
                icon = painterResource(R.drawable.ic_add),
                onClick = { draft = CollectionDraft() },
                contentDescription = stringResource(R.string.new_collection),
                text = stringResource(R.string.new_collection),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = PvotSpacing.L,
                    end = PvotSpacing.L,
                    top = padding.calculateTopPadding() + PvotSpacing.S,
                    bottom = padding.calculateBottomPadding() + AppDimens.FabClearance,
                ),
        ) {
            item {
                PvotListGroup {
                    collections.forEach { collection ->
                        item {
                            PvotListRow(
                                title = collection.name,
                                supportingText =
                                    pluralStringResource(
                                        R.plurals.note_count,
                                        collection.count,
                                        collection.count,
                                    ),
                                leading = {
                                    Box(
                                        Modifier.size(AppDimens.IconL)
                                            .background(
                                                collection.color?.let(::Color)
                                                    ?: MaterialTheme.colorScheme.outline,
                                                CircleShape,
                                            )
                                    )
                                },
                                onClick = {
                                    draft =
                                        CollectionDraft(
                                            collection.id,
                                            collection.name,
                                            collection.color,
                                        )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    draft?.let { current ->
        var name by rememberSaveable(current.id) { mutableStateOf(current.name) }
        var color by rememberSaveable(current.id) { mutableStateOf(current.color) }
        PvotDialog(
            visible = true,
            onDismissRequest = { draft = null },
            title =
                stringResource(
                    if (current.id == 0L) R.string.new_collection else R.string.edit_collection
                ),
            content = {
                PvotTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.collection_name),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                EntryColorRow(
                    selected = color,
                    onSelect = { color = it },
                    modifier = Modifier.padding(top = PvotSpacing.M),
                )
            },
        ) {
            PvotDialogAction(
                label = stringResource(R.string.save),
                style = PvotDialogActionStyle.Primary,
                enabled = name.isNotBlank(),
                onClick = {
                    onEvent(CollectionsEvent.Save(current.id, name, color))
                    draft = null
                },
            )
            if (current.id != 0L && collections.size > 1) {
                PvotDialogAction(
                    label = stringResource(R.string.delete),
                    style = PvotDialogActionStyle.Destructive,
                    onClick = {
                        deleting = collections.firstOrNull { it.id == current.id }
                        draft = null
                    },
                )
            }
            PvotDialogAction(label = stringResource(R.string.cancel), onClick = { draft = null })
        }
    }

    deleting?.let { collection ->
        PvotDialog(
            visible = true,
            onDismissRequest = { deleting = null },
            title = stringResource(R.string.delete_collection_title, collection.name),
            message =
                pluralStringResource(
                    R.plurals.delete_collection_message,
                    collection.count,
                    collection.count,
                ),
            destructive = true,
        ) {
            PvotDialogAction(
                label = stringResource(R.string.delete),
                style = PvotDialogActionStyle.Destructive,
                onClick = {
                    onEvent(CollectionsEvent.Delete(collection.id))
                    deleting = null
                },
            )
            PvotDialogAction(label = stringResource(R.string.cancel), onClick = { deleting = null })
        }
    }
}
