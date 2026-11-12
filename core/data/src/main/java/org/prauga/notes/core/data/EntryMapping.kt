// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.prauga.notes.core.model.Alarm
import org.prauga.notes.core.model.Attachment
import org.prauga.notes.core.model.Classification
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.model.EntryCollection
import org.prauga.notes.core.model.EntryTime
import org.prauga.notes.core.model.Status

internal fun Entry.toEntity() =
    EntryEntity(
        id = id,
        uid = uid,
        component = component.name,
        collectionId = collectionId,
        summary = summary,
        description = description,
        color = color,
        start = start?.toColumns(),
        due = due?.toColumns(),
        completed = completed?.toEpochMilli(),
        status = status?.value,
        classification = classification?.value,
        priority = priority,
        percentComplete = percentComplete,
        created = created.toEpochMilli(),
        lastModified = lastModified.toEpochMilli(),
        sequence = sequence,
        parentUid = parentUid,
        rrule = rrule,
    )

internal fun EntryWithRelations.toModel() =
    Entry(
        id = entry.id,
        uid = entry.uid,
        component = Component.valueOf(entry.component),
        collectionId = entry.collectionId,
        summary = entry.summary,
        description = entry.description,
        categories = categories.sortedBy { it.position }.map { it.name },
        color = entry.color,
        start = entry.start?.toTime(),
        due = entry.due?.toTime(),
        completed = entry.completed?.let(Instant::ofEpochMilli),
        status = entry.status?.let(Status::fromValue),
        classification = entry.classification?.let(Classification::fromValue),
        priority = entry.priority,
        percentComplete = entry.percentComplete,
        created = Instant.ofEpochMilli(entry.created),
        lastModified = Instant.ofEpochMilli(entry.lastModified),
        sequence = entry.sequence,
        parentUid = entry.parentUid,
        rrule = entry.rrule,
        alarms = alarms.map { Alarm(it.minutesBefore) }.sortedBy { it.minutesBefore },
        attachments =
            attachments.sortedBy { it.position }.map { Attachment(it.uri, it.mimeType, it.name) },
    )

internal fun CollectionEntity.toModel() = EntryCollection(id = id, name = name, color = color)

internal fun EntryCollection.toEntity() = CollectionEntity(id = id, name = name, color = color)

private fun EntryTime.toColumns() =
    when (this) {
        is EntryTime.Date -> TimeColumns(date.toString(), null, isDate = true)
        is EntryTime.DateTime -> TimeColumns(dateTime.toString(), zone?.id, isDate = false)
    }

private fun TimeColumns.toTime(): EntryTime =
    if (isDate) EntryTime.Date(LocalDate.parse(value))
    else EntryTime.DateTime(LocalDateTime.parse(value), zone?.let(ZoneId::of))
