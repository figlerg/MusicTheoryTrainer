package at.gigler.musictheorytrainer.data

/**
 * Scores only mean something within the same setup: reading on the guitar is a different task
 * than reading on the keys, and the low strings are harder than the middle of the staff. So a
 * best score belongs to the setup it was played in, and a different setup keeps its own.
 */
fun variantKey(exercise: Exercise, settings: Settings): String = when (exercise) {
    Exercise.SHEET_SPEED -> listOf(
        "speed",
        settings.inputMode.name,
        settings.sheetRange.name,
        settings.speedSeconds.toString(),
        if (settings.sheetExactOctave) "oct" else "any",
    ).joinToString("_")

    Exercise.SHEET_RHYTHM -> listOf(
        "rhythm",
        settings.inputMode.name,
        settings.sheetRange.name,
        settings.rhythmLevel.name,
        settings.rhythmBars.toString(),
        settings.metronomeBpm.toString(),
        if (settings.rhythmRests) "rests" else "plain",
        if (settings.sheetExactOctave) "oct" else "any",
    ).joinToString("_")

    else -> exercise.name.lowercase()
}

/** The same setup in words, so the screen can say which best score it is showing. */
fun variantLabel(exercise: Exercise, settings: Settings): String {
    val mode = when (settings.inputMode) {
        InputMode.TEXT -> "Text"
        InputMode.KEYS -> "Tasten"
        InputMode.GUITAR -> "Gitarre"
    }
    val range = settings.sheetRange.label.substringBefore(" (")
    return when (exercise) {
        Exercise.SHEET_SPEED -> listOf(mode, range, "${settings.speedSeconds} s").joinToString(" · ")
        Exercise.SHEET_RHYTHM -> listOf(
            mode,
            range,
            settings.rhythmLevel.label,
            "${settings.rhythmBars} Takte",
            "${settings.metronomeBpm} bpm",
        ).joinToString(" · ")

        else -> mode
    }
}
