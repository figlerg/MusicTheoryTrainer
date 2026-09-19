package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import at.gigler.musictheorytrainer.theory.EarChord
import at.gigler.musictheorytrainer.theory.EarInterval
import at.gigler.musictheorytrainer.theory.EarQuiz
import at.gigler.musictheorytrainer.theory.NoteNames
import at.gigler.musictheorytrainer.theory.PitchClass
import at.gigler.musictheorytrainer.theory.Spelling
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Hearing exercises: the question is a sound, the answer a name. */
@Composable
fun EarScreen(
    exercise: Exercise,
    settings: Settings,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
) {
    ExerciseScaffold(exercise, settings, updateSettings, onBack) {
        when (exercise) {
            Exercise.EAR_INTERVAL -> IntervalEar(settings, sound, onResult)
            Exercise.EAR_QUALITY -> ChordEar(EarChord.MAJOR_MINOR, settings, sound, onResult)
            Exercise.EAR_CHORD -> ChordEar(EarChord.ALL, settings, sound, onResult)
            else -> PitchEar(settings, sound, onResult, updateSettings)
        }
    }
}

private data class IntervalTask(val interval: EarInterval, val root: Int)

@Composable
private fun ColumnScope.IntervalEar(settings: Settings, sound: Sound, onResult: (Boolean) -> Unit) {
    val pool = settings.earIntervalSet.intervals
    var task by remember(pool) { mutableStateOf(IntervalTask(EarQuiz.next(pool, Random), EarQuiz.randomRoot(Random))) }
    var picked by remember(task) { mutableStateOf<EarInterval?>(null) }
    var counted by remember(task) { mutableStateOf(false) }
    var revealed by remember(task) { mutableStateOf(false) }

    fun play() {
        val notes = EarQuiz.intervalNotes(task.root, task.interval)
        if (settings.earHarmonic) sound.strum(notes) else sound.playSequence(notes, secondsEach = 0.7)
    }

    fun nextTask() {
        task = IntervalTask(EarQuiz.next(pool, Random, task.interval), EarQuiz.randomRoot(Random))
    }

    fun pick(choice: EarInterval) {
        if (picked == task.interval || revealed) return
        if (!counted) {
            onResult(choice == task.interval)
            counted = true
        }
        picked = choice
    }

    LaunchedEffect(task) { play() }
    LaunchedEffect(picked) {
        if (picked == task.interval) {
            delay(AUTO_ADVANCE_MILLIS + 500)
            nextTask()
        }
    }

    val solved = picked == task.interval || revealed
    Listen(sound, ::play)
    FeedbackLine(feedbackText(revealed, picked?.label, task.interval.label, picked == task.interval), verdictOf(revealed, picked, task.interval))
    Spacer(Modifier.weight(1f))
    ChoiceGrid(
        labels = pool.map { it.label },
        onPick = { pick(pool[it]) },
        columns = 2,
        enabled = !solved,
        verdictOf = { index ->
            when {
                solved && pool[index] == task.interval -> Verdict.CORRECT
                picked == pool[index] -> Verdict.WRONG
                else -> null
            }
        },
    )
    GiveUp(show = picked != null && !solved) {
        revealed = true
        if (!counted) {
            onResult(false)
            counted = true
        }
    }
    NextRound(show = revealed, onNext = ::nextTask)
}

private data class ChordTask(val chord: EarChord, val root: Int)

@Composable
private fun ColumnScope.ChordEar(
    pool: List<EarChord>,
    settings: Settings,
    sound: Sound,
    onResult: (Boolean) -> Unit,
) {
    var task by remember(pool) { mutableStateOf(ChordTask(EarQuiz.next(pool, Random), EarQuiz.randomRoot(Random))) }
    var picked by remember(task) { mutableStateOf<EarChord?>(null) }
    var counted by remember(task) { mutableStateOf(false) }
    var revealed by remember(task) { mutableStateOf(false) }

    fun play() {
        val notes = EarQuiz.chordNotes(task.root, task.chord)
        if (settings.earHarmonic) sound.strum(notes) else sound.playSequence(notes, secondsEach = 0.45)
    }

    fun nextTask() {
        task = ChordTask(EarQuiz.next(pool, Random, task.chord), EarQuiz.randomRoot(Random))
    }

    fun pick(choice: EarChord) {
        if (picked == task.chord || revealed) return
        if (!counted) {
            onResult(choice == task.chord)
            counted = true
        }
        picked = choice
    }

    LaunchedEffect(task) { play() }
    LaunchedEffect(picked) {
        if (picked == task.chord) {
            delay(AUTO_ADVANCE_MILLIS + 500)
            nextTask()
        }
    }

    val solved = picked == task.chord || revealed
    Listen(sound, ::play)
    FeedbackLine(feedbackText(revealed, picked?.label, task.chord.label, picked == task.chord), verdictOf(revealed, picked, task.chord))
    Spacer(Modifier.weight(1f))
    ChoiceGrid(
        labels = pool.map { it.label },
        onPick = { pick(pool[it]) },
        columns = 2,
        enabled = !solved,
        verdictOf = { index ->
            when {
                solved && pool[index] == task.chord -> Verdict.CORRECT
                picked == pool[index] -> Verdict.WRONG
                else -> null
            }
        },
    )
    GiveUp(show = picked != null && !solved) {
        revealed = true
        if (!counted) {
            onResult(false)
            counted = true
        }
    }
    NextRound(show = revealed, onNext = ::nextTask)
}

