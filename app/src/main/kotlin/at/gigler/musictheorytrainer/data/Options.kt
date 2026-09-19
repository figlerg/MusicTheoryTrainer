package at.gigler.musictheorytrainer.data

import at.gigler.musictheorytrainer.audio.Voice
import at.gigler.musictheorytrainer.theory.EarIntervalSet
import at.gigler.musictheorytrainer.theory.Guitar
import at.gigler.musictheorytrainer.theory.IntervalDifficulty
import at.gigler.musictheorytrainer.theory.KeyChoice
import at.gigler.musictheorytrainer.theory.KeyOrder
import at.gigler.musictheorytrainer.theory.Notation
import at.gigler.musictheorytrainer.theory.RhythmLevel
import at.gigler.musictheorytrainer.theory.StaffRange

/**
 * One adjustable parameter. Exercises declare their own list, so every setting sits where it is
 * used instead of in one long settings screen.
 */
sealed interface Option {
    val title: String
    val hint: String

    /** A row of segments, e.g. Dur / Moll / Beide. */
    data class Choice(
        override val title: String,
        override val hint: String = "",
        val labels: List<String>,
        val selected: (Settings) -> Int,
        val select: (Settings, Int) -> Settings,
    ) : Option

    data class Switch(
        override val title: String,
        override val hint: String = "",
        val checked: (Settings) -> Boolean,
        val set: (Settings, Boolean) -> Settings,
    ) : Option

    /** The six strings, individually switchable; at least one stays on. */
    data class Strings(
        override val title: String,
        override val hint: String = "",
        val selected: (Settings) -> Set<Int>,
        val set: (Settings, Set<Int>) -> Settings,
    ) : Option
}

private fun <T> choice(
    title: String,
    hint: String = "",
    values: List<T>,
    labels: List<String>,
    get: (Settings) -> T,
    set: (Settings, T) -> Settings,
) = Option.Choice(
    title = title,
    hint = hint,
    labels = labels,
    selected = { settings -> values.indexOf(get(settings)).coerceAtLeast(0) },
    select = { settings, index -> set(settings, values[index]) },
)

object Options {

    val notation = choice(
        "Notation",
        values = Notation.entries,
        labels = listOf("Deutsch (H, B)", "Englisch (B, Bb)"),
        get = { it.notation },
        set = { s, v -> s.copy(notation = v) },
    )

    val inputMode = choice(
        "Eingabe",
        hint = "Lässt sich auch in der Übung umschalten.",
        values = InputMode.entries,
        labels = listOf("Text", "Tasten", "Gitarre"),
        get = { it.inputMode },
        set = { s, v -> s.copy(inputMode = v) },
    )

    val sound = Option.Switch("Ton abspielen", checked = { it.sound }, set = { s, v -> s.copy(sound = v) })

    val voice = choice(
        "Klang",
        hint = "Gitarre zupft eine Saite an, Ton ist ein weicher Synth-Klang.",
        values = Voice.entries,
        labels = listOf("Ton", "Gitarre"),
        get = { it.voice },
        set = { s, v -> s.copy(voice = v) },
    )

    val showTab = Option.Switch("Tab-Ansicht", checked = { it.showTab }, set = { s, v -> s.copy(showTab = v) })

    val strings = Option.Strings(
        "Saiten",
        hint = "Welche Saiten abgefragt werden.",
        selected = { it.strings },
        set = { s, v -> s.copy(strings = v.ifEmpty { setOf(0) }) },
    )

    val intervalDifficulty = choice(
        "Schwierigkeit",
        values = IntervalDifficulty.entries,
        labels = listOf("Halb/Ganz", "bis 5", "bis 11"),
        get = { it.intervalDifficulty },
        set = { s, v -> s.copy(intervalDifficulty = v) },
    )

    val keyChoice = choice(
        "Tonarten",
        values = KeyChoice.entries,
        labels = listOf("Dur", "Moll", "Beide"),
        get = { it.keyChoice },
        set = { s, v -> s.copy(keyChoice = v) },
    )

    val keyOrder = choice(
        "Reihenfolge",
        hint = "Der Quintenzirkel nimmt pro Tonart ein Vorzeichen dazu.",
        values = KeyOrder.entries,
        labels = listOf("Quintenzirkel", "Zufall"),
        get = { it.keyOrder },
        set = { s, v -> s.copy(keyOrder = v) },
    )

    val chordRootGiven = Option.Switch(
        "Grundton vorgegeben",
        hint = "Dann gibst du nur Terz und Quinte ein.",
        checked = { it.chordRootGiven },
        set = { s, v -> s.copy(chordRootGiven = v) },
    )

    val gripRootInBass = Option.Switch(
        "Griff: Grundton im Bass",
        hint = "Verlangt den Grundton als tiefsten Ton des Griffs.",
        checked = { it.gripRootInBass },
        set = { s, v -> s.copy(gripRootInBass = v) },
    )

    val sheetRange = choice(
        "Tonumfang",
        hint = "Gilt für Zufallszeilen.",
        values = StaffRange.entries,
        labels = StaffRange.entries.map { it.label },
        get = { it.sheetRange },
        set = { s, v -> s.copy(sheetRange = v) },
    )

