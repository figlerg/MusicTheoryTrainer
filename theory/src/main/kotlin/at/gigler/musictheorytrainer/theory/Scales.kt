package at.gigler.musictheorytrainer.theory

import kotlin.random.Random

enum class Step(val semitones: Int, val label: String) {
    HALF(1, "Halbton"),
    WHOLE(2, "Ganzton"),
}

enum class ScaleType(val steps: List<Step>) {
    /** Ganz, Ganz, Halb, Ganz, Ganz, Ganz, Halb. */
    MAJOR(listOf(Step.WHOLE, Step.WHOLE, Step.HALF, Step.WHOLE, Step.WHOLE, Step.WHOLE, Step.HALF)),

    /** Natural minor: Ganz, Halb, Ganz, Ganz, Halb, Ganz, Ganz. */
    MINOR(listOf(Step.WHOLE, Step.HALF, Step.WHOLE, Step.WHOLE, Step.HALF, Step.WHOLE, Step.WHOLE)),
    ;

    /** Semitones above the root for degrees 1..8 (8 = octave). */
    val offsets: List<Int> = steps.runningFold(0) { total, step -> total + step.semitones }

    val roots: List<SpelledNote>
        get() = if (this == MAJOR) Scales.MAJOR_ROOTS else Scales.MINOR_ROOTS
}

data class Key(val root: SpelledNote, val type: ScaleType) {

    val scale: List<SpelledNote> get() = Scales.spell(root, type)

    /** "F-Dur", "fis-Moll" (German writes minor keys in lower case). */
    fun name(notation: Notation): String {
        val rootName = root.name(notation)
        return when (type) {
            ScaleType.MAJOR -> "$rootName-Dur"
            ScaleType.MINOR -> (if (notation == Notation.GERMAN) rootName.lowercase() else rootName) + "-Moll"
        }
    }
}

object Scales {
    /** The twelve usual major keys, one per pitch class. */
    val MAJOR_ROOTS: List<SpelledNote> = listOf(
        SpelledNote(Letter.C), SpelledNote(Letter.D, -1), SpelledNote(Letter.D), SpelledNote(Letter.E, -1),
        SpelledNote(Letter.E), SpelledNote(Letter.F), SpelledNote(Letter.F, 1), SpelledNote(Letter.G),
        SpelledNote(Letter.A, -1), SpelledNote(Letter.A), SpelledNote(Letter.B, -1), SpelledNote(Letter.B),
    )

    /** The twelve usual minor keys: c cis d es e f fis g gis a b h. */
    val MINOR_ROOTS: List<SpelledNote> = listOf(
        SpelledNote(Letter.C), SpelledNote(Letter.C, 1), SpelledNote(Letter.D), SpelledNote(Letter.E, -1),
        SpelledNote(Letter.E), SpelledNote(Letter.F), SpelledNote(Letter.F, 1), SpelledNote(Letter.G),
        SpelledNote(Letter.G, 1), SpelledNote(Letter.A), SpelledNote(Letter.B, -1), SpelledNote(Letter.B),
    )

    /** The 7 scale notes, each letter exactly once (F-Dur has B, not Ais). */
    fun spell(root: SpelledNote, type: ScaleType): List<SpelledNote> =
        (0 until 7).map { degree -> root.above(degree, type.offsets[degree]) }

    /**
     * Keys along the circle of fifths, each root 7 semitones above the previous one:
     * C G D A E H Fis Des As Es B F, and for minor a e h fis cis gis es b f c g d.
     */
    fun circleOfFifths(type: ScaleType): List<Key> {
        val start = if (type == ScaleType.MAJOR) PitchClass.C else PitchClass.A
        return (0 until 12).map { i ->
            val pitch = start + 7 * i
            Key(type.roots.first { it.pitchClass == pitch }, type)
        }
    }
}

enum class KeyChoice { MAJOR, MINOR, BOTH }

enum class KeyOrder { CIRCLE_OF_FIFTHS, RANDOM }

object KeyQuiz {
    /** With [KeyChoice.BOTH] each major key is followed by its relative minor (same accidentals). */
    fun keys(choice: KeyChoice): List<Key> = when (choice) {
        KeyChoice.MAJOR -> Scales.circleOfFifths(ScaleType.MAJOR)
        KeyChoice.MINOR -> Scales.circleOfFifths(ScaleType.MINOR)
        KeyChoice.BOTH -> Scales.circleOfFifths(ScaleType.MAJOR)
            .zip(Scales.circleOfFifths(ScaleType.MINOR)) { major, minor -> listOf(major, minor) }
            .flatten()
    }

    fun next(choice: KeyChoice, order: KeyOrder, random: Random, previous: Key? = null): Key {
        val keys = keys(choice)
        return when (order) {
            KeyOrder.CIRCLE_OF_FIFTHS ->
                if (previous == null || previous !in keys) keys.first() else keys[(keys.indexOf(previous) + 1) % keys.size]
            KeyOrder.RANDOM -> keys.filter { it != previous }.random(random)
        }
    }
}

/** Kept for the major-only call sites and tests. */
object MajorScale {
    val STEPS: List<Step> = ScaleType.MAJOR.steps
    val OFFSETS: List<Int> = ScaleType.MAJOR.offsets
    val ROOTS: List<SpelledNote> = Scales.MAJOR_ROOTS

    fun pitchClasses(root: PitchClass): List<PitchClass> = OFFSETS.take(7).map { root + it }

    fun spell(root: SpelledNote): List<SpelledNote> = Scales.spell(root, ScaleType.MAJOR)
}

/**
 * One scale-building round. The root is given; the user enters degrees 2..7 and then the octave,
 * so the 7 inputs cover all 7 scale notes and each input tests exactly one step.
 */
class ScaleDrill(val key: Key) {

    constructor(root: SpelledNote) : this(Key(root, ScaleType.MAJOR))

    val root: SpelledNote get() = key.root

    val scale: List<SpelledNote> = key.scale

    /** Expected note for input 0..6. */
    val targets: List<SpelledNote> = scale.drop(1) + key.root

    /** Checks input [index] against the correct previous note, so one mistake does not cascade. */
    fun check(index: Int, given: PitchClass): StepCheck {
        val from = if (index == 0) key.root else targets[index - 1]
        return StepCheck(index, from, targets[index], given, key.type.steps[index])
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
