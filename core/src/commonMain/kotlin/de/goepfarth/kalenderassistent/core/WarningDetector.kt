package de.goepfarth.kalenderassistent.core

enum class WarningKind { OVERLAP, NO_BREAK, PRESENCE_AFTER_ONLINE }

/** Standardwerte aus PLAN.md, Abschnitt 5 („Erweitert"). */
data class WarningSettings(
    val overlap: Boolean = true,
    val noBreak: Boolean = true,
    /** Weniger Abstand als das gilt als „keine Pause". */
    val noBreakMinutes: Int = 5,
    val presenceAfterOnline: Boolean = true,
    /** Weniger Abstand zwischen Ende Online und Beginn Präsenz löst die Warnung aus. */
    val presenceAfterOnlineMinutes: Int = 30,
)

data class ScheduleWarning(
    val kind: WarningKind,
    val first: ClassifiedEvent,
    val second: ClassifiedEvent,
    /** Bei Überschneidung die Dauer der Überschneidung, sonst der Abstand zwischen den Terminen. */
    val minutes: Long,
)

/** Findet Überschneidungen, fehlende Pausen und Präsenztermine direkt nach Online-Terminen. */
object WarningDetector {

    private const val MINUTE = 60_000L

    /**
     * Ganztägige, abgesagte und abgelehnte Termine zählen nicht. Die Termine sollten
     * vorher mit [Deduplicator] zusammengeführt sein, sonst überschneidet sich eine
     * Besprechung mit ihrer eigenen Kopie aus einem anderen Kalender.
     */
    fun detect(events: List<ClassifiedEvent>, settings: WarningSettings = WarningSettings()): List<ScheduleWarning> {
        val timed = events
            .filter { !it.event.allDay && !it.event.isIgnored && it.event.endMillis > it.event.startMillis }
            .sortedWith(compareBy({ it.event.startMillis }, { it.event.endMillis }))
        val warnings = mutableListOf<ScheduleWarning>()

        if (settings.overlap) {
            for (i in timed.indices) {
                val a = timed[i]
                for (j in i + 1 until timed.size) {
                    val b = timed[j]
                    // Sortiert nach Beginn: Ab hier fangen alle weiteren erst nach dem Ende von a an.
                    if (b.event.startMillis >= a.event.endMillis) break
                    val overlap = minOf(a.event.endMillis, b.event.endMillis) - b.event.startMillis
                    warnings += ScheduleWarning(WarningKind.OVERLAP, a, b, overlap / MINUTE)
                }
            }
        }

        for (j in timed.indices) {
            val b = timed[j]
            val a = timed.indices
                .filter { it != j && timed[it].event.endMillis <= b.event.startMillis }
                .maxByOrNull { timed[it].event.endMillis }
                ?.let(timed::get)
                ?: continue
            val gap = b.event.startMillis - a.event.endMillis
            val presenceAfterOnline = settings.presenceAfterOnline &&
                a.type == EventType.ONLINE && b.type == EventType.PRESENCE &&
                gap < settings.presenceAfterOnlineMinutes * MINUTE
            when {
                // Die genauere Warnung ersetzt „keine Pause".
                presenceAfterOnline -> warnings += ScheduleWarning(WarningKind.PRESENCE_AFTER_ONLINE, a, b, gap / MINUTE)
                settings.noBreak && gap < settings.noBreakMinutes * MINUTE ->
                    warnings += ScheduleWarning(WarningKind.NO_BREAK, a, b, gap / MINUTE)
            }
        }

        return warnings.sortedWith(compareBy({ it.second.event.startMillis }, { it.kind.ordinal }))
    }
}
