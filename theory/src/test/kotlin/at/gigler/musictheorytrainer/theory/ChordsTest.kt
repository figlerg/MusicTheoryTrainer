package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordsTest {

    private fun major(letter: Letter, alteration: Int = 0) = Chord(SpelledNote(letter, alteration), ChordType.MAJOR)
    private fun minor(letter: Letter, alteration: Int = 0) = Chord(SpelledNote(letter, alteration), ChordType.MINOR)
    private fun tones(chord: Chord) = chord.tones.joinToString(" ") { it.name(Notation.GERMAN) }

    private val allChords = KeyQuiz.keys(KeyChoice.BOTH).map(Chord::of)

    @Test
    fun `triads are spelled with every other letter`() {
        assertEquals("C E G", tones(major(Letter.C)))
        assertEquals("A C E", tones(minor(Letter.A)))
        assertEquals("A Cis E", tones(major(Letter.A)))
        assertEquals("Es G B", tones(major(Letter.E, -1)))
        assertEquals("As C Es", tones(major(Letter.A, -1)))
        assertEquals("Es Ges B", tones(minor(Letter.E, -1)))
        assertEquals("H Dis Fis", tones(major(Letter.B)))
        assertEquals("Fis Ais Cis", tones(major(Letter.F, 1)))
        assertEquals("B Des F", tones(minor(Letter.B, -1)))
        assertEquals("Gis H Dis", tones(minor(Letter.G, 1)))
    }

    @Test
    fun `third and fifth intervals`() {
        for (chord in allChords) {
            val (root, third, fifth) = chord.tones.map { it.pitchClass }
            assertEquals(if (chord.type == ChordType.MAJOR) 4 else 3, root.semitonesUpTo(third))
            assertEquals(7, root.semitonesUpTo(fifth))
        }
    }

    @Test
    fun `labels`() {
        assertEquals("a-Moll (Am)", minor(Letter.A).label(Notation.GERMAN))
        assertEquals("h-Moll (Hm)", minor(Letter.B).label(Notation.GERMAN))
        assertEquals("B-Dur (B)", major(Letter.B, -1).label(Notation.GERMAN))
        assertEquals("Bb-Dur (Bb)", major(Letter.B, -1).label(Notation.ENGLISH))
        assertEquals("Fis-Dur (Fis)", major(Letter.F, 1).label(Notation.GERMAN))
    }

    @Test
    fun `standard grips are valid for all 24 chords`() {
        for (chord in allChords) {
            val grip = Grips.standard(chord)
            assertEquals("${chord.label(Notation.GERMAN)} ${grip.shorthand()}", emptyList<GripProblem>(), Grips.check(grip, chord, rootInBass = true))
            assertEquals(chord.pitches, grip.sounding.map(Guitar::pitchAt).toSet())
        }
    }

    @Test
    fun `known standard grips`() {
        assertEquals("x32010", Grips.standard(major(Letter.C)).shorthand())
        assertEquals("x02210", Grips.standard(minor(Letter.A)).shorthand())
        assertEquals("133211", Grips.standard(major(Letter.F)).shorthand())
        assertEquals("x13331", Grips.standard(major(Letter.B, -1)).shorthand())
        assertEquals("x24432", Grips.standard(minor(Letter.B)).shorthand())
        assertEquals("244222", Grips.standard(minor(Letter.F, 1)).shorthand())
        assertEquals("x68886", Grips.standard(major(Letter.E, -1)).shorthand())
        assertEquals("x46654", Grips.standard(minor(Letter.C, 1)).shorthand())
    }

    @Test
    fun `wrong grip lists foreign and missing notes`() {
        val problems = Grips.check(Grip.parse("x02220"), minor(Letter.A), rootInBass = true)
        assertEquals(
            listOf(GripProblem.ForeignNote(FretPosition(4, 2)), GripProblem.MissingTone(SpelledNote(Letter.C))),
            problems,
        )
    }

    @Test
    fun `span and bass rules`() {
        val c = major(Letter.C)
        assertEquals(listOf(GripProblem.TooWide(7)), Grips.check(Grip.parse("x-3-x-9-8-x"), c, rootInBass = true))
        assertTrue(Grips.check(Grip.parse("8x5x5x"), c, rootInBass = true).isEmpty())
        assertEquals(listOf(GripProblem.RootNotInBass(PitchClass.E)), Grips.check(Grip.parse("032010"), c, rootInBass = true))
        assertTrue(Grips.check(Grip.parse("032010"), c, rootInBass = false).isEmpty())
        assertEquals(3, Grips.check(Grip.EMPTY, c, rootInBass = true).size)
    }

    @Test
    fun `shorthand round trip`() {
        assertEquals("x32010", Grip.parse("x32010").shorthand())
        assertEquals("x-13-15-15-15-13", Grip(listOf(null, 13, 15, 15, 15, 13)).shorthand())
        assertEquals(Grip(listOf(null, 13, 15, 15, 15, 13)), Grip.parse("x-13-15-15-15-13"))
    }
}
