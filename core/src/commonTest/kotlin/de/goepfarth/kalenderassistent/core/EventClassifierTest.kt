package de.goepfarth.kalenderassistent.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EventClassifierTest {

    private fun classify(
        location: String? = null,
        description: String? = null,
        allDay: Boolean = false,
        settings: ClassificationSettings = ClassificationSettings(),
    ) = EventClassifier.classify(
        event("e", 0, 60, location = location, description = description, allDay = allDay),
        settings,
    )

    @Test
    fun teamsLinkInDescriptionIsOnline() {
        val result = classify(description = TEAMS_DESCRIPTION)

        assertEquals(EventType.ONLINE, result.type)
        assertEquals(ClassificationReason.MEETING_LINK, result.reason)
        assertEquals(MeetingProvider.TEAMS, result.meetingLink?.provider)
    }

    @Test
    fun teamsLocationWithoutLinkIsOnline() {
        val result = classify(location = "Microsoft Teams-Besprechung")

        assertEquals(EventType.ONLINE, result.type)
        assertEquals(ClassificationReason.ONLINE_LOCATION, result.reason)
        assertNull(result.physicalLocation)
    }

    @Test
    fun addressIsPresence() {
        val result = classify(location = "Hauptstraße 1, 10115 Berlin")

        assertEquals(EventType.PRESENCE, result.type)
        assertEquals(ClassificationReason.PHYSICAL_LOCATION, result.reason)
        assertEquals("Hauptstraße 1, 10115 Berlin", result.physicalLocation)
    }

    @Test
    fun linkAsOnlyLocationIsOnline() {
        val result = classify(location = "https://us02web.zoom.us/j/123")

        assertEquals(EventType.ONLINE, result.type)
        assertNull(result.physicalLocation)
    }

    @Test
    fun hybridIsPresenceByDefaultAndOnlineWhenConfigured() {
        val location = "Raum 3.01; Microsoft Teams-Besprechung"

        val byDefault = classify(location = location, description = TEAMS_DESCRIPTION)
        assertEquals(EventType.PRESENCE, byDefault.type)
        assertEquals(ClassificationReason.HYBRID, byDefault.reason)
        assertEquals("Raum 3.01", byDefault.physicalLocation)
        assertNotNull(byDefault.meetingLink)

        val asOnline = classify(
            location = location,
            description = TEAMS_DESCRIPTION,
            settings = ClassificationSettings(hybridHandling = HybridHandling.AS_ONLINE),
        )
        assertEquals(EventType.ONLINE, asOnline.type)
        assertEquals(ClassificationReason.HYBRID, asOnline.reason)
    }

    @Test
    fun nothingFoundIsOther() {
        assertEquals(EventType.OTHER, classify().type)
        assertEquals(EventType.OTHER, classify(location = "  ", allDay = true).type)
    }

    @Test
    fun allDayWithLocationIsPresence() {
        assertEquals(EventType.PRESENCE, classify(location = "Messe Frankfurt", allDay = true).type)
    }

    @Test
    fun eventOverrideBeatsCalendarOverrideBeatsDetection() {
        val online = event("e", 0, 60, description = TEAMS_DESCRIPTION)

        val calendar = EventClassifier.classify(online, calendarOverride = EventType.OTHER)
        assertEquals(EventType.OTHER, calendar.type)
        assertEquals(ClassificationReason.CALENDAR_OVERRIDE, calendar.reason)

        val single = EventClassifier.classify(online, calendarOverride = EventType.OTHER, eventOverride = EventType.PRESENCE)
        assertEquals(EventType.PRESENCE, single.type)
        assertEquals(ClassificationReason.EVENT_OVERRIDE, single.reason)
        assertNotNull(single.meetingLink, "Der Link bleibt für den Button erhalten")
    }
}
