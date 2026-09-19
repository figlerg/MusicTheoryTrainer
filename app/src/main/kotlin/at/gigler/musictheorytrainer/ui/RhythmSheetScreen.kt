package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import at.gigler.musictheorytrainer.theory.RhythmNote
import at.gigler.musictheorytrainer.theory.RhythmQuiz
import at.gigler.musictheorytrainer.theory.Timing
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val COUNT_IN_BEATS = 4
private const val REST_CHANCE = 0.25

/** Green for on time, amber for loose, red for missed or wrong. */
private fun Timing.verdict(): Verdict = when (this) {
    Timing.PERFECT, Timing.GOOD -> Verdict.CORRECT
    Timing.LOOSE -> Verdict.UNUSUAL
    Timing.MISSED -> Verdict.WRONG
}

/**
 * Reading in time: the metronome counts in and keeps going, the cursor moves with the beat, and
 * every note is graded by how close the answer was to its beat. Rests are simply skipped.
 */
@Composable
fun RhythmSheetScreen(
    settings: Settings,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    best: Int,
    bestLabel: String,
    onBest: (Int) -> Unit,
    onBack: () -> Unit,
) {
    ExerciseScaffold(Exercise.SHEET_RHYTHM, settings, updateSettings, onBack) {
        val beatMillis = 60_000L / settings.metronomeBpm.coerceAtLeast(20)
        var line by remember { mutableStateOf(emptyList<RhythmNote>()) }
        var grades by remember { mutableStateOf(mapOf<Int, Timing>()) }
        var startTime by remember { mutableLongStateOf(0L) }
        var running by remember { mutableStateOf(false) }
        var finished by remember { mutableStateOf(false) }
        var lastLabel by remember { mutableStateOf<String?>(null) }
        var countIn by remember { mutableIntStateOf(0) }

        val offsets = remember(line) { RhythmQuiz.beatOffsets(line) }
        val noteIndices = remember(line) { line.indices.filter { !line[it].isRest } }

        fun expectedAt(index: Int) = startTime + (offsets[index] * beatMillis).toLong()
        fun nextIndex(): Int? = noteIndices.firstOrNull { it !in grades }

        fun finish() {
            running = false
            finished = true
            onBest(grades.values.sumOf { it.points })
        }

        fun start() {
            line = RhythmQuiz.line(
                range = settings.sheetRange,
                level = settings.rhythmLevel,
                bars = settings.rhythmBars,
                random = Random,
                restChance = if (settings.rhythmRests) REST_CHANCE else 0.0,
            )
            grades = emptyMap()
            lastLabel = null
            finished = false
            startTime = System.currentTimeMillis() + COUNT_IN_BEATS * beatMillis
            countIn = COUNT_IN_BEATS
            running = true
        }

        fun answer(entered: at.gigler.musictheorytrainer.theory.EnteredNote) {
            val index = nextIndex() ?: return
            val note = line[index].note ?: return
            val now = System.currentTimeMillis()
            val judgement = Answer.judge(entered, note.pitchClass, settings.notation)
            val position = entered.position
            val wrongOctave = settings.sheetExactOctave && position != null &&
                judgement.result.isHit && Guitar.midiAt(position) != note.guitarMidi
            val rightNote = judgement.result.isHit && !wrongOctave
            val timing = if (rightNote) Timing.of(now - expectedAt(index)) else Timing.MISSED
            grades = grades + (index to timing)
            lastLabel = when {
                !rightNote && wrongOctave -> "Falsche Oktave"
                !rightNote -> "Falsche Note"
                else -> timing.label
            }
            onResult(timing.points > 0)
            if (nextIndex() == null) finish()
        }

        // The metronome keeps time and the same loop retires notes whose beat has passed.
        LaunchedEffect(running) {
            if (!running) return@LaunchedEffect
            var nextBeat = startTime - COUNT_IN_BEATS * beatMillis
            val end = startTime + (RhythmQuiz.totalBeats(line) * beatMillis).toLong() + Timing.LAST_CHANCE_MILLIS
            while (running && System.currentTimeMillis() < end) {
                val now = System.currentTimeMillis()
                if (now >= nextBeat) {
                    sound.click()
                    nextBeat += beatMillis
                }
                countIn = (((startTime - now) + beatMillis - 1) / beatMillis).toInt().coerceAtLeast(0)
                val overdue = noteIndices.filter { it !in grades && now > expectedAt(it) + Timing.LAST_CHANCE_MILLIS }
                if (overdue.isNotEmpty()) {
                    grades = grades + overdue.associateWith { Timing.MISSED }
                    lastLabel = Timing.MISSED.label
                    repeat(overdue.size) { onResult(false) }
                }
                delay(16)
            }
            if (running) finish()
        }

        val points = grades.values.sumOf { it.points }
        val maximum = noteIndices.size * Timing.PERFECT.points
        when {
            running -> {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (countIn > 0) "Los in $countIn" else "${settings.metronomeBpm} bpm",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text("$points von $maximum", style = MaterialTheme.typography.titleLarge)
                }
                StaffRow(
                    events = line,
                    notation = settings.notation,
                    current = nextIndex(),
                    verdicts = line.indices.map { grades[it]?.verdict() },
                    showNames = false,
                )
                FeedbackLine(lastLabel, grades[grades.keys.maxOrNull() ?: -1]?.verdict())
                Spacer(Modifier.weight(1f))
                NoteInput(
                    notation = settings.notation,
                    mode = settings.inputMode,
                    hideKeyLabels = settings.hideKeyLabels,
                    showStringNames = !settings.hideStringNames,
                    onModeChange = { mode -> updateSettings { it.copy(inputMode = mode) } },
                    state = InputState.ACCEPTING,
                    onNote = ::answer,
                    onContinue = {},
                )
            }

            finished -> {
                StaffRow(
                    events = line,
                    notation = settings.notation,
                    current = null,
                    verdicts = line.indices.map { grades[it]?.verdict() },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "$points von $maximum Punkten",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                for (grade in Timing.entries) {
                    val count = grades.values.count { it == grade }
                    if (count == 0) continue
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(grade.label, style = MaterialTheme.typography.bodyLarge, color = verdictColor(grade.verdict()))
                        Text("$count", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Text(
                    "Bestwert ($bestLabel): ${maxOf(best, points)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.weight(1f))
                Button(onClick = ::start, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Nochmal") }
                Spacer(Modifier.height(16.dp))
            }

            else -> {
                Spacer(Modifier.weight(1f))
                Text(
                    "Das Metronom zählt einen Takt ein und läuft dann weiter. Spiel jede Note auf ihrem Schlag: " +
                        "je genauer, desto mehr Punkte. Perfekt gibt 3, Gut 2, Mäßig 1.",
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
