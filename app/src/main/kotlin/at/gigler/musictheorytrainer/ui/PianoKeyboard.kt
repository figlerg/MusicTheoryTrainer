package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.theory.Piano

private val IVORY = Color(0xFFF8F6F0)
private val EBONY = Color(0xFF1C1B1F)
private val KEY_EDGE = Color(0xFF8A8A8A)
private const val BLACK_WIDTH = 0.6f
private const val BLACK_HEIGHT = 0.62f

/** Where the keys of [range] sit in a canvas [width] x [height] wide. */
private class KeyboardGeometry(val range: IntRange, val width: Float, val height: Float) {
    val whites = Piano.whiteKeys(range)
    val whiteWidth = width / whites.size
    val blackWidth = whiteWidth * BLACK_WIDTH
    val blackHeight = height * BLACK_HEIGHT

    fun whiteLeft(midi: Int) = whites.indexOf(midi) * whiteWidth

    /** A black key straddles the line between the white keys left and right of it. */
    fun blackCenter(midi: Int) = Piano.whiteKeysBelow(midi, range) * whiteWidth

    fun keyAt(offset: Offset): Int? {
        if (offset.y < blackHeight) {
            range.filter(Piano::isBlack).firstOrNull { kotlin.math.abs(offset.x - blackCenter(it)) < blackWidth / 2 }
                ?.let { return it }
        }
        val index = (offset.x / whiteWidth).toInt()
        return whites.getOrNull(index)
    }
}

/**
 * A piano keyboard as it looks, black keys in groups of two and three. [marks] colour keys like the
 * dots on the fretboard, [label] writes a name on a key, and with [onTap] the keys can be pressed.
 */
@Composable
fun PianoKeyboard(
    range: IntRange,
    modifier: Modifier = Modifier,
    marks: Map<Int, MarkStyle> = emptyMap(),
    label: (Int) -> String? = { null },
    onTap: ((Int) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val feedback = LocalFeedbackColors.current
    val measurer = rememberTextMeasurer()
    // The gesture detector lives across recompositions; it must call the current callback, not
    // the one from the first question.
    val currentOnTap by rememberUpdatedState(onTap)
    val tapModifier = if (onTap == null) Modifier else Modifier.pointerInput(range) {
        detectTapGestures { offset ->
            KeyboardGeometry(range, size.width.toFloat(), size.height.toFloat()).keyAt(offset)?.let { currentOnTap?.invoke(it) }
        }
    }
    Canvas(modifier.then(tapModifier)) {
        val g = KeyboardGeometry(range, size.width, size.height)
        val corner = CornerRadius(4.dp.toPx())
        val edge = 1.dp.toPx()

        fun styleColors(style: MarkStyle): Pair<Color, Color> = when (style) {
            MarkStyle.NORMAL -> colors.primary to colors.onPrimary
            MarkStyle.ROOT -> colors.tertiary to colors.onTertiary
            MarkStyle.CORRECT -> feedback.correct to feedback.onCorrect
            MarkStyle.UNUSUAL -> feedback.unusual to feedback.onUnusual
            MarkStyle.WRONG -> colors.error to colors.onError
            MarkStyle.HINT -> colors.secondaryContainer to colors.onSecondaryContainer
        }

        for (midi in g.whites) {
            val left = g.whiteLeft(midi)
            drawRoundRect(IVORY, Offset(left, 0f), Size(g.whiteWidth, size.height), corner)
            drawRoundRect(KEY_EDGE, Offset(left, 0f), Size(g.whiteWidth, size.height), corner, style = Stroke(edge))
            keyContent(measurer, marks[midi]?.let(::styleColors), label(midi), left + g.whiteWidth / 2, size.height, g.whiteWidth, EBONY)
        }
        for (midi in range.filter(Piano::isBlack)) {
            val left = g.blackCenter(midi) - g.blackWidth / 2
            drawRoundRect(EBONY, Offset(left, 0f), Size(g.blackWidth, g.blackHeight), corner)
            keyContent(measurer, marks[midi]?.let(::styleColors), label(midi), left + g.blackWidth / 2, g.blackHeight, g.blackWidth, Color.White)
        }
    }
}

/** A mark (filled circle) and/or a name near the lower end of a key that ends at [bottom]. */
private fun DrawScope.keyContent(
    measurer: TextMeasurer,
    mark: Pair<Color, Color>?,
    text: String?,
    centerX: Float,
    bottom: Float,
    keyWidth: Float,
    textColor: Color,
) {
    val radius = keyWidth * 0.38f
    val center = Offset(centerX, bottom - radius * 1.3f)
    if (mark != null) drawCircle(mark.first, radius, center)
    if (text == null) return
    val fontPx = radius * if (text.length <= 2) 0.95f else 0.7f
    val layout = measurer.measure(
        text,
        TextStyle(color = mark?.second ?: textColor, fontSize = fontPx.toSp(), fontWeight = FontWeight.Medium),
    )
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}
