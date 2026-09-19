package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import at.gigler.musictheorytrainer.audio.Sound
import at.gigler.musictheorytrainer.data.Exercise
import at.gigler.musictheorytrainer.data.Settings
import at.gigler.musictheorytrainer.theory.Answer
import at.gigler.musictheorytrainer.theory.Direction
import at.gigler.musictheorytrainer.theory.Intervals
import at.gigler.musictheorytrainer.theory.IntervalQuiz
import at.gigler.musictheorytrainer.theory.NoteNames
import kotlin.random.Random

@Composable
fun IntervalScreen(
    settings: Settings,
    sound: Sound,
    onResult: (Boolean) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
) {
    ExerciseScaffold(Exercise.INTERVALS, settings, updateSettings, onBack) {
        val notation = settings.notation
        val difficulty = settings.intervalDifficulty
        var question by remember(difficulty) { mutableStateOf(IntervalQuiz.next(difficulty, Random)) }
        val state = rememberSingleAnswer(question) { question = IntervalQuiz.next(difficulty, Random, question) }
        val solved = state.revealed || state.verdict?.isHit == true
        val spelling = question.direction.spelling

        Spacer(Modifier.weight(1f))
        Text(
            if (solved) question.text(notation).removeSuffix("?") + question.answerName(notation) else question.text(notation),
            style = MaterialTheme.typography.displaySmall,
            color = verdictColor(state.feedbackVerdict),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        FeedbackLine(
            state.feedback(question.answerName(notation)) { given ->
                given.spelled?.name(notation) ?: NoteNames.name(given.pitch, notation, spelling)
            },
            state.feedbackVerdict,
        )
        if (state.showMistakeActions || state.revealed) {
            MistakeActions(
                onSolution = if (state.revealed) null else ({
                    state.reveal(onResult)
                    sound.play(60 + question.answer.semitone)
                }),
                onExplain = { state.explain(onResult) },
            )
        }
        Spacer(Modifier.weight(1f))
        NoteInput(
            notation = notation,
            mode = settings.inputMode,
            hideKeyLabels = settings.hideKeyLabels,
            onModeChange = { mode -> updateSettings { it.copy(inputMode = mode) } },
            state = state.inputState,
            onNote = { entered ->
                val judgement = Answer.judge(entered, question.answer, notation)
                state.answer(entered, judgement, onResult)
                if (judgement.result.isHit) sound.play(60 + question.answer.semitone)
            },
            onContinue = { question = IntervalQuiz.next(difficulty, Random, question) },
            onUnreadable = state::markUnreadable,
        )

        if (state.explaining) {
            val up = question.direction == Direction.UP
            val n = question.semitones
            // Upwards the start is cell 0, downwards the start is cell 12 (its octave) and we walk left.
            val first = question.start
            val startCell = if (up) 0 else 12
            val targetCell = if (up) n else 12 - n
            val given = state.given?.pitch?.takeIf { state.verdict == Verdict.WRONG }
            ExplanationDialog(question.text(notation).removeSuffix(" = ?"), onDismiss = { state.explaining = false }) {
                SemitoneStrip(
                    first = first,
                    notation = notation,
                    spelling = spelling,
                    arrows = listOf(StripArrow(startCell, targetCell, if (up) jumpLabel(n) else downLabel(n))),
                    marks = buildMap {
                        if (given != null) put(stripIndex(first, given, preferOctave = !up), MarkStyle.WRONG)
                        put(startCell, MarkStyle.ROOT)
                        put(targetCell, MarkStyle.CORRECT)
                    },
                )
                Text(
                    "${question.startName(notation)} ${if (up) "+" else "−"} ${Intervals.describe(n)}: " +
                        "$n ${if (n == 1) "Feld" else "Felder"} nach ${if (up) "rechts" else "links"} ergibt ${question.answerName(notation)}.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (state.unreadable) UnreadableAnswerLine()
            }
        }
    }
}
