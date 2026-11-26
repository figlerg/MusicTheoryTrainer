package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PianoTest {

    @Test
    fun `black keys come in groups of two and three`() {
        val octave = (Piano.MIDDLE_C until Piano.MIDDLE_C + 12).map { if (Piano.isBlack(it)) 'b' else 'w' }.joinToString("")
        assertEquals("wbwbwwbwbwbw", octave)
    }

    @Test
    fun `two octaves have fourteen white keys`() {
        assertEquals(14, Piano.whiteKeys(Piano.TWO_OCTAVES).size)
        assertEquals(PitchClass.C, PitchClass.of(Piano.TWO_OCTAVES.first))
        assertEquals(PitchClass.H, PitchClass.of(Piano.TWO_OCTAVES.last))
    }

    @Test
    fun `black keys sit after their white neighbour`() {
        assertEquals(1, Piano.whiteKeysBelow(61, Piano.TWO_OCTAVES)) // Cis right of C
        assertEquals(4, Piano.whiteKeysBelow(66, Piano.TWO_OCTAVES)) // Fis right of F
        assertEquals(13, Piano.whiteKeysBelow(82, Piano.TWO_OCTAVES)) // Ais'' right of A''
    }

    @Test
    fun `quiz stays in range and never repeats`() {
        val random = Random(5)
        var previous: Int? = null
        repeat(200) {
            val key = PianoQuiz.randomKey(Piano.TWO_OCTAVES, random, previous)
            assertTrue(key in Piano.TWO_OCTAVES)
            assertNotEquals(previous, key)
            previous = key
        }
    }
}
