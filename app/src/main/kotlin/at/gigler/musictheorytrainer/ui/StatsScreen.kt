package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.data.Exercise
import at.gigler.musictheorytrainer.data.InputMode
import at.gigler.musictheorytrainer.data.PracticeEntry
import at.gigler.musictheorytrainer.data.PracticeLog
import at.gigler.musictheorytrainer.data.PracticeStats
import at.gigler.musictheorytrainer.data.Tally
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val CHART_DAYS = 14

@Composable
fun StatsScreen(
    log: PracticeLog,
    periodStart: Long,
    onResetPeriod: () -> Unit,
    onBack: () -> Unit,
) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var entries by remember { mutableStateOf<List<PracticeEntry>?>(null) }
    LaunchedEffect(reload) { entries = log.entries() }
    val zoneOffset = remember { TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong() }

    ScreenScaffold("Statistik", onBack) {
        val data = entries
        if (data == null) {
            Text("Wird geladen …", style = MaterialTheme.typography.bodyLarge)
            return@ScreenScaffold
        }
        val summary = remember(data, showAll, periodStart, zoneOffset) {
            PracticeStats.summarise(data, since = if (showAll) 0L else periodStart, zoneOffsetMillis = zoneOffset)
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (periodStart > 0L) {
                Segmented(
                    listOf(false, true),
                    showAll,
                    { if (it) "Gesamt" else "Seit Reset" },
                    { showAll = it },
                )
                Spacer(Modifier.height(12.dp))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BigNumber("Übungszeit", duration(summary.total.millis))
                BigNumber("Antworten", summary.total.attempts.toString())
                BigNumber("Treffer", summary.total.percent?.let { "$it %" } ?: "–")
            }
            Text(
                buildString {
                    append("An ${summary.activeDays} ")
                    append(if (summary.activeDays == 1) "Tag" else "Tagen")
                    append(" geübt")
                    summary.first?.let { append(", seit ${date(it)}") }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (summary.byDay.isNotEmpty()) {
                Section("Letzte $CHART_DAYS Tage")
                DayChart(summary.byDay, zoneOffset)
            }

            Section("Nach Übung")
            for (exercise in Exercise.entries) {
                summary.byExercise[exercise]?.let { TallyRow(exercise.title, it) }
            }

            Section("Nach Eingabe")
            for (mode in InputMode.entries) {
                summary.byMode[mode]?.let { TallyRow(mode.label, it) }
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = {
                    onResetPeriod()
                    showAll = false
                    reload++
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Zähler zurücksetzen")
            }
            Text(
                "Setzt nur die angezeigten Zähler zurück. Die Gesamtwerte bleiben erhalten.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
        }
    }
}

private val InputMode.label: String
    get() = when (this) {
        InputMode.TEXT -> "Text"
        InputMode.KEYS -> "Tasten"
        InputMode.GUITAR -> "Gitarre"
    }

@Composable
private fun BigNumber(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDivider(Modifier.padding(top = 20.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
    )
}

@Composable
private fun TallyRow(title: String, tally: Tally) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            "${tally.attempts} · ${tally.percent?.let { "$it %" } ?: "–"} · ${duration(tally.millis)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Answers per day, most recent day on the right. */
@Composable
private fun DayChart(byDay: List<Pair<Long, Tally>>, zoneOffset: Long) {
    val today = Math.floorDiv(System.currentTimeMillis() + zoneOffset, PracticeStats.DAY_MILLIS)
    val counts = (0 until CHART_DAYS).map { back ->
        val day = today - (CHART_DAYS - 1 - back)
        byDay.firstOrNull { it.first == day }?.second?.attempts ?: 0
    }
    val peak = counts.max().coerceAtLeast(1)
    val bar = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(Modifier.fillMaxWidth().height(80.dp)) {
        val slot = size.width / CHART_DAYS
        val width = slot * 0.6f
        counts.forEachIndexed { index, attempts ->
            val height = size.height * attempts / peak
            val x = index * slot + (slot - width) / 2
            drawRect(empty, Offset(x, 0f), Size(width, size.height))
            if (attempts > 0) drawRect(bar, Offset(x, size.height - height), Size(width, height))
        }
    }
    Text(
        "Höchster Balken: $peak Antworten",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun duration(millis: Long): String {
    val minutes = millis / 60_000
    return when {
        minutes >= 60 -> "${minutes / 60} h ${minutes % 60} min"
        minutes > 0 -> "$minutes min"
        else -> "${millis / 1000} s"
    }
}

private fun date(millis: Long): String = SimpleDateFormat("d. MMM yyyy", Locale.GERMAN).format(Date(millis))
