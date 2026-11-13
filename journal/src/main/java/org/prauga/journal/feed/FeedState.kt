// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal.feed

import java.time.LocalDate
import org.prauga.notes.core.model.Entry

internal data class FeedState(
    val controls: FeedControls,
    val entries: List<Entry>,
    val daysWithEntries: Set<LocalDate>,
)
