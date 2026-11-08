package de.goepfarth.kalenderassistent.core

/** Ein Termin, der in einem oder mehreren Kalendern vorkommt. */
data class MergedEvent(val event: CalendarEvent, val calendarIds: List<String>)

/**
 * Führt dieselbe Besprechung aus mehreren Kalendern zusammen.
 * Gleich ist ein Vorkommen bei gleicher UID und gleichem Beginn, oder bei gleichem Titel,
 * gleichem Beginn und gleichem Ende.
 */
object Deduplicator {

    fun merge(events: List<CalendarEvent>): List<MergedEvent> {
        val groups = mutableListOf<MutableList<CalendarEvent>>()
        val byUid = HashMap<String, MutableList<CalendarEvent>>()
        val byTitleAndTime = HashMap<String, MutableList<CalendarEvent>>()

        for (event in events) {
            val uidKey = uidKey(event)
            val titleKey = titleAndTimeKey(event)
            val group = uidKey?.let(byUid::get) ?: byTitleAndTime[titleKey]
                ?: mutableListOf<CalendarEvent>().also(groups::add)
            group += event
            uidKey?.let { byUid.getOrPut(it) { group } }
            byTitleAndTime.getOrPut(titleKey) { group }
        }

        return groups.map { copies ->
            // Ist eine Kopie abgelehnt und eine andere nicht, zählt die nicht abgelehnte.
            val representative = copies.firstOrNull { !it.isIgnored } ?: copies.first()
            MergedEvent(representative, copies.map { it.calendarId }.distinct())
        }
    }

    private fun uidKey(event: CalendarEvent): String? =
        event.uid?.takeIf { it.isNotBlank() }?.let { "$it@${event.startMillis}" }

    private fun titleAndTimeKey(event: CalendarEvent): String {
        val title = event.title.trim().lowercase().replace(Regex("""\s+"""), " ")
        return "$title@${event.startMillis}-${event.endMillis}"
    }
}
