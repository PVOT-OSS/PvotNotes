// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.editor

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotDialogActionStyle
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.components.PvotSegmentedControl
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.navigation.PvotTopBarAction
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.prauga.notes.core.model.EntryTime
import org.prauga.notes.core.ui.PlainTextField
import org.prauga.reminders.R
import org.prauga.reminders.alarm.ReminderNotifier
import org.prauga.reminders.alarm.ReminderScheduler
import org.prauga.reminders.list.DayWords
import org.prauga.reminders.list.Priority
import org.prauga.reminders.list.dueLabel
import org.prauga.reminders.list.label

enum class RepeatPreset(val rule: String?, val label: Int) {
    NONE(null, R.string.repeat_none),
    DAILY("FREQ=DAILY", R.string.repeat_daily),
    WEEKDAYS("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", R.string.repeat_weekdays),
    WEEKLY("FREQ=WEEKLY", R.string.repeat_weekly),
    MONTHLY("FREQ=MONTHLY", R.string.repeat_monthly),
    YEARLY("FREQ=YEARLY", R.string.repeat_yearly),
}

private val ALARM_PRESETS = listOf(0, 10, 60, 24 * 60)

@Composable
fun TaskEditorScreen(
    viewModel: TaskEditorViewModel,
    scheduler: ReminderScheduler,
    notifier: ReminderNotifier,
    onClose: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    PvotScreenScaffold(
        topBar = {
            PvotTopBar(title = "", onBack = onClose) {
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
                scheduler = scheduler,
                notifier = notifier,
                modifier =
                    Modifier.fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            top = padding.calculateTopPadding(),
                            bottom = padding.calculateBottomPadding(),
                        )
                        .padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
    PvotDialog(
        visible = confirmDelete,
        onDismissRequest = { confirmDelete = false },
        title = stringResource(R.string.delete_task_title),
        message =
            if (state.subtasks.isNotEmpty()) stringResource(R.string.delete_task_message) else null,
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
    state: TaskEditorUiState,
    viewModel: TaskEditorViewModel,
    scheduler: ReminderScheduler,
    notifier: ReminderNotifier,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (state.focusTitle) titleFocus.requestFocus() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlainTextField(
            value = state.title,
            onValueChange = { viewModel.setTitle(it.replace("\n", "")) },
            placeholder = stringResource(R.string.title),
            style = typography.headlineSmall.copy(color = colors.onSurface),
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth().focusRequester(titleFocus),
        )
        PlainTextField(
            value = state.notes,
            onValueChange = viewModel::setNotes,
            placeholder = stringResource(R.string.notes),
            style = typography.bodyLarge.copy(color = colors.onSurface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )

        Label(stringResource(R.string.due))
        DueChips(state.due, viewModel)

        if (state.due != null) {
            Label(stringResource(R.string.repeat))
            RepeatChip(state.rrule, viewModel::setRepeat)
        }

        Label(stringResource(R.string.reminders))
        if (state.due == null) {
            Text(
                stringResource(R.string.alarm_needs_date),
                style = typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        } else {
            AlarmChips(state.alarms, viewModel)
            if (state.alarms.isNotEmpty()) PermissionPrompts(scheduler, notifier)
        }

        Label(stringResource(R.string.priority))
        PvotSegmentedControl(
            options = Priority.entries.map { stringResource(it.label) },
            selectedIndex = state.priority.ordinal,
            onSelect = { viewModel.setPriority(Priority.entries[it]) },
            modifier = Modifier.fillMaxWidth(),
        )

        Label(stringResource(R.string.subtasks))
        state.subtasks.forEach { subtask ->
            val done = subtask.completed != null
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = done,
                    onCheckedChange = { viewModel.setSubtaskDone(subtask.id, it) },
                )
                Text(
                    subtask.summary,
                    style = typography.bodyLarge,
                    color = if (done) colors.onSurfaceVariant else colors.onSurface,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.removeSubtask(subtask.id) }) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        stringResource(R.string.remove_subtask, subtask.summary),
                        Modifier.size(18.dp),
                    )
                }
            }
        }
        var newSubtask by rememberSaveable { mutableStateOf("") }
        PlainTextField(
            value = newSubtask,
            onValueChange = { newSubtask = it },
            placeholder = stringResource(R.string.add_subtask),
            style = typography.bodyLarge.copy(color = colors.primary),
            singleLine = true,
            imeAction = ImeAction.Done,
            onImeAction = {
                viewModel.addSubtask(newSubtask)
                newSubtask = ""
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )

        state.lastModified?.let {
            val now = System.currentTimeMillis()
            Spacer(Modifier.height(4.dp))
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
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DueChips(due: EntryTime?, viewModel: TaskEditorViewModel) {
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val locale = LocalConfiguration.current.locales[0]
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (due == null) {
            AssistChip(
                onClick = { viewModel.setDate(today) },
                label = { Text(stringResource(R.string.today)) },
            )
            AssistChip(
                onClick = { viewModel.setDate(today.plusDays(1)) },
                label = { Text(stringResource(R.string.tomorrow)) },
            )
            AssistChip(
                onClick = { pickDate = true },
                label = { Text(stringResource(R.string.pick_date)) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_event), null, Modifier.size(18.dp))
                },
            )
        } else {
            val words =
                DayWords(
                    stringResource(R.string.today),
                    stringResource(R.string.tomorrow),
                    stringResource(R.string.yesterday),
                )
            val dateOnly =
                when (due) {
                    is EntryTime.Date -> due
                    is EntryTime.DateTime -> EntryTime.Date(due.dateTime.toLocalDate())
                }
            InputChip(
                selected = true,
                onClick = { pickDate = true },
                label = { Text(dueLabel(dateOnly, today, zone, words, locale)) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_event), null, Modifier.size(18.dp))
                },
            )
            if (due is EntryTime.DateTime) {
                InputChip(
                    selected = true,
                    onClick = { pickTime = true },
                    label = {
                        Text(
                            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                                .withLocale(locale)
                                .format(due.dateTime.toLocalTime())
                        )
                    },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_schedule), null, Modifier.size(18.dp))
                    },
                )
                AssistChip(
                    onClick = { viewModel.setTime(null) },
                    label = { Text(stringResource(R.string.remove_time)) },
                )
            } else {
                AssistChip(
                    onClick = { pickTime = true },
                    label = { Text(stringResource(R.string.add_time)) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_schedule), null, Modifier.size(18.dp))
                    },
                )
            }
            AssistChip(
                onClick = { viewModel.setDate(null) },
                label = { Text(stringResource(R.string.remove_date)) },
            )
        }
    }
    if (pickDate) {
        val initial =
            (due?.let {
                if (it is EntryTime.Date) it.date
                else (it as EntryTime.DateTime).dateTime.toLocalDate()
            } ?: today)
        val picker =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
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
        val initial = (due as? EntryTime.DateTime)?.dateTime?.toLocalTime() ?: LocalTime.of(9, 0)
        val context = LocalContext.current
        val picker =
            rememberTimePickerState(
                initialHour = initial.hour,
                initialMinute = initial.minute,
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

@Composable
private fun RepeatChip(rrule: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val preset = RepeatPreset.entries.firstOrNull { it.rule == rrule }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = {
                Text(
                    preset?.let { stringResource(it.label) }
                        ?: stringResource(R.string.repeat_custom, rrule.orEmpty())
                )
            },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_repeat), null, Modifier.size(18.dp))
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RepeatPreset.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label)) },
                    onClick = {
                        onSelect(option.rule)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun alarmLabel(minutes: Int): String =
    when {
        minutes == 0 -> stringResource(R.string.alarm_at_due)
        minutes % (24 * 60) == 0 ->
            pluralStringResource(
                R.plurals.alarm_days_before,
                minutes / (24 * 60),
                minutes / (24 * 60),
            )
        minutes % 60 == 0 ->
            pluralStringResource(R.plurals.alarm_hours_before, minutes / 60, minutes / 60)
        else -> pluralStringResource(R.plurals.alarm_minutes_before, minutes, minutes)
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlarmChips(alarms: List<Int>, viewModel: TaskEditorViewModel) {
    var expanded by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        alarms.forEach { minutes ->
            val label = alarmLabel(minutes)
            InputChip(
                selected = false,
                onClick = { viewModel.removeAlarm(minutes) },
                label = { Text(label) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_alarm), null, Modifier.size(18.dp))
                },
                trailingIcon = {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        stringResource(R.string.remove_alarm, label),
                        Modifier.size(16.dp),
                    )
                },
            )
        }
        val remaining = ALARM_PRESETS - alarms.toSet()
        if (remaining.isNotEmpty()) {
            Box {
                AssistChip(
                    onClick = { expanded = true },
                    label = { Text(stringResource(R.string.alarm_add)) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_add), null, Modifier.size(18.dp))
                    },
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    remaining.forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text(alarmLabel(minutes)) },
                            onClick = {
                                viewModel.addAlarm(minutes)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionPrompts(scheduler: ReminderScheduler, notifier: ReminderNotifier) {
    val context = LocalContext.current
    var notificationsOn by remember { mutableStateOf(notifier.canNotify()) }
    var exactOn by remember { mutableStateOf(scheduler.canScheduleExact()) }
    val request =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            notificationsOn = notifier.canNotify()
        }
    LaunchedEffect(Unit) {
        if (!notificationsOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            request.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        notificationsOn = notifier.canNotify()
        exactOn = scheduler.canScheduleExact()
        onPauseOrDispose {}
    }
    if (!notificationsOn) {
        Text(
            stringResource(R.string.notifications_off),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
    if (!exactOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PvotCard(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.exact_alarm_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.exact_alarm_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                "package:${context.packageName}".toUri(),
                            )
                        )
                    }
                ) {
                    Text(stringResource(R.string.exact_alarm_allow))
                }
            }
        }
    }
}
