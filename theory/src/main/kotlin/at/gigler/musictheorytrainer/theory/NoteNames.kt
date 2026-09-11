package at.gigler.musictheorytrainer.theory

/** German: H = B natural, B = B flat. English: B = B natural, Bb = B flat. */
enum class Notation { GERMAN, ENGLISH }

/** Which enharmonic name to use for the five black keys: Cis vs. Des. */
enum class Spelling { SHARP, FLAT }

object NoteNames {
    private val GERMAN_SHARP = listOf("C", "Cis", "D", "Dis", "E", "F", "Fis", "G", "Gis", "A", "Ais", "H")
    private val GERMAN_FLAT = listOf("C", "Des", "D", "Es", "E", "F", "Ges", "G", "As", "A", "B", "H")
    private val ENGLISH_SHARP = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val ENGLISH_FLAT = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

    fun name(pitch: PitchClass, notation: Notation, spelling: Spelling): String {
        val table = when (notation) {
            Notation.GERMAN -> if (spelling == Spelling.SHARP) GERMAN_SHARP else GERMAN_FLAT
            Notation.ENGLISH -> if (spelling == Spelling.SHARP) ENGLISH_SHARP else ENGLISH_FLAT
        }
        return table[pitch.semitone]
    }

    /** "Cis/Des" for black keys, just "C" for white keys. */
    fun bothNames(pitch: PitchClass, notation: Notation): String {
        val sharp = name(pitch, notation, Spelling.SHARP)
        val flat = name(pitch, notation, Spelling.FLAT)
        return if (sharp == flat) sharp else "$sharp/$flat"
    }

    fun isNatural(pitch: PitchClass): Boolean =
        name(pitch, Notation.GERMAN, Spelling.SHARP) == name(pitch, Notation.GERMAN, Spelling.FLAT)
}
