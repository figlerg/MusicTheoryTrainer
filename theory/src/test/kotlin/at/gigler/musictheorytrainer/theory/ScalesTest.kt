package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.random.Random

class ScalesTest {

    private fun names(notes: List<SpelledNote>) = notes.joinToString(" ") { it.name(Notation.GERMAN) }

    @Test
    fun `minor step pattern`() {
        assertEquals(listOf(0, 2, 3, 5, 7, 8, 10, 12), ScaleType.MINOR.offsets)
    }

    @Test
    fun `all twelve minor keys are spelled correctly`() {
        val expected = mapOf(
            "C" to "C D Es F G As B",
            "Cis" to "Cis Dis E Fis Gis A H",
            "D" to "D E F G A B C",
            "Es" to "Es F Ges As B Ces Des",
            "E" to "E Fis G A H C D",
            "F" to "F G As B C Des Es",
            "Fis" to "Fis Gis A H Cis D E",
            "G" to "G A B C D Es F",
            "Gis" to "Gis Ais H Cis Dis E Fis",
            "A" to "A H C D E F G",
            "B" to "B C Des Es F Ges As",
            "H" to "H Cis D E Fis G A",
        )
        assertEquals(expected.keys.toList(), Scales.MINOR_ROOTS.map { it.name(Notation.GERMAN) })
        for (root in Scales.MINOR_ROOTS) {
            assertEquals(expected.getValue(root.name(Notation.GERMAN)), names(Scales.spell(root, ScaleType.MINOR)))
        }
    }

    @Test
    fun `key names`() {
        assertEquals("F-Dur", Key(SpelledNote(Letter.F), ScaleType.MAJOR).name(Notation.GERMAN))
        assertEquals("a-Moll", Key(SpelledNote(Letter.A), ScaleType.MINOR).name(Notation.GERMAN))
        assertEquals("fis-Moll", Key(SpelledNote(Letter.F, 1), ScaleType.MINOR).name(Notation.GERMAN))
        assertEquals("b-Moll", Key(SpelledNote(Letter.B, -1), ScaleType.MINOR).name(Notation.GERMAN))
        assertEquals("Bb-Moll", Key(SpelledNote(Letter.B, -1), ScaleType.MINOR).name(Notation.ENGLISH))
    }

    @Test
    fun `circle of fifths`() {
        assertEquals(
            "C G D A E H Fis Des As Es B F",
            names(Scales.circleOfFifths(ScaleType.MAJOR).map { it.root }),
        )
        assertEquals(
            "A E H Fis Cis Gis Es B F C G D",
            names(Scales.circleOfFifths(ScaleType.MINOR).map { it.root }),
        )
    }

    @Test
    fun `mixed keys pair each major key with its relative minor`() {
        val keys = KeyQuiz.keys(KeyChoice.BOTH)
        assertEquals(24, keys.size)
        assertEquals(
            "C-Dur a-Moll G-Dur e-Moll D-Dur h-Moll",
            keys.take(6).joinToString(" ") { it.name(Notation.GERMAN) },
        )
    }

    @Test
    fun `circle order walks through and wraps around`() {
        val random = Random(1)
        var key = KeyQuiz.next(KeyChoice.MAJOR, KeyOrder.CIRCLE_OF_FIFTHS, random)
        assertEquals("C-Dur", key.name(Notation.GERMAN))
        key = KeyQuiz.next(KeyChoice.MAJOR, KeyOrder.CIRCLE_OF_FIFTHS, random, key)
        assertEquals("G-Dur", key.name(Notation.GERMAN))
        val last = Scales.circleOfFifths(ScaleType.MAJOR).last()
        assertEquals("C-Dur", KeyQuiz.next(KeyChoice.MAJOR, KeyOrder.CIRCLE_OF_FIFTHS, random, last).name(Notation.GERMAN))
        // A key from another choice restarts the circle.
        assertEquals("a-Moll", KeyQuiz.next(KeyChoice.MINOR, KeyOrder.CIRCLE_OF_FIFTHS, random, last).name(Notation.GERMAN))
    }

    @Test
    fun `random order covers all keys without repeating`() {
        val random = Random(3)
        var previous: Key? = null
        val seen = mutableSetOf<Key>()
        repeat(500) {
            val key = KeyQuiz.next(KeyChoice.BOTH, KeyOrder.RANDOM, random, previous)
            assertNotEquals(previous, key)
            seen += key
            previous = key
        }
        assertEquals(KeyQuiz.keys(KeyChoice.BOTH).toSet(), seen)
    }

    @Test
    fun `minor drill checks the minor steps`() {
        val drill = ScaleDrill(Key(SpelledNote(Letter.A), ScaleType.MINOR))
        assertEquals("H C D E F G A", names(drill.targets))
        val check = drill.check(1, PitchClass.of(1)) // Cis instead of C
        assertFalse(check.correct)
        assertEquals(Step.HALF, check.expectedStep)
        assertEquals(2, check.givenSemitones)
    }

    @Test
    fun `minor scale on a string`() {
        assertEquals(
            listOf(0, 2, 3, 5, 7, 8, 10, 12),
            Guitar.scaleOnString(PitchClass.A, 1, ScaleType.MINOR).map { it.fret },
        )
    }
}
