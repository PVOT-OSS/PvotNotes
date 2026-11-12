// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.prauga.journal.AppContainer
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.model.EntryTime

data class FeedControls(
    val grouping: Grouping,
    val week: LocalDate = LocalDate.now(),
    val selectedDay: LocalDate? = null,
)

data class FeedUiState(
    val controls: FeedControls = FeedControls(Grouping.DAY),
    val entries: List<Entry> = emptyList(),
    val daysWithEntries: Set<LocalDate> = emptySet(),
    val loading: Boolean = true,
)

class FeedViewModel(private val container: AppContainer) : ViewModel() {
    private val repository = container.repository
    private val controls = MutableStateFlow(FeedControls(container.grouping))

    val uiState: StateFlow<FeedUiState> =
        combine(repository.observeEntries(Component.VJOURNAL), controls) { entries, controls ->
                val zone = ZoneId.systemDefault()
                val days = entries.map { it.moment(zone).toLocalDate() }.toSet()
                val shown =
                    controls.selectedDay?.let { day ->
                        entries.filter { it.moment(zone).toLocalDate() == day }
                    } ?: entries
                FeedUiState(
                    controls = controls,
                    entries = shown,
                    daysWithEntries = days,
                    loading = false,
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                FeedUiState(controls.value),
            )

    fun setGrouping(grouping: Grouping) {
        container.grouping = grouping
        controls.update { it.copy(grouping = grouping) }
    }

    fun shiftWeek(weeks: Long) = controls.update { it.copy(week = it.week.plusWeeks(weeks)) }

    fun selectDay(day: LocalDate) =
        controls.update {
            it.copy(selectedDay = if (it.selectedDay == day) null else day, week = day)
        }

    /** A quick entry is for now, or for noon on the selected day when looking at another day. */
    fun quickAdd(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            val day = controls.value.selectedDay?.takeIf { it != now.toLocalDate() }
            val start =
                EntryTime.DateTime(
                    day?.atTime(12, 0) ?: now.toLocalDateTime().withSecond(0).withNano(0),
                    zone,
                )
            val collection = repository.defaultCollection(container.defaultCollectionName)
            repository.save(
                repository
                    .newEntry(Component.VJOURNAL, collection.id)
                    .copy(description = body, start = start)
            )
        }
    }
}
