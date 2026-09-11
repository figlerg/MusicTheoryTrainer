package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerTest {

    private fun german(input: String, expected: PitchClass) = Answer.check(input, expected, Notation.GERMAN)
    private fun english(input: String, expected: PitchClass) = Answer.check(input, expected, Notation.ENGLISH)

    @Test
    fun `usual enharmonic names are correct`() {
        val cis = PitchClass.of(1)
        val ais = PitchClass.of(10)
        for (input in listOf("Cis", "Des", "c#", "Db", "C♯")) {
            assertEquals(input, AnswerResult.CORRECT, german(input, cis).result)
        }
        assertEquals(AnswerResult.CORRECT, german("Ais", ais).result)
        assertEquals(AnswerResult.CORRECT, german("B", ais).result)
        assertEquals(AnswerResult.CORRECT, english("Bb", ais).result)
        assertEquals(AnswerResult.CORRECT, english("Cis", cis).result)
    }

    @Test
    fun `unusual spellings are half right with the usual name`() {
        assertEquals(Judgement(AnswerResult.UNUSUAL, "E"), german("Fes", PitchClass.E))
        assertEquals(Judgement(AnswerResult.UNUSUAL, "C"), german("His", PitchClass.C))
        assertEquals(Judgement(AnswerResult.UNUSUAL, "F"), german("Eis", PitchClass.F))
        assertEquals(Judgement(AnswerResult.UNUSUAL, "Ais/B"), german("Hes", PitchClass.of(10)))
        assertEquals(Judgement(AnswerResult.UNUSUAL, "Ais/B"), german("Bb", PitchClass.of(10)))
        assertEquals(Judgement(AnswerResult.UNUSUAL, "Gis/As"), german("Aes", PitchClass.of(8)))
        assertEquals(Judgement(AnswerResult.UNUSUAL, "D"), german("Cisis", PitchClass.D))
        assertEquals(Judgement(AnswerResult.UNUSUAL, "B"), english("H", PitchClass.H))
        assertTrue(AnswerResult.UNUSUAL.isHit)
    }

    @Test
    fun `key spelling is the norm when given`() {
        val eis = SpelledNote(Letter.E, 1)
        val f = PitchClass.F
        fun judge(input: String) = Answer.judge(EnteredNote.fromText(input, Notation.GERMAN)!!, f, Notation.GERMAN, eis)
        assertEquals(Judgement(AnswerResult.CORRECT), judge("Eis"))
        assertEquals(Judgement(AnswerResult.UNUSUAL, "Eis"), judge("F"))
        val b = SpelledNote(Letter.B, -1)
        assertEquals(
            Judgement(AnswerResult.UNUSUAL, "B"),
            Answer.judge(EnteredNote.fromText("Ais", Notation.GERMAN)!!, PitchClass.of(10), Notation.GERMAN, b),
        )
    }

    @Test
    fun `keys and fretboard only know the pitch`() {
        assertEquals(AnswerResult.CORRECT, Answer.judge(EnteredNote(PitchClass.F), PitchClass.F, Notation.GERMAN, SpelledNote(Letter.E, 1)).result)
        assertEquals(AnswerResult.WRONG, Answer.judge(EnteredNote(PitchClass.E), PitchClass.F, Notation.GERMAN).result)
    }

    @Test
    fun `wrong and unreadable answers`() {
        assertEquals(AnswerResult.WRONG, german("D", PitchClass.C).result)
        assertEquals(AnswerResult.WRONG, english("B", PitchClass.of(10)).result)
        assertEquals(AnswerResult.UNREADABLE, german("xyz", PitchClass.C).result)
        assertEquals(AnswerResult.UNREADABLE, german("", PitchClass.C).result)
    }
}
