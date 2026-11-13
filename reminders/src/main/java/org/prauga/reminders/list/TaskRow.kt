// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.list

import org.prauga.notes.core.model.Entry

internal data class TaskRow(val task: Entry, val subtasksDone: Int, val subtasksTotal: Int)
