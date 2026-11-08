package de.goepfarth.kalenderassistent.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WarningDetectorTest {

    private fun online(id: String, start: Int, end: Int) = event(id, start, end, description = TEAMS_DESCRIPTION).classified()
    private fun presence(id: String, start: Int, end: Int) = event(id, start, end, location = "Hauptstraße 1").classified()
    private fun other(id: String, start: Int, end: Int) = event(id, start, end).classified()

    private fun List<ScheduleWarning>.summary() = map { "${it.kind} ${it.first.event.id}-${it.second.event.id} ${it.minutes}" }

    @Test
    fun overlapReportsOverlappingMinutes() {
        val warnings = WarningDetector.detect(listOf(other("a", 0, 60), other("b", 30, 90)))

        assertEquals(listOf("OVERLAP a-b 30"), warnings.summary())
    }

    @Test
    fun everyOverlappingPairIsReported() {
        val warnings = WarningDetector.detect(listOf(other("a", 0, 120), other("b", 30, 60), other("c", 45, 90)))

        assertEquals(listOf("OVERLAP a-b 30", "OVERLAP a-c 45", "OVERLAP b-c 15"), warnings.summary())
    }

    @Test
    fun backToBackHasNoBreakButNoOverlap() {
        val warnings = WarningDetector.detect(listOf(other("a", 0, 60), other("b", 60, 90)))

        assertEquals(listOf("NO_BREAK a-b 0"), warnings.summary())
    }

    @Test
    fun noBreakThresholdIsExclusive() {
        assertEquals(
            listOf("NO_BREAK a-b 4"),
            WarningDetector.detect(listOf(other("a", 0, 60), other("b", 64, 90))).summary(),
        )
        assertTrue(WarningDetector.detect(listOf(other("a", 0, 60), other("b", 65, 90))).isEmpty())
    }

    @Test
    fun presenceShortlyAfterOnlineReplacesNoBreak() {
        assertEquals(
            listOf("PRESENCE_AFTER_ONLINE a-b 0"),
            WarningDetector.detect(listOf(online("a", 0, 60), presence("b", 60, 90))).summary(),
        )
        assertEquals(
            listOf("PRESENCE_AFTER_ONLINE a-b 20"),
            WarningDetector.detect(listOf(online("a", 0, 60), presence("b", 80, 120))).summary(),
        )
        assertTrue(WarningDetector.detect(listOf(online("a", 0, 60), presence("b", 90, 120))).isEmpty())
    }

    @Test
    fun onlyTheDirectPredecessorCounts() {
        // a (online) endet um 60, c (sonstig) liegt dazwischen, b (präsenz) beginnt 120.
        val warnings = WarningDetector.detect(listOf(online("a", 0, 60), other("c", 70, 100), presence("b", 120, 150)))

        assertTrue(warnings.isEmpty(), "Erwartet keine Warnung, war ${warnings.summary()}")
    }

    @Test
    fun allDayCanceledAndDeclinedAreIgnored() {
        val warnings = WarningDetector.detect(
            listOf(
                other("a", 0, 60),
                event("ganztags", 0, 24 * 60, allDay = true).classified(),
                event("abgesagt", 30, 90, status = EventStatus.CANCELED).classified(),
                event("abgelehnt", 30, 90, response = ResponseStatus.DECLINED).classified(),
            )
        )

        assertTrue(warnings.isEmpty(), "Erwartet keine Warnung, war ${warnings.summary()}")
    }

    @Test
    fun settingsSwitchWarningsOffAndChangeThresholds() {
        val events = listOf(online("a", 0, 60), presence("b", 30, 90), presence("c", 100, 120))

        val off = WarningSettings(overlap = false, noBreak = false, presenceAfterOnline = false)
        assertTrue(WarningDetector.detect(events, off).isEmpty())

        val longerBreak = WarningSettings(overlap = false, noBreakMinutes = 15)
        assertEquals(listOf("NO_BREAK b-c 10"), WarningDetector.detect(events, longerBreak).summary())
    }
}
