// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.model

/** A reminder [minutesBefore] the entry's due time (its start when it has no due time). */
data class Alarm(val minutesBefore: Int)
