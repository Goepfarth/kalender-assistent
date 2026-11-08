package de.goepfarth.kalenderassistent.core

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// Zeiten in `core` sind Millisekunden seit 1970 (UTC). Kalendertage und Uhrzeiten
// entstehen erst mit einer Zeitzone, damit Sommerzeitwechsel richtig behandelt werden.

internal const val MINUTE_MILLIS = 60_000L

internal fun Long.toLocalDateTime(zone: TimeZone): LocalDateTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(zone)

internal fun LocalDateTime.toMillis(zone: TimeZone): Long = toInstant(zone).toEpochMilliseconds()

/** Beginn des Tages in der Zeitzone; an Tagen mit Zeitumstellung hat der Tag 23 oder 25 Stunden. */
fun LocalDate.startMillis(zone: TimeZone): Long = atStartOfDayIn(zone).toEpochMilliseconds()

internal fun LocalDate.plusDays(days: Int): LocalDate = plus(days, DateTimeUnit.DAY)

/** Höchstens so viele Tage wird ein mehrtägiger Termin aufgefächert. */
private const val MAX_EVENT_DAYS = 400

/**
 * Die Kalendertage, an denen ein Termin stattfindet.
 * Ganztägige Termine liegen in UTC (siehe [CalendarEvent.allDay]), ihr Ende ist exklusiv.
 * Ein Termin, der genau um Mitternacht endet, gehört nicht mehr zum nächsten Tag.
 */
internal fun CalendarEvent.dates(zone: TimeZone): List<LocalDate> {
    val first: LocalDate
    val last: LocalDate
    if (allDay) {
        first = startMillis.toLocalDateTime(TimeZone.UTC).date
        val endExclusive = endMillis.toLocalDateTime(TimeZone.UTC).date
        last = if (endExclusive > first) endExclusive.plusDays(-1) else first
    } else {
        first = startMillis.toLocalDateTime(zone).date
        last = if (endMillis > startMillis) (endMillis - 1).toLocalDateTime(zone).date else first
    }
    return generateSequence(first) { it.plusDays(1) }
        .takeWhile { it <= last }
        .take(MAX_EVENT_DAYS)
        .toList()
}
