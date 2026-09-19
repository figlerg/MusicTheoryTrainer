package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StaffTest {

    private fun note(text: String) = StaffNote.parseAll(text).single()

    @Test
    fun `staff positions`() {
        assertEquals(0, note("E4").staffPosition)
        assertEquals(1, note("F4").staffPosition)
        assertEquals(8, note("F5").staffPosition)
        assertEquals(-1, note("D4").staffPosition)
        assertEquals(9, note("G5").staffPosition)
        assertEquals(-7, note("E3").staffPosition)
        for (position in -10..14) assertEquals(position, StaffNote.fromPosition(position).staffPosition)
    }

    @Test
    fun `ledger lines`() {
        assertEquals(emptyList<Int>(), note("D4").ledgerLines())
        assertEquals(emptyList<Int>(), note("G5").ledgerLines())
        assertEquals(listOf(-2), note("C4").ledgerLines())
        assertEquals(listOf(-2), note("B3").ledgerLines())
        assertEquals(listOf(-2, -4, -6), note("E3").ledgerLines())
        assertEquals(listOf(10), note("A5").ledgerLines())
    }

    @Test
    fun `guitar sounds an octave lower`() {
        assertEquals(64, note("E4").writtenMidi)
        assertEquals(Guitar.midiAt(FretPosition(2, 2)), note("E4").guitarMidi)
        assertEquals(Guitar.midiAt(FretPosition(0, 0)), note("E3").guitarMidi)
        assertEquals(Guitar.midiAt(FretPosition(5, 5)), note("A5").guitarMidi)
    }

    @Test
    fun `every note of the guitar range is playable in frets 0 to 5`() {
        val first = (0 until Guitar.STRING_COUNT).flatMap { s -> (0..5).map { Guitar.midiAt(FretPosition(s, it)) } }.toSet()
        for (position in StaffRange.FIRST_POSITION.low..StaffRange.FIRST_POSITION.high) {
            assertTrue(StaffNote.fromPosition(position).guitarMidi in first)
        }
    }

    @Test
    fun `melodies stay in the staff and need no key signature`() {
        for (melody in Melodies.ALL) {
            for (n in melody.notes) {
                assertTrue(melody.title, n.staffPosition in StaffRange.IN_STAFF.low..StaffRange.IN_STAFF.high)
                assertTrue(melody.title, n.letter != Letter.F)
            }
        }
    }

    @Test
    fun `melodies have a length per note and fill whole 4-4 bars`() {
        for (melody in Melodies.ALL) {
            assertEquals(melody.title, melody.notes.size, melody.beats.size)
            assertEquals(melody.title, 0.0, melody.beats.sum() % 4, 1e-9)
            assertTrue(melody.title, melody.beats.all { it > 0 })
        }
        val alle = Melodies.ALL.first()
        assertEquals(listOf(1.0, 1.0, 1.0, 1.0, 2.0, 2.0), alle.beats.take(6))
        assertEquals(60.0 / 112, alle.seconds.first(), 1e-9)
    }

    @Test
    fun `random lines stay in range and never repeat a note`() {
        val random = Random(11)
        for (range in StaffRange.entries) {
            repeat(200) {
                val line = SheetQuiz.randomLine(range, random)
                assertEquals(SheetQuiz.LINE_LENGTH, line.size)
                assertTrue(line.all { it.staffPosition in range.low..range.high })
                line.zipWithNext { a, b -> assertNotEquals(a, b) }
            }
        }
    }
}
