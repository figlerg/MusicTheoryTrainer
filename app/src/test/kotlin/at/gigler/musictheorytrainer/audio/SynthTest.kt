package at.gigler.musictheorytrainer.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * A click is a sudden jump between two samples. The smooth tone itself changes by at most ~0.15
 * per sample even at high notes; a waveform cut mid-swing would jump by up to the full level.
 * The plucked string starts from a noise burst, so only its edges and its level are checked.
 */
class SynthTest {

    private fun maxJump(samples: FloatArray) = (1 until samples.size).maxOf { abs(samples[it] - samples[it - 1]) }

    @Test
    fun `a single tone starts and ends in silence`() {
        val samples = Synth.render(listOf(PlayNote(64, 0.0, 0.9)), Voice.TONE)
        assertTrue(abs(samples.first()) < 1e-3f)
        assertTrue(abs(samples.last()) < 1e-3f)
        assertTrue(maxJump(samples) < 0.2f)
    }

    @Test
    fun `back to back melody notes meet at zero`() {
        val length = 0.3
        val midis = listOf(55, 57, 59, 60, 62, 62, 64, 76)
        val samples = Synth.render(midis.mapIndexed { i, midi -> PlayNote(midi, i * length, length) }, Voice.TONE)
        assertTrue("jump ${maxJump(samples)}", maxJump(samples) < 0.2f)
        for (i in 1 until midis.size) {
            val boundary = (i * length * Synth.SAMPLE_RATE).toInt()
            assertTrue("boundary $i: ${samples[boundary]}", abs(samples[boundary]) < 0.01f)
        }
    }

    @Test
    fun `a strum rings together without clipping`() {
        val samples = Synth.render(
            listOf(40, 47, 52, 56, 59, 64).mapIndexed { i, midi -> PlayNote(midi, i * 0.05, 1.8 - i * 0.05) },
            Voice.TONE,
        )
        assertTrue(samples.maxOf { abs(it) } <= Synth.MAX_LEVEL + 1e-6f)
        assertTrue(maxJump(samples) < 0.2f)
        assertTrue(abs(samples.last()) < 1e-3f)
    }

    @Test
    fun `a plucked string fades in and out and stays in range`() {
        val samples = Synth.render(listOf(PlayNote(52, 0.0, 1.5)), Voice.PLUCK)
        assertTrue(abs(samples.first()) < 1e-3f)
        assertTrue(abs(samples.last()) < 1e-3f)
        assertTrue(samples.maxOf { abs(it) } <= Synth.MAX_LEVEL + 1e-6f)
    }

    @Test
    fun `a plucked string keeps ringing but loses energy`() {
        val samples = Synth.render(listOf(PlayNote(52, 0.0, 2.0)), Voice.PLUCK)
        fun peakAround(second: Double): Float {
            val from = (second * Synth.SAMPLE_RATE).toInt()
            return (from until from + Synth.SAMPLE_RATE / 10).maxOf { abs(samples[it]) }
        }
        val early = peakAround(0.1)
        val late = peakAround(1.5)
        assertTrue("early $early", early > 0.05f)
        assertTrue("late $late vs early $early", late in 0.001f..(early / 2))
    }

    @Test
    fun `the same note always sounds the same`() {
        val first = Synth.render(listOf(PlayNote(45, 0.0, 0.5)), Voice.PLUCK)
        val second = Synth.render(listOf(PlayNote(45, 0.0, 0.5)), Voice.PLUCK)
        assertEquals(first.size, second.size)
        assertTrue(first.indices.all { first[it] == second[it] })
    }
}
