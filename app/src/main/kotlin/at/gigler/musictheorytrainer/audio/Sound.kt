package at.gigler.musictheorytrainer.audio

import kotlinx.coroutines.flow.StateFlow

/** What the screens use to make noise; respects the "Ton abspielen" setting. */
class Sound(private val player: TonePlayer, val enabled: Boolean) {

    val playing: StateFlow<Boolean> get() = player.playing

    fun play(midi: Int) {
        if (enabled) player.play(midi)
    }

    /** Evenly spaced notes such as a scale; the last one rings a little longer. */
    fun playSequence(midis: List<Int>, secondsEach: Double = 0.4) {
        if (!enabled) return
        player.playNotes(midis.mapIndexed { i, midi -> PlayNote(midi, i * secondsEach, if (i == midis.lastIndex) 1.0 else secondsEach) })
    }

    /** Notes with their own lengths in seconds, played back to back (a melody). */
    fun playTimed(notes: List<Pair<Int, Double>>) {
        if (!enabled) return
        var start = 0.0
        player.playNotes(
            notes.map { (midi, seconds) ->
                PlayNote(midi, start, seconds).also { start += seconds }
            },
        )
    }

    /** All notes ring together, started one after another like a strum from low to high. */
    fun strum(midis: List<Int>) {
        if (!enabled) return
        player.playNotes(midis.sorted().mapIndexed { i, midi -> PlayNote(midi, i * STRUM_SECONDS, CHORD_SECONDS - i * STRUM_SECONDS) })
    }

    fun stop() = player.stop()

    private companion object {
        const val STRUM_SECONDS = 0.05
        const val CHORD_SECONDS = 1.8
    }
}
