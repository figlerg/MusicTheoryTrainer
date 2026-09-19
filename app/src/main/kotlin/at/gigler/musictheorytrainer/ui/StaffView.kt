package at.gigler.musictheorytrainer.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.theory.NoteValue
import at.gigler.musictheorytrainer.theory.Notation
import at.gigler.musictheorytrainer.theory.RhythmNote
import at.gigler.musictheorytrainer.theory.RhythmQuiz
import at.gigler.musictheorytrainer.theory.StaffNote

/** True if the system font can draw the treble clef symbol. */
private val clefGlyphAvailable: Boolean by lazy { Paint().hasGlyph(TREBLE_CLEF) }
private const val TREBLE_CLEF = "𝄞"

/** Quarter notes without rhythm, which is what the plain reading exercises show. */
fun plainNotes(notes: List<StaffNote>): List<RhythmNote> = notes.map { RhythmNote(it, NoteValue.QUARTER) }

/**
 * One line of treble-clef notation: note heads with stems and flags, rests, answered notes
 * coloured and named below the staff, the current note highlighted. Notes are spaced by their
 * length, so a whole note takes the room of four quarters. The small 8 under the clef marks that
 * guitar music sounds an octave lower than written.
 */
@Composable
internal fun StaffRow(
    events: List<RhythmNote>,
    notation: Notation,
    current: Int?,
    verdicts: List<Verdict?>,
    showNames: Boolean = true,
) {
    if (events.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium
    val feedback = LocalFeedbackColors.current
    val positions = events.mapNotNull { it.note?.staffPosition }
    val low = minOf((positions.minOrNull() ?: 0) - 4, -4)
    val high = maxOf((positions.maxOrNull() ?: 0) + 4, 13)
    val step = 6.dp
    val offsets = RhythmQuiz.beatOffsets(events)
    val total = RhythmQuiz.totalBeats(events).coerceAtLeast(0.001)

    Canvas(Modifier.fillMaxWidth().height(step * (high - low))) {
        val s = step.toPx() * 2
        fun y(position: Double) = ((high - position) * step.toPx()).toFloat()
        fun yOf(position: Int) = y(position.toDouble())
        val clefWidth = 44.dp.toPx()
        val usable = size.width - clefWidth - 8.dp.toPx()
        fun x(i: Int) = clefWidth + ((offsets[i] + events[i].value.beats / 2) / total * usable).toFloat()
        val slot = (usable / total).toFloat()

        current?.let { i ->
            if (i in events.indices) {
                val width = (slot * events[i].value.beats).toFloat().coerceAtLeast(s)
                drawRoundRect(
                    colors.primaryContainer,
                    Offset(x(i) - width / 2, yOf(11)),
                    Size(width, yOf(-2) - yOf(11)),
                    CornerRadius(6.dp.toPx()),
                )
            }
        }
        for (line in listOf(0, 2, 4, 6, 8)) {
            drawLine(colors.onSurfaceVariant, Offset(4.dp.toPx(), yOf(line)), Offset(size.width - 4.dp.toPx(), yOf(line)), 1.2.dp.toPx())
        }
        if (clefGlyphAvailable) {
            drawCentered(measurer, TREBLE_CLEF, Offset(clefWidth / 2, yOf(4) + 0.1f * s), TextStyle(color = colors.onSurface, fontSize = (s * 3.6f).toSp()))
        } else {
            drawCentered(measurer, "G", Offset(clefWidth / 2, yOf(2)), TextStyle(color = colors.onSurface, fontSize = (s * 2f).toSp()))
        }
        drawCentered(measurer, "8", Offset(clefWidth / 2, yOf(-3)), labelStyle.copy(color = colors.onSurface))

        events.forEachIndexed { i, event ->
            val color: Color = when (verdicts.getOrNull(i)) {
                Verdict.CORRECT -> feedback.correct
                Verdict.UNUSUAL -> feedback.unusual
                Verdict.WRONG -> colors.error
                null -> if (i == current) colors.primary else colors.onSurface
            }
            val note = event.note
            if (note == null) {
                drawRest(event.value, x(i), s, color, ::yOf)
            } else {
                for (ledger in note.ledgerLines()) {
                    drawLine(colors.onSurfaceVariant, Offset(x(i) - s * 0.9f, yOf(ledger)), Offset(x(i) + s * 0.9f, yOf(ledger)), 1.2.dp.toPx())
                }
                drawHead(event.value, x(i), yOf(note.staffPosition), s, color)
                drawStem(event.value, x(i), yOf(note.staffPosition), note.staffPosition, s, color)
                if (showNames && verdicts.getOrNull(i) != null) {
                    drawCentered(measurer, note.name(notation), Offset(x(i), yOf(low + 1)), labelStyle.copy(color = color))
                }
            }
        }
    }
}

/** Quarters and eighths are filled, halves and wholes are open rings. */
private fun DrawScope.drawHead(value: NoteValue, x: Float, y: Float, s: Float, color: Color) {
    val width = s * 1.24f
    val height = s * 0.9f
    val topLeft = Offset(x - width / 2, y - height / 2)
    val size = Size(width, height)
    if (value == NoteValue.QUARTER || value == NoteValue.EIGHTH) {
        drawOval(color, topLeft, size)
    } else {
        drawOval(color, topLeft, size, style = Stroke(s * 0.22f))
    }
}

/** Stems go up on the right below the middle line, down on the left above it. Wholes have none. */
private fun DrawScope.drawStem(value: NoteValue, x: Float, y: Float, position: Int, s: Float, color: Color) {
    if (value == NoteValue.WHOLE) return
    val up = position < 4
    val stemX = if (up) x + s * 0.58f else x - s * 0.58f
    val tipY = if (up) y - s * 3.2f else y + s * 3.2f
    val width = s * 0.13f
    drawLine(color, Offset(stemX, y), Offset(stemX, tipY), width)
    if (value != NoteValue.EIGHTH) return
    // A flag hanging off the tip, always curving to the right.
    val flag = Path().apply {
        moveTo(stemX, tipY)
        val direction = if (up) 1f else -1f
        quadraticTo(stemX + s * 1.1f, tipY + direction * s * 0.5f, stemX + s * 0.55f, tipY + direction * s * 1.5f)
        quadraticTo(stemX + s * 0.8f, tipY + direction * s * 0.6f, stemX, tipY + direction * s * 0.7f)
        close()
    }
    drawPath(flag, color)
}

/** Rests sit around the middle of the staff, each with its usual shape. */
private fun DrawScope.drawRest(value: NoteValue, x: Float, s: Float, color: Color, y: (Int) -> Float) {
    when (value) {
        // A whole rest hangs under the second line from the top, a half rest sits on the middle one.
        NoteValue.WHOLE -> drawRect(color, Offset(x - s * 0.55f, y(6)), Size(s * 1.1f, s * 0.36f))
        NoteValue.HALF -> drawRect(color, Offset(x - s * 0.55f, y(4) - s * 0.36f), Size(s * 1.1f, s * 0.36f))
        NoteValue.QUARTER -> {
            val zigzag = Path().apply {
                moveTo(x - s * 0.3f, y(7))
                lineTo(x + s * 0.3f, y(6))
                lineTo(x - s * 0.25f, y(5))
                lineTo(x + s * 0.35f, y(3))
            }
            drawPath(zigzag, color, style = Stroke(s * 0.22f))
        }
        NoteValue.EIGHTH -> {
            drawLine(color, Offset(x + s * 0.3f, y(6)), Offset(x - s * 0.25f, y(3)), s * 0.16f)
            drawCircle(color, s * 0.22f, Offset(x - s * 0.15f, y(6) - s * 0.1f))
        }
    }
}
