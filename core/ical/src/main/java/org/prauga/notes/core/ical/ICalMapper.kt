// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.ical

import java.io.StringReader
import java.io.StringWriter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.Temporal
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Component as ICalComponent
import net.fortuna.ical4j.model.Parameter
import net.fortuna.ical4j.model.ParameterList
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.TextList
import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import net.fortuna.ical4j.model.component.CalendarComponent
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.component.VJournal
import net.fortuna.ical4j.model.component.VToDo
import net.fortuna.ical4j.model.parameter.RelType
import net.fortuna.ical4j.model.parameter.Related
import net.fortuna.ical4j.model.parameter.TzId
import net.fortuna.ical4j.model.property.Action
import net.fortuna.ical4j.model.property.Attach
import net.fortuna.ical4j.model.property.Categories
import net.fortuna.ical4j.model.property.Clazz
import net.fortuna.ical4j.model.property.Color
import net.fortuna.ical4j.model.property.Completed
import net.fortuna.ical4j.model.property.Created
import net.fortuna.ical4j.model.property.DateProperty
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.DtStamp
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Due
import net.fortuna.ical4j.model.property.LastModified
import net.fortuna.ical4j.model.property.PercentComplete
import net.fortuna.ical4j.model.property.Priority
import net.fortuna.ical4j.model.property.ProdId
import net.fortuna.ical4j.model.property.RRule
import net.fortuna.ical4j.model.property.RelatedTo
import net.fortuna.ical4j.model.property.Sequence
import net.fortuna.ical4j.model.property.Summary
import net.fortuna.ical4j.model.property.Trigger
import net.fortuna.ical4j.model.property.Uid
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion
import org.prauga.notes.core.model.Alarm
import org.prauga.notes.core.model.Attachment
import org.prauga.notes.core.model.Classification
import org.prauga.notes.core.model.Component
import org.prauga.notes.core.model.Entry
import org.prauga.notes.core.model.EntryTime
import org.prauga.notes.core.model.Status

object ICalMapper {
    const val PROD_ID = "-//PVOT-OSS//Notes//EN"
    private const val COLOR = "COLOR"

    private val registry by lazy { TimeZoneRegistryFactory.getInstance().createRegistry() }

    fun toICalendar(entries: List<Entry>, now: Instant = Instant.now()): String {
        val calendar = Calendar()
        calendar.add<Calendar>(ProdId(PROD_ID))
        calendar.add<Calendar>(ImmutableVersion.VERSION_2_0)
        entries
            .flatMap { listOfNotNull(it.start, it.due) }
            .mapNotNull { (it as? EntryTime.DateTime)?.zone?.id }
            .filter { it != ZoneOffset.UTC.id }
            .distinct()
            .mapNotNull { registry.getTimeZone(it)?.vTimeZone }
            .forEach { calendar.add<Calendar>(it) }
        entries.forEach { calendar.add<Calendar>(toComponent(it, now)) }
        return StringWriter().also { CalendarOutputter(false).output(calendar, it) }.toString()
    }

    fun fromICalendar(text: String, collectionId: Long): List<Entry> {
        val calendar = CalendarBuilder().build(StringReader(text))
        return calendar.getComponents<CalendarComponent>().mapNotNull { component ->
            when (component) {
                is VJournal -> fromComponent(component, Component.VJOURNAL, collectionId)
                is VToDo -> fromComponent(component, Component.VTODO, collectionId)
                else -> null
            }
        }
    }

    private fun toComponent(entry: Entry, now: Instant): CalendarComponent {
        val component: CalendarComponent =
            when (entry.component) {
                Component.VJOURNAL -> VJournal(false)
                Component.VTODO -> VToDo(false)
            }
        val properties =
            buildList<Property> {
                add(Uid(entry.uid))
                add(DtStamp(now))
                add(Created(entry.created))
                add(LastModified(entry.lastModified))
                if (entry.sequence > 0) add(Sequence(entry.sequence))
                if (entry.summary.isNotEmpty()) add(Summary(entry.summary))
                if (entry.description.isNotEmpty()) add(Description(entry.description))
                if (entry.categories.isNotEmpty()) add(Categories(TextList(entry.categories)))
                entry.color?.let { add(Color(ParameterList(), Css3Colors.nearestName(it))) }
                entry.start?.let { add(DtStart(it.toTemporal())) }
                if (entry.component == Component.VTODO) {
                    entry.due?.let { add(Due(it.toTemporal())) }
                    entry.completed?.let { add(Completed(it)) }
                    entry.priority?.let { add(Priority(it)) }
                    entry.percentComplete?.let { add(PercentComplete(it)) }
                }
                entry.status?.let { add(net.fortuna.ical4j.model.property.Status(it.value)) }
                entry.rrule?.let { add(RRule<Temporal>(it)) }
                entry.parentUid?.let { add(RelatedTo(it)) }
                entry.attachments.forEach { add(Attach(ParameterList(), it.uri)) }
                entry.classification?.let { add(Clazz(it.value)) }
            }
        properties.forEach { component.add<ICalComponent>(it) }
        if (component is VToDo) {
            entry.alarms.forEach { alarm ->
                val valarm = VAlarm()
                val related = if (entry.due != null) Related.END else Related.START
                valarm.add<VAlarm>(
                    Trigger(
                        ParameterList(listOf(related)),
                        Recurrence.duration(-alarm.minutesBefore),
                    )
                )
                valarm.add<VAlarm>(Action(Action.VALUE_DISPLAY))
                valarm.add<VAlarm>(Description(entry.summary.ifEmpty { " " }))
                component.add<VToDo>(valarm)
            }
        }
        return component
    }

