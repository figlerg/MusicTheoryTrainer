package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import at.gigler.musictheorytrainer.theory.FretPosition
import at.gigler.musictheorytrainer.theory.Guitar
import at.gigler.musictheorytrainer.theory.Notation
import kotlin.math.min

enum class MarkStyle { NORMAL, ROOT, CORRECT, WRONG, HINT }

data class FretMark(val position: FretPosition, val label: String, val style: MarkStyle = MarkStyle.NORMAL)

private val SINGLE_INLAYS = setOf(3, 5, 7, 9, 15, 17, 19, 21)
private val DOUBLE_INLAYS = setOf(12, 24)

/** Height the fretboard needs for frets [firstFret]..[lastFret] with rows of [rowHeight], header included. */
fun fretboardHeight(firstFret: Int, lastFret: Int, rowHeight: Dp): Dp =
    FretboardGeometry.HEADER +
        (if (firstFret == 0) FretboardGeometry.NUT else 0.dp) +
        rowHeight * (lastFret - firstFret + 1)

/**
 * Vertical fretboard like a chord chart: nut on top, low E on the left.
 * With [firstFret] 0, fret 0 is the row above the nut so open strings have a place for their dot;
 * otherwise only the section [firstFret]..[lastFret] is drawn, without nut.
 */
