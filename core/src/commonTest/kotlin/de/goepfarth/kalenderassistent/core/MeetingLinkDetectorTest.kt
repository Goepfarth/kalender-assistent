package de.goepfarth.kalenderassistent.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MeetingLinkDetectorTest {

    @Test
    fun findsTeamsLinkInAngleBrackets() {
        val description = """
            Microsoft Teams-Besprechung
            Jetzt an der Besprechung teilnehmen <https://teams.microsoft.com/l/meetup-join/19%3ameeting_abc%40thread.v2/0?context=%7b%22Tid%22%3a%221%22%7d>
        """.trimIndent()

        val link = MeetingLinkDetector.find(null, description)

        assertEquals(MeetingProvider.TEAMS, link?.provider)
        assertEquals(
            "https://teams.microsoft.com/l/meetup-join/19%3ameeting_abc%40thread.v2/0?context=%7b%22Tid%22%3a%221%22%7d",
            link?.url,
        )
    }

    @Test
    fun findsNewTeamsMeetingIdLinkAndTeamsLive() {
        assertEquals(
            MeetingProvider.TEAMS,
            MeetingLinkDetector.find("https://teams.microsoft.com/meet/34512345678?p=abcDEF")?.provider,
        )
        assertEquals(
            MeetingProvider.TEAMS,
            MeetingLinkDetector.find("https://teams.live.com/meet/9876543210")?.provider,
        )
    }

    @Test
    fun findsZoomMeetAndWebex() {
        assertEquals(
            MeetingLink(MeetingProvider.ZOOM, "https://us02web.zoom.us/j/1234567890?pwd=xyz"),
            MeetingLinkDetector.find("Einwahl: https://us02web.zoom.us/j/1234567890?pwd=xyz"),
        )
        assertEquals(
            MeetingLink(MeetingProvider.GOOGLE_MEET, "https://meet.google.com/abc-defg-hij"),
            MeetingLinkDetector.find("Link: https://meet.google.com/abc-defg-hij."),
        )
        assertEquals(
            MeetingProvider.WEBEX,
            MeetingLinkDetector.find("https://firma.webex.com/firma/j.php?MTID=m123")?.provider,
        )
    }

    @Test
    fun unwrapsMicrosoftSafeLinks() {
        val safeLink = "https://eur01.safelinks.protection.outlook.com/?url=" +
            "https%3A%2F%2Fteams.microsoft.com%2Fl%2Fmeetup-join%2F19%253ameeting_abc" +
            "&data=05%7C01&sdata=xyz&reserved=0"

        assertEquals(
            MeetingLink(MeetingProvider.TEAMS, "https://teams.microsoft.com/l/meetup-join/19%3ameeting_abc"),
            MeetingLinkDetector.find(safeLink),
        )
    }

    @Test
    fun checksTextsInGivenOrder() {
        val location = "https://meet.google.com/aaa-bbbb-ccc"
        val description = "https://us02web.zoom.us/j/1"

        assertEquals(MeetingProvider.GOOGLE_MEET, MeetingLinkDetector.find(location, description)?.provider)
    }

    @Test
    fun ignoresTextWithoutMeetingLink() {
        assertNull(MeetingLinkDetector.find(null, ""))
        assertNull(MeetingLinkDetector.find("Besprechungsraum 3, Hauptstraße 1"))
        assertNull(MeetingLinkDetector.find("Infos: https://example.com/agenda"))
        assertNull(MeetingLinkDetector.find("Teams herunterladen: https://teams.microsoft.com/downloads"))
    }

    @Test
    fun percentDecodeHandlesUtf8AndInvalidSequences() {
        assertEquals("Größe ä", percentDecode("Gr%C3%B6%C3%9Fe%20%C3%A4"))
        assertEquals("100%ZZ", percentDecode("100%ZZ"))
        assertEquals("ende%", percentDecode("ende%"))
    }
}
