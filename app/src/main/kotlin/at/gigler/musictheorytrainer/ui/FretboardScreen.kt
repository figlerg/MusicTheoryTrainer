package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.audio.Sound
import at.gigler.musictheorytrainer.data.Exercise
import at.gigler.musictheorytrainer.data.Settings
import at.gigler.musictheorytrainer.theory.FretPosition
import at.gigler.musictheorytrainer.theory.FretboardQuiz
import at.gigler.musictheorytrainer.theory.Guitar
import at.gigler.musictheorytrainer.theory.NoteNames
import at.gigler.musictheorytrainer.theory.PitchClass
import at.gigler.musictheorytrainer.theory.Spelling
import at.gigler.musictheorytrainer.theory.Tab
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun FretboardScreen(
    settings: Settings,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    onReverseChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(Exercise.FRETBOARD.title, onBack) {
        Segmented(
            listOf(false, true),
            settings.fretboardReverse,
            { if (it) "Stelle finden" else "Ton nennen" },
            onReverseChange,
        )
        Spacer(Modifier.height(8.dp))
        val strings = settings.stringSet.strings
        key(strings, settings.fretboardReverse) {
            if (settings.fretboardReverse) {
                FindPositionDrill(settings, strings, sound, onResult)
            } else {
                NameNoteDrill(settings, strings, sound, onResult)
            }
        }
    }
}

/** A dot is shown, the user names the note. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.NameNoteDrill(
    settings: Settings,
    strings: List<Int>,
    sound: Sound,
    onResult: (Boolean) -> Unit,
) {
    val notation = settings.notation
    var position by remember { mutableStateOf(FretboardQuiz.randomPosition(strings, Random)) }
    var given by remember { mutableStateOf<PitchClass?>(null) }
    val answer = Guitar.pitchAt(position)
    val verdict = given?.let { if (it == answer) Verdict.CORRECT else Verdict.WRONG }

    fun next() {
        position = FretboardQuiz.randomPosition(strings, Random, position)
        given = null
    }

    LaunchedEffect(position, verdict) {
        if (verdict == Verdict.CORRECT) {
            delay(AUTO_ADVANCE_MILLIS)
            next()
        }
    }

    val mark = FretMark(
        position,
        label = if (verdict == null) "?" else NoteNames.name(answer, notation, Spelling.SHARP),
        style = when (verdict) {
            null -> MarkStyle.NORMAL
            Verdict.CORRECT -> MarkStyle.CORRECT
            Verdict.WRONG -> MarkStyle.WRONG
        },
    )
    Fretboard(listOf(mark), notation, Modifier.weight(1f).fillMaxWidth(), activeStrings = strings)
    // With the soft keyboard open there is no room to spare.
    if (settings.showTab && !WindowInsets.isImeVisible) {
        TabText(Tab.renderSequence(listOf(position), notation))
    }
    FeedbackLine(
        text = when (verdict) {
            null -> null
            Verdict.CORRECT -> "Richtig: ${NoteNames.bothNames(answer, notation)}"
            Verdict.WRONG -> "Falsch: ${NoteNames.bothNames(answer, notation)}, nicht ${NoteNames.bothNames(given!!, notation)}"
        },
        verdict = verdict,
    )
    NoteInput(
        notation = notation,
        defaultMode = settings.inputMode,
        state = when (verdict) {
            null -> InputState.ACCEPTING
            Verdict.CORRECT -> InputState.LOCKED
            Verdict.WRONG -> InputState.CONTINUE
        },
        onNote = { pitch ->
            given = pitch
            sound.play(Guitar.midiAt(position))
            onResult(pitch == answer)
        },
        onContinue = ::next,
    )
}

/** A note is named, the user taps every place it occurs on the active strings. */
@Composable
private fun ColumnScope.FindPositionDrill(
    settings: Settings,
    strings: List<Int>,
    sound: Sound,
    onResult: (Boolean) -> Unit,
) {
    val notation = settings.notation
    var target by remember { mutableStateOf(FretboardQuiz.randomPitch(Random)) }
    var spelling by remember { mutableStateOf(randomSpelling()) }
    var found by remember { mutableStateOf(emptySet<FretPosition>()) }
    var wrong by remember { mutableStateOf(emptySet<FretPosition>()) }
    var revealed by remember { mutableStateOf(false) }
    val targets = remember(target) { Guitar.positionsOf(target, strings) }
    val allFound = found.size == targets.size
    val done = allFound || revealed
    val success = allFound && wrong.isEmpty() && !revealed

    fun next() {
        target = FretboardQuiz.randomPitch(Random, target)
        spelling = randomSpelling()
        found = emptySet()
        wrong = emptySet()
        revealed = false
    }

    fun tap(position: FretPosition) {
        if (done) return
        if (position in targets) {
            if (position in found) return
            found = found + position
            sound.play(Guitar.midiAt(position))
            if (found.size == targets.size) onResult(wrong.isEmpty())
        } else {
            wrong = wrong + position
        }
    }

    LaunchedEffect(target, success) {
        if (success) {
            delay(AUTO_ADVANCE_MILLIS)
            next()
        }
    }

    val name = NoteNames.name(target, notation, spelling)
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Finde: $name", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        Text(
            "${found.size}/${targets.size}",
            style = MaterialTheme.typography.titleLarge,
            color = verdictColor(if (!done) null else if (success) Verdict.CORRECT else Verdict.WRONG),
        )
    }

    val marks = buildList {
        found.forEach { add(FretMark(it, name, MarkStyle.CORRECT)) }
        wrong.forEach { add(FretMark(it, NoteNames.name(Guitar.pitchAt(it), notation, spelling), MarkStyle.WRONG)) }
        if (revealed) (targets - found).forEach { add(FretMark(it, name, MarkStyle.HINT)) }
    }
    Fretboard(marks, notation, Modifier.weight(1f).fillMaxWidth(), activeStrings = strings, onTap = ::tap)
    if (settings.showTab && done) {
        TabText(Tab.render(targets.map { listOf(it) }, notation))
    }
    Spacer(Modifier.height(8.dp))
    Box(Modifier.fillMaxWidth().height(56.dp)) {
        if (!done) {
            OutlinedButton(
                onClick = {
                    revealed = true
                    onResult(false)
                },
                modifier = Modifier.fillMaxSize(),
            ) { Text("Lösung zeigen") }
        } else if (!success) {
            Button(onClick = ::next, modifier = Modifier.fillMaxSize()) { Text("Weiter") }
        }
    }
    Spacer(Modifier.height(8.dp))
}

private fun randomSpelling() = if (Random.nextBoolean()) Spelling.SHARP else Spelling.FLAT
