package at.gigler.musictheorytrainer.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** One tone of a phrase: starts at [start] seconds and sounds for [length] seconds. */
data class PlayNote(val midi: Int, val start: Double, val length: Double)

/**
 * Tiny additive synth: a few decaying harmonics give a plucked, guitar-ish tone. Every tone fades
 * in over 5 ms and out to exactly zero over 30 ms, so no waveform is ever cut mid-swing, which is
 * what makes clicks. Plain Kotlin without Android so it can be unit tested.
 */
object Synth {
    const val SAMPLE_RATE = 44_100
    const val MAX_LEVEL = 0.9f

    private const val TAIL_SECONDS = 0.05
    private const val ATTACK_SECONDS = 0.005
    private const val RELEASE_SECONDS = 0.03
    private const val HARMONICS = 6
    private const val LEVEL = 0.6

    /** Sum of the harmonic amplitudes, so a single tone peaks at about [LEVEL]. */
    private val NORMALIZE = (1..HARMONICS).sumOf { 1.0 / it.toDouble().pow(1.2) }

    /** Mixes all notes into one buffer; overlapping notes (a strum) are scaled down if they would clip. */
    fun render(notes: List<PlayNote>): FloatArray {
        val seconds = notes.maxOf { it.start + it.length } + TAIL_SECONDS
        val out = FloatArray((seconds * SAMPLE_RATE).toInt())
        notes.forEach { addTone(out, it) }
        val peak = out.maxOf { abs(it) }
        if (peak > MAX_LEVEL) {
            val scale = MAX_LEVEL / peak
            for (i in out.indices) out[i] *= scale
        }
        return out
    }

    private fun addTone(out: FloatArray, note: PlayNote) {
        val frequency = 440.0 * 2.0.pow((note.midi - 69) / 12.0)
        val first = (note.start * SAMPLE_RATE).toInt()
        val count = min((note.length * SAMPLE_RATE).toInt(), out.size - first)
        val attack = ATTACK_SECONDS * SAMPLE_RATE
        val release = min(RELEASE_SECONDS * SAMPLE_RATE, count / 2.0)
        val harmonics = (1..HARMONICS).filter { frequency * it < SAMPLE_RATE / 2 }
        // Per harmonic: phase step, current amplitude and per-sample decay factor.
        val step = harmonics.map { 2 * PI * frequency * it / SAMPLE_RATE }
        val amplitude = harmonics.map { 1.0 / it.toDouble().pow(1.2) }.toDoubleArray()
        val decay = harmonics.map { exp(-(2.5 + it * 1.8) / SAMPLE_RATE) }
        for (i in 0 until count) {
            var value = 0.0
            for (h in harmonics.indices) {
                value += sin(step[h] * i) * amplitude[h]
                amplitude[h] *= decay[h]
            }
            val envelope = min(1.0, i / attack) * min(1.0, (count - i) / release)
            out[first + i] += (value * envelope * LEVEL / NORMALIZE).toFloat()
        }
    }
}
