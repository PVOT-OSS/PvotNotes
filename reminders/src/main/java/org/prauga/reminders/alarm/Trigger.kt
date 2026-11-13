// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.alarm

import java.time.Instant

/** [armedAt] is when the task was last changed. */
internal data class Trigger(
    val taskId: Long,
    val at: Instant,
    val armedAt: Instant = Instant.EPOCH,
)
