package at.gigler.musictheorytrainer.theory

import kotlin.random.Random

/** How a finger covers a hole. Only the thumb hole is ever half open here, for the upper octave. */
enum class Hole { OPEN, CLOSED, HALF }

/**
 * Holes from top to bottom: 0 is the thumb hole at the back, 1..3 the left hand (index, middle,
 * ring finger), 4..7 the right hand (index, middle, ring, little finger).
 */
data class Fingering(val holes: List<Hole>) {
    init {
        require(holes.size == Recorder.HOLE_COUNT) { "A fingering has ${Recorder.HOLE_COUNT} holes, not ${holes.size}" }
    }

    /** The same fingering with [hole] set to [state]. */
    fun with(hole: Int, state: Hole): Fingering = Fingering(holes.toMutableList().also { it[hole] = state })

    override fun toString(): String = holes.joinToString("") {
        when (it) {
            Hole.OPEN -> "O"
            Hole.CLOSED -> "X"
            Hole.HALF -> "H"
        }
    }.let { "${it[0]} ${it.substring(1, 4)} ${it.substring(4)}" }

    companion object {
        val ALL_OPEN = Fingering(List(Recorder.HOLE_COUNT) { Hole.OPEN })

        /** "X XXX XXOO": X closed, O open, H half open; spaces only group the hands. */
        fun parse(pattern: String): Fingering = Fingering(
            pattern.filterNot { it == ' ' }.map {
                when (it) {
                    'X' -> Hole.CLOSED
                    'O' -> Hole.OPEN
                    'H' -> Hole.HALF
                    else -> throw IllegalArgumentException("Unknown hole state '$it' in \"$pattern\"")
                }
            },
        )
    }
}

/**
 * Baroque (also called English) fingering is the usual one today. German fingering was common on
 * school recorders for a while; it makes F simpler and the other half steps harder.
 */
enum class RecorderSystem { BAROQUE, GERMAN }

/** One fingered note of the soprano recorder, written as notated; it sounds an octave higher. */
data class RecorderNote(val spelled: SpelledNote, val octave: Int, val fingering: Fingering) {

    val writtenMidi: Int get() = 12 * (octave + 1) + spelled.letter.naturalSemitone + spelled.alteration

    val soundingMidi: Int get() = writtenMidi + Recorder.SOUNDING_OFFSET

    val pitchClass: PitchClass get() = spelled.pitchClass

    fun name(notation: Notation): String = NoteNames.withOctave(spelled, octave, notation)
}

/**
 * Soprano recorder in C. Only the standard beginner fingerings are listed: c' to d'', the first
 * notes of the upper octave with the thumb hole half open, and the two common half steps of the
 * baroque system. Alternative, trill and high fingerings are left out on purpose.
 */
object Recorder {
    const val HOLE_COUNT = 8
    const val THUMB = 0

    /** A soprano recorder sounds an octave above the written note. */
    const val SOUNDING_OFFSET = 12

    private val COMMON = listOf(
        "C4" to "X XXX XXXX",
        "D4" to "X XXX XXXO",
        "E4" to "X XXX XXOO",
        "G4" to "X XXX OOOO",
        "A4" to "X XXO OOOO",
        "B4" to "X XOO OOOO",
        "C5" to "X OXO OOOO",
        "D5" to "O OXO OOOO",
        "E5" to "H XXX XXOO",
        "G5" to "H XXX OOOO",
        "A5" to "H XXO OOOO",
    )

    /** Baroque F is a forked fingering; F sharp and B flat are the usual baroque half steps. */
    private val BAROQUE = listOf(
        "F4" to "X XXX XOXX",
        "F#4" to "X XXX OXXO",
        "Bb4" to "X XOX XOOO",
    )

    /** German F simply lifts the fingers below the index finger. */
    private val GERMAN = listOf(
        "F4" to "X XXX XOOO",
    )

    private val TABLES: Map<RecorderSystem, List<RecorderNote>> = mapOf(
        RecorderSystem.BAROQUE to build(COMMON + BAROQUE),
        RecorderSystem.GERMAN to build(COMMON + GERMAN),
    )

    /** Every fingering of [system], low to high. */
    fun notes(system: RecorderSystem): List<RecorderNote> = TABLES.getValue(system)

    /** The note [fingering] plays, or null if it is not one of the listed fingerings. */
    fun noteFor(fingering: Fingering, system: RecorderSystem): RecorderNote? =
        notes(system).firstOrNull { it.fingering == fingering }

    fun noteAt(writtenMidi: Int, system: RecorderSystem): RecorderNote? =
        notes(system).firstOrNull { it.writtenMidi == writtenMidi }

    /** Whether some fingering plays [pitch] in any octave. */
    fun canPlay(pitch: PitchClass, system: RecorderSystem): Boolean = notes(system).any { it.pitchClass == pitch }

    private fun build(entries: List<Pair<String, String>>): List<RecorderNote> =
        entries.map { (note, pattern) -> parseNote(note, Fingering.parse(pattern)) }.sortedBy { it.writtenMidi }

    /** "C4", "F#4", "Bb4" with English letters. */
    private fun parseNote(text: String, fingering: Fingering): RecorderNote {
        val letter = Letter.valueOf(text.take(1))
        val alteration = when (text[1]) {
            '#' -> 1
            'b' -> -1
            else -> 0
        }
        return RecorderNote(SpelledNote(letter, alteration), text.last().digitToInt(), fingering)
    }
}

object RecorderQuiz {
    fun randomNote(system: RecorderSystem, random: Random, previous: RecorderNote? = null): RecorderNote =
        Recorder.notes(system).filter { it != previous }.random(random)
}
