package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import at.gigler.musictheorytrainer.theory.Guitar
import at.gigler.musictheorytrainer.theory.Intervals
import at.gigler.musictheorytrainer.theory.MajorScale
import at.gigler.musictheorytrainer.theory.Notation
import at.gigler.musictheorytrainer.theory.PitchClass
import at.gigler.musictheorytrainer.theory.ScaleDrill
import at.gigler.musictheorytrainer.theory.SpelledNote
import at.gigler.musictheorytrainer.theory.Step
import at.gigler.musictheorytrainer.theory.StepCheck
import at.gigler.musictheorytrainer.theory.Tab

private val RESULT_ROW_HEIGHT = 34.dp

@Composable
fun ScaleScreen(settings: Settings, sound: Sound, onResult: (Boolean) -> Unit, onBack: () -> Unit) {
    ScreenScaffold(Exercise.SCALE.title, onBack) {
        val notation = settings.notation
        var root by remember { mutableStateOf(MajorScale.ROOTS.random()) }
        val drill = remember(root) { ScaleDrill(root) }
        var checks by remember(root) { mutableStateOf(emptyList<StepCheck>()) }
        val rootMidi = remember(root) { comfortableMidi(root.pitchClass) }

        Text(
            "${root.name(notation)}-Dur",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        ScaleSlots(root, checks, notation)
        val last = checks.lastOrNull()
        FeedbackLine(
            text = last?.let { feedbackText(it, notation) },
            verdict = last?.let { if (it.correct) Verdict.CORRECT else Verdict.WRONG },
        )

        if (checks.size < drill.targets.size) {
            Spacer(Modifier.weight(1f))
            NoteInput(
                notation = notation,
                defaultMode = settings.inputMode,
                state = InputState.ACCEPTING,
                onNote = { pitch ->
                    val check = drill.check(checks.size, pitch)
                    checks = checks + check
                    sound.play(rootMidi + MajorScale.OFFSETS[check.toDegree - 1])
                    onResult(check.correct)
                },
                onContinue = {},
            )
        } else {
            ScaleResult(
                settings = settings,
                drill = drill,
                correctCount = checks.count { it.correct },
                sound = sound,
                onNext = { root = MajorScale.ROOTS.filter { it != root }.random() },
            )
        }
    }
}

private fun feedbackText(check: StepCheck, notation: Notation): String {
    val expected = check.expected.name(notation)
    if (check.correct) return "Richtig: $expected"
    return "${check.fromDegree}→${check.toDegree} ist ein ${check.expectedStep.label}, " +
        "nicht ${Intervals.describe(check.givenSemitones)}. Richtig: $expected"
}

/** Keeps played notes roughly between G3 and F#4. */
private fun comfortableMidi(pitch: PitchClass): Int = 60 + pitch.semitone - if (pitch.semitone > 6) 12 else 0

/** Given root plus 7 slots for degrees 2..8, each labelled with the step that leads into it once answered. */
@Composable
private fun ScaleSlots(root: SpelledNote, checks: List<StepCheck>, notation: Notation) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (slot in 0..7) {
            val check = if (slot == 0) null else checks.getOrNull(slot - 1)
            val verdict = check?.let { if (it.correct) Verdict.CORRECT else Verdict.WRONG }
            val isCurrent = slot == checks.size + 1
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (slot == 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            when {
                                slot == 0 -> root.name(notation)
                                check != null -> check.expected.name(notation)
                                else -> ""
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (verdict != null) verdictColor(verdict) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    check?.let { if (it.expectedStep == Step.WHOLE) "GT" else "HT" } ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = verdictColor(verdict),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ScaleResult(
    settings: Settings,
    drill: ScaleDrill,
    correctCount: Int,
    sound: Sound,
    onNext: () -> Unit,
) {
    val notation = settings.notation
    var string by remember(drill) {
        mutableIntStateOf(Guitar.lowestRootString(drill.root.pitchClass, settings.stringSet.strings))
    }
    val positions = Guitar.majorScaleOnString(drill.root.pitchClass, string)
    val names = drill.scale + drill.root
    val lastFret = maxOf(Guitar.MAX_FRET, positions.last().fret)

    Text(
        "$correctCount von ${drill.targets.size} richtig",
        style = MaterialTheme.typography.titleMedium,
        color = verdictColor(if (correctCount == drill.targets.size) Verdict.CORRECT else null),
    )
    Spacer(Modifier.height(8.dp))
    Segmented(Guitar.ALL_STRINGS, string, { Guitar.stringName(it, notation) }, { string = it })
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Fretboard(
            marks = positions.mapIndexed { degree, position ->
                FretMark(
                    position,
                    names[degree].name(notation),
                    if (degree == 0 || degree == names.lastIndex) MarkStyle.ROOT else MarkStyle.NORMAL,
                )
            },
            notation = notation,
            modifier = Modifier.fillMaxWidth().height(fretboardHeight(lastFret, RESULT_ROW_HEIGHT)),
            lastFret = lastFret,
            activeStrings = listOf(string),
        )
        if (settings.showTab) TabText(Tab.renderSequence(positions, notation))
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (sound.enabled) {
            OutlinedButton(
                onClick = { sound.playSequence(positions.map(Guitar::midiAt)) },
                modifier = Modifier.height(56.dp),
            ) {
                Icon(PlayIcon, contentDescription = "Anhören")
            }
        }
        Button(onClick = onNext, modifier = Modifier.weight(1f).height(56.dp)) { Text("Nächste Tonleiter") }
    }
}
