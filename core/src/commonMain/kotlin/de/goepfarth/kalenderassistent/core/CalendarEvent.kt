package de.goepfarth.kalenderassistent.core

enum class EventStatus { CONFIRMED, TENTATIVE, CANCELED }

enum class ResponseStatus { NONE, ACCEPTED, TENTATIVE, DECLINED }

/**
 * Ein Terminvorkommen, unabhängig von der Plattform. Serientermine liefern die Adapter
 * bereits als einzelne Vorkommen.
 */
data class CalendarEvent(
    /** Plattformspezifische ID des Vorkommens, eindeutig innerhalb eines Syncs. */
    val id: String,
    val calendarId: String,
    val title: String,
    /** Millisekunden seit 1970-01-01T00:00:00Z. */
    val startMillis: Long,
    val endMillis: Long,
    /**
     * Ganztägige Termine geben Beginn und Ende als Mitternacht in UTC an, das Ende exklusiv
     * (so speichert sie Android). Der iOS-Adapter rechnet in diese Form um.
     */
    val allDay: Boolean = false,
    val location: String? = null,
    val description: String? = null,
    /** iCalendar-UID; bei Serien für alle Vorkommen gleich. */
    val uid: String? = null,
    val status: EventStatus = EventStatus.CONFIRMED,
    val selfResponse: ResponseStatus = ResponseStatus.NONE,
) {
    /** Abgesagte und von mir abgelehnte Termine werden für Briefing und Erinnerungen ignoriert. */
    val isIgnored: Boolean
        get() = status == EventStatus.CANCELED || selfResponse == ResponseStatus.DECLINED

    val isTentative: Boolean
        get() = status == EventStatus.TENTATIVE || selfResponse == ResponseStatus.TENTATIVE
}
