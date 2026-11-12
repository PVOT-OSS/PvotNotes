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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.components.button.PvotFab
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.textfield.PvotTextField
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import java.time.Instant
import java.time.ZoneId
import org.prauga.reminders.R

@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    onOpenTask: (Long) -> Unit,
    onNewTask: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PvotScreenScaffold(
        topBar = { PvotTopBar(title = stringResource(R.string.app_name)) },
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
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { QuickAdd(onAdd = viewModel::quickAdd) }
            if (!state.loading && state.sections.none { it.first != Section.COMPLETED }) {
                item {
                    PvotEmptyContent(
                        message = stringResource(R.string.no_reminders),
                        modifier = Modifier.padding(top = 48.dp),
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
                        onToggle = viewModel::toggleCompleted,
                    )
                }
                if (!completed || state.showCompleted) {
                    items(rows, key = { it.task.id }) { row ->
                        TaskRowCard(
                            row = row,
                            overdue = section == Section.OVERDUE,
                            now = state.now,
                            onToggle = {
                                viewModel.setDone(row.task.id, row.task.completed == null)
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
        modifier = modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (expanded != null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                painterResource(
                    if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
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
            modifier = Modifier.padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
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
                modifier = Modifier.padding(4.dp).semantics { contentDescription = toggleLabel },
            )
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
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
                    Icon(
                        painterResource(R.drawable.ic_flag),
                        contentDescription = stringResource(priority.label),
                        tint = priority.color(),
                        modifier = Modifier.size(18.dp),
                    )
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (parts.isNotEmpty())
            Text(
                parts.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = tint,
            )
        if (task.alarms.isNotEmpty())
            Icon(painterResource(R.drawable.ic_alarm), null, Modifier.size(14.dp), tint = tint)
        if (task.rrule != null)
            Icon(painterResource(R.drawable.ic_repeat), null, Modifier.size(14.dp), tint = tint)
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

val Priority.label: Int
    get() =
        when (this) {
            Priority.NONE -> R.string.priority_none
            Priority.LOW -> R.string.priority_low
            Priority.MEDIUM -> R.string.priority_medium
            Priority.HIGH -> R.string.priority_high
        }

@Composable
fun Priority.color(): Color =
    when (this) {
        Priority.HIGH -> MaterialTheme.colorScheme.error
        Priority.MEDIUM -> Color(0xFFFFA500)
        Priority.LOW -> MaterialTheme.colorScheme.primary
        Priority.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
