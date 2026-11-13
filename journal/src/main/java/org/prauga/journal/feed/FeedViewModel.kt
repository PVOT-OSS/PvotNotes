// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.prauga.journal.JournalPreferences
import org.prauga.notes.core.data.EntryRepository
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.EntryTime

@OptIn(ExperimentalCoroutinesApi::class)
internal class FeedViewModel(
    private val repository: EntryRepository,
    private val preferences: JournalPreferences,
    private val defaultCollectionName: String,
) : ViewModel() {
    private val controls = MutableStateFlow<FeedControls?>(null)
    private val attempt = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            val grouping = preferences.grouping()
            controls.update { it ?: FeedControls(grouping) }
        }
    }

    val state: StateFlow<UiState<FeedState>> =
        attempt
            .flatMapLatest {
                feed()
                    .map<FeedState, UiState<FeedState>> { UiState.Success(it) }
                    .catch { emit(UiState.Error(it.message.orEmpty(), it)) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun onEvent(event: FeedEvent) {
        when (event) {
            is FeedEvent.SetGrouping -> {
                controls.update { it?.copy(grouping = event.grouping) }
                viewModelScope.launch { preferences.setGrouping(event.grouping) }
            }
            is FeedEvent.ShiftWeek ->
                controls.update { it?.copy(week = it.week.plusWeeks(event.weeks)) }
            is FeedEvent.SelectDay ->
                controls.update {
                    it?.copy(
                        selectedDay = if (it.selectedDay == event.day) null else event.day,
                        week = event.day,
                    )
                }
            is FeedEvent.QuickAdd -> quickAdd(event.text)
            FeedEvent.Retry -> attempt.update { it + 1 }
        }
    }

    private fun feed() =
        combine(repository.observeEntries(Component.VJOURNAL), controls.filterNotNull()) {
            entries,
            controls ->
            val zone = ZoneId.systemDefault()
            val days = entries.map { it.moment(zone).toLocalDate() }.toSet()
            val shown =
                controls.selectedDay?.let { day ->
                    entries.filter { it.moment(zone).toLocalDate() == day }
                } ?: entries
            FeedState(controls = controls, entries = shown, daysWithEntries = days)
        }

    // A quick entry is for now, or for noon on the selected day when looking at another day.
    private fun quickAdd(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            val day = controls.value?.selectedDay?.takeIf { it != now.toLocalDate() }
            val start =
                EntryTime.DateTime(
                    day?.atTime(12, 0) ?: now.toLocalDateTime().withSecond(0).withNano(0),
                    zone,
                )
            val collection = repository.defaultCollection(defaultCollectionName)
            repository.save(
                repository
                    .newEntry(Component.VJOURNAL, collection.id)
                    .copy(description = body, start = start)
            )
        }
    }
}
