package de.goepfarth.kalenderassistent.calendar

import android.content.ContentResolver
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Events
import de.goepfarth.kalenderassistent.core.CalendarEvent
import de.goepfarth.kalenderassistent.core.ClassifiedEvent
import de.goepfarth.kalenderassistent.core.Clock
import de.goepfarth.kalenderassistent.core.Deduplicator
import de.goepfarth.kalenderassistent.core.EventClassifier
import de.goepfarth.kalenderassistent.core.EventStatus
import de.goepfarth.kalenderassistent.core.EventType
import de.goepfarth.kalenderassistent.core.ResponseStatus
import de.goepfarth.kalenderassistent.core.ScheduleWarning
import de.goepfarth.kalenderassistent.core.WarningDetector
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

// Testansicht für Phase 1a. In Phase 1d ersetzt das Briefing aus `core` diese Aufbereitung.

data class TestEntry(
    val item: ClassifiedEvent,
    val color: Int,
    val calendars: List<DeviceCalendar>,
)

data class TestDay(val date: LocalDate, val entries: List<TestEntry>, val warnings: List<ScheduleWarning>)

data class CalendarSummary(val calendar: DeviceCalendar, val eventCount: Int)

data class TestStats(
    val read: Int,
    val merged: Int,
    val withUid: Int,
    val online: Int,
    val presence: Int,
    val other: Int,
    val ignored: Int,
    val tentative: Int,
    val warnings: Int,
)

data class TestAgenda(
    val calendars: List<CalendarSummary>,
    val days: List<TestDay>,
    val stats: TestStats,
    val loadedAt: Instant,
)

fun loadTestAgenda(
    resolver: ContentResolver,
    clock: Clock,
    zone: ZoneId = ZoneId.systemDefault(),
    dayCount: Long = 7,
): TestAgenda {
    val reader = CalendarReader(resolver)
    val now = Instant.ofEpochMilli(clock.nowEpochMillis())
    val startOfToday = now.atZone(zone).toLocalDate().atStartOfDay(zone)
    val from = startOfToday.toInstant().toEpochMilli()
    val to = startOfToday.plusDays(dayCount).toInstant().toEpochMilli()

    val calendars = reader.calendars()
    val calendarsById = calendars.associateBy { it.id.toString() }
    val deviceEvents = reader.events(from, to)
    val colorsById = deviceEvents.associate { it.coreId to it.color }

    val classified = Deduplicator.merge(deviceEvents.map { it.toCoreEvent() })
        .map { EventClassifier.classify(it) }

    val days = classified
        .groupBy { it.event.startDate(zone) }
        .toSortedMap()
        .map { (date, items) ->
            TestDay(
                date = date,
                entries = items
                    .sortedWith(compareBy({ !it.event.allDay }, { it.event.startMillis }))
                    .map { item ->
                        TestEntry(item, colorsById[item.event.id] ?: 0, item.calendarIds.mapNotNull(calendarsById::get))
                    },
                warnings = WarningDetector.detect(items),
            )
        }

    val countsByCalendar = deviceEvents.groupingBy { it.calendarId }.eachCount()
    return TestAgenda(
        calendars = calendars
            .map { CalendarSummary(it, countsByCalendar[it.id] ?: 0) }
            .sortedWith(compareBy({ it.calendar.accountName }, { it.calendar.name })),
        days = days,
        stats = TestStats(
            read = deviceEvents.size,
            merged = deviceEvents.size - classified.size,
            withUid = deviceEvents.count { it.uid != null },
            online = classified.count { it.type == EventType.ONLINE },
            presence = classified.count { it.type == EventType.PRESENCE },
            other = classified.count { it.type == EventType.OTHER },
            ignored = classified.count { it.event.isIgnored },
            tentative = classified.count { it.event.isTentative },
            warnings = days.sumOf { it.warnings.size },
        ),
        loadedAt = now,
    )
}

private val DeviceEvent.coreId get() = "$eventId@$begin"

private fun DeviceEvent.toCoreEvent() = CalendarEvent(
    id = coreId,
    calendarId = calendarId.toString(),
    title = title,
    startMillis = begin,
    endMillis = end,
    allDay = allDay,
    location = location,
    description = description,
    uid = uid,
    status = when (status) {
        Events.STATUS_TENTATIVE -> EventStatus.TENTATIVE
        Events.STATUS_CANCELED -> EventStatus.CANCELED
        else -> EventStatus.CONFIRMED
    },
    selfResponse = when (selfAttendeeStatus) {
        Attendees.ATTENDEE_STATUS_ACCEPTED -> ResponseStatus.ACCEPTED
        Attendees.ATTENDEE_STATUS_TENTATIVE -> ResponseStatus.TENTATIVE
        Attendees.ATTENDEE_STATUS_DECLINED -> ResponseStatus.DECLINED
        else -> ResponseStatus.NONE
    },
)

// Ganztägige Termine speichert Android in UTC, sie gehören zum UTC-Datum.
private fun CalendarEvent.startDate(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(startMillis).atZone(if (allDay) ZoneOffset.UTC else zone).toLocalDate()
