package at.gigler.musictheorytrainer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import at.gigler.musictheorytrainer.theory.EnteredNote
import at.gigler.musictheorytrainer.theory.Judgement
import kotlinx.coroutines.delay

/**
 * Flow of a question with one answer: a wrong answer can simply be corrected by answering again,
 * "Lösung" gives up, and only the first attempt counts for the hit rate.
 */
@Stable
class SingleAnswerState {
    var verdict by mutableStateOf<Verdict?>(null)
        private set
    var given by mutableStateOf<EnteredNote?>(null)
        private set
    var usualName by mutableStateOf<String?>(null)
        private set
    var revealed by mutableStateOf(false)
        private set
    var unreadable by mutableStateOf(false)
        private set
    var explaining by mutableStateOf(false)

    private var recorded = false

    val inputState: InputState
        get() = when {
            revealed -> InputState.CONTINUE
            verdict?.isHit == true -> InputState.LOCKED
            else -> InputState.ACCEPTING
        }

    /** Wrong answer or unreadable input, not yet given up: offer "Lösung" and "Erklärung". */
    val showMistakeActions: Boolean
        get() = !revealed && (verdict == Verdict.WRONG || unreadable)

    fun answer(entered: EnteredNote, judgement: Judgement, onResult: (Boolean) -> Unit) {
        val v = Verdict.of(judgement.result) ?: return
        if (!recorded) {
            onResult(v.isHit)
            recorded = true
        }
        verdict = v
        given = entered
        usualName = judgement.usualName
        unreadable = false
    }

    fun markUnreadable() {
        unreadable = true
    }

    /** Giving up or asking for the explanation before any counted answer counts as a miss. */
    fun reveal(onResult: (Boolean) -> Unit) {
        countMiss(onResult)
        revealed = true
    }

    fun explain(onResult: (Boolean) -> Unit) {
        countMiss(onResult)
        explaining = true
    }

    private fun countMiss(onResult: (Boolean) -> Unit) {
        if (!recorded) {
            onResult(false)
            recorded = true
        }
    }
}

/** A fresh state per question; moves on by itself after a hit. */
@Composable
fun rememberSingleAnswer(question: Any, onNext: () -> Unit): SingleAnswerState {
    val state = remember(question) { SingleAnswerState() }
    LaunchedEffect(question, state.verdict) {
        when (state.verdict) {
            Verdict.CORRECT -> {
                delay(AUTO_ADVANCE_MILLIS)
                onNext()
            }
            Verdict.UNUSUAL -> {
                delay(UNUSUAL_ADVANCE_MILLIS)
                onNext()
            }
            else -> Unit
        }
    }
    return state
}

/** Feedback line text for the common cases; [answerName] is the full solution. */
fun SingleAnswerState.feedback(answerName: String, givenName: (EnteredNote) -> String): String? = when {
    revealed -> "Lösung: $answerName"
    unreadable -> null
    verdict == Verdict.CORRECT -> "Richtig: $answerName"
    verdict == Verdict.UNUSUAL -> "Richtig (üblich: $usualName)"
    verdict == Verdict.WRONG -> "Falsch (du: ${given?.let(givenName)}) – nochmal?"
    else -> null
}

val SingleAnswerState.feedbackVerdict: Verdict?
    get() = if (revealed) null else verdict
