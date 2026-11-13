// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal.feed

import java.time.LocalDate

internal data class FeedControls(
    val grouping: Grouping,
    val week: LocalDate = LocalDate.now(),
    val selectedDay: LocalDate? = null,
)
