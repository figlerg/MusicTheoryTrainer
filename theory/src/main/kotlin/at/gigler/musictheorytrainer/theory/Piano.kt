package at.gigler.musictheorytrainer.theory

import kotlin.random.Random

object Piano {
    /** Middle C, c' in German names: the C closest to the middle of the keyboard. */
    const val MIDDLE_C = 60

    /** Two octaves from middle C, c' to h'': enough to see the pattern repeat. */
    val TWO_OCTAVES: IntRange = MIDDLE_C until MIDDLE_C + 24

    /** The five black keys are the altered notes; they sit in groups of two (Cis Dis) and three (Fis Gis Ais). */
    fun isBlack(midi: Int): Boolean = !NoteNames.isNatural(PitchClass.of(midi))

    fun whiteKeys(range: IntRange): List<Int> = range.filterNot(::isBlack)

    /** For a black key: how many white keys of [range] lie left of it. */
    fun whiteKeysBelow(midi: Int, range: IntRange): Int = range.count { it < midi && !isBlack(it) }
}

object PianoQuiz {
    fun randomKey(range: IntRange, random: Random, previous: Int? = null): Int =
        range.filter { it != previous }.random(random)
}