@Composable
fun Fretboard(
    marks: List<FretMark>,
    notation: Notation,
    modifier: Modifier = Modifier,
    firstFret: Int = 0,
    lastFret: Int = Guitar.MAX_FRET,
    activeStrings: Collection<Int> = Guitar.ALL_STRINGS,
    onTap: ((FretPosition) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val feedback = LocalFeedbackColors.current
    val measurer = rememberTextMeasurer()
    val smallText = MaterialTheme.typography.labelMedium
    val active = activeStrings.toSet()
    val currentOnTap by rememberUpdatedState(onTap)

    val tapModifier = if (onTap == null) {
        Modifier
    } else {
        Modifier.pointerInput(firstFret, lastFret, active) {
            detectTapGestures { offset ->
                val position = FretboardGeometry(size.toSize(), firstFret, lastFret, this).hit(offset)
                if (position != null && position.string in active) currentOnTap?.invoke(position)
            }
        }
    }

    Canvas(modifier.then(tapModifier).semantics { contentDescription = "Griffbrett" }) {
        val g = FretboardGeometry(size, firstFret, lastFret, this)
        val neckWidth = g.neckRight - g.neckLeft

        drawRect(colors.surfaceContainerHigh, Offset(g.neckLeft, g.neckTop), Size(neckWidth, size.height - g.neckTop))

        val inlayRadius = min(g.columnWidth, g.rowHeight) * 0.13f
        for (fret in maxOf(1, firstFret)..lastFret) {
            val y = g.rowCenter(fret)
            if (fret in SINGLE_INLAYS) {
                drawCircle(colors.outlineVariant, inlayRadius, Offset(g.left + 3 * g.columnWidth, y))
            } else if (fret in DOUBLE_INLAYS) {
                drawCircle(colors.outlineVariant, inlayRadius, Offset(g.left + 2 * g.columnWidth, y))
                drawCircle(colors.outlineVariant, inlayRadius, Offset(g.left + 4 * g.columnWidth, y))
            }
            val wireY = g.rowTop(fret) + g.rowHeight
            drawLine(colors.outline, Offset(g.neckLeft, wireY), Offset(g.neckRight, wireY), 1.5.dp.toPx())
            drawCentered(
                measurer,
                fret.toString(),
                Offset(g.left / 2, y),
                smallText.copy(color = colors.onSurfaceVariant),
            )
        }

        if (firstFret == 0) {
            drawRect(colors.onSurface, Offset(g.neckLeft, g.neckTop), Size(neckWidth, g.nut))
        } else {
            drawLine(colors.outline, Offset(g.neckLeft, g.neckTop), Offset(g.neckRight, g.neckTop), 1.5.dp.toPx())
        }

        val stringTop = if (firstFret == 0) g.top + g.rowHeight * 0.2f else g.top
        for (string in 0 until Guitar.STRING_COUNT) {
            val x = g.stringX(string)
            val isActive = string in active
            drawLine(
                colors.onSurfaceVariant.copy(alpha = if (isActive) 1f else 0.25f),
                Offset(x, stringTop),
                Offset(x, size.height),
                (2.6f - string * 0.3f).dp.toPx(),
            )
            drawCentered(
                measurer,
                Guitar.stringName(string, notation),
                Offset(x, g.top / 2),
                smallText.copy(
                    color = colors.onSurface.copy(alpha = if (isActive) 1f else 0.35f),
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        val radius = min(g.columnWidth, g.rowHeight) * 0.42f
        for (mark in marks) {
            if (mark.position.fret !in firstFret..lastFret) continue
            val (fill, content) = when (mark.style) {
                MarkStyle.NORMAL -> colors.primary to colors.onPrimary
                MarkStyle.ROOT -> colors.tertiary to colors.onTertiary
                MarkStyle.CORRECT -> feedback.correct to feedback.onCorrect
                MarkStyle.WRONG -> colors.error to colors.onError
                MarkStyle.HINT -> colors.secondaryContainer to colors.onSecondaryContainer
            }
            val center = Offset(g.stringX(mark.position.string), g.rowCenter(mark.position.fret))
            drawCircle(fill, radius, center)
            if (mark.style == MarkStyle.HINT) {
                drawCircle(colors.secondary, radius, center, style = Stroke(2.dp.toPx()))
            }
            val fontPx = radius * if (mark.label.length <= 2) 0.9f else 0.72f
            drawCentered(
                measurer,
                mark.label,
                center,
                TextStyle(color = content, fontSize = fontPx.toSp(), fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

private fun DrawScope.drawCentered(measurer: TextMeasurer, text: String, center: Offset, style: TextStyle) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}

/** Shared by drawing and hit testing so taps land exactly where things are drawn. */
private class FretboardGeometry(size: Size, private val firstFret: Int, private val lastFret: Int, density: Density) {
    val top = with(density) { HEADER.toPx() }
    val nut = if (firstFret == 0) with(density) { NUT.toPx() } else 0f
    val left = with(density) { 28.dp.toPx() }
    private val right = with(density) { 8.dp.toPx() }

    val columnWidth = (size.width - left - right) / Guitar.STRING_COUNT
    val rowHeight = (size.height - top - nut) / (lastFret - firstFret + 1)
    val neckLeft = left + columnWidth * 0.1f
    val neckRight = size.width - right - columnWidth * 0.1f

    /** Where the fretted part starts: the nut below the open-string row, or the top for a section. */
    val neckTop = if (firstFret == 0) top + rowHeight else top

    fun stringX(string: Int) = left + (string + 0.5f) * columnWidth

    fun rowTop(fret: Int) = when {
        firstFret > 0 -> top + (fret - firstFret) * rowHeight
        fret == 0 -> top
        else -> top + nut + fret * rowHeight
    }

    fun rowCenter(fret: Int) = rowTop(fret) + rowHeight / 2

    fun hit(offset: Offset): FretPosition? {
        if (offset.x < left || offset.y < top) return null
        val string = ((offset.x - left) / columnWidth).toInt()
        val fret = when {
            firstFret > 0 -> firstFret + ((offset.y - top) / rowHeight).toInt()
            offset.y < neckTop + nut -> 0
            else -> ((offset.y - top - nut) / rowHeight).toInt()
        }
        return if (string in 0 until Guitar.STRING_COUNT && fret in firstFret..lastFret) FretPosition(string, fret) else null
    }

    companion object {
        val HEADER = 22.dp
        val NUT = 5.dp
    }
}
