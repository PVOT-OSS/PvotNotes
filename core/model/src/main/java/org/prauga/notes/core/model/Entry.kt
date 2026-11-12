// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.model

import java.time.Instant

data class Entry(
    val id: Long = 0,
    val uid: String,
    val component: Component,
    val collectionId: Long,
    val summary: String = "",
    val description: String = "",
    val categories: List<String> = emptyList(),
    val color: Int? = null,
    val start: EntryTime? = null,
    val due: EntryTime? = null,
    val completed: Instant? = null,
    val status: Status? = null,
    val classification: Classification? = null,
    val priority: Int? = null,
    val percentComplete: Int? = null,
    val created: Instant,
    val lastModified: Instant,
    val sequence: Int = 0,
    val parentUid: String? = null,
    val rrule: String? = null,
    val alarms: List<Alarm> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
)
