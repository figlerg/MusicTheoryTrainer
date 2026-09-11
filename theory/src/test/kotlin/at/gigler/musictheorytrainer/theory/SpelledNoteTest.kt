package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Test

class SpelledNoteTest {

    @Test
    fun `german names`() {
        assertEquals("H", SpelledNote(Letter.B).name(Notation.GERMAN))
        assertEquals("B", SpelledNote(Letter.B, -1).name(Notation.GERMAN))
        assertEquals("Heses", SpelledNote(Letter.B, -2).name(Notation.GERMAN))
        assertEquals("His", SpelledNote(Letter.B, 1).name(Notation.GERMAN))
        assertEquals("Es", SpelledNote(Letter.E, -1).name(Notation.GERMAN))
        assertEquals("As", SpelledNote(Letter.A, -1).name(Notation.GERMAN))
        assertEquals("Ases", SpelledNote(Letter.A, -2).name(Notation.GERMAN))
        assertEquals("Des", SpelledNote(Letter.D, -1).name(Notation.GERMAN))
        assertEquals("Ces", SpelledNote(Letter.C, -1).name(Notation.GERMAN))
        assertEquals("Eis", SpelledNote(Letter.E, 1).name(Notation.GERMAN))
        assertEquals("Fisis", SpelledNote(Letter.F, 2).name(Notation.GERMAN))
    }

    @Test
    fun `english names`() {
        assertEquals("B", SpelledNote(Letter.B).name(Notation.ENGLISH))
        assertEquals("Bb", SpelledNote(Letter.B, -1).name(Notation.ENGLISH))
        assertEquals("E#", SpelledNote(Letter.E, 1).name(Notation.ENGLISH))
        assertEquals("F##", SpelledNote(Letter.F, 2).name(Notation.ENGLISH))
        assertEquals("Abb", SpelledNote(Letter.A, -2).name(Notation.ENGLISH))
    }

    @Test
    fun `pitch classes`() {
        assertEquals(PitchClass.of(5), SpelledNote(Letter.E, 1).pitchClass)
        assertEquals(PitchClass.H, SpelledNote(Letter.C, -1).pitchClass)
        assertEquals(PitchClass.A, SpelledNote(Letter.B, -2).pitchClass)
    }

    @Test
    fun `spelling from pitch class`() {
        assertEquals(SpelledNote(Letter.C, 1), SpelledNote.of(PitchClass.of(1), Spelling.SHARP))
        assertEquals(SpelledNote(Letter.D, -1), SpelledNote.of(PitchClass.of(1), Spelling.FLAT))
        assertEquals(SpelledNote(Letter.B, -1), SpelledNote.of(PitchClass.of(10), Spelling.FLAT))
        assertEquals(SpelledNote(Letter.E), SpelledNote.of(PitchClass.E, Spelling.FLAT))
    }

    @Test
    fun `every name parses back to the same note`() {
        for (notation in Notation.entries) {
            for (letter in Letter.entries) {
                for (alteration in -2..2) {
                    val note = SpelledNote(letter, alteration)
                    assertEquals("$note in $notation", note, NoteParser.parse(note.name(notation), notation))
                }
            }
        }
    }
}
