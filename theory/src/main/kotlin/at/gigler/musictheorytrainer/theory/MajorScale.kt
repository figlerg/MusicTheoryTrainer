package at.gigler.musictheorytrainer.theory

enum class Step(val semitones: Int, val label: String) {
    HALF(1, "Halbton"),
    WHOLE(2, "Ganzton"),
}

object MajorScale {
    /** Ganz, Ganz, Halb, Ganz, Ganz, Ganz, Halb. */
    val STEPS: List<Step> = listOf(
        Step.WHOLE, Step.WHOLE, Step.HALF, Step.WHOLE, Step.WHOLE, Step.WHOLE, Step.HALF,
    )

    /** Semitones above the root for degrees 1..8 (8 = octave): 0 2 4 5 7 9 11 12. */
    val OFFSETS: List<Int> = STEPS.runningFold(0) { total, step -> total + step.semitones }

    /** The twelve usual major keys, one per pitch class. */
    val ROOTS: List<SpelledNote> = listOf(
        SpelledNote(Letter.C),
        SpelledNote(Letter.D, -1),
        SpelledNote(Letter.D),
        SpelledNote(Letter.E, -1),
        SpelledNote(Letter.E),
        SpelledNote(Letter.F),
        SpelledNote(Letter.F, 1),
        SpelledNote(Letter.G),
        SpelledNote(Letter.A, -1),
        SpelledNote(Letter.A),
        SpelledNote(Letter.B, -1),
        SpelledNote(Letter.B),
    )

    fun pitchClasses(root: PitchClass): List<PitchClass> = OFFSETS.take(7).map { root + it }

    /** The 7 scale notes, each letter exactly once (F-Dur has B, not Ais). */
    fun spell(root: SpelledNote): List<SpelledNote> = (0 until 7).map { degree ->
        val letter = root.letter.next(degree)
        val target = root.pitchClass + OFFSETS[degree]
        val raw = (target.semitone - letter.naturalSemitone).mod(12)
        SpelledNote(letter, if (raw > 6) raw - 12 else raw)
    }
}

/**
 * One scale-building round. The root is given; the user enters degrees 2..7 and then the octave,
 * so the 7 inputs cover all 7 scale notes and each input tests exactly one step.
 */
class ScaleDrill(val root: SpelledNote) {

    val scale: List<SpelledNote> = MajorScale.spell(root)

    /** Expected note for input 0..6. */
    val targets: List<SpelledNote> = scale.drop(1) + root

    /** Checks input [index] against the correct previous note, so one mistake does not cascade. */
    fun check(index: Int, given: PitchClass): StepCheck {
        val from = if (index == 0) root else targets[index - 1]
        return StepCheck(index, from, targets[index], given, MajorScale.STEPS[index])
    }
}

data class StepCheck(
    val index: Int,
    val from: SpelledNote,
    val expected: SpelledNote,
    val given: PitchClass,
    val expectedStep: Step,
) {
    val correct: Boolean get() = given == expected.pitchClass

    val givenSemitones: Int get() = from.pitchClass.semitonesUpTo(given)

    /** Scale degrees of this step, 1-based, the octave is 8. */
    val fromDegree: Int get() = index + 1
    val toDegree: Int get() = index + 2
}
