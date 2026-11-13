// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal.feed

import java.time.LocalDate

/** A header in the timeline: the first day of its day, week or month. */
internal data class Period(val grouping: Grouping, val start: LocalDate)
