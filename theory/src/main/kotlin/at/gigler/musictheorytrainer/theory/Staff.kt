package at.gigler.musictheorytrainer.theory

import kotlin.random.Random

/**
 * A natural note as written in treble clef. Guitar music sounds an octave lower than written,
 * so the bottom line E4 is played as E3 (D string, 2nd fret).
 */
data class StaffNote(val letter: Letter, val octave: Int) {

    /** Diatonic steps above the bottom line: 0 = E4 (line), 1 = F4 (space) … 8 = F5 (top line). */
    val staffPosition: Int get() = octave * 7 + letter.ordinal - BOTTOM_LINE

    val pitchClass: PitchClass get() = PitchClass.of(letter.naturalSemitone)

    val writtenMidi: Int get() = 12 * (octave + 1) + letter.naturalSemitone

    val guitarMidi: Int get() = writtenMidi - 12

    fun name(notation: Notation): String = SpelledNote(letter).name(notation)

    /** Positions of the ledger lines this note needs, empty inside the staff. */
    fun ledgerLines(): List<Int> = when {
        staffPosition < -1 -> (-2 downTo staffPosition).filter { it % 2 == 0 }
        staffPosition > 9 -> (10..staffPosition).filter { it % 2 == 0 }
        else -> emptyList()
    }

    companion object {
        private const val BOTTOM_LINE = 4 * 7 + 2 // E4

        fun fromPosition(position: Int): StaffNote {
            val index = position + BOTTOM_LINE
            return StaffNote(Letter.entries[index.mod(7)], index.floorDiv(7))
        }

        /** "G4 A4 B4" with English letters (B = H). */
        fun parseAll(notes: String): List<StaffNote> = notes.split(' ').filter { it.isNotBlank() }.map {
            StaffNote(Letter.valueOf(it.dropLast(1)), it.last().digitToInt())
        }
    }
}

enum class StaffRange(val low: Int, val high: Int) {
    /** D4 (hanging below the bottom line) to G5 (on top of the staff), no ledger lines. */
    IN_STAFF(-1, 9),

    /** The guitar's first position as written: E3 (low E string) to A5 (high e, 5th fret). */
    GUITAR(-7, 10),
}

data class Melody(val title: String, val notes: List<StaffNote>)

/** Public-domain melodies, all in G major using only G A H C D E so no key signature is needed. */
object Melodies {
    val ALL: List<Melody> = listOf(
        Melody(
            "Alle meine Entchen",
            StaffNote.parseAll("G4 A4 B4 C5 D5 D5 E5 E5 E5 E5 D5 E5 E5 E5 E5 D5 C5 C5 C5 C5 B4 B4 A4 A4 A4 A4 G4"),
        ),
        Melody(
            "Hänschen klein",
            StaffNote.parseAll("D5 B4 B4 C5 A4 A4 G4 A4 B4 C5 D5 D5 D5 D5 B4 B4 C5 A4 A4 G4 B4 D5 D5 G4"),
        ),
        Melody(
            "Bruder Jakob",
            StaffNote.parseAll("G4 A4 B4 G4 G4 A4 B4 G4 B4 C5 D5 B4 C5 D5 D5 E5 D5 C5 B4 G4 D5 E5 D5 C5 B4 G4 G4 D4 G4 G4 D4 G4"),
        ),
        Melody(
            "Morgen kommt der Weihnachtsmann",
            StaffNote.parseAll("G4 G4 D5 D5 E5 E5 D5 C5 C5 B4 B4 A4 A4 G4 D5 D5 C5 C5 B4 B4 A4 D5 D5 C5 C5 B4 B4 A4"),
        ),
        Melody(
            "Freude schöner Götterfunken",
            StaffNote.parseAll("B4 B4 C5 D5 D5 C5 B4 A4 G4 G4 A4 B4 B4 A4 A4 B4 B4 C5 D5 D5 C5 B4 A4 G4 G4 A4 B4 A4 G4 G4"),
        ),
    )
}

object SheetQuiz {
    const val LINE_LENGTH = 8

    /** Mostly steps of one or two, now and then a small leap, never the same note twice in a row. */
    fun randomLine(range: StaffRange, random: Random, length: Int = LINE_LENGTH): List<StaffNote> {
        var position = random.nextInt(range.low, range.high + 1)
        val line = mutableListOf(StaffNote.fromPosition(position))
        while (line.size < length) {
            val size = if (random.nextInt(10) < 8) random.nextInt(1, 3) else random.nextInt(3, 5)
            var next = position + if (random.nextBoolean()) size else -size
            if (next < range.low || next > range.high) next = position - (next - position)
            position = next.coerceIn(range.low, range.high)
            line += StaffNote.fromPosition(position)
        }
        return line
    }

    fun nextMelody(random: Random, previous: Melody? = null): Melody = Melodies.ALL.filter { it != previous }.random(random)
}
