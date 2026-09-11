package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MajorScaleTest {

    @Test
    fun `step pattern and offsets`() {
        assertEquals("GGHGGGH", MajorScale.STEPS.joinToString("") { if (it == Step.WHOLE) "G" else "H" })
        assertEquals(listOf(0, 2, 4, 5, 7, 9, 11, 12), MajorScale.OFFSETS)
    }

    @Test
    fun `c major pitch classes`() {
        assertEquals(
            listOf(PitchClass.C, PitchClass.D, PitchClass.E, PitchClass.F, PitchClass.G, PitchClass.A, PitchClass.H),
            MajorScale.pitchClasses(PitchClass.C),
        )
    }

    @Test
    fun `all twelve keys are spelled correctly`() {
        val expected = mapOf(
            "C" to "C D E F G A H",
            "Des" to "Des Es F Ges As B C",
            "D" to "D E Fis G A H Cis",
            "Es" to "Es F G As B C D",
            "E" to "E Fis Gis A H Cis Dis",
            "F" to "F G A B C D E",
            "Fis" to "Fis Gis Ais H Cis Dis Eis",
            "G" to "G A H C D E Fis",
            "As" to "As B C Des Es F G",
            "A" to "A H Cis D E Fis Gis",
            "B" to "B C D Es F G A",
            "H" to "H Cis Dis E Fis Gis Ais",
        )
        assertEquals(expected.keys.toList(), MajorScale.ROOTS.map { it.name(Notation.GERMAN) })
        for (root in MajorScale.ROOTS) {
            val spelled = MajorScale.spell(root).joinToString(" ") { it.name(Notation.GERMAN) }
            assertEquals(expected.getValue(root.name(Notation.GERMAN)), spelled)
        }
    }

    @Test
    fun `english spelling`() {
        assertEquals(
            "F G A Bb C D E",
            MajorScale.spell(SpelledNote(Letter.F)).joinToString(" ") { it.name(Notation.ENGLISH) },
        )
    }

    @Test
    fun `every key uses each letter once and follows the step pattern`() {
        for (root in MajorScale.ROOTS) {
            val scale = MajorScale.spell(root)
            assertEquals(Letter.entries.toSet(), scale.map { it.letter }.toSet())
            assertEquals(MajorScale.pitchClasses(root.pitchClass), scale.map { it.pitchClass })
        }
    }

    @Test
    fun `roots cover all pitch classes`() {
        assertEquals(PitchClass.ALL.toSet(), MajorScale.ROOTS.map { it.pitchClass }.toSet())
    }

    @Test
    fun `drill targets are degrees two to octave`() {
        val drill = ScaleDrill(SpelledNote(Letter.C))
        assertEquals("D E F G A H C", drill.targets.joinToString(" ") { it.name(Notation.GERMAN) })
    }

    @Test
    fun `drill reports the wrong step`() {
        val drill = ScaleDrill(SpelledNote(Letter.C))

        val wrong = drill.check(2, PitchClass.of(6)) // Fis instead of F
        assertFalse(wrong.correct)
        assertEquals(Step.HALF, wrong.expectedStep)
        assertEquals(2, wrong.givenSemitones)
        assertEquals(3, wrong.fromDegree)
        assertEquals(4, wrong.toDegree)
        assertEquals(SpelledNote(Letter.E), wrong.from)

        val tooSmall = drill.check(0, PitchClass.of(1)) // Cis instead of D
        assertFalse(tooSmall.correct)
        assertEquals(Step.WHOLE, tooSmall.expectedStep)
        assertEquals(1, tooSmall.givenSemitones)

        val octave = drill.check(6, PitchClass.C)
        assertTrue(octave.correct)
        assertEquals(Step.HALF, octave.expectedStep)
        assertEquals(8, octave.toDegree)
    }

    @Test
    fun `drill accepts enharmonic input`() {
        val drill = ScaleDrill(SpelledNote(Letter.F))
        assertTrue(drill.check(2, NoteParser.parsePitch("Ais", Notation.GERMAN)!!).correct)
    }
}
