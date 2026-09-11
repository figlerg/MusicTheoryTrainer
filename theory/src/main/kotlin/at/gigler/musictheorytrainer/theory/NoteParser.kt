package at.gigler.musictheorytrainer.theory

/**
 * A parsed note plus whether the way it was written is unusual for the notation, e.g. "Hes" or
 * "Aes" in German (usual: "B", "As") or "H" in English (usual: "B"). Sign variants like "C#" in
 * German mode are just typing convenience and do not count as irregular.
 */
data class ParsedNote(val note: SpelledNote, val irregular: Boolean)

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

    fun parse(input: String, notation: Notation): SpelledNote? = parseDetailed(input, notation)?.note

    fun parsePitch(input: String, notation: Notation): PitchClass? = parse(input, notation)?.pitchClass

    fun parseDetailed(input: String, notation: Notation): ParsedNote? {
        val s = input
            .filterNot { it.isWhitespace() }
            .replace("𝄪", "x")
            .replace("𝄫", "bb")
            .replace('♯', '#')
            .replace('♭', 'b')
            .lowercase()
        if (s.isEmpty()) return null

        val first = s[0]
        val rest = s.substring(1)
        val letter = when (first) {
            'c' -> Letter.C
            'd' -> Letter.D
            'e' -> Letter.E
            'f' -> Letter.F
            'g' -> Letter.G
            'a' -> Letter.A
            'h' -> Letter.B
            'b' -> if (notation == Notation.GERMAN && rest.isEmpty()) {
                return ParsedNote(SpelledNote(Letter.B, -1), irregular = false)
            } else {
                Letter.B
            }
            else -> return null
        }

        val alteration = parseAccidentals(rest, allowShortS = letter == Letter.A || letter == Letter.E)
            ?: return null
        val irregular = when {
            first == 'h' -> notation == Notation.ENGLISH || alteration < 0 // "Hes" instead of "B"
            first == 'b' -> notation == Notation.GERMAN // "Bb", "Bes" instead of "B"
            letter == Letter.A || letter == Letter.E -> rest.startsWith("es") // "Aes" instead of "As"
            else -> false
        }
        return ParsedNote(SpelledNote(letter, alteration), irregular)
    }

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
