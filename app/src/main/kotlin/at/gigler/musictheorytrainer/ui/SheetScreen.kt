package at.gigler.musictheorytrainer.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.audio.Sound
import at.gigler.musictheorytrainer.data.Exercise
import at.gigler.musictheorytrainer.data.Settings
import at.gigler.musictheorytrainer.data.SheetSource
import at.gigler.musictheorytrainer.theory.Answer
import at.gigler.musictheorytrainer.theory.EnteredNote
import at.gigler.musictheorytrainer.theory.FretPosition
import at.gigler.musictheorytrainer.theory.Guitar
import at.gigler.musictheorytrainer.theory.Melody
import at.gigler.musictheorytrainer.theory.Notation
import at.gigler.musictheorytrainer.theory.NoteNames
import at.gigler.musictheorytrainer.theory.SheetQuiz
import at.gigler.musictheorytrainer.theory.StaffNote
import at.gigler.musictheorytrainer.theory.StaffRange
import kotlin.random.Random

private data class SheetLine(val notes: List<StaffNote>, val melody: Melody?)

private fun newLine(source: SheetSource, range: StaffRange, previous: SheetLine?): SheetLine = when (source) {
    SheetSource.MELODIES -> SheetQuiz.nextMelody(Random, previous?.melody).let { SheetLine(it.notes, it) }
    SheetSource.RANDOM -> SheetLine(SheetQuiz.randomLine(range, Random), null)
}

@Composable
fun SheetScreen(
    settings: Settings,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(Exercise.SHEET.title, onBack) {
        val notation = settings.notation
        Segmented(
            SheetSource.entries,
            settings.sheetSource,
            { if (it == SheetSource.MELODIES) "Melodien" else "Zufall" },
            { source -> updateSettings { it.copy(sheetSource = source) } },
        )
        var line by remember(settings.sheetSource, settings.sheetRange) {
            mutableStateOf(newLine(settings.sheetSource, settings.sheetRange, null))
        }
        var results by remember(line) { mutableStateOf(emptyList<Verdict>()) }
        var counted by remember(line) { mutableIntStateOf(-1) }
        var firstVerdict by remember(line) { mutableStateOf<Verdict?>(null) }
        var wrong by remember(line) { mutableStateOf<EnteredNote?>(null) }
        var message by remember(line) { mutableStateOf<Pair<String, Verdict?>?>(null) }
        var unreadable by remember(line) { mutableStateOf(false) }
        var explaining by remember(line) { mutableStateOf(false) }
        val index = results.size
        val complete = index == line.notes.size

        fun countFirst(verdict: Verdict) {
            if (counted != index) {
                onResult(verdict.isHit)
                counted = index
                firstVerdict = verdict
            }
        }

        fun advance() {
            results = results + (firstVerdict ?: Verdict.WRONG)
            firstVerdict = null
            wrong = null
            unreadable = false
        }

        val scroll = rememberScrollState()
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)) {
            line.notes.chunked(SheetQuiz.LINE_LENGTH).forEachIndexed { row, chunk ->
                val offset = row * SheetQuiz.LINE_LENGTH
                StaffRow(
                    notes = chunk,
                    notation = notation,
                    current = (index - offset).takeIf { it in chunk.indices },
                    verdicts = results.drop(offset).take(chunk.size),
                )
            }
        }
        LaunchedEffect(index) {
            // Keep the row with the current note in view.
            val rows = (line.notes.size + SheetQuiz.LINE_LENGTH - 1) / SheetQuiz.LINE_LENGTH
            if (rows > 1) scroll.animateScrollTo(scroll.maxValue * (index / SheetQuiz.LINE_LENGTH) / (rows - 1).coerceAtLeast(1))
        }
        FeedbackLine(message?.first, message?.second)

        if (!complete) {
            val note = line.notes[index]
            if (wrong != null || unreadable) {
                MistakeActions(
                    onSolution = {
                        countFirst(Verdict.WRONG)
                        message = "Lösung: ${note.name(notation)}" to null
                        sound.play(note.guitarMidi)
                        advance()
                    },
                    onExplain = {
                        countFirst(Verdict.WRONG)
                        explaining = true
                    },
                )
            }
            NoteInput(
                notation = notation,
                mode = settings.inputMode,
                onModeChange = { mode -> updateSettings { it.copy(inputMode = mode) } },
                state = InputState.ACCEPTING,
                onNote = { entered ->
                    val judgement = Answer.judge(entered, note.pitchClass, notation)
                    var verdict = Verdict.of(judgement.result) ?: return@NoteInput
                    val position = entered.position
                    val wrongOctave = settings.sheetExactOctave && position != null && verdict.isHit &&
                        Guitar.midiAt(position) != note.guitarMidi
                    if (wrongOctave) verdict = Verdict.WRONG
                    countFirst(verdict)
                    if (verdict.isHit) {
                        message = (
                            if (verdict == Verdict.CORRECT) "Richtig: ${note.name(notation)}"
                            else "Richtig (üblich: ${judgement.usualName})"
                            ) to verdict
                        sound.play(note.guitarMidi)
                        advance()
                    } else {
                        wrong = entered
                        message = (
                            if (wrongOctave) "Richtiger Ton, falsche Oktave – nochmal?"
                            else "Falsch (du: ${entered.spelled?.name(notation) ?: NoteNames.bothNames(entered.pitch, notation)}) – nochmal?"
                            ) to Verdict.WRONG
                    }
                },
                onContinue = {},
                onUnreadable = { unreadable = true },
            )
            if (explaining) {
                SheetExplanation(note, wrong, unreadable, notation) { explaining = false }
            }
        } else {
            val hits = results.count { it.isHit }
            Text(
                "$hits von ${line.notes.size} beim ersten Versuch richtig" + (line.melody?.let { " · ${it.title}" } ?: ""),
                style = MaterialTheme.typography.titleMedium,
                color = verdictColor(if (hits == line.notes.size) Verdict.CORRECT else null),
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sound.enabled) {
                    OutlinedButton(onClick = { sound.playSequence(line.notes.map { it.guitarMidi }) }, modifier = Modifier.height(56.dp)) {
                        Icon(PlayIcon, contentDescription = "Anhören")
                    }
                }
                Button(
                    onClick = { line = newLine(settings.sheetSource, settings.sheetRange, line) },
                    modifier = Modifier.weight(1f).height(56.dp),
                ) { Text(if (line.melody != null) "Nächste Melodie" else "Nächste Zeile") }
            }
        }
    }
}

