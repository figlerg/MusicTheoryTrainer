package at.gigler.musictheorytrainer.theory

/** Stammton. [B] is the German H. */
enum class Letter(val naturalSemitone: Int) {
    C(0), D(2), E(4), F(5), G(7), A(9), B(11);

    fun next(steps: Int = 1): Letter = entries[(ordinal + steps).mod(entries.size)]
}

/**
 * A note with a concrete spelling, e.g. Fis-Dur needs "Eis" rather than "F".
 * [alteration] counts sharps (> 0) or flats (< 0).
 */
data class SpelledNote(val letter: Letter, val alteration: Int = 0) {

    val pitchClass: PitchClass get() = PitchClass.of(letter.naturalSemitone + alteration)

    fun name(notation: Notation): String = when (notation) {
        Notation.GERMAN -> germanName()
        Notation.ENGLISH -> englishName()
    }

    private fun germanName(): String {
        val base = if (letter == Letter.B) "H" else letter.name
        return when {
            alteration == 0 -> base
            alteration > 0 -> base + "is".repeat(alteration)
            letter == Letter.B && alteration == -1 -> "B"
            letter == Letter.E || letter == Letter.A -> base + "s" + "es".repeat(-alteration - 1)
            else -> base + "es".repeat(-alteration)
        }
    }

    private fun englishName(): String =
        letter.name + if (alteration >= 0) "#".repeat(alteration) else "b".repeat(-alteration)

    override fun toString(): String = germanName()

    companion object {
        /** Spelling of a pitch class following the plain 12-tone name tables (Cis vs. Des). */
        fun of(pitch: PitchClass, spelling: Spelling): SpelledNote {
            val natural = Letter.entries.firstOrNull { it.naturalSemitone == pitch.semitone }
            if (natural != null) return SpelledNote(natural)
            return if (spelling == Spelling.SHARP) {
                SpelledNote(Letter.entries.first { it.naturalSemitone == pitch.semitone - 1 }, 1)
            } else {
                SpelledNote(Letter.entries.first { it.naturalSemitone == pitch.semitone + 1 }, -1)
            }
        }
    }
}
