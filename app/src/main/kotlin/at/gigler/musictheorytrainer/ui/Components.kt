package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.gigler.musictheorytrainer.data.Exercise
import at.gigler.musictheorytrainer.theory.AnswerResult

/** Correct answers move on by themselves after this delay, wrong ones wait for the user. */
const val AUTO_ADVANCE_MILLIS = 700L

/** Half-right answers stay a little longer so the usual name can be read. */
const val UNUSUAL_ADVANCE_MILLIS = 1800L

enum class Verdict {
    CORRECT, UNUSUAL, WRONG;

    val isHit: Boolean get() = this != WRONG

    companion object {
        fun of(result: AnswerResult): Verdict? = when (result) {
            AnswerResult.CORRECT -> CORRECT
            AnswerResult.UNUSUAL -> UNUSUAL
            AnswerResult.WRONG -> WRONG
            AnswerResult.UNREADABLE -> null
        }
    }
}

val Exercise.title: String
    get() = when (this) {
        Exercise.FRETBOARD -> "Griffbrett-Töne"
        Exercise.INTERVALS -> "Intervalle"
        Exercise.SCALE -> "Tonleitern"
        Exercise.CHORDS -> "Akkorde"
        Exercise.SHEET -> "Notenlesen"
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
    Verdict.UNUSUAL -> LocalFeedbackColors.current.unusual
    Verdict.WRONG -> MaterialTheme.colorScheme.error
    null -> MaterialTheme.colorScheme.onSurface
}

fun Verdict?.markStyle(): MarkStyle = when (this) {
    null -> MarkStyle.NORMAL
    Verdict.CORRECT -> MarkStyle.CORRECT
    Verdict.UNUSUAL -> MarkStyle.UNUSUAL
    Verdict.WRONG -> MarkStyle.WRONG
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

/**
 * Shown after a wrong answer. Trying again needs no button: the input stays open.
 * [onSolution] gives up and reveals the answer, [onExplain] opens the explanation.
 */
@Composable
fun MistakeActions(onSolution: (() -> Unit)?, onExplain: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        if (onSolution != null) OutlinedButton(onClick = onSolution) { Text("Lösung") }
        OutlinedButton(onClick = onExplain) { Text("Erklärung") }
    }
}

@Composable
fun ExplanationDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) },
    )
}

@Composable
fun TabText(tab: String, modifier: Modifier = Modifier) {
    Text(
        tab,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.sp,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        softWrap = false,
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
    )
}
