// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.editor

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotDialogActionStyle
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.navigation.PvotTopBarAction
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.notes.R
import org.prauga.notes.core.model.EntryCollection
import org.prauga.notes.core.ui.AppDimens
import org.prauga.notes.core.ui.EntryColorRow
import org.prauga.notes.core.ui.PlainTextField
import org.prauga.notes.core.ui.markdown.MarkdownText

@Composable
fun NoteEditorScreen(viewModel: NoteEditorViewModel, onClose: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    val shareTitle = stringResource(R.string.share)

    PvotScreenScaffold(
        topBar = {
            PvotTopBar(title = "", onBack = onClose) {
                PvotTopBarAction(
                    icon =
                        painterResource(
                            if (state.editing) R.drawable.ic_check else R.drawable.ic_edit
                        ),
                    contentDescription =
                        stringResource(if (state.editing) R.string.done else R.string.edit),
                    onClick = { viewModel.setEditing(!state.editing) },
                )
                PvotTopBarAction(
                    icon = painterResource(R.drawable.ic_share),
                    contentDescription = shareTitle,
                    enabled = state.title.isNotBlank() || state.body.isNotBlank(),
                    onClick = {
                        val send =
                            Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, viewModel.shareText())
                        if (state.title.isNotBlank())
                            send.putExtra(Intent.EXTRA_SUBJECT, state.title)
                        context.startActivity(Intent.createChooser(send, shareTitle))
                    },
                )
                PvotTopBarAction(
                    icon = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.delete),
                    onClick = {
                        if (state.saved) confirmDelete = true else viewModel.delete(onClose)
                    },
                )
            }
        }
    ) { padding ->
        if (!state.loading) {
            EditorContent(
                state = state,
                viewModel = viewModel,
                modifier =
                    Modifier.fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            top = padding.calculateTopPadding(),
                            bottom = padding.calculateBottomPadding(),
                        )
                        .padding(horizontal = AppDimens.ScreenGutter, vertical = PvotSpacing.S),
            )
        }
    }

    PvotDialog(
        visible = confirmDelete,
        onDismissRequest = { confirmDelete = false },
        title = stringResource(R.string.delete_note_title),
        message = stringResource(R.string.delete_note_message),
        destructive = true,
    ) {
        PvotDialogAction(
            label = stringResource(R.string.delete),
            style = PvotDialogActionStyle.Destructive,
            onClick = {
                confirmDelete = false
                viewModel.delete(onClose)
            },
        )
        PvotDialogAction(
            label = stringResource(R.string.cancel),
            onClick = { confirmDelete = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorContent(
    state: NoteEditorUiState,
    viewModel: NoteEditorViewModel,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Column(modifier = modifier) {
        val titleFocus = remember { FocusRequester() }
        val bodyFocus = remember { FocusRequester() }
        if (state.editing) {
            LaunchedEffect(Unit) {
                if (state.focusTitle) titleFocus.requestFocus() else bodyFocus.requestFocus()
            }
            PlainTextField(
                value = state.title,
                onValueChange = { value ->
                    viewModel.setTitle(value.replace("\n", ""))
                    if (value.contains('\n')) bodyFocus.requestFocus()
                },
                placeholder = stringResource(R.string.title),
                style = typography.headlineSmall.copy(color = colors.onSurface),
                imeAction = ImeAction.Next,
                onImeAction = { bodyFocus.requestFocus() },
                modifier = Modifier.fillMaxWidth().focusRequester(titleFocus),
            )
            Spacer(Modifier.height(PvotSpacing.M))
            PlainTextField(
                value = state.body,
                onValueChange = viewModel::setBody,
                placeholder = stringResource(R.string.note),
                style = typography.bodyLarge.copy(color = colors.onSurface),
                modifier =
                    Modifier.fillMaxWidth()
                        .focusRequester(bodyFocus)
                        .padding(bottom = AppDimens.BlockGap),
            )
        } else {
            val startEditing = { viewModel.setEditing(true) }
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = startEditing,
                        )
                        .padding(bottom = AppDimens.BlockGap)
            ) {
                if (state.title.isNotBlank()) {
                    Text(state.title, style = typography.headlineSmall, color = colors.onSurface)
                    Spacer(Modifier.height(PvotSpacing.M))
                }
                if (state.body.isNotBlank()) {
                    MarkdownText(markdown = state.body, onToggleTask = viewModel::toggleTask)
                } else {
                    Text(
                        stringResource(R.string.note),
                        style = typography.bodyLarge,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
            verticalArrangement = Arrangement.spacedBy(PvotSpacing.XS),
        ) {
            state.categories.forEach { category ->
                InputChip(
                    selected = false,
                    onClick = { viewModel.removeCategory(category) },
                    label = { Text("#$category") },
                    trailingIcon = {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.remove_tag, category),
                            modifier = Modifier.size(AppDimens.IconS),
                        )
                    },
                )
            }
        }
        if (state.collections.size > 1) {
            CollectionPicker(
                collections = state.collections,
                selected = state.collectionId,
                onSelect = viewModel::setCollection,
                modifier = Modifier.padding(top = PvotSpacing.M),
            )
        }
        EntryColorRow(
            selected = state.color,
            onSelect = viewModel::setColor,
            modifier = Modifier.padding(top = PvotSpacing.M, bottom = PvotSpacing.XS),
        )
        var newTag by rememberSaveable { mutableStateOf("") }
        PlainTextField(
            value = newTag,
            onValueChange = { value ->
                if (value.endsWith(',')) {
                    viewModel.addCategory(value.dropLast(1))
                    newTag = ""
                } else {
                    newTag = value
                }
            },
            placeholder = stringResource(R.string.add_tag),
            style = typography.bodyMedium.copy(color = colors.primary),
            singleLine = true,
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Done,
            onImeAction = {
                viewModel.addCategory(newTag)
                newTag = ""
            },
            modifier =
                Modifier.widthIn(min = AppDimens.TagFieldMin).padding(vertical = PvotSpacing.S),
        )
        state.lastModified?.let {
            Spacer(Modifier.height(PvotSpacing.S))
            val now = System.currentTimeMillis()
            Text(
                text =
                    if (now - it.toEpochMilli() < DateUtils.MINUTE_IN_MILLIS) {
                        stringResource(R.string.edited_just_now)
                    } else {
                        stringResource(
                            R.string.edited_at,
                            DateUtils.getRelativeTimeSpanString(
                                it.toEpochMilli(),
                                now,
                                DateUtils.MINUTE_IN_MILLIS,
                            ),
                        )
                    },
                style = typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CollectionPicker(
    collections: List<EntryCollection>,
    selected: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = collections.firstOrNull { it.id == selected }
    Box(modifier) {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(current?.name.orEmpty()) },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_folder), null, Modifier.size(AppDimens.IconM))
            },
            trailingIcon = {
                Icon(
                    painterResource(R.drawable.ic_arrow_drop_down),
                    stringResource(R.string.collection),
                    Modifier.size(AppDimens.IconM),
                )
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            collections.forEach { collection ->
                DropdownMenuItem(
                    text = { Text(collection.name) },
                    onClick = {
                        onSelect(collection.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