    private fun fromComponent(
        component: ICalComponent,
        type: Component,
        collectionId: Long,
    ): Entry? {
        val uid = component.text(Property.UID) ?: return null
        val stamp = component.instant(Property.DTSTAMP) ?: Instant.now()
        return Entry(
            uid = uid,
            component = type,
            collectionId = collectionId,
            summary = component.text(Property.SUMMARY).orEmpty(),
            description = component.text(Property.DESCRIPTION).orEmpty(),
            categories =
                component.getProperties<Categories>(Property.CATEGORIES).flatMap {
                    it.categories.texts
                },
            start = component.time(Property.DTSTART),
            due = component.time(Property.DUE),
            completed = component.instant(Property.COMPLETED),
            status = component.text(Property.STATUS)?.let(Status::fromValue),
            classification = component.text(Property.CLASS)?.let(Classification::fromValue),
            priority = component.text(Property.PRIORITY)?.toIntOrNull()?.takeIf { it in 1..9 },
            percentComplete =
                component.text(Property.PERCENT_COMPLETE)?.toIntOrNull()?.coerceIn(0, 100),
            created = component.instant(Property.CREATED) ?: stamp,
            lastModified = component.instant(Property.LAST_MODIFIED) ?: stamp,
            sequence = component.text(Property.SEQUENCE)?.toIntOrNull() ?: 0,
            color = component.text(COLOR)?.let(Css3Colors::argb),
            rrule = component.text(Property.RRULE),
            parentUid = component.parentUid(),
            alarms = (component as? VToDo)?.alarms().orEmpty(),
            attachments =
                component.getProperties<Attach>(Property.ATTACH).mapNotNull { attach ->
                    attach.value?.takeIf { it.isNotBlank() }?.let { Attachment(it) }
                },
        )
    }

    private fun ICalComponent.parentUid(): String? =
        getProperties<RelatedTo>(Property.RELATED_TO)
            .firstOrNull { related ->
                related
                    .getParameter<RelType>(Parameter.RELTYPE)
                    .map { it.value }
                    .orElse("PARENT") == "PARENT"
            }
            ?.value

    private fun VToDo.alarms(): List<Alarm> =
        getComponents<VAlarm>(ICalComponent.VALARM).mapNotNull { alarm ->
            alarm
                .getProperty<Trigger>(Property.TRIGGER)
                .map { it.value }
                .orElse(null)
                ?.let(Recurrence::durationMinutes)
                ?.let { Alarm(-it) }
        }

    private fun ICalComponent.text(name: String): String? =
        getProperty<Property>(name).map { it.value }.orElse(null)?.takeIf { it.isNotEmpty() }

    private fun ICalComponent.instant(name: String): Instant? =
        getProperty<DateProperty<*>>(name).map { it.date }.orElse(null)?.let(::toInstant)

    private fun ICalComponent.time(name: String): EntryTime? {
        val property = getProperty<DateProperty<*>>(name).orElse(null) ?: return null
        val time = toEntryTime(property.date) ?: return null
        val tzid = property.getParameter<TzId>(Parameter.TZID).map { it.value }.orElse(null)
        val zone = tzid?.let { runCatching { ZoneId.of(it) }.getOrNull() }
        return if (time is EntryTime.DateTime && zone != null) time.copy(zone = zone) else time
    }

    private fun EntryTime.toTemporal(): Temporal =
        when (this) {
            is EntryTime.Date -> date
            is EntryTime.DateTime ->
                when (zone) {
                    null -> dateTime
                    ZoneOffset.UTC -> dateTime.toInstant(ZoneOffset.UTC)
                    else -> dateTime.atZone(zone)
                }
        }

    private fun toEntryTime(temporal: Temporal): EntryTime? =
        when (temporal) {
            is LocalDate -> EntryTime.Date(temporal)
            is LocalDateTime -> EntryTime.DateTime(temporal, null)
            is ZonedDateTime -> EntryTime.DateTime(temporal.toLocalDateTime(), temporal.zone)
            is OffsetDateTime ->
                EntryTime.DateTime(
                    temporal.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                    ZoneOffset.UTC,
                )
            is Instant ->
                EntryTime.DateTime(
                    LocalDateTime.ofInstant(temporal, ZoneOffset.UTC),
                    ZoneOffset.UTC,
                )
            else -> null
        }

    private fun toInstant(temporal: Temporal): Instant? =
        when (temporal) {
            is Instant -> temporal
            is OffsetDateTime -> temporal.toInstant()
            is ZonedDateTime -> temporal.toInstant()
            is LocalDateTime -> temporal.toInstant(ZoneOffset.UTC)
            else -> null
        }
}
