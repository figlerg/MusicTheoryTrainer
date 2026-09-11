package at.gigler.musictheorytrainer.theory

/**
 * One of the 12 pitch classes, C = 0 … H = 11. Enharmonic spellings (Cis/Des) share a pitch class,
 * which is what all answer checking compares.
 */
@JvmInline
value class PitchClass private constructor(val semitone: Int) {

    operator fun plus(semitones: Int): PitchClass = of(semitone + semitones)

    operator fun minus(semitones: Int): PitchClass = of(semitone - semitones)

    /** Distance going upwards from this pitch class to [other], in 0..11. */
    fun semitonesUpTo(other: PitchClass): Int = (other.semitone - semitone).mod(12)

    override fun toString(): String = NoteNames.name(this, Notation.GERMAN, Spelling.SHARP)

    companion object {
        fun of(semitone: Int): PitchClass = PitchClass(semitone.mod(12))

        val ALL: List<PitchClass> = (0 until 12).map(::of)

        val C = of(0)
        val D = of(2)
        val E = of(4)
        val F = of(5)
        val G = of(7)
        val A = of(9)
        val H = of(11)
    }
}
