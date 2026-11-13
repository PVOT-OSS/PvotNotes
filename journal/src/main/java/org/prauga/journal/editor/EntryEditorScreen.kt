// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal.editor

import android.content.Intent
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotDialogActionStyle
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.navigation.PvotTopBarAction
import com.prauga.pvot.designsystem.theme.PvotSpacing
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.prauga.journal.AttachmentImage
import org.prauga.journal.AttachmentStore
import org.prauga.journal.R
import org.prauga.notes.core.model.Attachment
import org.prauga.notes.core.ui.AppDimens
import org.prauga.notes.core.ui.PlainTextField
import org.prauga.notes.core.ui.markdown.MarkdownText

@Composable
fun EntryEditorScreen(
    viewModel: EntryEditorViewModel,
    attachments: AttachmentStore,
    onClose: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Attachment?>(null) }
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
                attachments = attachments,
                onViewPhoto = { viewing = it },
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
        title = stringResource(R.string.delete_entry_title),
        message =
            if (state.attachments.isNotEmpty()) stringResource(R.string.delete_entry_message)
            else null,
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
    viewing?.let { photo ->
        Dialog(
            onDismissRequest = { viewing = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim).clickable {
                    viewing = null
                }
            ) {
                AttachmentImage(
                    file = attachments.file(photo),
                    contentDescription = stringResource(R.string.photo),
                    maxPx = 2048,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorContent(
    state: EntryEditorUiState,
    viewModel: EntryEditorViewModel,
    attachments: AttachmentStore,
    onViewPhoto: (Attachment) -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val pickPhotos =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris
            ->
            if (uris.isNotEmpty()) viewModel.addPhotos(uris)
        }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(PvotSpacing.M)) {
        MomentChips(state, viewModel)
        if (state.editing) {
            val bodyFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) { if (state.focusBody) bodyFocus.requestFocus() }
            PlainTextField(
                value = state.title,
                onValueChange = { viewModel.setTitle(it.replace("\n", "")) },
                placeholder = stringResource(R.string.title),
                style = typography.headlineSmall.copy(color = colors.onSurface),
                imeAction = ImeAction.Next,
                onImeAction = { bodyFocus.requestFocus() },
                modifier = Modifier.fillMaxWidth(),
            )
            PlainTextField(
                value = state.body,
                onValueChange = viewModel::setBody,
                placeholder = stringResource(R.string.body),
                style = typography.bodyLarge.copy(color = colors.onSurface),
                modifier = Modifier.fillMaxWidth().focusRequester(bodyFocus),
            )
        } else {
            Column(
                Modifier.fillMaxWidth().clickable(interactionSource = null, indication = null) {
                    viewModel.setEditing(true)
                },
                verticalArrangement = Arrangement.spacedBy(PvotSpacing.M),
            ) {
                if (state.title.isNotBlank())
                    Text(state.title, style = typography.headlineSmall, color = colors.onSurface)
                if (state.body.isNotBlank()) {
                    MarkdownText(markdown = state.body, onToggleTask = viewModel::toggleTask)
                } else {
                    Text(
                        stringResource(R.string.body),
                        style = typography.bodyLarge,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
            verticalArrangement = Arrangement.spacedBy(PvotSpacing.S),
        ) {
            state.attachments.forEach { photo ->
                Box(Modifier.size(AppDimens.PhotoTile)) {
                    AttachmentImage(
                        file = attachments.file(photo),
                        contentDescription = stringResource(R.string.photo),
                        maxPx = 360,
                        modifier =
                            Modifier.fillMaxSize()
                                .clip(RoundedCornerShape(AppDimens.PhotoCorner))
                                .clickable { onViewPhoto(photo) },
                    )
                    Box(
                        Modifier.align(Alignment.TopEnd)
                            .size(AppDimens.TouchTarget)
                            .clip(CircleShape)
                            .clickable { viewModel.removePhoto(photo) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(AppDimens.PhotoRemoveBadge)
                                .clip(CircleShape)
                                .background(colors.surface.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_close),
                                stringResource(R.string.remove_photo),
                                Modifier.size(AppDimens.IconS),
                            )
                        }
                    }
                }
            }
        }
        AssistChip(
            onClick = {
                pickPhotos.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            label = { Text(stringResource(R.string.add_photos)) },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_add_photo), null, Modifier.size(AppDimens.IconM))
            },
        )

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
                            stringResource(R.string.remove_tag, category),
                            Modifier.size(AppDimens.IconS),
                        )
                    },
                )
            }
        }
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
                Modifier.widthIn(min = AppDimens.TagFieldMin).padding(vertical = PvotSpacing.XS),
        )

        state.lastModified?.let {
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MomentChips(state: EntryEditorUiState, viewModel: EntryEditorViewModel) {
    val locale = LocalConfiguration.current.locales[0]
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S)) {
        AssistChip(
            onClick = { pickDate = true },
            label = {
                Text(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        .withLocale(locale)
                        .format(state.moment)
                )
            },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_event), null, Modifier.size(AppDimens.IconM))
            },
        )
        AssistChip(
            onClick = { pickTime = true },
            label = {
                Text(
                    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                        .withLocale(locale)
                        .format(state.moment)
                )
            },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_schedule), null, Modifier.size(AppDimens.IconM))
            },
        )
    }
    if (pickDate) {
        val picker =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    state.moment
                        .toLocalDate()
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
            )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        picker.selectedDateMillis?.let {
                            viewModel.setDate(
                                Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                            )
                        }
                        pickDate = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = picker)
        }
    }
    if (pickTime) {
        val context = LocalContext.current
        val picker =
            rememberTimePickerState(
                initialHour = state.moment.hour,
                initialMinute = state.moment.minute,
                is24Hour = android.text.format.DateFormat.is24HourFormat(context),
            )
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setTime(LocalTime.of(picker.hour, picker.minute))
                        pickTime = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.cancel)) }
            },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = picker)
                }
            },
        )
    }
}
