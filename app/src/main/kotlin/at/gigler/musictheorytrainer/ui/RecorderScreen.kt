package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import at.gigler.musictheorytrainer.theory.AnswerResult
import at.gigler.musictheorytrainer.theory.EnteredNote
import at.gigler.musictheorytrainer.theory.Hole
import at.gigler.musictheorytrainer.theory.Judgement
import at.gigler.musictheorytrainer.theory.Notation
import at.gigler.musictheorytrainer.theory.NoteNames
import at.gigler.musictheorytrainer.theory.Recorder
import at.gigler.musictheorytrainer.theory.RecorderNote
import at.gigler.musictheorytrainer.theory.RecorderQuiz
import at.gigler.musictheorytrainer.theory.RecorderSystem
import at.gigler.musictheorytrainer.theory.StaffNote
import kotlin.random.Random

@Composable
fun RecorderScreen(
    settings: Settings,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
) {
    ExerciseScaffold(Exercise.RECORDER, settings, updateSettings, onBack) {
        Segmented(
            listOf(false, true),
            settings.recorderReverse,
            { if (it) "Ton greifen" else "Griff lesen" },
            { reverse -> updateSettings { it.copy(recorderReverse = reverse) } },
        )
        Spacer(Modifier.height(8.dp))
        key(settings.recorderSystem, settings.recorderReverse) {
            if (settings.recorderReverse) {
                FingerNoteDrill(settings, sound, onResult)
            } else {
                ReadFingeringDrill(settings, sound, onResult, updateSettings)
            }
        }
    }
}

/** The written note for the staff, if it is a natural one; the staff here has no accidentals. */
private fun staffNoteOf(note: RecorderNote): StaffNote? =
    StaffNote(note.spelled.letter, note.octave).takeIf { note.spelled.alteration == 0 }

private fun givenName(given: EnteredNote, system: RecorderSystem, notation: Notation): String =
    given.writtenMidi?.let { Recorder.noteAt(it, system)?.name(notation) }
        ?: given.spelled?.name(notation)
        ?: NoteNames.bothNames(given.pitch, notation)

/** A fingering is shown, the user names the note. */
@Composable
private fun ColumnScope.ReadFingeringDrill(
    settings: Settings,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
) {
    val notation = settings.notation
    val system = settings.recorderSystem
    var note by remember { mutableStateOf(RecorderQuiz.randomNote(system, Random)) }
    val state = rememberSingleAnswer(note) { note = RecorderQuiz.randomNote(system, Random, note) }

    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        RecorderFingeringView(note.fingering, Modifier.widthIn(max = 440.dp).fillMaxWidth())
    }
    FeedbackLine(
        state.feedback(note.name(notation)) { givenName(it, system, notation) },
        state.feedbackVerdict,
    )
    if (state.showMistakeActions || state.revealed) {
        MistakeActions(
            onSolution = if (state.revealed) null else ({
                state.reveal(onResult)
                sound.play(note.soundingMidi)
            }),
            onExplain = { state.explain(onResult) },
        )
    }
    NoteInput(
        notation = notation,
        mode = settings.inputMode,
        hideKeyLabels = settings.hideKeyLabels,
        onModeChange = { mode -> updateSettings { it.copy(inputMode = mode) } },
        state = state.inputState,
        onNote = { entered ->
            // Read from state: a captured note can be stale after moving on.
            val judgement = Answer.judge(entered, note.pitchClass, notation)
            state.answer(entered, judgement, onResult)
            if (judgement.result.isHit) sound.play(note.soundingMidi)
        },
        onContinue = { note = RecorderQuiz.randomNote(system, Random, note) },
        allowGuitar = false,
        onUnreadable = state::markUnreadable,
    )
    if (state.explaining) {
        FingeringExplanation(note, notation, state.unreadable) { state.explaining = false }
    }
}

/** A note is named (and written on the staff), the user fingers it. */
@Composable
private fun ColumnScope.FingerNoteDrill(settings: Settings, sound: Sound, onResult: (Boolean) -> Unit) {
    val notation = settings.notation
    val system = settings.recorderSystem
    var note by remember { mutableStateOf(RecorderQuiz.randomNote(system, Random)) }
    val state = rememberSingleAnswer(note) { note = RecorderQuiz.randomNote(system, Random, note) }

    Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Greife: ${note.name(notation)}", style = MaterialTheme.typography.headlineMedium)
        staffNoteOf(note)?.let {
            StaffRow(plainNotes(listOf(it)), notation, current = null, verdicts = emptyList())
        }
        if (state.revealed) {
            RecorderFingeringView(note.fingering, Modifier.widthIn(max = 300.dp).fillMaxWidth(), captions = false)
        }
    }
    FeedbackLine(
        state.feedback(note.name(notation)) { givenName(it, system, notation) },
        state.feedbackVerdict,
    )
    if (state.showMistakeActions || state.revealed) {
        MistakeActions(
            onSolution = if (state.revealed) null else ({
                state.reveal(onResult)
                sound.play(note.soundingMidi)
            }),
            onExplain = { state.explain(onResult) },
        )
    }
    if (state.revealed) {
        Button(
            onClick = { note = RecorderQuiz.randomNote(system, Random, note) },
            modifier = Modifier.fillMaxWidth().height(RECORDER_INPUT_HEIGHT),
        ) { Text("Weiter", style = MaterialTheme.typography.titleLarge) }
    } else {
        Box(Modifier.fillMaxWidth().height(RECORDER_INPUT_HEIGHT)) {
            RecorderInput(
                enabled = state.inputState == InputState.ACCEPTING,
                onNote = { played ->
                    // The fingering must give exactly this note, octave included.
                    val result = if (played.writtenMidi == note.writtenMidi) AnswerResult.CORRECT else AnswerResult.WRONG
                    state.answer(
                        EnteredNote(played.pitchClass, writtenMidi = played.writtenMidi),
                        Judgement(result),
                        onResult,
                    )
                    sound.play(played.soundingMidi)
                },
                onUnknown = state::markUnreadable,
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    if (state.explaining) {
        FingeringExplanation(note, notation, state.unreadable) { state.explaining = false }
    }
}

@Composable
private fun FingeringExplanation(note: RecorderNote, notation: Notation, unreadable: Boolean, onDismiss: () -> Unit) {
    ExplanationDialog("Griff für ${note.name(notation)}", onDismiss) {
        RecorderFingeringView(note.fingering, Modifier.fillMaxWidth())
        Text(describe(note), style = MaterialTheme.typography.bodyMedium)
        if (unreadable) UnreadableAnswerLine()
    }
}

/** The fingering in words: which holes are covered, and how the thumb is used. */
private fun describe(note: RecorderNote): String {
    val holes = note.fingering.holes
    val thumb = when (holes[Recorder.THUMB]) {
        Hole.CLOSED -> "Daumenloch zu"
        Hole.HALF -> "Daumenloch halb offen (Daumennagel)"
        Hole.OPEN -> "Daumenloch offen"
    }
    val closed = (1 until Recorder.HOLE_COUNT).filter { holes[it] != Hole.OPEN }
    val front = if (closed.isEmpty()) "vorne alles offen" else "vorne zu: ${closed.joinToString(" ")}"
    return "$thumb, $front. Klingt eine Oktave höher als notiert."
}
