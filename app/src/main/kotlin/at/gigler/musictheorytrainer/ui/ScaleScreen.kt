package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import at.gigler.musictheorytrainer.theory.Guitar
import at.gigler.musictheorytrainer.theory.Intervals
import at.gigler.musictheorytrainer.theory.Key
import at.gigler.musictheorytrainer.theory.KeyQuiz
import at.gigler.musictheorytrainer.theory.Notation
import at.gigler.musictheorytrainer.theory.PitchClass
import at.gigler.musictheorytrainer.theory.ScaleDrill
import at.gigler.musictheorytrainer.theory.ScaleType
import at.gigler.musictheorytrainer.theory.Spelling
import at.gigler.musictheorytrainer.theory.SpelledNote
import at.gigler.musictheorytrainer.theory.Step
import at.gigler.musictheorytrainer.theory.StepCheck
import at.gigler.musictheorytrainer.theory.Tab
import kotlin.random.Random

private val MIN_ROW_HEIGHT = 26.dp

/** A filled slot, coloured by the first attempt. */
private data class Slot(val note: SpelledNote, val verdict: Verdict)

@Composable
fun ScaleScreen(
    settings: Settings,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
) {
    ExerciseScaffold(Exercise.SCALE, settings, updateSettings, onBack) {
        val notation = settings.notation
        var current by remember(settings.keyChoice, settings.keyOrder) {
            mutableStateOf(KeyQuiz.next(settings.keyChoice, settings.keyOrder, Random))
        }
        val drill = remember(current) { ScaleDrill(current) }
        var slots by remember(current) { mutableStateOf(emptyList<Slot>()) }
        var countedSlot by remember(current) { mutableIntStateOf(-1) }
        var firstVerdict by remember(current) { mutableStateOf<Verdict?>(null) }
        var wrong by remember(current) { mutableStateOf<StepCheck?>(null) }
        var message by remember(current) { mutableStateOf<Pair<String, Verdict?>?>(null) }
        var unreadable by remember(current) { mutableStateOf(false) }
        var explaining by remember(current) { mutableStateOf(false) }
        val rootMidi = remember(current) { comfortableMidi(current.root.pitchClass) }
        val index = slots.size
        val complete = index == drill.targets.size

        /** Only the first attempt per slot counts. The slot is read from state, never captured. */
        fun countFirst(verdict: Verdict) {
            val slot = slots.size
            if (countedSlot != slot) {
                onResult(verdict.isHit)
                countedSlot = slot
                firstVerdict = verdict
            }
        }

        fun fill() {
            val slot = slots.size
            slots = slots + Slot(ScaleDrill(current).targets[slot], firstVerdict ?: Verdict.WRONG)
            sound.play(rootMidi + current.type.offsets[slot + 1])
            wrong = null
            unreadable = false
            firstVerdict = null
        }

        Text(current.name(notation), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 4.dp))
        ScaleSlots(current.root, slots, drill, notation)
        FeedbackLine(message?.first, message?.second)

        if (!complete) {
            if (wrong != null || unreadable) {
                MistakeActions(
                    onSolution = {
                        countFirst(Verdict.WRONG)
                        message = "Lösung: ${drill.targets[slots.size].name(notation)}" to null
                        fill()
                    },
                    onExplain = {
                        countFirst(Verdict.WRONG)
                        explaining = true
                    },
                )
            }
            Spacer(Modifier.weight(1f))
            NoteInput(
                notation = notation,
                mode = settings.inputMode,
                hideKeyLabels = settings.hideKeyLabels,
                onModeChange = { mode -> updateSettings { it.copy(inputMode = mode) } },
                state = InputState.ACCEPTING,
                onNote = { entered ->
                    val check = ScaleDrill(current).check(slots.size, entered.pitch)
                    val judgement = Answer.judge(entered, check.expected.pitchClass, notation, check.expected)
                    val verdict = Verdict.of(judgement.result) ?: return@NoteInput
                    countFirst(verdict)
                    if (verdict.isHit) {
                        message = (
                            if (verdict == Verdict.CORRECT) "Richtig: ${check.expected.name(notation)}"
                            else "Richtig (in ${current.name(notation)}: ${judgement.usualName})"
                            ) to verdict
                        fill()
                    } else {
                        wrong = check
                        message = stepFeedback(check) to Verdict.WRONG
                    }
                },
                onContinue = {},
                onUnreadable = { unreadable = true },
            )
        } else {
            ScaleResult(
                settings = settings,
                drill = drill,
                hits = slots.count { it.verdict.isHit },
                sound = sound,
                onNext = { current = KeyQuiz.next(settings.keyChoice, settings.keyOrder, Random, current) },
            )
        }

        if (explaining) {
            val check = wrong ?: drill.check(index, drill.targets[index].pitchClass)
            ScaleExplanation(current, check, given = wrong?.given, unreadable, notation) { explaining = false }
        }
    }
}

