package at.gigler.musictheorytrainer.audio

/** What the screens use to make noise; respects the "Ton abspielen" setting. */
class Sound(private val player: TonePlayer, val enabled: Boolean) {
    fun play(midi: Int) {
        if (enabled) player.play(midi)
    }

    fun playSequence(midis: List<Int>) {
        if (enabled) player.playSequence(midis)
    }
}
