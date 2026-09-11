package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import at.gigler.musictheorytrainer.audio.Sound
import at.gigler.musictheorytrainer.data.Exercise
import at.gigler.musictheorytrainer.data.Settings
import at.gigler.musictheorytrainer.theory.IntervalQuiz
import at.gigler.musictheorytrainer.theory.NoteNames
import at.gigler.musictheorytrainer.theory.PitchClass
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun IntervalScreen(settings: Settings, sound: Sound, onResult: (Boolean) -> Unit, onBack: () -> Unit) {
    ScreenScaffold(Exercise.INTERVALS.title, onBack) {
        val notation = settings.notation
        val difficulty = settings.intervalDifficulty
        var question by remember(difficulty) { mutableStateOf(IntervalQuiz.next(difficulty, Random)) }
        var given by remember(difficulty) { mutableStateOf<PitchClass?>(null) }
        val verdict = given?.let { if (it == question.answer) Verdict.CORRECT else Verdict.WRONG }

        fun next() {
            question = IntervalQuiz.next(difficulty, Random, question)
            given = null
        }

        LaunchedEffect(question, verdict) {
            if (verdict == Verdict.CORRECT) {
                delay(AUTO_ADVANCE_MILLIS)
                next()
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            if (verdict == null) question.text(notation) else question.text(notation).removeSuffix("?") + question.answerName(notation),
            style = MaterialTheme.typography.displaySmall,
            color = verdictColor(verdict),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        FeedbackLine(
            text = when (verdict) {
                null -> null
                Verdict.CORRECT -> "Richtig"
                Verdict.WRONG -> "Falsch (du: ${NoteNames.name(given!!, notation, question.direction.spelling)})"
            },
            verdict = verdict,
        )
        Spacer(Modifier.weight(1f))
        NoteInput(
            notation = notation,
            defaultMode = settings.inputMode,
            state = when (verdict) {
                null -> InputState.ACCEPTING
                Verdict.CORRECT -> InputState.LOCKED
                Verdict.WRONG -> InputState.CONTINUE
            },
            onNote = { pitch ->
                given = pitch
                sound.play(60 + question.answer.semitone)
                onResult(pitch == question.answer)
            },
            onContinue = ::next,
        )
    }
}
