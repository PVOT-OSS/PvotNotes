// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.coreui.PvotErrorContent
import com.prauga.pvot.coreui.PvotLoadingContent
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.components.button.PvotFab
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.textfield.PvotTextField
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.navigation.rememberPvotTopBarCompact
import com.prauga.pvot.designsystem.theme.PvotSpacing
import java.time.Instant
import java.time.ZoneId
import org.prauga.notes.core.ui.AppDimens
import org.prauga.reminders.R

@Composable
internal fun TaskListScreen(
    viewModel: TaskListViewModel,
    onOpenTask: (Long) -> Unit,
    onNewTask: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        UiState.Loading -> PvotLoadingContent()
        is UiState.Error ->
            PvotErrorContent(
                title = stringResource(R.string.error_title),
                message = stringResource(R.string.error_message),
                onRetry = { viewModel.onEvent(TaskListEvent.Retry) },
            )
        is UiState.Success ->
            TaskListScaffold(
                state = current.data,
                onEvent = viewModel::onEvent,
                onOpenTask = onOpenTask,
                onNewTask = onNewTask,
            )
    }
}

@Composable
private fun TaskListScaffold(
    state: TaskListState,
    onEvent: (TaskListEvent) -> Unit,
    onOpenTask: (Long) -> Unit,
    onNewTask: () -> Unit,
) {
    val listState = rememberLazyListState()
    val compact = rememberPvotTopBarCompact(listState)
    PvotScreenScaffold(
        topBar = { PvotTopBar(title = stringResource(R.string.app_name), compact = compact) },
        floatingAction = {
            PvotFab(
                icon = painterResource(R.drawable.ic_add),
                onClick = onNewTask,
                contentDescription = stringResource(R.string.new_reminder),
                text = stringResource(R.string.new_reminder),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = PvotSpacing.L,
                    end = PvotSpacing.L,
                    top = padding.calculateTopPadding() + PvotSpacing.S,
                    bottom = padding.calculateBottomPadding() + AppDimens.FabClearance,
                ),
            verticalArrangement = Arrangement.spacedBy(PvotSpacing.S),
        ) {
            item { QuickAdd(onAdd = { onEvent(TaskListEvent.QuickAdd(it)) }) }
            if (state.sections.none { it.first != Section.COMPLETED }) {
                item {
                    PvotEmptyContent(
                        message = stringResource(R.string.no_reminders),
                        modifier = Modifier.padding(top = AppDimens.EmptyStateTop),
                    )
                }
            }
            state.sections.forEach { (section, rows) ->
                val completed = section == Section.COMPLETED
                item(key = "header-$section") {
                    SectionHeader(
                        title =
                            when (section) {
                                Section.OVERDUE -> stringResource(R.string.section_overdue)
                                Section.TODAY -> stringResource(R.string.section_today)
                                Section.UPCOMING -> stringResource(R.string.section_upcoming)
                                Section.NO_DATE -> stringResource(R.string.section_no_date)
                                Section.COMPLETED ->
                                    stringResource(R.string.section_completed, rows.size)
                            },
                        expanded = if (completed) state.showCompleted else null,
                        onToggle = { onEvent(TaskListEvent.ToggleCompleted) },
                    )
                }
                if (!completed || state.showCompleted) {
                    items(rows, key = { it.task.id }) { row ->
                        TaskRowCard(
                            row = row,
                            overdue = section == Section.OVERDUE,
                            now = state.now,
                            onToggle = {
                                onEvent(
                                    TaskListEvent.SetDone(row.task.id, row.task.completed == null)
                                )
                            },
                            onOpen = { onOpenTask(row.task.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, expanded: Boolean?, onToggle: () -> Unit) {
    val modifier = if (expanded != null) Modifier.clickable(onClick = onToggle) else Modifier
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = PvotSpacing.M, bottom = PvotSpacing.XS, start = PvotSpacing.XS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (expanded != null) {
            Spacer(Modifier.width(PvotSpacing.XS))
            Icon(
                painterResource(
                    if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppDimens.IconL),
            )
        }
    }
}

@Composable
private fun TaskRowCard(
    row: TaskRow,
    overdue: Boolean,
    now: Instant,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val task = row.task
    val done = task.completed != null
    val colors = MaterialTheme.colorScheme
    PvotCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier.padding(
                    end = PvotSpacing.L,
                    top = PvotSpacing.XS,
                    bottom = PvotSpacing.XS,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val toggleLabel =
                stringResource(
                    if (done) R.string.mark_not_done else R.string.mark_done,
                    task.summary,
                )
            Checkbox(
                checked = done,
                onCheckedChange = { onToggle() },
                modifier =
                    Modifier.padding(PvotSpacing.XS).semantics { contentDescription = toggleLabel },
            )
            Column(modifier = Modifier.weight(1f).padding(vertical = PvotSpacing.S)) {
                Text(
                    text = task.summary.ifBlank { " " },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (done) colors.onSurfaceVariant else colors.onSurface,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                MetaRow(row, overdue, now)
            }
            Priority.of(task.priority)
                .takeIf { it != Priority.NONE }
                ?.let { priority ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.ic_flag),
                            contentDescription = null,
                            tint = priority.color(),
                            modifier = Modifier.size(AppDimens.IconM),
                        )
                        Text(
                            stringResource(priority.label),
                            style = MaterialTheme.typography.labelSmall,
                            color = priority.color(),
                        )
                    }
                }
        }
    }
}

@Composable
private fun MetaRow(row: TaskRow, overdue: Boolean, now: Instant) {
    val task = row.task
    val zone = ZoneId.systemDefault()
    val words =
        DayWords(
            stringResource(R.string.today),
            stringResource(R.string.tomorrow),
            stringResource(R.string.yesterday),
        )
    val parts = buildList {
        task.due?.let {
            add(
                dueLabel(
                    it,
                    now.atZone(zone).toLocalDate(),
                    zone,
                    words,
                    LocalConfiguration.current.locales[0],
                )
            )
        }
        if (row.subtasksTotal > 0)
            add(stringResource(R.string.subtask_progress, row.subtasksDone, row.subtasksTotal))
    }
    if (parts.isEmpty() && task.rrule == null && task.alarms.isEmpty()) return
    val tint =
        if (overdue && task.completed == null) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
    ) {
        if (parts.isNotEmpty())
            Text(
                parts.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = tint,
            )
        if (task.alarms.isNotEmpty())
            Icon(
                painterResource(R.drawable.ic_alarm),
                null,
                Modifier.size(AppDimens.IconXs),
                tint = tint,
            )
        if (task.rrule != null)
            Icon(
                painterResource(R.drawable.ic_repeat),
                null,
                Modifier.size(AppDimens.IconXs),
                tint = tint,
            )
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
        placeholder = stringResource(R.string.quick_reminder),
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
                        contentDescription = stringResource(R.string.add_reminder_item),
                        onClick = submit,
                    )
                }
            } else {
                null
            },
        modifier = Modifier.fillMaxWidth(),
    )
}

internal val Priority.label: Int
    get() =
        when (this) {
            Priority.NONE -> R.string.priority_none
            Priority.LOW -> R.string.priority_low
            Priority.MEDIUM -> R.string.priority_medium
            Priority.HIGH -> R.string.priority_high
        }

@Composable
internal fun Priority.color(): Color =
    when (this) {
        Priority.HIGH -> MaterialTheme.colorScheme.error
        Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
        Priority.LOW -> MaterialTheme.colorScheme.primary
        Priority.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
