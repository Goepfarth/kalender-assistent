package de.goepfarth.kalenderassistent.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import de.goepfarth.kalenderassistent.R
import de.goepfarth.kalenderassistent.calendar.CalendarSummary
import de.goepfarth.kalenderassistent.calendar.TestAgenda
import de.goepfarth.kalenderassistent.calendar.TestDay
import de.goepfarth.kalenderassistent.calendar.TestEntry
import de.goepfarth.kalenderassistent.calendar.TestStats
import de.goepfarth.kalenderassistent.calendar.loadTestAgenda
import de.goepfarth.kalenderassistent.core.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DeviceClock = Clock { System.currentTimeMillis() }
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)
private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)

/** Testansicht für Phase A0: zeigt, was die App aus dem Kalenderspeicher liest. */
@Composable
fun TestScreen() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasCalendarPermission()) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var agenda by remember { mutableStateOf<TestAgenda?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        askedOnce = true
    }

    // Bei jeder Rückkehr in die App neu lesen, z. B. nach Änderungen im Samsung Kalender.
    LifecycleResumeEffect(Unit) {
        granted = context.hasCalendarPermission()
        reloadKey++
        onPauseOrDispose { }
    }

    LaunchedEffect(granted, reloadKey) {
        if (!granted) return@LaunchedEffect
        try {
            agenda = withContext(Dispatchers.IO) { loadTestAgenda(context.contentResolver, DeviceClock) }
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: e::class.simpleName
        }
    }

    Scaffold { insets ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = insets.calculateTopPadding() + 16.dp,
                bottom = insets.calculateBottomPadding() + 32.dp,
            ),
        ) {
            item {
                Header(loadedAt = agenda?.loadedAt, onRefresh = { reloadKey++ }, showRefresh = granted)
            }
            val current = agenda
            when {
                !granted -> item {
                    val activity = context.findActivity()
                    val permanentlyDenied = askedOnce &&
                        activity?.shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR) == false
                    PermissionCard(
                        permanentlyDenied = permanentlyDenied,
                        onRequest = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                        onOpenSettings = { context.openAppSettings() },
                    )
                }
                error != null -> item {
                    Text(
                        stringResource(R.string.error_reading, error.orEmpty()),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                current == null -> item {
                    Text(stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> agendaItems(current)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.agendaItems(agenda: TestAgenda) {
    item { SectionTitle(stringResource(R.string.section_calendars)) }
    if (agenda.calendars.isEmpty()) {
        item { MutedText(stringResource(R.string.no_calendars)) }
    }
    items(agenda.calendars, key = { "cal-${it.calendar.id}" }) { CalendarRow(it) }

    item { SectionTitle(stringResource(R.string.section_checks)) }
    item { StatsCard(agenda.stats) }

    item { SectionTitle(stringResource(R.string.section_events)) }
    if (agenda.days.isEmpty()) {
        item { MutedText(stringResource(R.string.no_events)) }
    }
    agenda.days.forEach { day ->
        item(key = "day-${day.date}") { DayHeader(day) }
        items(day.entries, key = { "ev-${it.event.eventId}-${it.event.begin}" }) { EventRow(it) }
    }
}

@Composable
private fun Header(loadedAt: Instant?, onRefresh: () -> Unit, showRefresh: Boolean) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        MutedText(stringResource(R.string.test_view_subtitle))
        if (showRefresh) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (loadedAt != null) {
                    MutedText(
                        stringResource(R.string.loaded_at, timeFormat.format(loadedAt.atZone(ZoneId.systemDefault()))),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                TextButton(onClick = onRefresh) { Text(stringResource(R.string.refresh)) }
            }
        }
    }
}

@Composable
private fun PermissionCard(permanentlyDenied: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.permission_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(R.string.permission_text), style = MaterialTheme.typography.bodyMedium)
            if (permanentlyDenied) {
                MutedText(stringResource(R.string.permission_denied_text))
                Button(onClick = onOpenSettings) { Text(stringResource(R.string.permission_open_settings)) }
            } else {
                Button(onClick = onRequest) { Text(stringResource(R.string.permission_button)) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun MutedText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun CalendarRow(summary: CalendarSummary) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(calendarColor(summary.calendar.color)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(summary.calendar.name, style = MaterialTheme.typography.bodyLarge)
            MutedText(summary.calendar.accountName)
        }
        if (!summary.calendar.visible) {
            Badge(stringResource(R.string.badge_hidden))
            Spacer(Modifier.width(8.dp))
        }
        MutedText(stringResource(R.string.event_count, summary.eventCount))
    }
}

@Composable
private fun StatsCard(stats: TestStats) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.stats_total, stats.total))
            Text(stringResource(R.string.stats_uid, stats.withUid, stats.total))
            Text(stringResource(R.string.stats_links, stats.withMeetingLink, stats.total))
            Text(stringResource(R.string.stats_status, stats.canceled, stats.declined, stats.tentative))
            Spacer(Modifier.height(4.dp))
            MutedText(stringResource(R.string.stats_explanation))
        }
    }
}

@Composable
private fun DayHeader(day: TestDay) {
    val today = LocalDate.now()
    val prefix = when (day.date) {
        today -> stringResource(R.string.today) + " · "
        today.plusDays(1) -> stringResource(R.string.tomorrow) + " · "
        else -> ""
    }
    Text(
        prefix + dayFormat.format(day.date),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun EventRow(entry: TestEntry) {
    val event = entry.event
    val zone = ZoneId.systemDefault()
    val time = if (event.allDay) {
        stringResource(R.string.all_day)
    } else {
        timeFormat.format(Instant.ofEpochMilli(event.begin).atZone(zone)) + "–" +
            timeFormat.format(Instant.ofEpochMilli(event.end).atZone(zone))
    }
    val details = listOfNotNull(entry.calendar?.name, event.location?.takeIf { it.isNotBlank() })

    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 6.dp)) {
        Box(
            Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp))
                .background(calendarColor(event.color)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(event.title.ifBlank { stringResource(R.string.no_title) }, style = MaterialTheme.typography.bodyLarge)
            if (details.isNotEmpty()) {
                Text(
                    details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val badges = buildList {
                entry.meetingLink?.let { add(stringResource(R.string.badge_online, it.provider.displayName) to true) }
                if (entry.tentative) add(stringResource(R.string.badge_tentative) to false)
                if (entry.declined) add(stringResource(R.string.badge_declined) to false)
                if (entry.canceled) add(stringResource(R.string.badge_canceled) to false)
                if (event.uid == null) add(stringResource(R.string.badge_no_uid) to false)
            }
            if (badges.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                    badges.forEach { (label, accent) -> Badge(label, accent) }
                }
            }
        }
    }
}

@Composable
private fun Badge(label: String, accent: Boolean = false) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

// Manche Kalender speichern Farben ohne Alpha-Kanal.
private fun calendarColor(argb: Int) = Color(argb or 0xFF000000.toInt())

private fun Context.hasCalendarPermission() =
    checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
