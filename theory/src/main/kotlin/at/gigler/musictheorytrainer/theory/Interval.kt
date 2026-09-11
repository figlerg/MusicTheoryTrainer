package at.gigler.musictheorytrainer.theory

import kotlin.random.Random

/** Upwards uses sharp names (Cis), downwards flat names (Des). */
enum class Direction(val sign: String, val spelling: Spelling) {
    UP("+", Spelling.SHARP),
    DOWN("−", Spelling.FLAT),
}

enum class IntervalDifficulty(val maxSemitones: Int) {
    EASY(2),
    MEDIUM(5),
    HARD(11),
}

object Intervals {
    fun describe(semitones: Int): String = when (semitones) {
        1 -> "Halbton"
        2 -> "Ganzton"
        else -> "$semitones Halbtöne"
    }
}

data class IntervalQuestion(val start: PitchClass, val direction: Direction, val semitones: Int) {

    val answer: PitchClass
        get() = if (direction == Direction.UP) start + semitones else start - semitones

    fun startName(notation: Notation): String = NoteNames.name(start, notation, direction.spelling)

    fun answerName(notation: Notation): String = NoteNames.name(answer, notation, direction.spelling)

    /** "Fis + Halbton = ?" */
    fun text(notation: Notation): String =
        "${startName(notation)} ${direction.sign} ${Intervals.describe(semitones)} = ?"
}

object IntervalQuiz {
    fun next(difficulty: IntervalDifficulty, random: Random, previous: IntervalQuestion? = null): IntervalQuestion {
        while (true) {
            val question = IntervalQuestion(
                start = PitchClass.of(random.nextInt(12)),
                direction = if (random.nextBoolean()) Direction.UP else Direction.DOWN,
                semitones = random.nextInt(1, difficulty.maxSemitones + 1),
            )
            if (question != previous) return question
        }
    }
}