private fun stepFeedback(check: StepCheck): String =
    "${check.fromDegree}→${check.toDegree} ist ein ${check.expectedStep.label}, " +
        "nicht ${Intervals.describe(check.givenSemitones)} – nochmal?"

/** Keeps played notes roughly between G3 and F#4. */
private fun comfortableMidi(pitch: PitchClass): Int = 60 + pitch.semitone - if (pitch.semitone > 6) 12 else 0

private fun ScaleType.pattern(): String = steps.joinToString(" ") { if (it == Step.WHOLE) "G" else "H" }

/** Flat keys get flat names in the explanation strip, sharp keys sharp names. */
private fun spellingOf(notes: List<SpelledNote>) = if (notes.any { it.alteration < 0 }) Spelling.FLAT else Spelling.SHARP

@Composable
private fun ScaleExplanation(
    key: Key,
    check: StepCheck,
    given: PitchClass?,
    unreadable: Boolean,
    notation: Notation,
    onDismiss: () -> Unit,
) {
    val first = check.from.pitchClass
    val step = check.expectedStep.semitones
    ExplanationDialog("${key.name(notation)}: Stufe ${check.fromDegree}→${check.toDegree}", onDismiss) {
        SemitoneStrip(
            first = first,
            notation = notation,
            spelling = spellingOf(key.scale),
            arrows = listOf(StripArrow(0, step, jumpLabel(step))),
            marks = buildMap {
                if (given != null) put(stripIndex(first, given), MarkStyle.WRONG)
                put(0, MarkStyle.ROOT)
                put(step, MarkStyle.CORRECT)
            },
            names = mapOf(0 to check.from.name(notation), step to check.expected.name(notation)),
        )
        Text(
            "${if (key.type == ScaleType.MAJOR) "Dur" else "Moll"}: ${key.type.pattern()} (G = Ganzton, H = Halbton). " +
                "Schritt ${check.fromDegree}→${check.toDegree} ist ein ${check.expectedStep.label}: " +
                "${check.from.name(notation)} → ${check.expected.name(notation)}.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (unreadable) UnreadableAnswerLine()
    }
}

/** Given root plus 7 slots for degrees 2..8, each labelled with the step that leads into it once filled. */
@Composable
private fun ScaleSlots(root: SpelledNote, slots: List<Slot>, drill: ScaleDrill, notation: Notation) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0..7) {
            val slot = if (i == 0) null else slots.getOrNull(i - 1)
            val isCurrent = i == slots.size + 1
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (i == 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            when {
                                i == 0 -> root.name(notation)
                                slot != null -> slot.note.name(notation)
                                else -> ""
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (slot != null) verdictColor(slot.verdict) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    if (slot != null) (if (drill.key.type.steps[i - 1] == Step.WHOLE) "GT" else "HT") else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = verdictColor(slot?.verdict),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ScaleResult(
    settings: Settings,
    drill: ScaleDrill,
    hits: Int,
    sound: Sound,
    onNext: () -> Unit,
) {
    val notation = settings.notation
    val key = drill.key
    var string by remember(drill) {
        mutableIntStateOf(Guitar.lowestRootString(key.root.pitchClass, settings.stringList))
    }
    val positions = Guitar.scaleOnString(key.root.pitchClass, string, key.type)
    val names = drill.scale + drill.root
    // Just root to octave; keep the nut in view when the root sits on fret 0 or 1.
    val firstFret = positions.first().fret.let { if (it <= 1) 0 else it }
    val lastFret = positions.last().fret

    Text(
        "$hits von ${drill.targets.size} beim ersten Versuch richtig",
        style = MaterialTheme.typography.titleMedium,
        color = verdictColor(if (hits == drill.targets.size) Verdict.CORRECT else null),
    )
    Spacer(Modifier.height(8.dp))
    Segmented(Guitar.ALL_STRINGS, string, { Guitar.stringName(it, notation) }, { string = it })
    // The fretboard fills the visible area; the tab below it is reachable by scrolling.
    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
        val rows = lastFret - firstFret + 1
        val rowHeight = ((maxHeight - fretboardHeight(firstFret, lastFret, 0.dp)) / rows).coerceAtLeast(MIN_ROW_HEIGHT)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Fretboard(
                marks = positions.mapIndexed { degree, position ->
                    FretMark(
                        position,
                        names[degree].name(notation),
                        if (degree == 0 || degree == names.lastIndex) MarkStyle.ROOT else MarkStyle.NORMAL,
                    )
                },
                notation = notation,
                modifier = Modifier.fillMaxWidth().height(fretboardHeight(firstFret, lastFret, rowHeight)),
                firstFret = firstFret,
                lastFret = lastFret,
                activeStrings = listOf(string),
            )
            if (settings.showTab) TabText(Tab.renderSequence(positions, notation))
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (sound.enabled) {
            PlayStopButton(sound) { sound.playSequence(positions.map(Guitar::midiAt)) }
        }
        Button(onClick = onNext, modifier = Modifier.weight(1f).height(56.dp)) { Text("Nächste Tonleiter") }
    }
}
