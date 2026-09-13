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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.audio.Sound
import at.gigler.musictheorytrainer.data.Exercise
import at.gigler.musictheorytrainer.data.Settings
import at.gigler.musictheorytrainer.theory.Answer
import at.gigler.musictheorytrainer.theory.Chord
import at.gigler.musictheorytrainer.theory.ChordType
import at.gigler.musictheorytrainer.theory.EnteredNote
import at.gigler.musictheorytrainer.theory.FretPosition
import at.gigler.musictheorytrainer.theory.Grip
import at.gigler.musictheorytrainer.theory.GripProblem
import at.gigler.musictheorytrainer.theory.Grips
import at.gigler.musictheorytrainer.theory.Guitar
import at.gigler.musictheorytrainer.theory.KeyQuiz
import at.gigler.musictheorytrainer.theory.Notation
import at.gigler.musictheorytrainer.theory.NoteNames
import at.gigler.musictheorytrainer.theory.PitchClass
import at.gigler.musictheorytrainer.theory.Spelling
import at.gigler.musictheorytrainer.theory.SpelledNote
import kotlin.random.Random

@Composable
fun ChordScreen(
    settings: Settings,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(Exercise.CHORDS.title, onBack) {
        Segmented(
            listOf(false, true),
            settings.chordGrip,
            { if (it) "Griff" else "Töne" },
            { grip -> updateSettings { it.copy(chordGrip = grip) } },
        )
        var current by remember(settings.keyChoice, settings.keyOrder) {
            mutableStateOf(KeyQuiz.next(settings.keyChoice, settings.keyOrder, Random))
        }
        val chord = remember(current) { Chord.of(current) }
        val next = { current = KeyQuiz.next(settings.keyChoice, settings.keyOrder, Random, current) }
        Text(
            chord.label(settings.notation),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        key(chord, settings.chordGrip, settings.chordRootGiven) {
            if (settings.chordGrip) {
                GripDrill(settings, chord, sound, onResult, next)
            } else {
                ToneDrill(settings, chord, sound, onResult, updateSettings, next)
            }
        }
    }
}

private fun toneSpelling(chord: Chord) = if (chord.tones.any { it.alteration < 0 }) Spelling.FLAT else Spelling.SHARP

private fun chordMidi(chord: Chord, tone: Int): Int {
    val root = chord.root.pitchClass.semitone
    return 60 + root - (if (root > 6) 12 else 0) + chord.type.semitones[tone]
}

/** Enter the chord tones in any order; with the fretboard at most one note per string. */
@Composable
private fun ColumnScope.ToneDrill(
    settings: Settings,
    chord: Chord,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    onNext: () -> Unit,
) {
    val notation = settings.notation
    val rootGiven = settings.chordRootGiven
    val targets = if (rootGiven) chord.tones.drop(1) else chord.tones
    var typed by remember { mutableStateOf(mapOf<SpelledNote, Verdict>()) }
    var picks by remember { mutableStateOf(mapOf<Int, FretPosition>()) }
    var wrongPick by remember { mutableStateOf<FretPosition?>(null) }
    var lastWrong by remember { mutableStateOf<EnteredNote?>(null) }
    var message by remember { mutableStateOf<Pair<String, Verdict?>?>(null) }
    var mistakes by remember { mutableIntStateOf(0) }
    var unreadable by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    var explaining by remember { mutableStateOf(false) }
    var recorded by remember { mutableStateOf(false) }

    val found: Map<SpelledNote, Verdict> = typed + picks.values.mapNotNull { p ->
        targets.firstOrNull { it.pitchClass == Guitar.pitchAt(p) }?.let { it to Verdict.CORRECT }
    }
    val complete = targets.all { it in found }
    val solved = complete || revealed

    fun record(hit: Boolean) {
        if (!recorded) {
            onResult(hit)
            recorded = true
        }
    }

    LaunchedEffect(complete) {
        if (complete && !revealed) {
            record(mistakes == 0)
            sound.strum(Grips.standard(chord).sounding.map(Guitar::midiAt))
        }
    }

    fun enter(entered: EnteredNote) {
        val tone = targets.firstOrNull { it.pitchClass == entered.pitch }
        val position = entered.position
        unreadable = false
        when {
            tone == null && rootGiven && entered.pitch == chord.root.pitchClass ->
                message = "Der Grundton ist vorgegeben" to null
            tone == null -> {
                mistakes++
                lastWrong = entered
                if (position != null) {
                    picks = picks - position.string
                    wrongPick = position
                }
                val name = entered.spelled?.name(notation) ?: NoteNames.name(entered.pitch, notation, toneSpelling(chord))
                message = "$name gehört nicht zu ${chord.symbol(notation)} – nochmal?" to Verdict.WRONG
            }
            else -> {
                val judgement = Answer.judge(entered, tone.pitchClass, notation, tone)
                val verdict = Verdict.of(judgement.result) ?: return
                val already = tone in found
                if (position != null) {
                    picks = picks + (position.string to position)
                    wrongPick = null
                } else if (!already) {
                    typed = typed + (tone to verdict)
                }
                lastWrong = null
                sound.play(chordMidi(chord, chord.tones.indexOf(tone)))
                message = when {
                    already && position == null -> "${tone.name(notation)} hast du schon" to null
                    verdict == Verdict.UNUSUAL -> "Richtig (in ${chord.symbol(notation)}: ${judgement.usualName})" to verdict
                    else -> "Richtig: ${tone.name(notation)}" to verdict
                }
            }
        }
    }

    ToneSlots(chord, rootGiven, found, revealed, notation)
    FeedbackLine(
        when {
            complete && !revealed -> "Richtig: ${chord.tones.joinToString(" ") { it.name(notation) }}"
            revealed -> "Lösung: ${chord.tones.joinToString(" ") { it.name(notation) }}"
            else -> message?.first
        },
        when {
            complete && !revealed -> if (mistakes == 0) Verdict.CORRECT else Verdict.UNUSUAL
            revealed -> null
            else -> message?.second
        },
    )
    if (!solved && (lastWrong != null || unreadable)) {
        MistakeActions(
            onSolution = {
                record(false)
                revealed = true
            },
            onExplain = {
                if (mistakes == 0) record(false)
                explaining = true
            },
        )
    }

    if (!solved) {
        Spacer(Modifier.weight(1f))
        NoteInput(
            notation = notation,
            mode = settings.inputMode,
            onModeChange = { mode -> updateSettings { it.copy(inputMode = mode) } },
            state = InputState.ACCEPTING,
            onNote = ::enter,
            onContinue = {},
            guitarMarks = buildList {
                picks.values.forEach { p ->
                    val tone = targets.first { it.pitchClass == Guitar.pitchAt(p) }
                    add(FretMark(p, tone.name(notation), MarkStyle.CORRECT))
                }
                wrongPick?.let { add(FretMark(it, NoteNames.name(Guitar.pitchAt(it), notation, toneSpelling(chord)), MarkStyle.WRONG)) }
            },
            onUnreadable = { unreadable = true },
        )
    } else {
        StandardGripView(chord, notation, Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { explaining = true }, modifier = Modifier.height(56.dp)) { Text("Erklärung") }
            Button(onClick = onNext, modifier = Modifier.weight(1f).height(56.dp)) { Text("Nächster Akkord") }
        }
    }

    if (explaining) {
        ChordExplanation(chord, lastWrong?.pitch, unreadable, notation) { explaining = false }
    }
}

