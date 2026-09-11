package at.gigler.musictheorytrainer.theory

/**
 * Tolerant note name parser: case and whitespace are ignored, and German suffixes (is/es/s),
 * ASCII signs (#, b, x) and Unicode signs (♯, ♭, 𝄪, 𝄫) are accepted in both notations.
 *
 * The only notation-dependent part is the letter B: in German a bare "B" is B flat, in English it
 * is B natural. "H" is always B natural. In German mode "B" followed by an accidental is read the
 * English way ("Bb" = B flat), since nobody types "Bb" meaning Heses.
 */
object NoteParser {

    private const val MAX_ALTERATION = 2

    fun parse(input: String, notation: Notation): SpelledNote? {
        val s = input
            .filterNot { it.isWhitespace() }
            .replace("𝄪", "x")
            .replace("𝄫", "bb")
            .replace('♯', '#')
            .replace('♭', 'b')
            .lowercase()
        if (s.isEmpty()) return null

        val rest = s.substring(1)
        val letter = when (s[0]) {
            'c' -> Letter.C
            'd' -> Letter.D
            'e' -> Letter.E
            'f' -> Letter.F
            'g' -> Letter.G
            'a' -> Letter.A
            'h' -> Letter.B
            'b' -> if (notation == Notation.GERMAN && rest.isEmpty()) {
                return SpelledNote(Letter.B, -1)
            } else {
                Letter.B
            }
            else -> return null
        }

        val alteration = parseAccidentals(rest, allowShortS = letter == Letter.A || letter == Letter.E)
            ?: return null
        return SpelledNote(letter, alteration)
    }

    fun parsePitch(input: String, notation: Notation): PitchClass? = parse(input, notation)?.pitchClass

    private fun parseAccidentals(rest: String, allowShortS: Boolean): Int? {
        var i = 0
        var sharps = 0
        var flats = 0
        // German shorthand: "As", "Es" (and "Ases", "Eses").
        if (allowShortS && rest.startsWith("s")) {
            flats++
            i = 1
        }
        while (i < rest.length) {
            when {
                rest.startsWith("is", i) -> { sharps++; i += 2 }
                rest.startsWith("es", i) -> { flats++; i += 2 }
                rest[i] == '#' -> { sharps++; i++ }
                rest[i] == 'x' -> { sharps += 2; i++ }
                rest[i] == 'b' -> { flats++; i++ }
                else -> return null
            }
        }
        if (sharps > 0 && flats > 0) return null
        val alteration = sharps - flats
        return if (alteration in -MAX_ALTERATION..MAX_ALTERATION) alteration else null
    }
}
