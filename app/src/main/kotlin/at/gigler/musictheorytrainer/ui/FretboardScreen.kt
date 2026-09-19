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
import at.gigler.musictheorytrainer.theory.Answer
import at.gigler.musictheorytrainer.theory.FretPosition
import at.gigler.musictheorytrainer.theory.FretboardQuiz
import at.gigler.musictheorytrainer.theory.Guitar
import at.gigler.musictheorytrainer.theory.NoteNames
import at.gigler.musictheorytrainer.theory.Spelling
import at.gigler.musictheorytrainer.theory.Tab
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun FretboardScreen(
    settings: Settings,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
) {
    ExerciseScaffold(Exercise.FRETBOARD, settings, updateSettings, onBack) {
        Segmented(
            listOf(false, true),
            settings.fretboardReverse,
            { if (it) "Stelle finden" else "Ton nennen" },
            { reverse -> updateSettings { it.copy(fretboardReverse = reverse) } },
        )
        Spacer(Modifier.height(8.dp))
        val strings = settings.stringList
        key(strings, settings.fretboardReverse) {
            if (settings.fretboardReverse) {
                FindPositionDrill(settings, strings, sound, onResult)
            } else {
                NameNoteDrill(settings, strings, sound, onResult, updateSettings)
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
    updateSettings: ((Settings) -> Settings) -> Unit,
) {
    val notation = settings.notation
    var position by remember { mutableStateOf(FretboardQuiz.randomPosition(strings, Random)) }
    val answer = Guitar.pitchAt(position)
    val state = rememberSingleAnswer(position) {
        position = FretboardQuiz.randomPosition(strings, Random, position)
    }
    val solved = state.revealed || state.verdict?.isHit == true

    val mark = FretMark(
        position,
        label = if (solved) NoteNames.name(answer, notation, Spelling.SHARP) else "?",
        style = if (state.revealed) MarkStyle.HINT else state.verdict.markStyle(),
    )
    Fretboard(listOf(mark), notation, Modifier.weight(1f).fillMaxWidth(), activeStrings = strings)
    // With the soft keyboard open there is no room to spare.
    if (settings.showTab && !WindowInsets.isImeVisible) {
        TabText(Tab.renderSequence(listOf(position), notation))
    }
    FeedbackLine(
        state.feedback(NoteNames.bothNames(answer, notation)) { given -> given.spelled?.name(notation) ?: NoteNames.bothNames(given.pitch, notation) },
        state.feedbackVerdict,
    )
    if (state.showMistakeActions || state.revealed) {
        MistakeActions(
            onSolution = if (state.revealed) null else ({
                state.reveal(onResult)
                sound.play(Guitar.midiAt(position))
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
            // Read from state: a captured expectation can be stale after moving on.
            val judgement = Answer.judge(entered, Guitar.pitchAt(position), notation)
            state.answer(entered, judgement, onResult)
            if (judgement.result.isHit) sound.play(Guitar.midiAt(position))
        },
        onContinue = { position = FretboardQuiz.randomPosition(strings, Random, position) },
        allowGuitar = false,
        onUnreadable = state::markUnreadable,
    )

    if (state.explaining) {
        val open = Guitar.OPEN_STRINGS[position.string]
        val given = state.given?.pitch?.takeIf { state.verdict == Verdict.WRONG }
        ExplanationDialog("${Guitar.stringName(position.string, notation)}-Saite, Bund ${position.fret}", onDismiss = { state.explaining = false }) {
            SemitoneStrip(
                first = open,
                notation = notation,
                spelling = Spelling.SHARP,
                arrows = if (position.fret == 0) emptyList() else listOf(StripArrow(0, position.fret, "+${position.fret} Bünde")),
                marks = buildMap {
                    if (given != null) put(stripIndex(open, given), MarkStyle.WRONG)
                    put(0, MarkStyle.ROOT)
                    put(position.fret, MarkStyle.CORRECT)
                },
            )
            Text(
                "Leere Saite ${NoteNames.name(open, notation, Spelling.SHARP)}, jeder Bund einen Halbton höher: " +
                    "${position.fret} Bünde = ${NoteNames.bothNames(answer, notation)}.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.unreadable) UnreadableAnswerLine()
        }
    }
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

    // The fretboard keeps the callback it was given, so this must not capture the question:
    // a stale target list would mark correct taps as wrong. Everything here is read from state.
    fun tap(position: FretPosition) {
        val live = Guitar.positionsOf(target, strings)
        if (revealed || found.size == live.size) return
        if (position in live) {
            if (position in found) return
            found = found + position
            sound.play(Guitar.midiAt(position))
            if (found.size == live.size) onResult(wrong.isEmpty())
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
