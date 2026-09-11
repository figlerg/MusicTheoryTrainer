package at.gigler.musictheorytrainer.theory

import kotlin.random.Random

/** [string] 0 = low E … 5 = high e. */
data class FretPosition(val string: Int, val fret: Int)

object Guitar {
    const val STRING_COUNT = 6
    const val MAX_FRET = 12

    /** Semitones between neighbouring strings, low to high: 5 everywhere except G to H. */
    val STRING_INTERVALS: List<Int> = listOf(5, 5, 5, 4, 5)

    /** Standard tuning E A D G H E, derived from the low E and [STRING_INTERVALS]. */
    val OPEN_STRINGS: List<PitchClass> = STRING_INTERVALS.runningFold(PitchClass.E) { pitch, step -> pitch + step }

    /** Sounding pitch of the open strings as MIDI note numbers: E2 A2 D3 G3 H3 E4. */
    val OPEN_STRING_MIDI: List<Int> = listOf(40, 45, 50, 55, 59, 64)

    val ALL_STRINGS: List<Int> = (0 until STRING_COUNT).toList()

    fun pitchAt(position: FretPosition): PitchClass = OPEN_STRINGS[position.string] + position.fret

    fun midiAt(position: FretPosition): Int = OPEN_STRING_MIDI[position.string] + position.fret

    fun stringName(string: Int, notation: Notation): String = when (string) {
        4 -> if (notation == Notation.GERMAN) "H" else "B"
        5 -> "e"
        else -> NoteNames.name(OPEN_STRINGS[string], notation, Spelling.SHARP)
    }

    fun positionsOf(pitch: PitchClass, strings: Collection<Int>, frets: IntRange = 0..MAX_FRET): List<FretPosition> =
        strings.sorted().flatMap { string ->
            frets.filter { OPEN_STRINGS[string] + it == pitch }.map { FretPosition(string, it) }
        }

    /** Lowest fret of [pitch] on [string], 0..11. */
    fun lowestFret(pitch: PitchClass, string: Int): Int = OPEN_STRINGS[string].semitonesUpTo(pitch)

    /** The string among [strings] where [root] sits lowest; ties go to the lower string. */
    fun lowestRootString(root: PitchClass, strings: List<Int>): Int =
        strings.minWith(compareBy<Int> { lowestFret(root, it) }.thenBy { it })

    /** Degrees 1..8 of the major scale on one string, e.g. E on the low E string: 0 2 4 5 7 9 11 12. */
    fun majorScaleOnString(root: PitchClass, string: Int): List<FretPosition> {
        val rootFret = lowestFret(root, string)
        return MajorScale.OFFSETS.map { FretPosition(string, rootFret + it) }
    }
}

object FretboardQuiz {
    fun randomPosition(strings: List<Int>, random: Random, previous: FretPosition? = null): FretPosition {
        while (true) {
            val position = FretPosition(strings[random.nextInt(strings.size)], random.nextInt(Guitar.MAX_FRET + 1))
            if (position != previous) return position
        }
    }

    fun randomPitch(random: Random, previous: PitchClass? = null): PitchClass {
        while (true) {
            val pitch = PitchClass.of(random.nextInt(12))
            if (pitch != previous) return pitch
        }
    }
}
