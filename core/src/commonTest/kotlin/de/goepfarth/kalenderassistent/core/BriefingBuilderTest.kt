package de.goepfarth.kalenderassistent.core

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BriefingBuilderTest {

    private val berlin = TimeZone.of("Europe/Berlin")
    private val monday = LocalDate(2026, 11, 9)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0) = date.atTime(hour, minute).toMillis(berlin)

    private fun timed(
        id: String,
        date: LocalDate,
        from: Pair<Int, Int>,
        to: Pair<Int, Int>,
        endDate: LocalDate = date,
        location: String? = null,
        description: String? = null,
        response: ResponseStatus = ResponseStatus.NONE,
        status: EventStatus = EventStatus.CONFIRMED,
    ) = CalendarEvent(
        id = id,
        calendarId = "privat",
        title = id,
        startMillis = at(date, from.first, from.second),
        endMillis = at(endDate, to.first, to.second),
        location = location,
        description = description,
        selfResponse = response,
        status = status,
    ).classified()

    private fun allDay(id: String, first: LocalDate, days: Int, location: String? = null) = CalendarEvent(
        id = id,
        calendarId = "privat",
        title = id,
        startMillis = first.startMillis(TimeZone.UTC),
        endMillis = first.plusDays(days).startMillis(TimeZone.UTC),
        allDay = true,
        location = location,
    ).classified()

    private fun build(
        events: List<ClassifiedEvent>,
        date: LocalDate = monday,
        now: Long = at(date, 6),
        settings: BriefingSettings = BriefingSettings(),
    ) = BriefingBuilder.build(date, events, berlin, now, settings)

    private fun List<BriefingItem>?.ids() = orEmpty().map { it.event.event.id }

    private fun List<TimeSlot>?.text() = orEmpty().map {
        val start = it.startMillis.toLocalDateTime(berlin).time
        val end = it.endMillis.toLocalDateTime(berlin).time
        "$start-$end"
    }

    @Test
    fun todayListsAllDayFirstAndSkipsCanceledAndDeclined() {
        val briefing = build(
            listOf(
                timed("b", monday, 10 to 0, 11 to 0),
                timed("a", monday, 9 to 0, 9 to 30),
                allDay("urlaub", monday, 1),
                timed("abgesagt", monday, 12 to 0, 13 to 0, status = EventStatus.CANCELED),
                timed("abgelehnt", monday, 12 to 0, 13 to 0, response = ResponseStatus.DECLINED),
                timed("vorlaeufig", monday, 14 to 0, 15 to 0, response = ResponseStatus.TENTATIVE),
                timed("morgen", monday.plusDays(1), 9 to 0, 10 to 0),
            )
        )

        assertEquals(listOf("urlaub", "a", "b", "vorlaeufig"), briefing.today.ids())
    }

    @Test
    fun multiDayAllDayEventShowsItsDayNumber() {
        val messe = allDay("messe", monday.plusDays(-1), 3)

        val item = build(listOf(messe)).today.orEmpty().single()

        assertEquals(2, item.dayNumber)
        assertEquals(3, item.dayCount)
        assertTrue(build(listOf(messe), date = monday.plusDays(2)).today.orEmpty().isEmpty())
    }

    @Test
    fun eventAcrossMidnightBelongsToBothDays() {
        val nacht = timed("nacht", monday, 23 to 0, 1 to 0, endDate = monday.plusDays(1))

        assertEquals(listOf("nacht"), build(listOf(nacht)).today.ids())
        assertEquals(listOf("nacht"), build(listOf(nacht)).tomorrow.ids())
        assertEquals(2, build(listOf(nacht), date = monday.plusDays(1)).today.orEmpty().single().dayNumber)
    }

    @Test
    fun eventEndingAtMidnightBelongsToOneDay() {
        val spaet = timed("spaet", monday, 22 to 0, 0 to 0, endDate = monday.plusDays(1))

        assertEquals(1, build(listOf(spaet)).today.orEmpty().single().dayCount)
        assertTrue(build(listOf(spaet)).tomorrow.orEmpty().isEmpty())
    }

    @Test
    fun freeSlotsLieBetweenEventsAndSkipShortGaps() {
        val events = listOf(
            timed("a", monday, 9 to 0, 10 to 0),
            timed("b", monday, 10 to 20, 12 to 0),
            timed("c", monday, 13 to 0, 14 to 0),
            allDay("ganztags", monday, 1),
        )

        assertEquals(listOf("08:00-09:00", "12:00-13:00", "14:00-18:00"), build(events).freeSlots.text())
    }

    @Test
    fun freeSlotsOfTodayStartNow() {
        val events = listOf(timed("c", monday, 13 to 0, 14 to 0))

        assertEquals(listOf("12:30-13:00", "14:00-18:00"), build(events, now = at(monday, 12, 30)).freeSlots.text())
        assertTrue(build(events, now = at(monday, 18, 30)).freeSlots.orEmpty().isEmpty())
    }

    @Test
    fun freeSlotsFollowWeekdayWindowAndDaylightSaving() {
        val night = DayWindow(LocalTime(0, 0), LocalTime(6, 0))
        val settings = BriefingSettings(
            freeSlots = FreeSlotSettings(windowByWeekday = mapOf(DayOfWeek.SUNDAY to night)),
        )
        val springForward = LocalDate(2027, 3, 28)
        val fallBack = LocalDate(2027, 10, 31)

        // Umstellung auf Sommerzeit: Die Stunde von 2 bis 3 Uhr gibt es nicht.
        val spring = build(emptyList(), date = springForward, now = 0, settings = settings).freeSlots.orEmpty()
        assertEquals(listOf(300L), spring.map { it.minutes })

        // Zurück auf Winterzeit: Die Stunde von 2 bis 3 Uhr gibt es zweimal.
        val fall = build(emptyList(), date = fallBack, now = 0, settings = settings).freeSlots.orEmpty()
        assertEquals(listOf(420L), fall.map { it.minutes })

        // Montags gilt weiter der Standardrahmen.
        assertEquals(listOf("08:00-18:00"), build(emptyList(), settings = settings).freeSlots.text())
    }

    @Test
    fun dayBoundariesFollowDaylightSaving() {
        val springForward = LocalDate(2027, 3, 28)
        val lateEvening = timed("spaet", springForward, 23 to 30, 23 to 45)

        assertEquals(listOf("spaet"), build(listOf(lateEvening), date = springForward, now = 0).today.ids())
        assertEquals(
            23 * 60L,
            (springForward.plusDays(1).startMillis(berlin) - springForward.startMillis(berlin)) / MINUTE_MILLIS,
        )
    }

    @Test
    fun presenceAheadListsOnlyPresenceEventsOnThatDayAndIsAlwaysThere() {
        val inTwoDays = monday.plusDays(2)
        val events = listOf(
            timed("praesenz", inTwoDays, 10 to 0, 11 to 0, location = "Hauptstraße 1"),
            timed("online", inTwoDays, 12 to 0, 13 to 0, description = TEAMS_DESCRIPTION),
            timed("morgen", monday.plusDays(1), 10 to 0, 11 to 0, location = "Hauptstraße 1"),
        )

        val ahead = assertNotNull(build(events).presenceAhead)
        assertEquals(inTwoDays, ahead.date)
        assertEquals(listOf("praesenz"), ahead.items.ids())

        val empty = assertNotNull(build(emptyList()).presenceAhead)
        assertTrue(empty.items.isEmpty())
    }

    @Test
    fun warningsCoverToday() {
        val events = listOf(timed("a", monday, 9 to 0, 10 to 0), timed("b", monday, 9 to 30, 10 to 30))

        assertEquals(listOf(WarningKind.OVERLAP), build(events).warnings.orEmpty().map { it.kind })
    }

    @Test
    fun switchedOffSectionsAreMissing() {
        val settings = BriefingSettings(sections = setOf(BriefingSection.TODAY))

        val briefing = build(listOf(timed("a", monday, 9 to 0, 10 to 0)), settings = settings)

        assertEquals(listOf("a"), briefing.today.ids())
        assertNull(briefing.warnings)
        assertNull(briefing.freeSlots)
        assertNull(briefing.presenceAhead)
        assertNull(briefing.tomorrow)
    }

    @Test
    fun requiredRangeCoversPresenceLookahead() {
        val (from, to) = BriefingBuilder.requiredRange(monday, berlin, BriefingSettings(presenceLookaheadDays = 3))

        assertEquals(monday.startMillis(berlin), from)
        assertEquals(monday.plusDays(4).startMillis(berlin), to)
    }
}
