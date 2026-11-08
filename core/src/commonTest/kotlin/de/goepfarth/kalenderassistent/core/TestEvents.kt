package de.goepfarth.kalenderassistent.core

// Feste Bezugszeit für Tests; die Minutenangaben sind relativ dazu.
internal const val BASE_MILLIS = 1_800_000_000_000L

internal fun at(minutes: Int): Long = BASE_MILLIS + minutes * 60_000L

internal const val TEAMS_DESCRIPTION = "Teilnehmen: https://teams.microsoft.com/l/meetup-join/19%3ameeting_abc"

internal fun event(
    id: String,
    start: Int,
    end: Int,
    title: String = id,
    calendarId: String = "privat",
    location: String? = null,
    description: String? = null,
    uid: String? = null,
    allDay: Boolean = false,
    status: EventStatus = EventStatus.CONFIRMED,
    response: ResponseStatus = ResponseStatus.NONE,
) = CalendarEvent(
    id = id,
    calendarId = calendarId,
    title = title,
    startMillis = at(start),
    endMillis = at(end),
    allDay = allDay,
    location = location,
    description = description,
    uid = uid,
    status = status,
    selfResponse = response,
)

internal fun CalendarEvent.classified(settings: ClassificationSettings = ClassificationSettings()) =
    ClassifiedEvent(this, EventClassifier.classify(this, settings))
