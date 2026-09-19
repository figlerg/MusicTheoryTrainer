package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EarTest {

    @Test
    fun `interval labels match their semitones`() {
        assertEquals(1, EarInterval.MINOR_SECOND.semitones)
        assertEquals(7, EarInterval.FIFTH.semitones)
        assertEquals(12, EarInterval.OCTAVE.semitones)
        assertEquals(12, EarInterval.entries.size)
        assertEquals((1..12).toList(), EarInterval.entries.map { it.semitones })
    }

    @Test
    fun `interval sets grow from four to all twelve`() {
        assertEquals(4, EarIntervalSet.EASY.intervals.size)
        assertEquals(11, EarIntervalSet.COMMON.intervals.size)
        assertEquals(12, EarIntervalSet.ALL.intervals.size)
        assertTrue(EarInterval.TRITONE !in EarIntervalSet.COMMON.intervals)
    }

    @Test
    fun `triads are built from their semitones`() {
        assertEquals(listOf(60, 64, 67), EarQuiz.chordNotes(60, EarChord.MAJOR))
        assertEquals(listOf(60, 63, 67), EarQuiz.chordNotes(60, EarChord.MINOR))
        assertEquals(listOf(60, 63, 66), EarQuiz.chordNotes(60, EarChord.DIMINISHED))
        assertEquals(listOf(60, 64, 68), EarQuiz.chordNotes(60, EarChord.AUGMENTED))
    }

    @Test
    fun `an interval is played from its root upwards`() {
        assertEquals(listOf(55, 62), EarQuiz.intervalNotes(55, EarInterval.FIFTH))
    }

    @Test
    fun `questions never repeat the previous one and stay in range`() {
        val random = Random(3)
        var previous: EarInterval? = null
        repeat(200) {
            val next = EarQuiz.next(EarIntervalSet.ALL.intervals, random, previous)
            assertNotEquals(previous, next)
            previous = next
            val root = EarQuiz.randomRoot(random)
            assertTrue(root in EarQuiz.LOWEST_ROOT..EarQuiz.HIGHEST_ROOT)
        }
    }

    @Test
    fun `a pool of one still answers`() {
        val only = listOf(EarChord.MAJOR)
        assertEquals(EarChord.MAJOR, EarQuiz.next(only, Random(1), previous = EarChord.MAJOR))
    }
}
