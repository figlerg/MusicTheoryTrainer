package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerTest {

    @Test
    fun `enharmonic spellings are correct`() {
        val cis = PitchClass.of(1)
        val ais = PitchClass.of(10)
        assertEquals(AnswerResult.CORRECT, Answer.check("Cis", cis, Notation.GERMAN))
        assertEquals(AnswerResult.CORRECT, Answer.check("Des", cis, Notation.GERMAN))
        assertEquals(AnswerResult.CORRECT, Answer.check("Ais", ais, Notation.GERMAN))
        assertEquals(AnswerResult.CORRECT, Answer.check("B", ais, Notation.GERMAN))
        assertEquals(AnswerResult.CORRECT, Answer.check("His", PitchClass.C, Notation.GERMAN))
        assertEquals(AnswerResult.CORRECT, Answer.check("Fes", PitchClass.E, Notation.GERMAN))
    }

    @Test
    fun `wrong and unreadable answers`() {
        assertEquals(AnswerResult.WRONG, Answer.check("D", PitchClass.C, Notation.GERMAN))
        assertEquals(AnswerResult.WRONG, Answer.check("B", PitchClass.of(10), Notation.ENGLISH))
        assertEquals(AnswerResult.UNREADABLE, Answer.check("xyz", PitchClass.C, Notation.GERMAN))
        assertEquals(AnswerResult.UNREADABLE, Answer.check("", PitchClass.C, Notation.GERMAN))
    }
}