/** Where a note sits, in words, e.g. "auf der 2. Linie". */
private fun placeOf(note: StaffNote): String {
    val p = note.staffPosition
    val ledgers = note.ledgerLines().size
    return when {
        p in 0..8 && p % 2 == 0 -> "auf der ${p / 2 + 1}. Linie (von unten)"
        p in 0..8 -> "im ${(p - 1) / 2 + 1}. Zwischenraum (von unten)"
        p == -1 -> "direkt unter dem System"
        p == 9 -> "direkt über dem System"
        p < 0 -> "unter dem System, $ledgers Hilfslinie${if (ledgers == 1) "" else "n"}"
        else -> "über dem System, $ledgers Hilfslinie${if (ledgers == 1) "" else "n"}"
    }
}

@Composable
private fun SheetExplanation(note: StaffNote, wrong: EnteredNote?, unreadable: Boolean, notation: Notation, onDismiss: () -> Unit) {
    val lines = listOf(0, 2, 4, 6, 8).joinToString(" ") { StaffNote.fromPosition(it).name(notation) }
    val spaces = listOf(1, 3, 5, 7).joinToString(" ") { StaffNote.fromPosition(it).name(notation) }
    val places = Guitar.ALL_STRINGS.flatMap { s -> (0..GUITAR_KEYS_LAST_FRET).map { FretPosition(s, it) } }
        .filter { Guitar.midiAt(it) == note.guitarMidi }
    ExplanationDialog("Note: ${note.name(notation)}", onDismiss) {
        StaffRow(listOf(note), notation, current = null, verdicts = listOf(Verdict.CORRECT))
        Text("Die Note steht ${placeOf(note)}.", style = MaterialTheme.typography.bodyMedium)
        Text("Linien von unten: $lines\nZwischenräume von unten: $spaces", style = MaterialTheme.typography.bodyMedium)
        if (places.isNotEmpty()) {
            Text(
                "Auf der Gitarre (klingt eine Oktave tiefer als notiert): " +
                    places.joinToString(" oder ") { "${Guitar.stringName(it.string, notation)}-Saite ${if (it.fret == 0) "leer" else "Bund ${it.fret}"}" },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (wrong != null) {
            Text(
                "Deine Antwort: ${wrong.spelled?.name(notation) ?: NoteNames.bothNames(wrong.pitch, notation)}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (unreadable) UnreadableAnswerLine()
    }
}

/** True if the system font can draw the treble clef symbol. */
private val clefGlyphAvailable: Boolean by lazy { Paint().hasGlyph(TREBLE_CLEF) }
private const val TREBLE_CLEF = "𝄞"

/**
 * One line of treble-clef notation without rhythm: note heads only, answered notes coloured and
 * named below the staff, the current note highlighted. The small 8 under the clef marks that
 * guitar music sounds an octave lower.
 */
@Composable
private fun StaffRow(notes: List<StaffNote>, notation: Notation, current: Int?, verdicts: List<Verdict>) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium
    val feedback = LocalFeedbackColors.current
    val low = minOf(notes.minOf { it.staffPosition } - 4, -4)
    val high = maxOf(notes.maxOf { it.staffPosition } + 2, 11)
    val step = 6.dp

    Canvas(Modifier.fillMaxWidth().height(step * (high - low))) {
        val s = step.toPx() * 2
        fun y(position: Int) = (high - position) * step.toPx()
        val clefWidth = 44.dp.toPx()
        val slot = (size.width - clefWidth - 8.dp.toPx()) / SheetQuiz.LINE_LENGTH
        fun x(i: Int) = clefWidth + (i + 0.5f) * slot

        current?.let { i ->
            drawRoundRect(
                colors.primaryContainer,
                Offset(x(i) - slot * 0.45f, y(10)),
                Size(slot * 0.9f, y(-2) - y(10)),
                CornerRadius(6.dp.toPx()),
            )
        }
        for (line in listOf(0, 2, 4, 6, 8)) {
            drawLine(colors.onSurfaceVariant, Offset(4.dp.toPx(), y(line)), Offset(size.width - 4.dp.toPx(), y(line)), 1.2.dp.toPx())
        }
        if (clefGlyphAvailable) {
            drawCentered(measurer, TREBLE_CLEF, Offset(clefWidth / 2, y(4) + 0.1f * s), TextStyle(color = colors.onSurface, fontSize = (s * 3.6f).toSp()))
        } else {
            drawCentered(measurer, "G", Offset(clefWidth / 2, y(2)), TextStyle(color = colors.onSurface, fontSize = (s * 2f).toSp()))
        }
        drawCentered(measurer, "8", Offset(clefWidth / 2, y(-3)), labelStyle.copy(color = colors.onSurface))

        notes.forEachIndexed { i, note ->
            val p = note.staffPosition
            val color: Color = when (verdicts.getOrNull(i)) {
                Verdict.CORRECT -> feedback.correct
                Verdict.UNUSUAL -> feedback.unusual
                Verdict.WRONG -> colors.error
                null -> if (i == current) colors.primary else colors.onSurface
            }
            for (ledger in note.ledgerLines()) {
                drawLine(colors.onSurfaceVariant, Offset(x(i) - s * 0.9f, y(ledger)), Offset(x(i) + s * 0.9f, y(ledger)), 1.2.dp.toPx())
            }
            drawOval(color, Offset(x(i) - s * 0.62f, y(p) - s * 0.45f), Size(s * 1.24f, s * 0.9f))
            if (i < verdicts.size) {
                drawCentered(measurer, note.name(notation), Offset(x(i), y(low + 1)), labelStyle.copy(color = color))
            }
        }
    }
}
