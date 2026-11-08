package de.goepfarth.kalenderassistent.calendar

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances

data class DeviceCalendar(
    val id: Long,
    val name: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
    val visible: Boolean,
)

data class DeviceEvent(
    val eventId: Long,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val title: String,
    val location: String?,
    val description: String?,
    val calendarId: Long,
    val color: Int,
    val status: Int?,
    val selfAttendeeStatus: Int?,
    val availability: Int?,
    val uid: String?,
)

/**
 * Liest Kalender und Termine aus dem Android-Kalenderspeicher.
 * Ausschließlich Lesezugriffe (`query`), siehe PLAN.md, Abschnitt 1.
 */
class CalendarReader(private val resolver: ContentResolver) {

    fun calendars(): List<DeviceCalendar> {
        val projection = arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE,
            Calendars.CALENDAR_COLOR,
            Calendars.VISIBLE,
        )
        return resolver.query(Calendars.CONTENT_URI, projection, null, null, null)?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        DeviceCalendar(
                            id = c.getLong(0),
                            name = c.stringOrNull(1).orEmpty(),
                            accountName = c.stringOrNull(2).orEmpty(),
                            accountType = c.stringOrNull(3).orEmpty(),
                            color = c.getInt(4),
                            visible = c.getInt(5) == 1,
                        )
                    )
                }
            }
        }.orEmpty()
    }

    /** Alle Vorkommen im Zeitraum; Serientermine expandiert der Kalenderspeicher selbst. */
    fun events(fromMillis: Long, toMillis: Long): List<DeviceEvent> {
        val uri = Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, fromMillis)
            ContentUris.appendId(it, toMillis)
        }.build()
        val projection = arrayOf(
            Instances.EVENT_ID,
            Instances.BEGIN,
            Instances.END,
            Instances.ALL_DAY,
            Instances.TITLE,
            Instances.EVENT_LOCATION,
            Instances.DESCRIPTION,
            Instances.CALENDAR_ID,
            Instances.DISPLAY_COLOR,
            Instances.STATUS,
            Instances.SELF_ATTENDEE_STATUS,
            Instances.AVAILABILITY,
        )
        val instances = resolver.query(uri, projection, null, null, "${Instances.BEGIN} ASC")?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        DeviceEvent(
                            eventId = c.getLong(0),
                            begin = c.getLong(1),
                            end = c.getLong(2),
                            allDay = c.getInt(3) == 1,
                            title = c.stringOrNull(4).orEmpty(),
                            location = c.stringOrNull(5),
                            description = c.stringOrNull(6),
                            calendarId = c.getLong(7),
                            color = c.getInt(8),
                            status = c.intOrNull(9),
                            selfAttendeeStatus = c.intOrNull(10),
                            availability = c.intOrNull(11),
                            uid = null,
                        )
                    )
                }
            }
        }.orEmpty()

        val uids = uids(instances.map { it.eventId }.distinct())
        return instances.map { it.copy(uid = uids[it.eventId]) }
    }

    // Die iCalendar-UID steht in der Termintabelle, nicht in der Vorkommen-Tabelle.
    private fun uids(eventIds: List<Long>): Map<Long, String> {
        val result = HashMap<Long, String>()
        eventIds.chunked(500).forEach { chunk ->
            val selection = "${Events._ID} IN (${chunk.joinToString(",") { "?" }})"
            val args = chunk.map { it.toString() }.toTypedArray()
            resolver.query(Events.CONTENT_URI, arrayOf(Events._ID, Events.UID_2445), selection, args, null)?.use { c ->
                while (c.moveToNext()) {
                    c.stringOrNull(1)?.takeIf { it.isNotBlank() }?.let { result[c.getLong(0)] = it }
                }
            }
        }
        return result
    }
}

private fun Cursor.stringOrNull(index: Int): String? = if (isNull(index)) null else getString(index)

private fun Cursor.intOrNull(index: Int): Int? = if (isNull(index)) null else getInt(index)
