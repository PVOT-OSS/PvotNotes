// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.list

import java.time.Instant

internal data class TaskListState(
    val sections: List<Pair<Section, List<TaskRow>>>,
    val showCompleted: Boolean,
    val now: Instant,
)