    val sheetExactOctave = Option.Switch(
        "Gitarre: exakte Oktave",
        hint = "Die Note muss in der richtigen Oktave klingen, nicht nur den richtigen Namen haben. Wo du sie greifst, ist egal: dasselbe e gibt es auf mehreren Saiten.",
        checked = { it.sheetExactOctave },
        set = { s, v -> s.copy(sheetExactOctave = v) },
    )

    val hideStringNames = Option.Switch(
        "Saitennamen ausblenden",
        hint = "Wie auf der echten Gitarre: ohne Beschriftung der Saiten.",
        checked = { it.hideStringNames },
        set = { s, v -> s.copy(hideStringNames = v) },
    )

    val hideKeyLabels = Option.Switch(
        "Tastenbeschriftung ausblenden",
        hint = "Klaviertasten ohne Tonnamen, zum Auswendiglernen.",
        checked = { it.hideKeyLabels },
        set = { s, v -> s.copy(hideKeyLabels = v) },
    )

    val earIntervalSet = choice(
        "Intervalle",
        hint = "Welche Abstände abgefragt werden.",
        values = EarIntervalSet.entries,
        labels = EarIntervalSet.entries.map { it.label },
        get = { it.earIntervalSet },
        set = { s, v -> s.copy(earIntervalSet = v) },
    )

    val earHarmonic = Option.Switch(
        "Gleichzeitig spielen",
        hint = "Aus: die Töne kommen nacheinander, was am Anfang leichter ist.",
        checked = { it.earHarmonic },
        set = { s, v -> s.copy(earHarmonic = v) },
    )

    val earReference = Option.Switch(
        "Bezugston zuerst",
        hint = "Spielt erst ein C, dann den gesuchten Ton. Ohne Bezugston ist es absolutes Gehör, und das ist als Erwachsener kaum noch erlernbar.",
        checked = { it.earReference },
        set = { s, v -> s.copy(earReference = v) },
    )

    val speedSeconds = choice(
        "Rundenlänge",
        values = listOf(30, 60, 120),
        labels = listOf("30 s", "60 s", "2 min"),
        get = { it.speedSeconds },
        set = { s, v -> s.copy(speedSeconds = v) },
    )

    val metronome = Option.Switch(
        "Metronom",
        hint = "Tickt leise mit, damit du im Takt bleibst.",
        checked = { it.metronome },
        set = { s, v -> s.copy(metronome = v) },
    )

    val metronomeBpm = choice(
        "Tempo",
        hint = "Schläge pro Minute.",
        values = listOf(50, 60, 80, 100),
        labels = listOf("50", "60", "80", "100"),
        get = { it.metronomeBpm },
        set = { s, v -> s.copy(metronomeBpm = v) },
    )

    val rhythmLevel = choice(
        "Notenwerte",
        values = RhythmLevel.entries,
        labels = RhythmLevel.entries.map { it.label },
        get = { it.rhythmLevel },
        set = { s, v -> s.copy(rhythmLevel = v) },
    )

    val rhythmRests = Option.Switch(
        "Pausen",
        hint = "Streut Pausenzeichen ein, durch die du schweigst.",
        checked = { it.rhythmRests },
        set = { s, v -> s.copy(rhythmRests = v) },
    )

    val rhythmBars = choice(
        "Takte pro Runde",
        values = listOf(2, 4, 8),
        labels = listOf("2", "4", "8"),
        get = { it.rhythmBars },
        set = { s, v -> s.copy(rhythmBars = v) },
    )

    /** Shown in every exercise, below its own options. */
    val general: List<Option> = listOf(notation, inputMode, sound, voice)

    fun of(exercise: Exercise): List<Option> = when (exercise) {
        Exercise.FRETBOARD -> listOf(strings, showTab, hideKeyLabels)
        Exercise.CHORDS -> listOf(keyChoice, keyOrder, chordRootGiven, gripRootInBass, hideKeyLabels)
        Exercise.SHEET -> listOf(sheetRange, sheetExactOctave, hideStringNames, hideKeyLabels)
        Exercise.INTERVALS -> listOf(intervalDifficulty, hideKeyLabels)
        Exercise.SCALE -> listOf(keyChoice, keyOrder, showTab, hideKeyLabels)
        Exercise.EAR_INTERVAL -> listOf(earIntervalSet, earHarmonic)
        Exercise.EAR_QUALITY, Exercise.EAR_CHORD -> listOf(earHarmonic)
        Exercise.EAR_PITCH -> listOf(earReference, hideKeyLabels)
        Exercise.SHEET_RHYTHM ->
            listOf(rhythmLevel, rhythmRests, rhythmBars, metronomeBpm, sheetRange, sheetExactOctave, hideStringNames, hideKeyLabels)
        Exercise.SHEET_SPEED ->
            listOf(speedSeconds, metronome, metronomeBpm, sheetRange, sheetExactOctave, hideStringNames, hideKeyLabels)
    }
}
