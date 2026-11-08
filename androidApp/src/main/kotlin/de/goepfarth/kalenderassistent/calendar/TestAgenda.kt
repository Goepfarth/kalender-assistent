package de.goepfarth.kalenderassistent.calendar

import android.content.ContentResolver
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Events
import de.goepfarth.kalenderassistent.core.Clock
import de.goepfarth.kalenderassistent.core.MeetingLink
import de.goepfarth.kalenderassistent.core.MeetingLinkDetector
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

// Testansicht für Phase A0. In Phase 1 ersetzt das Briefing aus `core` diese Aufbereitung.

data class TestEntry(val event: DeviceEvent, val calendar: DeviceCalendar?, val meetingLink: MeetingLink?) {
    val canceled get() = event.status == Events.STATUS_CANCELED
    val declined get() = event.selfAttendeeStatus == Attendees.ATTENDEE_STATUS_DECLINED
    val tentative get() = event.status == Events.STATUS_TENTATIVE ||
        event.selfAttendeeStatus == Attendees.ATTENDEE_STATUS_TENTATIVE
}

data class TestDay(val date: LocalDate, val entries: List<TestEntry>)

data class CalendarSummary(val calendar: DeviceCalendar, val eventCount: Int)

data class TestStats(
    val total: Int,
    val withUid: Int,
    val withMeetingLink: Int,
    val canceled: Int,
    val declined: Int,
    val tentative: Int,
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
    val calendarsById = calendars.associateBy { it.id }
    val entries = reader.events(from, to).map {
        TestEntry(it, calendarsById[it.calendarId], MeetingLinkDetector.find(it.location, it.description))
    }

    val days = entries
        .groupBy { it.event.startDate(zone) }
        .toSortedMap()
        .map { (date, dayEntries) ->
            TestDay(date, dayEntries.sortedWith(compareBy({ !it.event.allDay }, { it.event.begin })))
        }

    val countsByCalendar = entries.groupingBy { it.event.calendarId }.eachCount()
    return TestAgenda(
        calendars = calendars
            .map { CalendarSummary(it, countsByCalendar[it.id] ?: 0) }
            .sortedWith(compareBy({ it.calendar.accountName }, { it.calendar.name })),
        days = days,
        stats = TestStats(
            total = entries.size,
            withUid = entries.count { it.event.uid != null },
            withMeetingLink = entries.count { it.meetingLink != null },
            canceled = entries.count { it.canceled },
            declined = entries.count { it.declined },
            tentative = entries.count { it.tentative },
        ),
        loadedAt = now,
    )
}

// Ganztägige Termine speichert Android in UTC, sie gehören zum UTC-Datum.
fun DeviceEvent.startDate(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(begin).atZone(if (allDay) ZoneOffset.UTC else zone).toLocalDate()
