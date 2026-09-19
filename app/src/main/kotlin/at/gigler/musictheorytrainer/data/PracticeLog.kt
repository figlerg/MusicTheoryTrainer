package at.gigler.musictheorytrainer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * One answered question. Stored as a tab separated line: small, parses without a library and
 * survives new fields, because readers ignore what they do not know.
 *
 * The [options] snapshot and [appVersion] make old entries readable even when an exercise changes,
 * so the history stays comparable across versions.
 */
data class PracticeEntry(
    val time: Long,
    val exercise: Exercise,
    val mode: InputMode,
    val hit: Boolean,
    val appVersion: Int,
    val options: String,
) {
    fun encode(): String =
        listOf(time.toString(), exercise.name, mode.name, if (hit) "1" else "0", appVersion.toString(), options)
            .joinToString("\t")

    companion object {
        fun decode(line: String): PracticeEntry? {
            val parts = line.split('\t')
            if (parts.size < 5) return null
            return PracticeEntry(
                time = parts[0].toLongOrNull() ?: return null,
                exercise = Exercise.entries.firstOrNull { it.name == parts[1] } ?: return null,
                mode = InputMode.entries.firstOrNull { it.name == parts[2] } ?: InputMode.TEXT,
                hit = parts[3] == "1",
                appVersion = parts[4].toIntOrNull() ?: 0,
                options = parts.getOrElse(5) { "" },
            )
        }
    }
}

/** Append-only practice history. Nothing here is ever deleted, resetting only starts a new period. */
class PracticeLog(private val file: File) {
    private val mutex = Mutex()

    suspend fun append(entry: PracticeEntry) = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching { file.appendText(entry.encode() + "\n") }
        }
        Unit
    }

    suspend fun entries(): List<PracticeEntry> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        runCatching { file.readLines().mapNotNull(PracticeEntry::decode) }.getOrDefault(emptyList())
    }
}

data class Tally(val attempts: Int = 0, val hits: Int = 0, val millis: Long = 0) {
    val percent: Int? get() = if (attempts == 0) null else (hits * 100 + attempts / 2) / attempts

    operator fun plus(other: Tally) = Tally(attempts + other.attempts, hits + other.hits, millis + other.millis)
}

data class PracticeSummary(
    val total: Tally = Tally(),
    val byExercise: Map<Exercise, Tally> = emptyMap(),
    val byMode: Map<InputMode, Tally> = emptyMap(),
    /** Day number (local) to tally, ascending. */
    val byDay: List<Pair<Long, Tally>> = emptyList(),
    val first: Long? = null,
    val last: Long? = null,
) {
    val activeDays: Int get() = byDay.size
}

object PracticeStats {
    /** A longer pause means the user was away; it does not count as practice time. */
    const val GAP_CAP_MILLIS = 60_000L

    /** What one answer counts when there is no previous answer to measure against. */
    const val SINGLE_ANSWER_MILLIS = 5_000L

    const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    /**
     * Practice time is the measured gap between consecutive answers in the same exercise, capped
     * so that a break does not inflate it. [since] keeps only newer entries, [zoneOffsetMillis]
     * puts answers into local days.
     */
    fun summarise(entries: List<PracticeEntry>, since: Long = 0L, zoneOffsetMillis: Long = 0L): PracticeSummary {
        val sorted = entries.sortedBy { it.time }
        var total = Tally()
        val byExercise = mutableMapOf<Exercise, Tally>()
        val byMode = mutableMapOf<InputMode, Tally>()
        val byDay = mutableMapOf<Long, Tally>()
        var first: Long? = null
        var last: Long? = null
        var previous: PracticeEntry? = null

        for (entry in sorted) {
            val before = previous
            val gap = if (before == null) Long.MAX_VALUE else entry.time - before.time
            val measured = before != null && before.exercise == entry.exercise && gap in 0..GAP_CAP_MILLIS
            val millis = if (measured) gap else SINGLE_ANSWER_MILLIS
            previous = entry
            if (entry.time < since) continue

            val tally = Tally(1, if (entry.hit) 1 else 0, millis)
            total += tally
            byExercise[entry.exercise] = (byExercise[entry.exercise] ?: Tally()) + tally
            byMode[entry.mode] = (byMode[entry.mode] ?: Tally()) + tally
            val day = Math.floorDiv(entry.time + zoneOffsetMillis, DAY_MILLIS)
            byDay[day] = (byDay[day] ?: Tally()) + tally
            if (first == null) first = entry.time
            last = entry.time
        }

        return PracticeSummary(
            total = total,
            byExercise = byExercise,
            byMode = byMode,
            byDay = byDay.toList().sortedBy { it.first },
            first = first,
            last = last,
        )
    }
}

/** What the user had set when answering, so a session can be reconstructed later. */
fun optionSummary(exercise: Exercise, settings: Settings): String {
    val mode = when (exercise) {
        Exercise.FRETBOARD -> "Modus=" + if (settings.fretboardReverse) "Stelle finden" else "Ton nennen"
        Exercise.CHORDS -> "Modus=" + if (settings.chordGrip) "Griff" else "Töne"
        Exercise.SHEET -> "Modus=" + if (settings.sheetSource == SheetSource.MELODIES) "Melodien" else "Zufall"
        else -> null
    }
    val options = Options.of(exercise).map { option ->
        when (option) {
            is Option.Choice -> "${option.title}=${option.labels.getOrElse(option.selected(settings)) { "?" }}"
            is Option.Switch -> "${option.title}=${if (option.checked(settings)) "an" else "aus"}"
            is Option.Strings -> "${option.title}=${option.selected(settings).sorted().joinToString("")}"
        }
    }
    return (listOfNotNull(mode) + options).joinToString(";")
}
