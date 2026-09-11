package at.gigler.musictheorytrainer.theory

enum class AnswerResult { CORRECT, WRONG, UNREADABLE }

object Answer {
    /** Enharmonic spellings count as correct: "Des" is accepted for Cis. */
    fun check(input: String, expected: PitchClass, notation: Notation): AnswerResult {
        val given = NoteParser.parsePitch(input, notation) ?: return AnswerResult.UNREADABLE
        return if (given == expected) AnswerResult.CORRECT else AnswerResult.WRONG
    }
}
