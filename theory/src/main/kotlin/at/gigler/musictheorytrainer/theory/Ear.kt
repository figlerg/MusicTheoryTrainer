package at.gigler.musictheorytrainer.theory

import kotlin.random.Random

/** Intervals as they are named by ear, from the semitone (kleine Sekunde) up to the octave. */
enum class EarInterval(val semitones: Int, val label: String) {
    MINOR_SECOND(1, "kleine Sekunde"),
    MAJOR_SECOND(2, "große Sekunde"),
    MINOR_THIRD(3, "kleine Terz"),
    MAJOR_THIRD(4, "große Terz"),
    FOURTH(5, "Quarte"),
    TRITONE(6, "Tritonus"),
    FIFTH(7, "Quinte"),
    MINOR_SIXTH(8, "kleine Sexte"),
    MAJOR_SIXTH(9, "große Sexte"),
    MINOR_SEVENTH(10, "kleine Septime"),
    MAJOR_SEVENTH(11, "große Septime"),
    OCTAVE(12, "Oktave"),
}

/** How many intervals are in play. */
enum class EarIntervalSet(val label: String, val intervals: List<EarInterval>) {
    EASY(
        "Die vier einfachen",
        listOf(EarInterval.MAJOR_THIRD, EarInterval.FOURTH, EarInterval.FIFTH, EarInterval.OCTAVE),
    ),
    COMMON(
        "Ohne Tritonus",
        EarInterval.entries.filter { it != EarInterval.TRITONE },
    ),
    ALL("Alle zwölf", EarInterval.entries.toList()),
}

/** The four triads by ear. Major and minor are the pair worth drilling first. */
enum class EarChord(val semitones: List<Int>, val label: String) {
    MAJOR(listOf(0, 4, 7), "Dur"),
    MINOR(listOf(0, 3, 7), "Moll"),
    DIMINISHED(listOf(0, 3, 6), "vermindert"),
    AUGMENTED(listOf(0, 4, 8), "übermäßig"),
    ;

    companion object {
        val MAJOR_MINOR = listOf(MAJOR, MINOR)
        val ALL = entries.toList()
    }
}

object EarQuiz {
    /** Roots stay in a comfortable range around the middle of the guitar, written as MIDI. */
    const val LOWEST_ROOT = 52
    const val HIGHEST_ROOT = 64

    fun randomRoot(random: Random): Int = random.nextInt(LOWEST_ROOT, HIGHEST_ROOT + 1)

    fun <T> next(pool: List<T>, random: Random, previous: T? = null): T {
        val choices = pool.filter { it != previous }.ifEmpty { pool }
        return choices[random.nextInt(choices.size)]
    }

    /** Two notes of an interval, either one after the other or together. */
    fun intervalNotes(root: Int, interval: EarInterval): List<Int> = listOf(root, root + interval.semitones)

    fun chordNotes(root: Int, chord: EarChord): List<Int> = chord.semitones.map { root + it }
}
