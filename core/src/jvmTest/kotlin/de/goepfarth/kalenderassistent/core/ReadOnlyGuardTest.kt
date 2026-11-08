package de.goepfarth.kalenderassistent.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Prüft im Quellcode der Apps, dass Kalender nur gelesen werden und die Android-App
 * keine Internet-Berechtigung hat (PLAN.md, Abschnitt 1).
 */
class ReadOnlyGuardTest {

    // Gradle führt die Tests im Modulordner `core` aus.
    private val repoRoot = File("..").canonicalFile

    private fun sourceFiles(dir: String, vararg extensions: String): List<File> =
        File(repoRoot, dir).takeIf { it.isDirectory }
            ?.walkTopDown()
            ?.filter { it.isFile && it.extension in extensions }
            ?.toList()
            .orEmpty()

    @Test
    fun androidReadsCalendarOnly() {
        val writeCall = Regex("""\.(insert|update|delete|bulkInsert|applyBatch)\(""")
        val offenders = sourceFiles("androidApp/src", "kt", "java")
            .filter { "CalendarContract" in it.readText() && writeCall.containsMatchIn(it.readText()) }

        assertTrue(offenders.isEmpty(), "Schreibzugriff auf den Kalender in: ${offenders.map { it.name }}")
    }

    @Test
    fun appleAppsReadCalendarOnly() {
        val writeCall = Regex("""\.(save|remove|commit)\(""")
        val offenders = (sourceFiles("iosApp", "swift") + sourceFiles("macApp", "swift"))
            .filter { "EKEventStore" in it.readText() && writeCall.containsMatchIn(it.readText()) }

        assertTrue(offenders.isEmpty(), "Schreibzugriff auf den Kalender in: ${offenders.map { it.name }}")
    }

    @Test
    fun androidManifestHasNoWriteOrInternetPermission() {
        val manifest = File(repoRoot, "androidApp/src/main/AndroidManifest.xml").readText()

        assertFalse("android.permission.WRITE_CALENDAR" in manifest, "WRITE_CALENDAR ist nicht erlaubt")
        val internetLines = manifest.lines().filter { "android.permission.INTERNET" in it }
        assertTrue(
            internetLines.all { "tools:node=\"remove\"" in it },
            "INTERNET darf nur als ausdrückliche Entfernung (tools:node=\"remove\") vorkommen",
        )
    }
}
