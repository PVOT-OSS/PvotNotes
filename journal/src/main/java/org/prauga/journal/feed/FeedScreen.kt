// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.coreui.PvotErrorContent
import com.prauga.pvot.coreui.PvotLoadingContent
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.components.PvotSegmentedControl
import com.prauga.pvot.designsystem.components.button.PvotFab
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.textfield.PvotTextField
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.theme.PvotSpacing
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.prauga.journal.AttachmentImage
import org.prauga.journal.R
import org.prauga.notes.core.model.Attachment
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.ui.AppDimens
import org.prauga.notes.core.ui.markdown.MarkdownText

@Composable
internal fun FeedScreen(
    viewModel: FeedViewModel,
    fileOf: (Attachment) -> File?,
    onOpenEntry: (Long) -> Unit,
    onNewEntry: (LocalDate?) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        UiState.Loading -> PvotLoadingContent()
        is UiState.Error ->
            PvotErrorContent(
                title = stringResource(R.string.error_title),
                message = stringResource(R.string.error_message),
                onRetry = { viewModel.onEvent(FeedEvent.Retry) },
            )
        is UiState.Success ->
            FeedScaffold(
                state = current.data,
                onEvent = viewModel::onEvent,
                fileOf = fileOf,
                onOpenEntry = onOpenEntry,
                onNewEntry = onNewEntry,
            )
    }
}

@Composable
private fun FeedScaffold(
    state: FeedState,
    onEvent: (FeedEvent) -> Unit,
    fileOf: (Attachment) -> File?,
    onOpenEntry: (Long) -> Unit,
    onNewEntry: (LocalDate?) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val zone = ZoneId.systemDefault()
    PvotScreenScaffold(
        topBar = { PvotTopBar(title = stringResource(R.string.app_name)) },
        floatingAction = {
            PvotFab(
                icon = painterResource(R.drawable.ic_add),
                onClick = { onNewEntry(state.controls.selectedDay) },
                contentDescription = stringResource(R.string.new_entry),
                text = stringResource(R.string.new_entry),
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
            verticalArrangement = Arrangement.spacedBy(PvotSpacing.M),
        ) {
            item {
                WeekStrip(
                    week = state.controls.week,
                    selected = state.controls.selectedDay,
                    daysWithEntries = state.daysWithEntries,
                    locale = locale,
                    onShift = { onEvent(FeedEvent.ShiftWeek(it)) },
                    onSelect = { onEvent(FeedEvent.SelectDay(it)) },
                )
            }
            item { QuickAdd(onAdd = { onEvent(FeedEvent.QuickAdd(it)) }) }
            if (state.controls.selectedDay == null) {
                item {
                    PvotSegmentedControl(
                        options = Grouping.entries.map { stringResource(it.label) },
                        selectedIndex = state.controls.grouping.ordinal,
                        onSelect = { onEvent(FeedEvent.SetGrouping(Grouping.entries[it])) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (state.entries.isEmpty()) {
                item {
                    PvotEmptyContent(
                        message =
                            stringResource(
                                if (state.controls.selectedDay != null) R.string.no_entries_on_day
                                else R.string.no_entries
                            ),
                        modifier = Modifier.padding(top = AppDimens.EmptyStateTop),
                    )
                }
            }
            val grouping =
                if (state.controls.selectedDay != null) Grouping.DAY else state.controls.grouping
            timeline(state.entries, grouping, zone, locale).forEach { (period, entries) ->
                item(key = "period-${period.grouping}-${period.start}") {
                    Text(
                        periodTitle(period, LocalDate.now(zone), locale),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = PvotSpacing.S, start = PvotSpacing.XS),
                    )
                }
                items(entries, key = { it.id }) { entry ->
                    EntryCard(
                        entry,
                        fileOf,
                        grouping,
                        zone,
                        locale,
                        onClick = { onOpenEntry(entry.id) },
                    )
                }
            }
        }
    }
}

private val Grouping.label: Int
    get() =
        when (this) {
            Grouping.DAY -> R.string.group_day
            Grouping.WEEK -> R.string.group_week
            Grouping.MONTH -> R.string.group_month
        }

@Composable
private fun periodTitle(period: Period, today: LocalDate, locale: Locale): String =
    when (period.grouping) {
        Grouping.DAY ->
            when (period.start) {
                today -> stringResource(R.string.today)
                today.minusDays(1) -> stringResource(R.string.yesterday)
                else ->
                    DateTimeFormatter.ofPattern(
                            if (period.start.year == today.year) "EEEE d MMMM"
                            else "EEEE d MMMM yyyy",
                            locale,
                        )
                        .format(period.start)
            }
        Grouping.WEEK ->
            stringResource(
                R.string.week_of,
                DateTimeFormatter.ofPattern("d MMMM yyyy", locale).format(period.start),
            )
        Grouping.MONTH -> DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(period.start)
    }

@Composable
private fun WeekStrip(
    week: LocalDate,
    selected: LocalDate?,
    daysWithEntries: Set<LocalDate>,
    locale: Locale,
    onShift: (Long) -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val today = LocalDate.now()
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onShift(-1) }) {
            Icon(
                painterResource(R.drawable.ic_chevron_left),
                stringResource(R.string.previous_week),
            )
        }
        weekDays(week, locale).forEach { day ->
            val isSelected = day == selected
            val hasEntries = day in daysWithEntries
            val label =
                stringResource(
                    R.string.show_day,
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                        .withLocale(locale)
                        .format(day),
                ) + if (hasEntries) ", " + stringResource(R.string.has_entries) else ""
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(AppDimens.CardCorner))
                        .background(if (isSelected) colors.primary else Color.Transparent)
                        .then(
                            if (day == today && !isSelected)
                                Modifier.border(
                                    AppDimens.Hairline,
                                    colors.primary,
                                    RoundedCornerShape(AppDimens.CardCorner),
                                )
                            else Modifier
                        )
                        .clickable { onSelect(day) }
                        .padding(vertical = PvotSpacing.S)
                        .semantics(mergeDescendants = true) { contentDescription = label },
            ) {
                val content = if (isSelected) colors.onPrimary else colors.onSurface
                Text(
                    DateTimeFormatter.ofPattern("EEEEE", locale).format(day),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) content else colors.onSurfaceVariant,
                )
                Text(
                    day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = content,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(PvotSpacing.XS))
                Box(
                    Modifier.size(AppDimens.Dot)
                        .background(
                            if (hasEntries) (if (isSelected) colors.onPrimary else colors.primary)
                            else Color.Transparent,
                            CircleShape,
                        )
                )
            }
        }
        IconButton(onClick = { onShift(1) }) {
            Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.next_week))
        }
    }
}

