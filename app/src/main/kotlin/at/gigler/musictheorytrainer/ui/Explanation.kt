package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.theory.Notation
import at.gigler.musictheorytrainer.theory.NoteNames
import at.gigler.musictheorytrainer.theory.PitchClass
import at.gigler.musictheorytrainer.theory.Spelling
import kotlin.math.abs
import kotlin.math.min

/** A jump between two cells of the strip, drawn as a labelled arc above it. */
data class StripArrow(val from: Int, val to: Int, val label: String)

/**
 * Explanations share one picture: a row of 13 semitone cells (an octave including both ends),
 * shaded like piano keys, with arcs for the jumps and the relevant cells marked.
 * [first] is the pitch of cell 0; [names] may override single cell labels (spelled notes).
 */
@Composable
fun SemitoneStrip(
    first: PitchClass,
    notation: Notation,
    spelling: Spelling,
    arrows: List<StripArrow>,
    marks: Map<Int, MarkStyle>,
    modifier: Modifier = Modifier,
    names: Map<Int, String> = emptyMap(),
) {
    val colors = MaterialTheme.colorScheme
    val feedback = LocalFeedbackColors.current
    val measurer = rememberTextMeasurer()
    val label = MaterialTheme.typography.labelMedium
    val arcRows = if (arrows.isEmpty()) 0 else 1

    Canvas(modifier.fillMaxWidth().height(58.dp + 40.dp * arcRows)) {
        val cells = CELLS
        val cellWidth = size.width / cells
        val cellHeight = 44.dp.toPx()
        val top = size.height - cellHeight - 4.dp.toPx()
        fun centerX(cell: Int) = (cell + 0.5f) * cellWidth

        for (cell in 0 until cells) {
            val pitch = first + cell
            val black = !NoteNames.isNatural(pitch)
            val style = marks[cell]
            val (fill, text) = when (style) {
                MarkStyle.ROOT -> colors.tertiary to colors.onTertiary
                MarkStyle.CORRECT -> feedback.correct to feedback.onCorrect
                MarkStyle.UNUSUAL -> feedback.unusual to feedback.onUnusual
                MarkStyle.WRONG -> colors.error to colors.onError
                MarkStyle.NORMAL, MarkStyle.HINT -> colors.primary to colors.onPrimary
                null -> (if (black) colors.inverseSurface else colors.surfaceContainerHighest) to
                    (if (black) colors.inverseOnSurface else colors.onSurface)
            }
            drawRoundRect(
                fill,
                Offset(cell * cellWidth + 1.dp.toPx(), top),
                Size(cellWidth - 2.dp.toPx(), cellHeight),
                CornerRadius(4.dp.toPx()),
            )
            val name = names[cell] ?: NoteNames.name(pitch, notation, spelling)
            val fontPx = min(cellWidth * if (name.length <= 2) 0.42f else 0.32f, cellHeight * 0.4f)
            drawCentered(
                measurer,
                name,
                Offset(centerX(cell), top + cellHeight / 2),
                TextStyle(color = text, fontSize = fontPx.toSp(), fontWeight = FontWeight.SemiBold),
            )
        }

        val stroke = 2.dp.toPx()
        for (arrow in arrows) {
            val x1 = centerX(arrow.from)
            val x2 = centerX(arrow.to)
            val base = top - 3.dp.toPx()
            val lift = min(30.dp.toPx(), 10.dp.toPx() + abs(x2 - x1) * 0.25f)
            val path = Path().apply {
                moveTo(x1, base)
                quadraticTo((x1 + x2) / 2, base - 2 * lift, x2, base)
            }
            drawPath(path, colors.onSurface, style = Stroke(stroke))
            val dir = if (x2 >= x1) -1f else 1f
            val head = 7.dp.toPx()
            drawLine(colors.onSurface, Offset(x2, base), Offset(x2 + dir * head * 0.6f, base - head), stroke)
            drawLine(colors.onSurface, Offset(x2, base), Offset(x2 + dir * head * 1.1f, base - head * 0.2f), stroke)
            drawCentered(
                measurer,
                arrow.label,
                Offset((x1 + x2) / 2, base - lift - 12.dp.toPx()),
                label.copy(color = colors.onSurface),
            )
        }
    }
}

/** Cells in the strip: 13 so a full octave up or down fits. */
const val CELLS = 13

/** Index of [pitch] in a strip starting at [first]; 12 stands for the octave of [first]. */
fun stripIndex(first: PitchClass, pitch: PitchClass, preferOctave: Boolean = false): Int {
    val i = first.semitonesUpTo(pitch)
    return if (i == 0 && preferOctave) 12 else i
}

fun jumpLabel(semitones: Int): String = when (semitones) {
    1 -> "+1 (Halbton)"
    2 -> "+2 (Ganzton)"
    else -> "+$semitones"
}

fun downLabel(semitones: Int): String = when (semitones) {
    1 -> "−1 (Halbton)"
    2 -> "−2 (Ganzton)"
    else -> "−$semitones"
}

/** "Deine Antwort: ?" when the input was no note at all. */
@Composable
fun UnreadableAnswerLine() {
    Text("Deine Antwort war kein Ton: ?", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}
