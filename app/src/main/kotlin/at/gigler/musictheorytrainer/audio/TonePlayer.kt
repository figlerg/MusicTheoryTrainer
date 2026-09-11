package at.gigler.musictheorytrainer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** Tiny additive synth: a few decaying harmonics give a plucked, guitar-ish tone. No samples. */
class TonePlayer {
    private val executor = Executors.newSingleThreadExecutor()
    private var track: AudioTrack? = null

    fun play(midi: Int) {
        executor.execute { playNow(midi) }
    }

    /** Plays [midis] one after another, e.g. a whole scale. */
    fun playSequence(midis: List<Int>, gapMillis: Long = 280) {
        executor.execute {
            midis.forEach {
                playNow(it)
                Thread.sleep(gapMillis)
            }
        }
    }

    fun release() {
        executor.execute {
            track?.release()
            track = null
        }
        executor.shutdown()
    }

    private fun playNow(midi: Int) {
        val samples = synthesize(440.0 * 2.0.pow((midi - 69) / 12.0))
        track?.release()
        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
            .apply {
                write(samples, 0, samples.size)
                play()
            }
    }

    private fun synthesize(frequency: Double): ShortArray {
        val count = (SAMPLE_RATE * DURATION_SECONDS).toInt()
        val attack = SAMPLE_RATE * 0.005
        val raw = DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            var value = 0.0
            for (harmonic in 1..HARMONICS) {
                val f = frequency * harmonic
                if (f > SAMPLE_RATE / 2) break
                value += sin(2 * PI * f * t) * exp(-t * (2.5 + harmonic * 1.8)) / harmonic.toDouble().pow(1.2)
            }
            value * min(1.0, i / attack)
        }
        val peak = raw.maxOf { kotlin.math.abs(it) }.takeIf { it > 0 } ?: 1.0
        return ShortArray(count) { (raw[it] / peak * Short.MAX_VALUE * VOLUME).toInt().toShort() }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val DURATION_SECONDS = 0.9
        const val HARMONICS = 6
        const val VOLUME = 0.7
    }
}