/** Grundton, Terz, Quinte as boxes; the root counts as found when it is given. */
@Composable
private fun ToneSlots(
    chord: Chord,
    rootGiven: Boolean,
    found: Map<SpelledNote, Verdict>,
    revealed: Boolean,
    notation: Notation,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chord.tones.forEachIndexed { i, tone ->
            val given = rootGiven && i == 0
            val verdict = found[tone]
            val shown = given || verdict != null || revealed
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (given) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (!shown) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (shown) tone.name(notation) else "",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (verdict != null) verdictColor(verdict) else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Text(Chord.TONE_NAMES[i], style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ChordExplanation(chord: Chord, given: PitchClass?, unreadable: Boolean, notation: Notation, onDismiss: () -> Unit) {
    val first = chord.root.pitchClass
    val third = chord.type.semitones[1]
    val thirdName = if (third == 4) "große Terz" else "kleine Terz"
    ExplanationDialog(chord.label(notation), onDismiss) {
        SemitoneStrip(
            first = first,
            notation = notation,
            spelling = toneSpelling(chord),
            arrows = listOf(StripArrow(0, third, "+$third"), StripArrow(third, 7, "+${7 - third}")),
            marks = buildMap {
                if (given != null) put(stripIndex(first, given), MarkStyle.WRONG)
                put(0, MarkStyle.ROOT)
                put(third, MarkStyle.CORRECT)
                put(7, MarkStyle.CORRECT)
            },
            names = mapOf(0 to chord.tones[0].name(notation), third to chord.tones[1].name(notation), 7 to chord.tones[2].name(notation)),
        )
        Text(
            "${if (chord.type == ChordType.MAJOR) "Dur" else "Moll"}: Grundton + $thirdName ($third Halbtöne) + " +
                "${7 - third} Halbtöne. Die Quinte liegt immer 7 Halbtöne über dem Grundton.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (unreadable) UnreadableAnswerLine()
    }
}

/** The songbook grip for comparison, as a fretboard section plus shorthand. */
@Composable
private fun StandardGripView(chord: Chord, notation: Notation, modifier: Modifier = Modifier) {
    val grip = Grips.standard(chord)
    Column(modifier.fillMaxWidth()) {
        Text(
            "Standardgriff: ${grip.shorthand()}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Fretboard(
            marks = grip.sounding.map { FretMark(it, toneName(chord, Guitar.pitchAt(it), notation), gripStyle(chord, it)) },
            notation = notation,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            lastFret = maxOf(5, (grip.sounding.maxOfOrNull { it.fret } ?: 0) + 1),
            mutedStrings = grip.frets.indices.filter { grip.frets[it] == null }.toSet(),
        )
    }
}

private fun toneName(chord: Chord, pitch: PitchClass, notation: Notation): String =
    chord.tones.firstOrNull { it.pitchClass == pitch }?.name(notation) ?: NoteNames.name(pitch, notation, toneSpelling(chord))

private fun gripStyle(chord: Chord, position: FretPosition): MarkStyle =
    if (Guitar.pitchAt(position) == chord.root.pitchClass) MarkStyle.ROOT else MarkStyle.NORMAL

/**
 * Build a playable grip: tap above the nut to switch a string between muted (×) and open,
 * tap a fret to press it (again to release). "Prüfen" judges by the rules in [Grips.check].
 */
@Composable
private fun ColumnScope.GripDrill(
    settings: Settings,
    chord: Chord,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val notation = settings.notation
    var frets by remember { mutableStateOf(List<Int?>(Guitar.STRING_COUNT) { null }) }
    var problems by remember { mutableStateOf<List<GripProblem>?>(null) }
    var showStandard by remember { mutableStateOf(false) }
    var recorded by remember { mutableStateOf(false) }
    var solved by remember { mutableStateOf(false) }
    val grip = Grip(frets)

    fun record(hit: Boolean) {
        if (!recorded) {
            onResult(hit)
            recorded = true
        }
    }

    fun tap(position: FretPosition) {
        if (solved) return
        frets = frets.toMutableList().also { list ->
            val old = list[position.string]
            list[position.string] = when {
                position.fret == 0 -> if (old == 0) null else 0
                old == position.fret -> null
                else -> position.fret
            }
        }
        problems = null
        frets[position.string]?.let { sound.play(Guitar.midiAt(FretPosition(position.string, it))) }
    }

    val checked = problems
    val marks = buildList {
        if (showStandard) {
            Grips.standard(chord).sounding.filter { it !in grip.sounding }.forEach { add(FretMark(it, toneName(chord, Guitar.pitchAt(it), notation), MarkStyle.HINT)) }
        }
        grip.sounding.forEach { p ->
            val style = when {
                checked == null -> MarkStyle.NORMAL
                Guitar.pitchAt(p) in chord.pitches -> MarkStyle.CORRECT
                else -> MarkStyle.WRONG
            }
            add(FretMark(p, toneName(chord, Guitar.pitchAt(p), notation), style))
        }
    }
    Fretboard(
        marks = marks,
        notation = notation,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        mutedStrings = frets.indices.filter { frets[it] == null }.toSet(),
        onTap = ::tap,
    )
    Text(
        when {
            checked == null && showStandard -> "Standardgriff: ${Grips.standard(chord).shorthand()}"
            checked == null -> "Über dem Sattel: stumm ×/leer, im Bund: greifen"
            checked.isEmpty() -> "Richtig! ${grip.shorthand()}" + if (showStandard) "  ·  Standard: ${Grips.standard(chord).shorthand()}" else ""
            else -> checked.joinToString("\n") { problemText(it, chord, notation) }
        },
        color = verdictColor(if (checked == null) null else if (checked.isEmpty()) Verdict.CORRECT else Verdict.WRONG),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
    )
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                if (!showStandard && !solved) record(false)
                showStandard = !showStandard
            },
            modifier = Modifier.height(56.dp),
        ) { Text(if (showStandard) "Ausblenden" else "Standardgriff") }
        if (solved) {
            Button(onClick = onNext, modifier = Modifier.weight(1f).height(56.dp)) { Text("Nächster Akkord") }
        } else {
            Button(
                onClick = {
                    val result = Grips.check(grip, chord, settings.gripRootInBass)
                    problems = result
                    record(result.isEmpty())
                    if (result.isEmpty()) {
                        solved = true
                        sound.strum(grip.sounding.map(Guitar::midiAt))
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp),
            ) { Text("Prüfen") }
        }
    }
}

private fun problemText(problem: GripProblem, chord: Chord, notation: Notation): String = when (problem) {
    is GripProblem.ForeignNote ->
        "${NoteNames.name(Guitar.pitchAt(problem.position), notation, toneSpelling(chord))} " +
            "(${Guitar.stringName(problem.position.string, notation)}-Saite) gehört nicht dazu"
    is GripProblem.MissingTone -> "${problem.tone.name(notation)} fehlt"
    is GripProblem.TooWide -> "Zu weit gegriffen: ${problem.frets} Bünde (höchstens ${Grips.MAX_SPAN})"
    is GripProblem.RootNotInBass ->
        "Tiefster Ton ist ${toneName(chord, problem.lowest, notation)}, nicht der Grundton ${chord.root.name(notation)}"
}
