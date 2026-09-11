package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class IntervalTest {

    @Test
    fun `interval descriptions`() {
        assertEquals("Halbton", Intervals.describe(1))
        assertEquals("Ganzton", Intervals.describe(2))
        assertEquals("3 Halbtöne", Intervals.describe(3))
        assertEquals("11 Halbtöne", Intervals.describe(11))
    }

    @Test
    fun `question texts and answers`() {
        val up = IntervalQuestion(PitchClass.of(6), Direction.UP, 1)
        assertEquals("Fis + Halbton = ?", up.text(Notation.GERMAN))
        assertEquals("F# + Halbton = ?", up.text(Notation.ENGLISH))
        assertEquals(PitchClass.G, up.answer)

        val down = IntervalQuestion(PitchClass.D, Direction.DOWN, 2)
        assertEquals("D − Ganzton = ?", down.text(Notation.GERMAN))
        assertEquals(PitchClass.C, down.answer)

        val three = IntervalQuestion(PitchClass.A, Direction.UP, 3)
        assertEquals("A + 3 Halbtöne = ?", three.text(Notation.GERMAN))
        assertEquals(PitchClass.C, three.answer)
    }

    @Test
    fun `upwards uses sharp names and downwards flat names`() {
        assertEquals("Dis", IntervalQuestion(PitchClass.D, Direction.UP, 1).answerName(Notation.GERMAN))
        assertEquals("Es", IntervalQuestion(PitchClass.E, Direction.DOWN, 1).answerName(Notation.GERMAN))
        assertEquals("Des − Halbton = ?", IntervalQuestion(PitchClass.of(1), Direction.DOWN, 1).text(Notation.GERMAN))
        assertEquals("Ais + Ganzton = ?", IntervalQuestion(PitchClass.of(10), Direction.UP, 2).text(Notation.GERMAN))
        assertEquals("B", IntervalQuestion(PitchClass.H, Direction.DOWN, 1).answerName(Notation.GERMAN))
        assertEquals("Bb", IntervalQuestion(PitchClass.H, Direction.DOWN, 1).answerName(Notation.ENGLISH))
    }

    @Test
    fun `generator respects difficulty and never repeats`() {
        val random = Random(42)
        for (difficulty in IntervalDifficulty.entries) {
            var previous: IntervalQuestion? = null
            val seen = mutableSetOf<Int>()
            val directions = mutableSetOf<Direction>()
            repeat(500) {
                val question = IntervalQuiz.next(difficulty, random, previous)
                assertTrue(question.semitones in 1..difficulty.maxSemitones)
                assertNotEquals(previous, question)
                seen += question.semitones
                directions += question.direction
                previous = question
            }
            assertEquals((1..difficulty.maxSemitones).toSet(), seen)
            assertEquals(Direction.entries.toSet(), directions)
        }
    }
}
