package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import at.gigler.musictheorytrainer.theory.FretPosition
import at.gigler.musictheorytrainer.theory.Guitar
import at.gigler.musictheorytrainer.theory.Notation
import kotlin.math.min

/** First position: open strings plus frets 1..5, which holds every note two or three times. */
const val GUITAR_KEYS_LAST_FRET = 5

private val ROW_HEIGHT = 28.dp
private val NUMBER_ROW = 16.dp
val GUITAR_KEYS_HEIGHT = ROW_HEIGHT * Guitar.STRING_COUNT + NUMBER_ROW

/**
 * A horizontal fretboard used as input, laid out like tab: high e on top, low E at the bottom,
 * open strings left of the nut. [lastFret] allows a longer neck later without changing callers.
 */
@Composable
fun GuitarKeys(
    notation: Notation,
    enabled: Boolean,
    marks: List<FretMark>,
    onTap: (FretPosition) -> Unit,
    modifier: Modifier = Modifier,
    lastFret: Int = GUITAR_KEYS_LAST_FRET,
) {
    val colors = MaterialTheme.colorScheme
    val feedback = LocalFeedbackColors.current
    val measurer = rememberTextMeasurer()
    val small = MaterialTheme.typography.labelMedium
    val currentOnTap by rememberUpdatedState(onTap)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(GUITAR_KEYS_HEIGHT)
            .alpha(if (enabled) 1f else 0.5f)
            .semantics { contentDescription = "Gitarrengriffbrett" }
            .pointerInput(enabled, lastFret) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    StripGeometry(size.toSize(), lastFret, this).hit(offset)?.let { currentOnTap(it) }
                }
            },
    ) {
        val g = StripGeometry(size, lastFret, this)
        drawRect(colors.surfaceContainerHigh, Offset(g.nutX, 0f), Size(g.right - g.nutX, g.stringsHeight))
        for (fret in 1..lastFret) {
            val x = g.nutX + fret * g.unit
            drawLine(colors.outline, Offset(x, 0f), Offset(x, g.stringsHeight), 1.5.dp.toPx())
            drawCentered(measurer, fret.toString(), Offset(g.columnCenter(fret), g.stringsHeight + g.numberRow / 2), small.copy(color = colors.onSurfaceVariant))
        }
        for (fret in listOf(3, 5, 7, 9).filter { it <= lastFret }) {
            drawCircle(colors.outlineVariant, min(g.unit, g.rowHeight) * 0.13f, Offset(g.columnCenter(fret), 3 * g.rowHeight))
        }
        drawLine(colors.onSurface, Offset(g.nutX, 0f), Offset(g.nutX, g.stringsHeight), 5.dp.toPx())
        for (string in 0 until Guitar.STRING_COUNT) {
            val y = g.rowCenter(string)
            drawLine(colors.onSurfaceVariant, Offset(g.left, y), Offset(g.right, y), (2.6f - string * 0.3f).dp.toPx())
            drawCentered(
                measurer,
                Guitar.stringName(string, notation),
                Offset(g.left / 2, y),
                small.copy(color = colors.onSurface, fontWeight = FontWeight.Bold),
            )
        }
        val radius = min(g.unit * 0.7f, g.rowHeight) * 0.46f
        for (mark in marks) {
            if (mark.position.fret > lastFret) continue
            val (fill, content) = when (mark.style) {
                MarkStyle.NORMAL -> colors.primary to colors.onPrimary
                MarkStyle.ROOT -> colors.tertiary to colors.onTertiary
                MarkStyle.CORRECT -> feedback.correct to feedback.onCorrect
                MarkStyle.UNUSUAL -> feedback.unusual to feedback.onUnusual
                MarkStyle.WRONG -> colors.error to colors.onError
                MarkStyle.HINT -> colors.secondaryContainer to colors.onSecondaryContainer
            }
            val center = Offset(g.columnCenter(mark.position.fret), g.rowCenter(mark.position.string))
            drawCircle(fill, radius, center)
            drawCentered(
                measurer,
                mark.label,
                center,
                TextStyle(color = content, fontSize = (radius * if (mark.label.length <= 2) 0.9f else 0.7f).toSp(), fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

private class StripGeometry(size: Size, private val lastFret: Int, density: Density) {
    val left = with(density) { 22.dp.toPx() }
    val right = size.width - with(density) { 4.dp.toPx() }
    val numberRow = with(density) { NUMBER_ROW.toPx() }
    val stringsHeight = size.height - numberRow
    val rowHeight = stringsHeight / Guitar.STRING_COUNT

    /** The open-string column is 70 % as wide as a fret. */
    val unit = (right - left) / (lastFret + 0.7f)
    val nutX = left + 0.7f * unit

    fun columnCenter(fret: Int) = if (fret == 0) left + 0.35f * unit else nutX + (fret - 0.5f) * unit

    /** Tab order: string 5 (high e) in the top row. */
    fun rowCenter(string: Int) = (Guitar.STRING_COUNT - 1 - string + 0.5f) * rowHeight

    fun hit(offset: Offset): FretPosition? {
        if (offset.x < left || offset.y >= stringsHeight) return null
        val fret = if (offset.x < nutX) 0 else (((offset.x - nutX) / unit).toInt() + 1).coerceAtMost(lastFret)
        val string = Guitar.STRING_COUNT - 1 - (offset.y / rowHeight).toInt()
        return if (string in 0 until Guitar.STRING_COUNT) FretPosition(string, fret) else null
    }
}
