package at.gigler.musictheorytrainer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Streams [Synth] output through one long-lived AudioTrack instead of opening a new one per tone.
 * A new request or [stop] fades out whatever is playing within one chunk (about 20 ms).
 */
class TonePlayer {
    private val executor = Executors.newSingleThreadExecutor()
    private val generation = AtomicInteger()
    private var track: AudioTrack? = null
    private val playingState = MutableStateFlow(false)

    /** True while a tone or phrase is sounding. */
    val playing: StateFlow<Boolean> = playingState

    fun play(midi: Int, voice: Voice = Voice.PLUCK) = playNotes(listOf(PlayNote(midi, 0.0, SINGLE_SECONDS)), voice)

    fun playNotes(notes: List<PlayNote>, voice: Voice = Voice.PLUCK) {
        if (notes.isEmpty()) return
        val mine = generation.incrementAndGet()
        playingState.value = true
        executor.execute {
            if (generation.get() == mine) stream(Synth.render(notes, voice), mine)
        }
    }

    fun stop() {
        generation.incrementAndGet()
        playingState.value = false
    }

    fun release() {
        stop()
        executor.execute {
            track?.release()
            track = null
        }
        executor.shutdown()
    }

    private fun stream(samples: FloatArray, mine: Int) {
        val out = track ?: createTrack().also { track = it }
        out.play()
        val buffer = ShortArray(CHUNK)
        var pos = 0
        while (pos < samples.size) {
            val n = min(CHUNK, samples.size - pos)
            val cancelled = generation.get() != mine
            for (i in 0 until n) {
                val fade = if (cancelled) 1f - i.toFloat() / n else 1f
                buffer[i] = (samples[pos + i] * fade * Short.MAX_VALUE).roundToInt().toShort()
            }
            out.write(buffer, 0, n)
            pos += n
            if (cancelled) break
        }
        // End on silence; stop() lets the written data drain instead of cutting it off.
        buffer.fill(0)
        out.write(buffer, 0, CHUNK)
        out.stop()
        if (generation.get() == mine) playingState.value = false
    }

    private fun createTrack(): AudioTrack {
        val minSize = AudioTrack.getMinBufferSize(Synth.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(Synth.SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setBufferSizeInBytes(maxOf(minSize, CHUNK * 2 * 2))
            .build()
    }

    private companion object {
        const val CHUNK = 1024
        const val SINGLE_SECONDS = 0.9
    }
}
