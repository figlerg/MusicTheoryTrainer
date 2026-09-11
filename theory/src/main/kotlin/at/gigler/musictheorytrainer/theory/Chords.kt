package at.gigler.musictheorytrainer.theory

/** Triads: root, third (4 or 3 semitones up) and perfect fifth (7 semitones up). */
enum class ChordType(val semitones: List<Int>, val symbolSuffix: String, val scaleType: ScaleType) {
    MAJOR(listOf(0, 4, 7), "", ScaleType.MAJOR),
    MINOR(listOf(0, 3, 7), "m", ScaleType.MINOR),
    ;

    companion object {
        fun of(type: ScaleType): ChordType = if (type == ScaleType.MAJOR) MAJOR else MINOR
    }
}

data class Chord(val root: SpelledNote, val type: ChordType) {

    /** Root, third, fifth; every other letter, so Es-Dur is Es G B. */
    val tones: List<SpelledNote> = type.semitones.mapIndexed { i, semitones -> root.above(2 * i, semitones) }

    val pitches: Set<PitchClass> = tones.map { it.pitchClass }.toSet()

    /** "a-Moll (Am)", "Fis-Dur (Fis)". */
    fun label(notation: Notation): String = "${Key(root, type.scaleType).name(notation)} (${symbol(notation)})"

    fun symbol(notation: Notation): String = root.name(notation) + type.symbolSuffix

    companion object {
        fun of(key: Key): Chord = Chord(key.root, ChordType.of(key.type))

        /** Names of the chord tones for explanations. */
        val TONE_NAMES = listOf("Grundton", "Terz", "Quinte")
    }
}

/** A chord shape: per string (0 = low E) muted (null), open (0) or a fret. */
data class Grip(val frets: List<Int?>) {
    init {
        require(frets.size == Guitar.STRING_COUNT)
    }

    val sounding: List<FretPosition>
        get() = frets.mapIndexedNotNull { string, fret -> fret?.let { FretPosition(string, it) } }

    /** Chord-chart shorthand from low E to high e: "x32010", with dashes once a fret has two digits. */
    fun shorthand(): String {
        val parts = frets.map { it?.toString() ?: "x" }
        return if (parts.any { it.length > 1 }) parts.joinToString("-") else parts.joinToString("")
    }

    companion object {
        val EMPTY = Grip(List(Guitar.STRING_COUNT) { null })

        fun parse(shorthand: String): Grip {
            val parts = if ('-' in shorthand) shorthand.split('-') else shorthand.map { it.toString() }
            return Grip(parts.map { if (it == "x") null else it.toInt() })
        }
    }
}

sealed interface GripProblem {
    data class ForeignNote(val position: FretPosition) : GripProblem
    data class MissingTone(val tone: SpelledNote) : GripProblem
    data class TooWide(val frets: Int) : GripProblem
    data class RootNotInBass(val lowest: PitchClass) : GripProblem
}

object Grips {
    /** A hand spans 4 frets: the fretted notes may be at most 3 frets apart. */
    const val MAX_SPAN = 4

    /** An empty list means the grip is a playable version of [chord]. */
    fun check(grip: Grip, chord: Chord, rootInBass: Boolean): List<GripProblem> {
        val sounding = grip.sounding
        val problems = mutableListOf<GripProblem>()
        sounding.filter { Guitar.pitchAt(it) !in chord.pitches }.forEach { problems += GripProblem.ForeignNote(it) }
        val present = sounding.map(Guitar::pitchAt).toSet()
        chord.tones.filter { it.pitchClass !in present }.forEach { problems += GripProblem.MissingTone(it) }
        val fretted = sounding.map { it.fret }.filter { it > 0 }
        if (fretted.isNotEmpty()) {
            val span = fretted.max() - fretted.min() + 1
            if (span > MAX_SPAN) problems += GripProblem.TooWide(span)
        }
        val lowest = sounding.minByOrNull { Guitar.midiAt(it) }
        if (rootInBass && lowest != null && Guitar.pitchAt(lowest) != chord.root.pitchClass) {
            problems += GripProblem.RootNotInBass(Guitar.pitchAt(lowest))
        }
        return problems
    }

    /** Open chords as found in songbooks; everything else gets a barre grip. */
    private val OPEN: Map<Pair<PitchClass, ChordType>, Grip> = mapOf(
        (PitchClass.C to ChordType.MAJOR) to Grip.parse("x32010"),
        (PitchClass.A to ChordType.MAJOR) to Grip.parse("x02220"),
        (PitchClass.A to ChordType.MINOR) to Grip.parse("x02210"),
        (PitchClass.D to ChordType.MAJOR) to Grip.parse("xx0232"),
        (PitchClass.D to ChordType.MINOR) to Grip.parse("xx0231"),
        (PitchClass.E to ChordType.MAJOR) to Grip.parse("022100"),
        (PitchClass.E to ChordType.MINOR) to Grip.parse("022000"),
        (PitchClass.G to ChordType.MAJOR) to Grip.parse("320003"),
    )

    /** The songbook grip: an open chord, or the E- or A-shape barre, whichever sits lower. */
    fun standard(chord: Chord): Grip {
        OPEN[chord.root.pitchClass to chord.type]?.let { return it }
        val e = Guitar.lowestFret(chord.root.pitchClass, 0)
        val a = Guitar.lowestFret(chord.root.pitchClass, 1)
        val major = chord.type == ChordType.MAJOR
        return if (e <= a) {
            Grip(listOf(e, e + 2, e + 2, if (major) e + 1 else e, e, e))
        } else {
            Grip(listOf(null, a, a + 2, a + 2, if (major) a + 2 else a + 1, a))
        }
    }
}
