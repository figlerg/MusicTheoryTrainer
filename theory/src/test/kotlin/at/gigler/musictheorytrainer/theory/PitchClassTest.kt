package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Test

class PitchClassTest {

    @Test
    fun `wraps around the octave`() {
        assertEquals(PitchClass.H, PitchClass.of(-1))
        assertEquals(PitchClass.of(1), PitchClass.of(13))
        assertEquals(PitchClass.C, PitchClass.H + 1)
        assertEquals(PitchClass.H, PitchClass.C - 1)
        assertEquals(PitchClass.C, PitchClass.C + 12)
    }

    @Test
    fun `examples from the exercises`() {
        assertEquals(PitchClass.G, PitchClass.of(6) + 1) // Fis + Halbton
        assertEquals(PitchClass.C, PitchClass.D - 2) // D − Ganzton
        assertEquals(PitchClass.C, PitchClass.A + 3) // A + 3 Halbtöne
    }

    @Test
    fun `upward distance`() {
        assertEquals(8, PitchClass.E.semitonesUpTo(PitchClass.C))
        assertEquals(4, PitchClass.C.semitonesUpTo(PitchClass.E))
        assertEquals(0, PitchClass.G.semitonesUpTo(PitchClass.G))
    }
}
