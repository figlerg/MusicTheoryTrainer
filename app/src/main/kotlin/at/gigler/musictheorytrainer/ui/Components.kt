package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.data.Exercise

/** Correct answers move on by themselves after this delay, wrong ones wait for "Weiter". */
const val AUTO_ADVANCE_MILLIS = 700L

enum class Verdict { CORRECT, WRONG }

val Exercise.title: String
    get() = when (this) {
        Exercise.FRETBOARD -> "Griffbrett-Töne"
        Exercise.INTERVALS -> "Intervalle"
        Exercise.SCALE -> "Durtonleiter"
    }

@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(BackArrowIcon, contentDescription = "Zurück") }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = if (onBack != null) 4.dp else 0.dp),
            )
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label(option), maxLines = 1) },
            )
        }
    }
}

@Composable
fun verdictColor(verdict: Verdict?): Color = when (verdict) {
    Verdict.CORRECT -> LocalFeedbackColors.current.correct
    Verdict.WRONG -> MaterialTheme.colorScheme.error
    null -> MaterialTheme.colorScheme.onSurface
}

/** Always reserves its height so the layout does not jump when feedback appears. */
@Composable
fun FeedbackLine(text: String?, verdict: Verdict?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
        if (text != null) {
            Text(
                text,
                color = verdictColor(verdict),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun TabText(tab: String, modifier: Modifier = Modifier) {
    Text(
        tab,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        softWrap = false,
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
    )
}
