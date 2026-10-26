package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import at.gigler.musictheorytrainer.theory.Answer
import at.gigler.musictheorytrainer.theory.EnteredNote
import at.gigler.musictheorytrainer.theory.FretboardQuiz
import at.gigler.musictheorytrainer.theory.NoteNames
import at.gigler.musictheorytrainer.theory.Piano
import at.gigler.musictheorytrainer.theory.PianoQuiz
import at.gigler.musictheorytrainer.theory.PitchClass
import at.gigler.musictheorytrainer.theory.Spelling
import kotlin.random.Random

private val KEYBOARD_HEIGHT = 180.dp

@Composable
fun PianoScreen(
    settings: Settings,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
) {
    ExerciseScaffold(Exercise.PIANO, settings, updateSettings, onBack) {
        Segmented(
            listOf(false, true),
            settings.pianoReverse,
            { if (it) "Taste finden" else "Taste benennen" },
            { reverse -> updateSettings { it.copy(pianoReverse = reverse) } },
        )
        Spacer(Modifier.height(8.dp))
        key(settings.pianoReverse) {
            if (settings.pianoReverse) {
                FindKeyDrill(settings, sound, onResult)
            } else {
                NameKeyDrill(settings, sound, onResult, updateSettings)
            }
        }
    }
}

/** One key is marked, the user names it. */
@Composable
private fun ColumnScope.NameKeyDrill(
    settings: Settings,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
) {
    val notation = settings.notation
    var midi by remember { mutableStateOf(PianoQuiz.randomKey(Piano.TWO_OCTAVES, Random)) }
    val pitch = PitchClass.of(midi)
    val state = rememberSingleAnswer(midi) { midi = PianoQuiz.randomKey(Piano.TWO_OCTAVES, Random, midi) }
    val solved = state.revealed || state.verdict?.isHit == true

    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        PianoKeyboard(
            Piano.TWO_OCTAVES,
            Modifier.fillMaxWidth().height(KEYBOARD_HEIGHT),
            marks = mapOf(midi to if (state.revealed) MarkStyle.HINT else state.verdict.markStyle()),
            label = { if (it == midi) (if (solved) NoteNames.bothNames(pitch, notation) else "?") else null },
        )
    }
    FeedbackLine(
        state.feedback(NoteNames.bothNames(pitch, notation)) { given -> given.spelled?.name(notation) ?: NoteNames.bothNames(given.pitch, notation) },
        state.feedbackVerdict,
    )
    if (state.showMistakeActions || state.revealed) {
        MistakeActions(
            onSolution = if (state.revealed) null else ({
                state.reveal(onResult)
                sound.play(midi)
            }),
            onExplain = { state.explain(onResult) },
        )
    }
    NoteInput(
        notation = notation,
        mode = settings.inputMode,
        onModeChange = { mode -> updateSettings { it.copy(inputMode = mode) } },
        state = state.inputState,
        onNote = { entered ->
            // Read from state: a captured key can be stale after moving on.
            val judgement = Answer.judge(entered, PitchClass.of(midi), notation)
            state.answer(entered, judgement, onResult)
            if (judgement.result.isHit) sound.play(midi)
        },
        onContinue = { midi = PianoQuiz.randomKey(Piano.TWO_OCTAVES, Random, midi) },
        // Tapping the same spot on a second keyboard would not be naming it.
        allowGuitar = false,
        allowKeys = false,
        onUnreadable = state::markUnreadable,
    )
    if (state.explaining) {
        KeyExplanation(midi, settings) { state.explaining = false }
    }
}

/** A note is named, the user presses a key with that name. */
@Composable
private fun ColumnScope.FindKeyDrill(settings: Settings, sound: Sound, onResult: (Boolean) -> Unit) {
    val notation = settings.notation
    var target by remember { mutableStateOf(FretboardQuiz.randomPitch(Random)) }
    var spelling by remember { mutableStateOf(if (Random.nextBoolean()) Spelling.SHARP else Spelling.FLAT) }
    var pressed by remember { mutableStateOf<Int?>(null) }

    fun next() {
        target = FretboardQuiz.randomPitch(Random, target)
        spelling = if (Random.nextBoolean()) Spelling.SHARP else Spelling.FLAT
        pressed = null
    }

    val state = rememberSingleAnswer(target) { next() }
    val name = NoteNames.name(target, notation, spelling)
    Text(
        "Finde: $name",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
    val marks = buildMap {
        if (state.revealed) Piano.TWO_OCTAVES.filter { PitchClass.of(it) == target }.forEach { put(it, MarkStyle.HINT) }
        pressed?.let { put(it, state.verdict.markStyle()) }
    }
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        PianoKeyboard(
            Piano.TWO_OCTAVES,
            Modifier.fillMaxWidth().height(KEYBOARD_HEIGHT),
            marks = marks,
            label = { key -> if (key == pressed || key in marks) NoteNames.bothNames(PitchClass.of(key), notation) else null },
            onTap = { key ->
                // Everything is read from state at tap time: the keyboard keeps this callback.
                if (state.inputState != InputState.ACCEPTING) return@PianoKeyboard
                pressed = key
                sound.play(key)
                state.answer(EnteredNote(PitchClass.of(key)), Answer.judge(EnteredNote(PitchClass.of(key)), target, notation), onResult)
            },
        )
    }
    FeedbackLine(
        state.feedback(NoteNames.bothNames(target, notation)) { NoteNames.bothNames(it.pitch, notation) },
        state.feedbackVerdict,
    )
    Spacer(Modifier.height(8.dp))
    Box(Modifier.fillMaxWidth().height(56.dp)) {
        if (state.revealed) {
            Button(onClick = ::next, modifier = Modifier.fillMaxSize()) { Text("Weiter") }
        } else if (state.verdict?.isHit != true) {
            OutlinedButton(
                onClick = {
                    state.reveal(onResult)
                    sound.play(Piano.TWO_OCTAVES.first { PitchClass.of(it) == target })
                },
                modifier = Modifier.fillMaxSize(),
            ) { Text("Lösung zeigen") }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun KeyExplanation(midi: Int, settings: Settings, onDismiss: () -> Unit) {
    val notation = settings.notation
    val pitch = PitchClass.of(midi)
    val c = midi - pitch.semitone
    ExplanationDialog("Taste: ${NoteNames.bothNames(pitch, notation)}", onDismiss) {
        PianoKeyboard(
            c until c + 12,
            Modifier.fillMaxWidth().height(120.dp),
            marks = mapOf(c to MarkStyle.ROOT, midi to MarkStyle.CORRECT),
            label = { if (it == c || it == midi) NoteNames.bothNames(PitchClass.of(it), notation) else null },
        )
        Text(
            "Das C liegt immer links von der Zweiergruppe schwarzer Tasten. " +
                if (pitch.semitone == 0) "Diese Taste ist das C selbst."
                else "Von dort aus ${pitch.semitone} Halbtöne nach rechts, jede Taste (weiß oder schwarz) ist ein Halbton.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
