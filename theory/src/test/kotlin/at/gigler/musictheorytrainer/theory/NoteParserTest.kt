package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteParserTest {

    private fun german(input: String) = NoteParser.parsePitch(input, Notation.GERMAN)
    private fun english(input: String) = NoteParser.parsePitch(input, Notation.ENGLISH)

    @Test
    fun `all table names parse`() {
        for (notation in Notation.entries) {
            for (spelling in Spelling.entries) {
                for (pitch in PitchClass.ALL) {
                    val name = NoteNames.name(pitch, notation, spelling)
                    assertEquals("$name in $notation", pitch, NoteParser.parsePitch(name, notation))
                }
            }
        }
    }

    @Test
    fun `case whitespace and sign variants are tolerated`() {
        val cis = PitchClass.of(1)
        for (input in listOf("cis", "Cis", "CIS", "c#", "C#", "C♯", " c # ", "c is", "\tCis\n")) {
            assertEquals(input, cis, german(input))
            assertEquals(input, cis, english(input))
        }
        for (input in listOf("des", "Des", "Db", "db", "D♭", "d b")) {
            assertEquals(input, cis, german(input))
        }
    }

    @Test
    fun `german B is B flat and H is B natural`() {
        assertEquals(PitchClass.of(10), german("B"))
        assertEquals(PitchClass.of(10), german("b"))
        assertEquals(PitchClass.of(10), german("Bb"))
        assertEquals(PitchClass.of(10), german("B♭"))
        assertEquals(PitchClass.of(10), german("Hes"))
        assertEquals(PitchClass.H, german("H"))
        assertEquals(PitchClass.C, german("His"))
    }

    @Test
    fun `english B is B natural`() {
        assertEquals(PitchClass.H, english("B"))
        assertEquals(PitchClass.H, english("b"))
        assertEquals(PitchClass.of(10), english("Bb"))
        assertEquals(PitchClass.of(10), english("bb"))
        assertEquals(PitchClass.H, english("H"))
    }

    @Test
    fun `german short flats`() {
        assertEquals(PitchClass.of(8), german("As"))
        assertEquals(PitchClass.of(8), german("Aes"))
        assertEquals(PitchClass.of(3), german("Es"))
        assertEquals(PitchClass.of(3), german("Ees"))
        assertEquals(PitchClass.G, german("Ases"))
        assertEquals(PitchClass.D, german("Eses"))
    }

    @Test
    fun `spelling is preserved`() {
        assertEquals(SpelledNote(Letter.E, 1), NoteParser.parse("Eis", Notation.GERMAN))
        assertEquals(SpelledNote(Letter.C, -1), NoteParser.parse("Ces", Notation.GERMAN))
        assertEquals(SpelledNote(Letter.F, 2), NoteParser.parse("Fisis", Notation.GERMAN))
        assertEquals(SpelledNote(Letter.C, 2), NoteParser.parse("Cx", Notation.ENGLISH))
        assertEquals(SpelledNote(Letter.F, 2), NoteParser.parse("F𝄪", Notation.ENGLISH))
        assertEquals(SpelledNote(Letter.E, -2), NoteParser.parse("E𝄫", Notation.ENGLISH))
    }

    @Test
    fun `garbage is rejected`() {
        for (input in listOf("", "   ", "x", "Q", "Cs", "C#b", "Cisis#", "Hallo", "C-Dur", "1", "Ci")) {
            assertNull(input, german(input))
            assertNull(input, english(input))
        }
    }
}
