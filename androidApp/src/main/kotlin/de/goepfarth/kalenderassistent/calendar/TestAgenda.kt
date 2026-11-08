package de.goepfarth.kalenderassistent.calendar

import android.content.ContentResolver
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Events
import de.goepfarth.kalenderassistent.core.Briefing
import de.goepfarth.kalenderassistent.core.BriefingBuilder
import de.goepfarth.kalenderassistent.core.BriefingItem
import de.goepfarth.kalenderassistent.core.CalendarEvent
import de.goepfarth.kalenderassistent.core.Clock
import de.goepfarth.kalenderassistent.core.Deduplicator
import de.goepfarth.kalenderassistent.core.EventClassifier
import de.goepfarth.kalenderassistent.core.EventStatus
import de.goepfarth.kalenderassistent.core.EventType
import de.goepfarth.kalenderassistent.core.ResponseStatus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinLocalDate
import java.time.Instant
import java.time.ZoneId

// Testansicht für Phase 1a. In Phase 1d bekommt das Briefing sein eigentliches Design.

data class TestEntry(
    val item: BriefingItem,
    val color: Int,
    val calendars: List<DeviceCalendar>,
)

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
)

data class TestAgenda(
    val briefing: Briefing,
    val today: List<TestEntry>,
    val presenceAhead: List<TestEntry>,
    val tomorrow: List<TestEntry>,
    val calendars: List<CalendarSummary>,
    val stats: TestStats,
    val loadedAt: Instant,
)

fun loadTestAgenda(resolver: ContentResolver, clock: Clock): TestAgenda {
    val reader = CalendarReader(resolver)
    val nowMillis = clock.nowEpochMillis()
    val now = Instant.ofEpochMilli(nowMillis)
    val javaZone = ZoneId.systemDefault()
    val zone = TimeZone.of(javaZone.id)
    val today = now.atZone(javaZone).toLocalDate().toKotlinLocalDate()
    val (from, to) = BriefingBuilder.requiredRange(today, zone)

    val calendars = reader.calendars()
    val calendarsById = calendars.associateBy { it.id.toString() }
    val deviceEvents = reader.events(from, to)
    val colorsById = deviceEvents.associate { it.coreId to it.color }

    val classified = Deduplicator.merge(deviceEvents.map { it.toCoreEvent() })
        .map { EventClassifier.classify(it) }
    val briefing = BriefingBuilder.build(today, classified, zone, nowMillis)

    fun entries(items: List<BriefingItem>?) = items.orEmpty().map { item ->
        TestEntry(item, colorsById[item.event.event.id] ?: 0, item.event.calendarIds.mapNotNull(calendarsById::get))
    }

    val countsByCalendar = deviceEvents.groupingBy { it.calendarId }.eachCount()
    return TestAgenda(
        briefing = briefing,
        today = entries(briefing.today),
        presenceAhead = entries(briefing.presenceAhead?.items),
        tomorrow = entries(briefing.tomorrow),
        calendars = calendars
            .map { CalendarSummary(it, countsByCalendar[it.id] ?: 0) }
            .sortedWith(compareBy({ it.calendar.accountName }, { it.calendar.name })),
        stats = TestStats(
            read = deviceEvents.size,
            merged = deviceEvents.size - classified.size,
            withUid = deviceEvents.count { it.uid != null },
            online = classified.count { it.type == EventType.ONLINE },
            presence = classified.count { it.type == EventType.PRESENCE },
            other = classified.count { it.type == EventType.OTHER },
            ignored = classified.count { it.event.isIgnored },
            tentative = classified.count { it.event.isTentative },
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
