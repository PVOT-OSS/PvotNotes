// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal.feed

import java.time.LocalDate

internal sealed interface FeedEvent {
    data class SetGrouping(val grouping: Grouping) : FeedEvent

    data class ShiftWeek(val weeks: Long) : FeedEvent

    data class SelectDay(val day: LocalDate) : FeedEvent

    data class QuickAdd(val text: String) : FeedEvent

    data object Retry : FeedEvent
}
