package de.goepfarth.kalenderassistent.core

enum class EventType { ONLINE, PRESENCE, OTHER }

enum class HybridHandling { AS_PRESENCE, AS_ONLINE }

data class ClassificationSettings(
    /** Termine mit Meeting-Link und physischem Ort. */
    val hybridHandling: HybridHandling = HybridHandling.AS_PRESENCE,
)

/** Warum ein Termin seinen Typ bekommen hat, für die Erklärung in der Oberfläche. */
enum class ClassificationReason {
    MEETING_LINK,
    ONLINE_LOCATION,
    PHYSICAL_LOCATION,
    HYBRID,
    NOTHING_FOUND,
    CALENDAR_OVERRIDE,
    EVENT_OVERRIDE,
}

data class Classification(
    val type: EventType,
    val reason: ClassificationReason,
    /** Auch bei Präsenz gesetzt, wenn ein Link vorhanden ist (hybride Termine). */
    val meetingLink: MeetingLink?,
    /** Der physische Teil des Orts, ohne Links und ohne Angaben wie „Microsoft Teams-Besprechung". */
    val physicalLocation: String?,
)

data class ClassifiedEvent(
    val event: CalendarEvent,
    val classification: Classification,
    val calendarIds: List<String> = listOf(event.calendarId),
) {
    val type: EventType get() = classification.type
}

object EventClassifier {

    private val urlPattern = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    // Ortsangaben, die Outlook und andere Kalender für Online-Besprechungen eintragen.
    private val onlineLocationFragments = listOf(
        "microsoft teams", "teams-besprechung", "teams meeting", "teams-meeting",
        "zoom meeting", "zoom-meeting", "google meet", "webex", "skype",
    )
    private val onlineLocationWords = setOf("teams", "zoom", "online", "virtuell", "remote", "videocall")

    fun classify(
        event: CalendarEvent,
        settings: ClassificationSettings = ClassificationSettings(),
        calendarOverride: EventType? = null,
        eventOverride: EventType? = null,
    ): Classification {
        val link = MeetingLinkDetector.find(event.location, event.description)
        // Outlook trennt mehrere Orte mit Semikolon, z. B. „Raum 3.01; Microsoft Teams-Besprechung".
        val parts = event.location.orEmpty().split(';').map { it.trim() }.filter { it.isNotEmpty() }
        val onlineLocation = parts.any(::isOnlineLocation)
        val physicalLocation = parts
            .filterNot(::isOnlineLocation)
            .filter { urlPattern.replace(it, "").trim(' ', ',', '-', '|').isNotEmpty() }
            .joinToString("; ")
            .ifEmpty { null }

        fun result(type: EventType, reason: ClassificationReason) =
            Classification(type, reason, link, physicalLocation)

        eventOverride?.let { return result(it, ClassificationReason.EVENT_OVERRIDE) }
        calendarOverride?.let { return result(it, ClassificationReason.CALENDAR_OVERRIDE) }

        val online = link != null || onlineLocation
        return when {
            online && physicalLocation != null -> result(
                if (settings.hybridHandling == HybridHandling.AS_ONLINE) EventType.ONLINE else EventType.PRESENCE,
                ClassificationReason.HYBRID,
            )
            link != null -> result(EventType.ONLINE, ClassificationReason.MEETING_LINK)
            onlineLocation -> result(EventType.ONLINE, ClassificationReason.ONLINE_LOCATION)
            physicalLocation != null -> result(EventType.PRESENCE, ClassificationReason.PHYSICAL_LOCATION)
            else -> result(EventType.OTHER, ClassificationReason.NOTHING_FOUND)
        }
    }

    fun classify(
        merged: MergedEvent,
        settings: ClassificationSettings = ClassificationSettings(),
        calendarOverride: EventType? = null,
        eventOverride: EventType? = null,
    ): ClassifiedEvent = ClassifiedEvent(
        merged.event,
        classify(merged.event, settings, calendarOverride, eventOverride),
        merged.calendarIds,
    )

    private fun isOnlineLocation(part: String): Boolean {
        val normalized = part.lowercase()
        return normalized in onlineLocationWords || onlineLocationFragments.any { it in normalized }
    }
}
