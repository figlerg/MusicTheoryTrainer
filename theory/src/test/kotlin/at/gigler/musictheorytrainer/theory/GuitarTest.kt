package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GuitarTest {

    @Test
    fun `standard tuning`() {
        assertEquals(
            listOf("E", "A", "D", "G", "H", "E"),
            Guitar.OPEN_STRINGS.map { NoteNames.name(it, Notation.GERMAN, Spelling.SHARP) },
        )
        val gaps = Guitar.OPEN_STRINGS.zipWithNext { low, high -> low.semitonesUpTo(high) }
        assertEquals(listOf(5, 5, 5, 4, 5), gaps)
    }

    @Test
    fun `midi numbers match the tuning`() {
        assertEquals(Guitar.OPEN_STRINGS, Guitar.OPEN_STRING_MIDI.map { PitchClass.of(it) })
        assertEquals(Guitar.STRING_INTERVALS, Guitar.OPEN_STRING_MIDI.zipWithNext { low, high -> high - low })
        assertEquals(57, Guitar.midiAt(FretPosition(1, 12))) // A3
        assertEquals(69, Guitar.midiAt(FretPosition(5, 5))) // A4 = 440 Hz
    }

    @Test
    fun `string names`() {
        assertEquals("E A D G H e", Guitar.ALL_STRINGS.joinToString(" ") { Guitar.stringName(it, Notation.GERMAN) })
        assertEquals("E A D G B e", Guitar.ALL_STRINGS.joinToString(" ") { Guitar.stringName(it, Notation.ENGLISH) })
    }

    @Test
    fun `pitch at fret`() {
        assertEquals(PitchClass.A, Guitar.pitchAt(FretPosition(0, 5)))
        assertEquals(PitchClass.C, Guitar.pitchAt(FretPosition(1, 3)))
        assertEquals(PitchClass.C, Guitar.pitchAt(FretPosition(4, 1)))
        assertEquals(PitchClass.H, Guitar.pitchAt(FretPosition(3, 4)))
        assertEquals(PitchClass.E, Guitar.pitchAt(FretPosition(5, 12)))
    }

    @Test
    fun `positions of a note`() {
        assertEquals(
            listOf(FretPosition(0, 8), FretPosition(1, 3)),
            Guitar.positionsOf(PitchClass.C, listOf(1, 0)),
        )
        assertEquals(listOf(FretPosition(0, 0), FretPosition(0, 12)), Guitar.positionsOf(PitchClass.E, listOf(0)))
        // Open and 12th fret on both E strings, plus one spot on each of the other four.
        assertEquals(8, Guitar.positionsOf(PitchClass.E, Guitar.ALL_STRINGS).size)
    }

    @Test
    fun `major scale on one string`() {
        assertEquals(
            listOf(0, 2, 4, 5, 7, 9, 11, 12),
            Guitar.majorScaleOnString(PitchClass.E, 0).map { it.fret },
        )
        assertEquals(
            listOf(3, 5, 7, 8, 10, 12, 14, 15),
            Guitar.majorScaleOnString(PitchClass.C, 1).map { it.fret },
        )
    }

    @Test
    fun `lowest root string`() {
        assertEquals(1, Guitar.lowestRootString(PitchClass.D, listOf(0, 1)))
        assertEquals(2, Guitar.lowestRootString(PitchClass.D, Guitar.ALL_STRINGS))
        assertEquals(0, Guitar.lowestRootString(PitchClass.E, Guitar.ALL_STRINGS))
        assertEquals(3, Guitar.lowestRootString(PitchClass.G, Guitar.ALL_STRINGS))
    }

    @Test
    fun `random positions stay on the chosen strings and never repeat`() {
        val random = Random(7)
        var previous: FretPosition? = null
        repeat(500) {
            val position = FretboardQuiz.randomPosition(listOf(0, 1), random, previous)
            assertTrue(position.string in 0..1)
            assertTrue(position.fret in 0..Guitar.MAX_FRET)
            assertNotEquals(previous, position)
            previous = position
        }
    }
}
