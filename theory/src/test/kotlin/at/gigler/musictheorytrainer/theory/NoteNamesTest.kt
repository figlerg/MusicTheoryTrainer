package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteNamesTest {

    private fun names(pitches: List<PitchClass>, notation: Notation, spelling: Spelling) =
        pitches.map { NoteNames.name(it, notation, spelling) }

    @Test
    fun `german names ascending use sharps`() {
        val ascending = (0 until 12).map { PitchClass.C + it }
        assertEquals(
            listOf("C", "Cis", "D", "Dis", "E", "F", "Fis", "G", "Gis", "A", "Ais", "H"),
            names(ascending, Notation.GERMAN, Spelling.SHARP),
        )
    }

    @Test
    fun `german names descending use flats`() {
        val descending = (0 until 12).map { PitchClass.C - it }
        assertEquals(
            listOf("C", "H", "B", "A", "As", "G", "Ges", "F", "E", "Es", "D", "Des"),
            names(descending, Notation.GERMAN, Spelling.FLAT),
        )
    }

    @Test
    fun `english names`() {
        assertEquals(
            listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"),
            names(PitchClass.ALL, Notation.ENGLISH, Spelling.SHARP),
        )
        assertEquals(
            listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"),
            names(PitchClass.ALL, Notation.ENGLISH, Spelling.FLAT),
        )
    }

    @Test
    fun `both names only for black keys`() {
        assertEquals("Cis/Des", NoteNames.bothNames(PitchClass.of(1), Notation.GERMAN))
        assertEquals("Ais/B", NoteNames.bothNames(PitchClass.of(10), Notation.GERMAN))
        assertEquals("A#/Bb", NoteNames.bothNames(PitchClass.of(10), Notation.ENGLISH))
        assertEquals("H", NoteNames.bothNames(PitchClass.H, Notation.GERMAN))
        assertEquals("B", NoteNames.bothNames(PitchClass.H, Notation.ENGLISH))
    }
}
