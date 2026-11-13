// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.collections

internal sealed interface CollectionsEvent {
    data class Save(val id: Long, val name: String, val color: Int?) : CollectionsEvent

    data class Delete(val id: Long) : CollectionsEvent

    data object Retry : CollectionsEvent
}
