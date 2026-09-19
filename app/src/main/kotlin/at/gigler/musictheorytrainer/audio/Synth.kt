package at.gigler.musictheorytrainer.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** One tone of a phrase: starts at [start] seconds and sounds for [length] seconds. */
data class PlayNote(val midi: Int, val start: Double, val length: Double)

/** How a note sounds: a soft synth tone, or a plucked string for the guitar exercises. */
enum class Voice { TONE, PLUCK }

/**
 * Two tiny synths, both plain Kotlin so they can be unit tested. Every tone fades in over 5 ms and
 * out to exactly zero over 30 ms, so no waveform is ever cut mid-swing, which is what makes clicks.
 */
object Synth {
    const val SAMPLE_RATE = 44_100
    const val MAX_LEVEL = 0.9f

    private const val TAIL_SECONDS = 0.05
    private const val ATTACK_SECONDS = 0.005
    private const val RELEASE_SECONDS = 0.03
    private const val HARMONICS = 6
    private const val LEVEL = 0.6
    private const val PLUCK_LEVEL = 0.6f

    /** Energy left after one sample; a note is down to about a tenth after a second. */
    private const val DAMPING = 0.99995f

    /** Sum of the harmonic amplitudes, so a single tone peaks at about [LEVEL]. */
    private val NORMALIZE = (1..HARMONICS).sumOf { 1.0 / it.toDouble().pow(1.2) }

    /** Mixes all notes into one buffer; overlapping notes (a strum) are scaled down if they clip. */
    fun render(notes: List<PlayNote>, voice: Voice = Voice.PLUCK): FloatArray {
        val seconds = notes.maxOf { it.start + it.length } + TAIL_SECONDS
        val out = FloatArray((seconds * SAMPLE_RATE).toInt())
        notes.forEach { if (voice == Voice.PLUCK) addPluck(out, it) else addTone(out, it) }
        val peak = out.maxOf { abs(it) }
        if (peak > MAX_LEVEL) {
            val scale = MAX_LEVEL / peak
            for (i in out.indices) out[i] *= scale
        }
        return out
    }

    private fun frequencyOf(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)

    /**
     * Karplus-Strong: a burst of noise runs around a delay line one wavelength long and is averaged
     * with its neighbour on every step. That averaging loses the high partials first, which is what
     * a plucked string does, so it sounds like a guitar without needing a single sample.
     */
    private fun addPluck(out: FloatArray, note: PlayNote) {
        val length = (SAMPLE_RATE / frequencyOf(note.midi)).roundToInt().coerceAtLeast(2)
        // Seeded by the note, so the same note always sounds the same.
        val random = Random(note.midi)
        val line = FloatArray(length)
        var smoothed = 0f
        for (i in line.indices) {
            // A gently filtered burst instead of raw noise: less fizz in the attack.
            smoothed = (random.nextFloat() * 2f - 1f + smoothed) * 0.5f
            line[i] = smoothed * PLUCK_LEVEL
        }

        val first = (note.start * SAMPLE_RATE).toInt()
        val count = min((note.length * SAMPLE_RATE).toInt(), out.size - first)
        val attack = ATTACK_SECONDS * SAMPLE_RATE
        val release = min(RELEASE_SECONDS * SAMPLE_RATE, count / 2.0)
        var index = 0
        for (i in 0 until count) {
            val value = line[index]
            line[index] = (value + line[(index + 1) % length]) * 0.5f * DAMPING
            index = (index + 1) % length
            val envelope = min(1.0, i / attack) * min(1.0, (count - i) / release)
            out[first + i] += (value * envelope).toFloat()
        }
    }

    /** A few decaying harmonics: softer and more neutral than the plucked string. */
    private fun addTone(out: FloatArray, note: PlayNote) {
        val frequency = frequencyOf(note.midi)
        val first = (note.start * SAMPLE_RATE).toInt()
        val count = min((note.length * SAMPLE_RATE).toInt(), out.size - first)
        val attack = ATTACK_SECONDS * SAMPLE_RATE
        val release = min(RELEASE_SECONDS * SAMPLE_RATE, count / 2.0)
        val harmonics = (1..HARMONICS).filter { frequency * it < SAMPLE_RATE / 2 }
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