@Composable
private fun EntryCard(
    entry: Entry,
    fileOf: (Attachment) -> File?,
    grouping: Grouping,
    zone: ZoneId,
    locale: Locale,
    onClick: () -> Unit,
) {
    val moment = entry.moment(zone)
    val stamp =
        if (grouping == Grouping.DAY) {
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(moment)
        } else {
            DateTimeFormatter.ofPattern("EEE d MMM", locale).format(moment) +
                ", " +
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                    .withLocale(locale)
                    .format(moment)
        }
    PvotCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(PvotSpacing.L),
            verticalArrangement = Arrangement.spacedBy(PvotSpacing.S),
        ) {
            Text(
                stamp,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.summary.isNotBlank()) {
                Text(
                    entry.summary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.description.isNotBlank()) {
                MarkdownText(
                    markdown = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxBlocks = 4,
                )
            }
            if (entry.attachments.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S)) {
                    entry.attachments.take(3).forEach { attachment ->
                        AttachmentImage(
                            file = fileOf(attachment),
                            contentDescription = stringResource(R.string.photo),
                            maxPx = 320,
                            modifier =
                                Modifier.size(AppDimens.Thumbnail)
                                    .clip(RoundedCornerShape(AppDimens.CardCorner)),
                        )
                    }
                    if (entry.attachments.size > 3) {
                        Text(
                            "+${entry.attachments.size - 3}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier.align(Alignment.CenterVertically)
                                    .padding(start = PvotSpacing.XS),
                        )
                    }
                }
            }
            if (entry.categories.isNotEmpty()) {
                Text(
                    entry.categories.joinToString("  ") { "#$it" },
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
        placeholder = stringResource(R.string.quick_entry),
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
                        contentDescription = stringResource(R.string.add_entry),
                        onClick = submit,
                    )
                }
            } else {
                null
            },
        modifier = Modifier.fillMaxWidth(),
    )
}
