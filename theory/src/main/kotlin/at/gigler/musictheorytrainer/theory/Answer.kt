package at.gigler.musictheorytrainer.theory

/**
 * One answer as entered by the user. Text input carries its spelling; keys and fretboard only
 * know the pitch (and the fretboard also the position, for exact-octave checks).
 */
data class EnteredNote(
    val pitch: PitchClass,
    val spelled: SpelledNote? = null,
    val irregular: Boolean = false,
    val position: FretPosition? = null,
) {
    companion object {
        fun fromText(input: String, notation: Notation): EnteredNote? =
            NoteParser.parseDetailed(input, notation)?.let { EnteredNote(it.note.pitchClass, it.note, it.irregular) }
    }
}

/** [UNUSUAL] is the right note in an unusual spelling (Fes for E); it counts as a hit. */
enum class AnswerResult {
    CORRECT, UNUSUAL, WRONG, UNREADABLE;

    val isHit: Boolean get() = this == CORRECT || this == UNUSUAL
}

/** [usualName] is set for [AnswerResult.UNUSUAL]: the spelling the user should learn. */
data class Judgement(val result: AnswerResult, val usualName: String? = null)

object Answer {

    /** The twelve spellings from the name tables: naturals, Cis Dis Fis Gis Ais and Des Es Ges As B. */
    private val USUAL_SPELLINGS: Set<SpelledNote> =
        PitchClass.ALL.flatMap { pitch -> Spelling.entries.map { SpelledNote.of(pitch, it) } }.toSet()

    fun isUsual(note: SpelledNote): Boolean = note in USUAL_SPELLINGS

    /**
     * Enharmonic spellings are right. With [expectedSpelling] (scales, chords) the spelling that
     * belongs to the key is the norm, e.g. Eis in Fis-Dur; otherwise any of the twelve usual names.
     */
    fun judge(
        entered: EnteredNote,
        expected: PitchClass,
        notation: Notation,
        expectedSpelling: SpelledNote? = null,
    ): Judgement {
        if (entered.pitch != expected) return Judgement(AnswerResult.WRONG)
        val spelled = entered.spelled ?: return Judgement(AnswerResult.CORRECT)
        val usual = if (expectedSpelling != null) spelled == expectedSpelling else isUsual(spelled)
        return when {
            usual && !entered.irregular -> Judgement(AnswerResult.CORRECT)
            expectedSpelling != null -> Judgement(AnswerResult.UNUSUAL, expectedSpelling.name(notation))
            else -> Judgement(AnswerResult.UNUSUAL, NoteNames.bothNames(expected, notation))
        }
    }

    fun check(input: String, expected: PitchClass, notation: Notation): Judgement {
        val entered = EnteredNote.fromText(input, notation) ?: return Judgement(AnswerResult.UNREADABLE)
        return judge(entered, expected, notation)
    }
}
