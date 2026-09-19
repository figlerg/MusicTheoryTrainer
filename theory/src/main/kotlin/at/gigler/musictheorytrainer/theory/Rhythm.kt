package at.gigler.musictheorytrainer.theory

import kotlin.math.abs
import kotlin.random.Random

/** Note values in 4/4, measured in beats (quarter = 1). */
enum class NoteValue(val beats: Double, val label: String) {
    WHOLE(4.0, "Ganze"),
    HALF(2.0, "Halbe"),
    QUARTER(1.0, "Viertel"),
    EIGHTH(0.5, "Achtel"),
}

/** One event of a rhythm line: a note to play, or a rest to stay quiet through. */
data class RhythmNote(val note: StaffNote?, val value: NoteValue) {
    val isRest: Boolean get() = note == null
}

/** Which note values are in play. */
enum class RhythmLevel(val label: String, val values: List<NoteValue>) {
    QUARTERS("Nur Viertel", listOf(NoteValue.QUARTER)),
    SIMPLE("Viertel bis Ganze", listOf(NoteValue.QUARTER, NoteValue.HALF, NoteValue.WHOLE)),
    EIGHTHS("Mit Achteln", listOf(NoteValue.EIGHTH, NoteValue.QUARTER, NoteValue.HALF, NoteValue.WHOLE)),
}

/**
 * How close an answer was to its beat, in the spirit of a rhythm game: every step still scores,
 * so a slightly late note is not simply lost.
 */
enum class Timing(val label: String, val points: Int, val toleranceMillis: Long) {
    PERFECT("Perfekt", 3, 90),
    GOOD("Gut", 2, 180),
    LOOSE("Mäßig", 1, 320),
    MISSED("Daneben", 0, Long.MAX_VALUE),
    ;

    companion object {
        /** The best grade whose tolerance still covers this offset from the beat. */
        fun of(offsetMillis: Long): Timing = entries.first { abs(offsetMillis) <= it.toleranceMillis }

        /** How long after its beat a note can still be answered before it counts as missed. */
        val LAST_CHANCE_MILLIS: Long = LOOSE.toleranceMillis
    }
}

object RhythmQuiz {
    const val BEATS_PER_BAR = 4

    /**
     * Fills [bars] of 4/4 with the values of [level]. Melodic steps stay small, like the plain
     * reading lines, and rests appear with [restChance] but never twice in a row or at the start.
     */
    fun line(
        range: StaffRange,
        level: RhythmLevel,
        bars: Int,
        random: Random,
        restChance: Double = 0.0,
    ): List<RhythmNote> {
        val events = mutableListOf<RhythmNote>()
        var position = random.nextInt(range.low, range.high + 1)
        var lastWasRest = false
        repeat(bars) {
            var left = BEATS_PER_BAR.toDouble()
            while (left > 0) {
                val fitting = level.values.filter { it.beats <= left }
                val value = fitting[random.nextInt(fitting.size)]
                left -= value.beats
                val rest = events.isNotEmpty() && !lastWasRest && random.nextDouble() < restChance
                if (rest) {
                    events += RhythmNote(null, value)
                    lastWasRest = true
                } else {
                    val size = if (random.nextInt(10) < 8) random.nextInt(1, 3) else random.nextInt(3, 5)
                    var next = position + if (random.nextBoolean()) size else -size
                    if (next < range.low || next > range.high) next = position - (next - position)
                    position = next.coerceIn(range.low, range.high)
                    events += RhythmNote(StaffNote.fromPosition(position), value)
                    lastWasRest = false
                }
            }
        }
        return events
    }

    /** When each event falls, in beats from the start of the line. */
    fun beatOffsets(line: List<RhythmNote>): List<Double> {
        var beat = 0.0
        return line.map { event ->
            val at = beat
            beat += event.value.beats
            at
        }
    }

    fun totalBeats(line: List<RhythmNote>): Double = line.sumOf { it.value.beats }
}
