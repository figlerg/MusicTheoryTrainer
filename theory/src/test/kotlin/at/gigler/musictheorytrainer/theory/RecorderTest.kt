package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RecorderTest {

    private fun names(system: RecorderSystem) = Recorder.notes(system).joinToString(" ") { it.name(Notation.GERMAN) }

    @Test
    fun `listed notes`() {
        assertEquals("c' d' e' f' fis' g' a' b' h' c'' d'' e'' g'' a''", names(RecorderSystem.BAROQUE))
        assertEquals("c' d' e' f' g' a' h' c'' d'' e'' g'' a''", names(RecorderSystem.GERMAN))
    }

    @Test
    fun `beginner fingerings`() {
        fun fingering(midi: Int) = Recorder.noteAt(midi, RecorderSystem.BAROQUE)!!.fingering.toString()
        assertEquals("X XXX XXXX", fingering(60)) // c': everything closed
        assertEquals("X XXX OOOO", fingering(67)) // g': left hand only
        assertEquals("X XXO OOOO", fingering(69)) // a'
        assertEquals("X XOO OOOO", fingering(71)) // h': thumb and index finger
        assertEquals("X OXO OOOO", fingering(72)) // c'': thumb and middle finger
        assertEquals("O OXO OOOO", fingering(74)) // d'': middle finger alone
    }

    @Test
    fun `upper octave pinches the thumb hole over the lower fingering`() {
        for (system in RecorderSystem.entries) {
            for (upper in Recorder.notes(system).filter { it.fingering.holes[Recorder.THUMB] == Hole.HALF }) {
                val lower = Recorder.noteAt(upper.writtenMidi - 12, system)!!
                assertEquals(lower.fingering.with(Recorder.THUMB, Hole.HALF), upper.fingering)
            }
        }
    }

    @Test
    fun `the systems differ in F`() {
        val baroque = Recorder.noteAt(65, RecorderSystem.BAROQUE)!!.fingering
        val german = Recorder.noteAt(65, RecorderSystem.GERMAN)!!.fingering
        assertNotEquals(baroque, german)
        assertEquals("X XXX XOOO", german.toString())
        val shared = Recorder.notes(RecorderSystem.GERMAN).filter { it.writtenMidi != 65 }
        shared.forEach { assertEquals(it, Recorder.noteAt(it.writtenMidi, RecorderSystem.BAROQUE)) }
    }

    @Test
    fun `every fingering plays one note`() {
        for (system in RecorderSystem.entries) {
            val notes = Recorder.notes(system)
            assertEquals(notes.size, notes.map { it.fingering }.toSet().size)
            assertEquals(notes.size, notes.map { it.writtenMidi }.toSet().size)
            notes.forEach { assertEquals(it, Recorder.noteFor(it.fingering, system)) }
        }
        assertNull(Recorder.noteFor(Fingering.ALL_OPEN, RecorderSystem.BAROQUE))
    }

    @Test
    fun `sounds an octave higher than written`() {
        val c = Recorder.noteAt(60, RecorderSystem.BAROQUE)!!
        assertEquals(72, c.soundingMidi)
        assertEquals(PitchClass.C, c.pitchClass)
    }

    @Test
    fun `half steps are spelled the usual way`() {
        assertEquals("F#4 Bb4", Recorder.notes(RecorderSystem.BAROQUE).filterNot { NoteNames.isNatural(it.pitchClass) }
            .joinToString(" ") { it.name(Notation.ENGLISH) })
        assertTrue(Recorder.canPlay(PitchClass.of(10), RecorderSystem.BAROQUE))
        assertFalse(Recorder.canPlay(PitchClass.of(10), RecorderSystem.GERMAN))
    }

    @Test
    fun `fingering text round trip`() {
        for (note in Recorder.notes(RecorderSystem.BAROQUE)) {
            assertEquals(note.fingering, Fingering.parse(note.fingering.toString()))
        }
    }

    @Test
    fun `the recorder staff range and the melodies are fingerable in both systems`() {
        val range = (StaffRange.RECORDER.low..StaffRange.RECORDER.high).map(StaffNote::fromPosition)
        assertEquals("C4", "${range.first().letter}${range.first().octave}")
        assertEquals("E5", "${range.last().letter}${range.last().octave}")
        val melodyNotes = Melodies.ALL.flatMap { it.notes }
        for (system in RecorderSystem.entries) {
            for (note in range + melodyNotes) {
                assertTrue("$note in $system", Recorder.noteAt(note.writtenMidi, system) != null)
            }
        }
    }

    @Test
    fun `quiz never repeats the previous note`() {
        val random = Random(3)
        var previous: RecorderNote? = null
        repeat(200) {
            val next = RecorderQuiz.randomNote(RecorderSystem.GERMAN, random, previous)
            assertNotEquals(previous, next)
            previous = next
        }
    }

    @Test
    fun `octave names`() {
        fun german(letter: Letter, alteration: Int, octave: Int) =
            NoteNames.withOctave(SpelledNote(letter, alteration), octave, Notation.GERMAN)
        assertEquals("C", german(Letter.C, 0, 2))
        assertEquals("c", german(Letter.C, 0, 3))
        assertEquals("c'", german(Letter.C, 0, 4))
        assertEquals("fis''", german(Letter.F, 1, 5))
        assertEquals("b'", german(Letter.B, -1, 4))
        assertEquals("h'", german(Letter.B, 0, 4))
        assertEquals("Bb4", NoteNames.withOctave(SpelledNote(Letter.B, -1), 4, Notation.ENGLISH))
    }

    @Test
    fun `octave check per input`() {
        val g4 = StaffNote(Letter.G, 4)
        assertFalse(EnteredNote(PitchClass.G, writtenMidi = 67).missesOctaveOf(g4, exactGuitar = false))
        assertTrue(EnteredNote(PitchClass.G, writtenMidi = 79).missesOctaveOf(g4, exactGuitar = false))
        // G string open sounds G3, which guitar notation writes as G4.
        val open = EnteredNote(PitchClass.G, position = FretPosition(3, 0))
        assertFalse(open.missesOctaveOf(g4, exactGuitar = true))
        assertTrue(EnteredNote(PitchClass.G, position = FretPosition(0, 3)).missesOctaveOf(g4, exactGuitar = true))
        assertFalse(EnteredNote(PitchClass.G, position = FretPosition(0, 3)).missesOctaveOf(g4, exactGuitar = false))
        assertFalse(EnteredNote(PitchClass.G).missesOctaveOf(g4, exactGuitar = true))
    }
}