@Composable
private fun ColumnScope.PitchEar(
    settings: Settings,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
) {
    val notation = settings.notation
    var target by remember { mutableIntStateOf(REFERENCE_MIDI + Random.nextInt(12)) }
    val state = rememberSingleAnswer(target) { target = REFERENCE_MIDI + Random.nextInt(12) }

    fun play() {
        if (settings.earReference) {
            sound.playTimed(listOf(REFERENCE_MIDI to 0.9, target to 1.4))
        } else {
            sound.play(target)
        }
    }

    LaunchedEffect(target) { play() }

    Listen(sound, ::play)
    if (!settings.earReference) {
        Text(
            "Ohne Bezugston ist das absolutes Gehör, und das lässt sich als Erwachsener kaum noch lernen. " +
                "Mit Bezugston übst du den Abstand, der ist lernbar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
    FeedbackLine(
        state.feedback(NoteNames.bothNames(PitchClass.of(target), notation)) { given ->
            given.spelled?.name(notation) ?: NoteNames.bothNames(given.pitch, notation)
        },
        state.feedbackVerdict,
    )
    if (state.showMistakeActions || state.revealed) {
        MistakeActions(
            onSolution = if (state.revealed) null else ({ state.reveal(onResult) }),
            onExplain = { state.explain(onResult) },
        )
    }
    Spacer(Modifier.weight(1f))
    NoteInput(
        notation = notation,
        mode = settings.inputMode,
        hideKeyLabels = settings.hideKeyLabels,
        onModeChange = { mode -> updateSettings { it.copy(inputMode = mode) } },
        state = state.inputState,
        onNote = { entered ->
            state.answer(entered, Answer.judge(entered, PitchClass.of(target), notation), onResult)
        },
        onContinue = { target = REFERENCE_MIDI + Random.nextInt(12) },
        onUnreadable = state::markUnreadable,
    )

    if (state.explaining) {
        ExplanationDialog("Ton erraten", onDismiss = { state.explaining = false }) {
            Text(
                "Gesucht war ${NoteNames.name(PitchClass.of(target), notation, Spelling.SHARP)}. " +
                    "Mit Bezugston hilft der Abstand: vom C aus die Halbtöne nach oben zählen.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private const val REFERENCE_MIDI = 60

private fun feedbackText(revealed: Boolean, picked: String?, answer: String, correct: Boolean): String? = when {
    revealed -> "Lösung: $answer"
    correct -> "Richtig: $answer"
    picked != null -> "Falsch (du: $picked) – nochmal?"
    else -> null
}

private fun <T> verdictOf(revealed: Boolean, picked: T?, answer: T): Verdict? = when {
    revealed || picked == null -> null
    picked == answer -> Verdict.CORRECT
    else -> Verdict.WRONG
}

/** The big replay button: the question is a sound, so it has to be repeatable. */
@Composable
private fun ColumnScope.Listen(sound: Sound, onPlay: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    PlayStopButton(sound, Modifier.fillMaxWidth()) { onPlay() }
}

@Composable
private fun GiveUp(show: Boolean, onGiveUp: () -> Unit) {
    if (!show) return
    OutlinedButton(onClick = onGiveUp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp)) {
        Text("Lösung")
    }
}

@Composable
private fun NextRound(show: Boolean, onNext: () -> Unit) {
    if (!show) return
    OutlinedButton(onClick = onNext, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp)) {
        Text("Weiter")
    }
}
