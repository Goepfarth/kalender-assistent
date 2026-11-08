package de.goepfarth.kalenderassistent.core

import kotlin.test.Test
import kotlin.test.assertEquals

class DeduplicatorTest {

    @Test
    fun sameUidAndStartInTwoCalendarsIsOneEvent() {
        val merged = Deduplicator.merge(
            listOf(
                event("a", 60, 120, title = "Jour fixe", calendarId = "privat", uid = "uid-1"),
                event("b", 60, 120, title = "Jour fixe (weitergeleitet)", calendarId = "arbeit", uid = "uid-1"),
            )
        )

        assertEquals(1, merged.size)
        assertEquals(listOf("privat", "arbeit"), merged.single().calendarIds)
    }

    @Test
    fun seriesOccurrencesStaySeparate() {
        val merged = Deduplicator.merge(
            listOf(
                event("mo", 60, 120, uid = "serie"),
                event("di", 60 + 24 * 60, 120 + 24 * 60, uid = "serie"),
            )
        )

        assertEquals(2, merged.size)
    }

    @Test
    fun sameTitleAndTimeWithoutUidIsOneEvent() {
        val merged = Deduplicator.merge(
            listOf(
                event("a", 60, 120, title = "Mittagspause", calendarId = "privat"),
                event("b", 60, 120, title = "  mittagspause ", calendarId = "arbeit"),
                event("c", 60, 120, title = "Zahnarzt", calendarId = "privat"),
            )
        )

        assertEquals(listOf(listOf("privat", "arbeit"), listOf("privat")), merged.map { it.calendarIds })
    }

    @Test
    fun copyWithUidJoinsCopyWithoutUid() {
        val merged = Deduplicator.merge(
            listOf(
                event("a", 60, 120, title = "Review", calendarId = "privat"),
                event("b", 60, 120, title = "Review", calendarId = "arbeit", uid = "uid-7"),
            )
        )

        assertEquals(1, merged.size)
    }

    @Test
    fun notDeclinedCopyRepresentsTheMeeting() {
        val merged = Deduplicator.merge(
            listOf(
                event("a", 60, 120, calendarId = "privat", uid = "uid-1", response = ResponseStatus.DECLINED),
                event("b", 60, 120, calendarId = "arbeit", uid = "uid-1", response = ResponseStatus.ACCEPTED),
            )
        ).single()

        assertEquals("b", merged.event.id)
    }
}
