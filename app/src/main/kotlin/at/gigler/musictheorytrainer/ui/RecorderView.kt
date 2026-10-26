package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.theory.Fingering
import at.gigler.musictheorytrainer.theory.Hole
import at.gigler.musictheorytrainer.theory.Recorder
import at.gigler.musictheorytrainer.theory.RecorderNote
import at.gigler.musictheorytrainer.theory.RecorderSystem

/** The fingering system from the settings, provided once at the top instead of passed through every screen. */
val LocalRecorderSystem = staticCompositionLocalOf { RecorderSystem.BAROQUE }

/** Room the input needs: hand captions, holes, numbers, buttons and the "unknown" line. */
val RECORDER_INPUT_HEIGHT = 176.dp

private val HAND_GAP_WEIGHT = 0.45f

/**
 * A soprano recorder lying on its side, mouthpiece to the left: the thumb hole at the back is
 * drawn first, then the left hand (1 2 3) and the right hand (4 5 6 7). With [onTap] the holes
 * can be tapped; without it the view only shows [fingering].
 */
@Composable
fun RecorderFingeringView(
    fingering: Fingering,
    modifier: Modifier = Modifier,
    onTap: ((Int) -> Unit)? = null,
    captions: Boolean = true,
    holeGap: Dp = 3.dp,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val wood = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(modifier) {
        if (captions) {
            Row(Modifier.fillMaxWidth()) {
                Caption("Daumen", 1f)
                Spacer(Modifier.weight(HAND_GAP_WEIGHT))
                Caption("linke Hand", 3f)
                Spacer(Modifier.weight(HAND_GAP_WEIGHT))
                Caption("rechte Hand", 4f)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HoleCell(0, fingering, onTap, captions, ink, holeGap)
            Spacer(Modifier.weight(HAND_GAP_WEIGHT))
            // The body of the recorder runs behind the seven front holes.
            Row(
                Modifier
                    .weight(7f + HAND_GAP_WEIGHT)
                    .background(wood, RoundedCornerShape(50)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (hole in 1..3) HoleCell(hole, fingering, onTap, captions, ink, holeGap)
                Spacer(Modifier.weight(HAND_GAP_WEIGHT))
                for (hole in 4..7) HoleCell(hole, fingering, onTap, captions, ink, holeGap)
            }
        }
    }
}

@Composable
private fun RowScope.Caption(text: String, weight: Float) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
}

@Composable
private fun RowScope.HoleCell(
    hole: Int,
    fingering: Fingering,
    onTap: ((Int) -> Unit)?,
    numbered: Boolean,
    ink: Color,
    gap: Dp,
) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        val state = fingering.holes[hole]
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(gap)
                .clip(CircleShape)
                .then(if (onTap != null) Modifier.clickable { onTap(hole) } else Modifier)
                .drawBehind {
                    val stroke = size.minDimension * 0.08f
                    val radius = size.minDimension / 2 - stroke
                    when (state) {
                        Hole.CLOSED -> drawCircle(ink, radius)
                        // Half open: the lower half stays covered, as when the thumbnail pinches the hole.
                        Hole.HALF -> drawArc(
                            ink, 0f, 180f, useCenter = true,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                        )
                        Hole.OPEN -> Unit
                    }
                    drawCircle(ink, radius, style = Stroke(stroke))
                },
        )
        if (numbered) {
            Text(
                if (hole == Recorder.THUMB) "0" else hole.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The next state of a tapped hole: front holes open and close, the thumb hole can also be half open. */
private fun Hole.next(thumb: Boolean): Hole = when (this) {
    Hole.OPEN -> Hole.CLOSED
    Hole.CLOSED -> if (thumb) Hole.HALF else Hole.OPEN
    Hole.HALF -> Hole.OPEN
}

/**
 * Note input by fingering: tap the holes you would cover, then "Spielen". The fingers stay where
 * they are for the next note, as on the real instrument.
 */
@Composable
fun RecorderInput(
    enabled: Boolean,
    onNote: (RecorderNote) -> Unit,
    onUnknown: () -> Unit,
) {
    val system = LocalRecorderSystem.current
    var pattern by rememberSaveable { mutableStateOf(Fingering.ALL_OPEN.toString()) }
    var unknown by remember { mutableStateOf(false) }
    val fingering = Fingering.parse(pattern)
    val tap: (Int) -> Unit = { hole ->
        pattern = fingering.with(hole, fingering.holes[hole].next(hole == Recorder.THUMB)).toString()
        unknown = false
    }
    Column(
        Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RecorderFingeringView(
            fingering = fingering,
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
            onTap = tap.takeIf { enabled },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    pattern = Fingering.ALL_OPEN.toString()
                    unknown = false
                },
                enabled = enabled,
                modifier = Modifier.weight(1f).height(48.dp),
            ) { Text("Alle auf") }
            Button(
                onClick = {
                    val note = Recorder.noteFor(Fingering.parse(pattern), system)
                    if (note == null) {
                        unknown = true
                        onUnknown()
                    } else {
                        onNote(note)
                    }
                },
                enabled = enabled,
                modifier = Modifier.weight(2f).height(48.dp),
            ) { Text("Spielen") }
        }
        Text(
            if (unknown) "Diesen Griff gibt es hier nicht" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
