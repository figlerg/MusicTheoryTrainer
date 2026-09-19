package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.audio.Sound
import at.gigler.musictheorytrainer.data.Exercise
import at.gigler.musictheorytrainer.data.Settings
import at.gigler.musictheorytrainer.theory.Answer
import at.gigler.musictheorytrainer.theory.Guitar
import at.gigler.musictheorytrainer.theory.SheetQuiz
import at.gigler.musictheorytrainer.theory.StaffNote
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val RUN_NOTES = 400

/**
 * Sight reading against the clock: name as many notes as you can before the time runs out.
 * A wrong answer moves on immediately, so the score is speed and accuracy together.
 */
@Composable
fun SpeedSheetScreen(
    settings: Settings,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    best: Int,
    bestLabel: String,
    onBest: (Int) -> Unit,
    onBack: () -> Unit,
) {
    ExerciseScaffold(Exercise.SHEET_SPEED, settings, updateSettings, onBack) {
        var running by remember { mutableStateOf(false) }
        var finished by remember { mutableStateOf(false) }
        var notes by remember { mutableStateOf(emptyList<StaffNote>()) }
        var results by remember { mutableStateOf(emptyList<Verdict>()) }
        var hits by remember { mutableIntStateOf(0) }
        var remaining by remember { mutableIntStateOf(settings.speedSeconds) }

        fun start() {
            notes = SheetQuiz.randomLine(settings.sheetRange, Random, length = RUN_NOTES)
            results = emptyList()
            hits = 0
            remaining = settings.speedSeconds
            finished = false
            running = true
        }

        LaunchedEffect(running) {
            if (!running) return@LaunchedEffect
            val end = System.currentTimeMillis() + settings.speedSeconds * 1000L
            while (System.currentTimeMillis() < end) {
                remaining = ((end - System.currentTimeMillis() + 999) / 1000).toInt()
                delay(150)
            }
            remaining = 0
            running = false
            finished = true
            onBest(hits)
        }

        LaunchedEffect(running, settings.metronome, settings.metronomeBpm) {
            if (!running || !settings.metronome) return@LaunchedEffect
            while (true) {
                sound.click()
                delay(60_000L / settings.metronomeBpm)
            }
        }

        val index = results.size
        when {
            running -> {
                val row = index / SheetQuiz.LINE_LENGTH
                val chunk = notes.drop(row * SheetQuiz.LINE_LENGTH).take(SheetQuiz.LINE_LENGTH)
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$remaining s", style = MaterialTheme.typography.headlineMedium)
                    Text("$hits richtig", style = MaterialTheme.typography.headlineMedium)
                }
                LinearProgressIndicator(
                    progress = { remaining.toFloat() / settings.speedSeconds.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth(),
                )
                StaffRow(
                    events = plainNotes(chunk),
                    notation = settings.notation,
                    current = index - row * SheetQuiz.LINE_LENGTH,
                    verdicts = results.drop(row * SheetQuiz.LINE_LENGTH),
                )
                Spacer(Modifier.weight(1f))
                NoteInput(
                    notation = settings.notation,
                    mode = settings.inputMode,
                    hideKeyLabels = settings.hideKeyLabels,
                    showStringNames = !settings.hideStringNames,
                    onModeChange = { mode -> updateSettings { it.copy(inputMode = mode) } },
                    state = InputState.ACCEPTING,
                    onNote = { entered ->
                        val note = notes[results.size]
                        val judgement = Answer.judge(entered, note.pitchClass, settings.notation)
                        val position = entered.position
                        val wrongOctave = settings.sheetExactOctave && position != null &&
                            judgement.result.isHit && Guitar.midiAt(position) != note.guitarMidi
                        val hit = judgement.result.isHit && !wrongOctave
                        if (hit) hits++
                        results = results + if (hit) Verdict.CORRECT else Verdict.WRONG
                        onResult(hit)
                    },
                    onContinue = {},
                )
            }

            finished -> {
                Spacer(Modifier.weight(1f))
                Text(
                    "$hits richtig in ${settings.speedSeconds} s",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                val perMinute = hits * 60 / settings.speedSeconds.coerceAtLeast(1)
                Text(
                    "$perMinute Noten pro Minute · ${results.size} Versuche",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Bestwert ($bestLabel): ${maxOf(best, hits)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.weight(1f))
                Button(onClick = ::start, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Nochmal") }
                Spacer(Modifier.height(16.dp))
            }

            else -> {
                Spacer(Modifier.weight(1f))
                Text(
                    "${settings.speedSeconds} Sekunden, so viele Noten wie möglich. Fehler zählen nicht als Treffer.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                if (best > 0) {
                    Text(
                        "Bestwert ($bestLabel): $best",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = ::start, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Start") }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
