package de.goepfarth.kalenderassistent.core

/**
 * Liefert die aktuelle Zeit. Logik in `core` fragt nie die Systemuhr direkt,
 * damit Tests die Zeit festlegen können.
 */
fun interface Clock {
    /** Millisekunden seit 1970-01-01T00:00:00Z. */
    fun nowEpochMillis(): Long
}
