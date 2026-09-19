package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RhythmTest {

    @Test
    fun `note values are worth their beats`() {
        assertEquals(4.0, NoteValue.WHOLE.beats, 0.0)
        assertEquals(2.0, NoteValue.HALF.beats, 0.0)
        assertEquals(1.0, NoteValue.QUARTER.beats, 0.0)
        assertEquals(0.5, NoteValue.EIGHTH.beats, 0.0)
    }

    @Test
    fun `every line fills whole bars`() {
        val random = Random(11)
        for (level in RhythmLevel.entries) {
            for (bars in 1..4) {
                val line = RhythmQuiz.line(StaffRange.IN_STAFF, level, bars, random, restChance = 0.3)
                assertEquals(
                    "$level, $bars bars",
                    (bars * RhythmQuiz.BEATS_PER_BAR).toDouble(),
                    RhythmQuiz.totalBeats(line),
                    1e-9,
                )
                assertTrue(line.all { it.value in level.values })
            }
        }
    }

    @Test
    fun `without rests every event is a note`() {
        val line = RhythmQuiz.line(StaffRange.FIRST_POSITION, RhythmLevel.SIMPLE, bars = 4, random = Random(2))
        assertTrue(line.none { it.isRest })
        assertTrue(line.all { it.note!!.staffPosition in StaffRange.FIRST_POSITION.low..StaffRange.FIRST_POSITION.high })
    }

    @Test
    fun `rests never start a line and never follow each other`() {
        val random = Random(7)
        repeat(30) {
            val line = RhythmQuiz.line(StaffRange.IN_STAFF, RhythmLevel.EIGHTHS, bars = 4, random = random, restChance = 0.9)
            assertFalse(line.first().isRest)
            assertTrue(line.zipWithNext().none { (a, b) -> a.isRest && b.isRest })
        }
    }

    @Test
    fun `events fall on their beat`() {
        val line = listOf(
            RhythmNote(StaffNote(Letter.G, 4), NoteValue.HALF),
            RhythmNote(null, NoteValue.QUARTER),
            RhythmNote(StaffNote(Letter.A, 4), NoteValue.EIGHTH),
            RhythmNote(StaffNote(Letter.B, 4), NoteValue.EIGHTH),
        )
        assertEquals(listOf(0.0, 2.0, 3.0, 3.5), RhythmQuiz.beatOffsets(line))
        assertEquals(4.0, RhythmQuiz.totalBeats(line), 1e-9)
    }

    @Test
    fun `timing is graded in steps and every step still scores`() {
        assertEquals(Timing.PERFECT, Timing.of(0))
        assertEquals(Timing.PERFECT, Timing.of(-90))
        assertEquals(Timing.GOOD, Timing.of(91))
        assertEquals(Timing.GOOD, Timing.of(-180))
        assertEquals(Timing.LOOSE, Timing.of(181))
        assertEquals(Timing.LOOSE, Timing.of(320))
        assertEquals(Timing.MISSED, Timing.of(321))
        assertEquals(listOf(3, 2, 1, 0), Timing.entries.map { it.points })
        assertEquals(Timing.LOOSE.toleranceMillis, Timing.LAST_CHANCE_MILLIS)
    }
}
