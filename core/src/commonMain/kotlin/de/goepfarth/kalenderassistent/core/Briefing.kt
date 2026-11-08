package de.goepfarth.kalenderassistent.core

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime

/** Abschnitte des Morgenbriefings in der Reihenfolge aus dem Auftrag. */
enum class BriefingSection { TODAY, WARNINGS, FREE_SLOTS, PRESENCE_AHEAD, TOMORROW }

data class DayWindow(val start: LocalTime, val end: LocalTime)

data class FreeSlotSettings(
    val window: DayWindow = DayWindow(LocalTime(8, 0), LocalTime(18, 0)),
    /** Abweichender Tagesrahmen für einzelne Wochentage. */
    val windowByWeekday: Map<DayOfWeek, DayWindow> = emptyMap(),
    val minMinutes: Int = 30,
) {
    fun windowFor(day: DayOfWeek): DayWindow = windowByWeekday[day] ?: window
}

/** Standardwerte aus PLAN.md, Abschnitt 5. */
data class BriefingSettings(
    val sections: Set<BriefingSection> = BriefingSection.entries.toSet(),
    /** Das Kapitel „Präsenztermine in N Tagen"; 2 bedeutet übermorgen. */
    val presenceLookaheadDays: Int = 2,
    val freeSlots: FreeSlotSettings = FreeSlotSettings(),
    val warnings: WarningSettings = WarningSettings(),
)

/** Ein Termin an einem bestimmten Tag; mehrtägige Termine erscheinen an jedem ihrer Tage. */
data class BriefingItem(val event: ClassifiedEvent, val dayNumber: Int, val dayCount: Int) {
    val isMultiDay: Boolean get() = dayCount > 1
}

data class TimeSlot(val startMillis: Long, val endMillis: Long) {
    val minutes: Long get() = (endMillis - startMillis) / MINUTE_MILLIS
}

data class PresenceAhead(val date: LocalDate, val items: List<BriefingItem>)

/** Das Briefing für einen Tag. Ein Abschnitt ist `null`, wenn er in den Einstellungen aus ist. */
data class Briefing(
    val date: LocalDate,
    val today: List<BriefingItem>?,
    val warnings: List<ScheduleWarning>?,
    val freeSlots: List<TimeSlot>?,
    /** Ist der Abschnitt an, ist er immer da, auch ohne Termine. */
    val presenceAhead: PresenceAhead?,
    val tomorrow: List<BriefingItem>?,
)

object BriefingBuilder {

    /** Der Zeitraum (Beginn, Ende exklusiv), für den die Adapter Termine lesen müssen. */
    fun requiredRange(date: LocalDate, zone: TimeZone, settings: BriefingSettings = BriefingSettings()): Pair<Long, Long> {
        val lastDay = maxOf(settings.presenceLookaheadDays, 1)
        return date.startMillis(zone) to date.plusDays(lastDay + 1).startMillis(zone)
    }

    /**
     * @param events zusammengeführt ([Deduplicator]) und klassifiziert, für mindestens [requiredRange]
     * @param nowMillis freie Zeitfenster von heute beginnen frühestens jetzt
     */
    fun build(
        date: LocalDate,
        events: List<ClassifiedEvent>,
        zone: TimeZone,
        nowMillis: Long,
        settings: BriefingSettings = BriefingSettings(),
    ): Briefing {
        val relevant = events.filterNot { it.event.isIgnored }
        val sections = settings.sections
        val today = itemsOn(date, relevant, zone)

        return Briefing(
            date = date,
            today = today.takeIf { BriefingSection.TODAY in sections },
            warnings = if (BriefingSection.WARNINGS in sections) {
                WarningDetector.detect(today.map { it.event }, settings.warnings)
            } else {
                null
            },
            freeSlots = if (BriefingSection.FREE_SLOTS in sections) {
                freeSlots(date, today, zone, nowMillis, settings.freeSlots)
            } else {
                null
            },
            presenceAhead = if (BriefingSection.PRESENCE_AHEAD in sections) {
                val aheadDate = date.plusDays(settings.presenceLookaheadDays)
                PresenceAhead(aheadDate, itemsOn(aheadDate, relevant, zone).filter { it.event.type == EventType.PRESENCE })
            } else {
                null
            },
            tomorrow = if (BriefingSection.TOMORROW in sections) itemsOn(date.plusDays(1), relevant, zone) else null,
        )
    }

    /** Ganztägige Termine zuerst, dann nach Beginn. */
    internal fun itemsOn(date: LocalDate, events: List<ClassifiedEvent>, zone: TimeZone): List<BriefingItem> =
        events.mapNotNull { event ->
            val dates = event.event.dates(zone)
            val index = dates.indexOf(date)
            if (index < 0) null else BriefingItem(event, dayNumber = index + 1, dayCount = dates.size)
        }.sortedWith(
            compareBy<BriefingItem>({ !it.event.event.allDay }, { it.event.event.startMillis }, { it.event.event.endMillis })
                .thenBy { it.event.event.title },
        )

    internal fun freeSlots(
        date: LocalDate,
        items: List<BriefingItem>,
        zone: TimeZone,
        nowMillis: Long,
        settings: FreeSlotSettings,
    ): List<TimeSlot> {
        val window = settings.windowFor(date.dayOfWeek)
        val windowEnd = date.atTime(window.end).toMillis(zone)
        var cursor = maxOf(date.atTime(window.start).toMillis(zone), nowMillis)
        if (cursor >= windowEnd) return emptyList()

        val busy = items
            .map { it.event.event }
            .filter { !it.allDay && it.endMillis > it.startMillis }
            .sortedBy { it.startMillis }
        val slots = mutableListOf<TimeSlot>()
        for (event in busy) {
            if (event.startMillis >= windowEnd) break
            if (event.endMillis <= cursor) continue
            if (event.startMillis > cursor) slots += TimeSlot(cursor, event.startMillis)
            cursor = maxOf(cursor, event.endMillis)
            if (cursor >= windowEnd) break
        }
        if (cursor < windowEnd) slots += TimeSlot(cursor, windowEnd)
        return slots.filter { it.minutes >= settings.minMinutes }
    }
}
