package at.gigler.musictheorytrainer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import at.gigler.musictheorytrainer.BuildConfig
import at.gigler.musictheorytrainer.audio.Sound
import at.gigler.musictheorytrainer.audio.TonePlayer
import at.gigler.musictheorytrainer.data.AppStore
import at.gigler.musictheorytrainer.data.Category
import at.gigler.musictheorytrainer.data.Exercise
import at.gigler.musictheorytrainer.data.PracticeEntry
import at.gigler.musictheorytrainer.data.PracticeLog
import at.gigler.musictheorytrainer.data.Settings
import at.gigler.musictheorytrainer.data.optionSummary
import kotlinx.coroutines.launch
import java.io.File

/**
 * Screens are addressed by a small string so they survive process death: null is the home screen,
 * "CAT:<name>" a category, "EX:<name>" an exercise, [SETTINGS_SCREEN] the settings.
 */
private const val SETTINGS_SCREEN = "SETTINGS"
private const val STATS_SCREEN = "STATS"
private const val CATEGORY_PREFIX = "CAT:"
private const val EXERCISE_PREFIX = "EX:"

/** Back goes one level up: exercise to its category, everything else home. */
private fun parentOf(screen: String): String? = when {
    screen.startsWith(EXERCISE_PREFIX) ->
        CATEGORY_PREFIX + Exercise.valueOf(screen.removePrefix(EXERCISE_PREFIX)).category.name
    else -> null
}

@Composable
fun App(store: AppStore, player: TonePlayer) {
    val settings by store.settings.collectAsState(initial = null)
    val scores by store.scores.collectAsState(initial = emptyMap())
    val periodStart by store.periodStart.collectAsState(initial = 0L)
    val context = LocalContext.current
    val log = remember(context) { PracticeLog(File(context.filesDir, "practice-log.tsv")) }
    val scope = rememberCoroutineScope()
    var screen by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = screen != null) { screen = screen?.let(::parentOf) }
    // Leaving a screen ends whatever it was playing.
    LaunchedEffect(screen) { player.stop() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // DataStore answers within milliseconds; showing nothing until then avoids flashing defaults.
        val current = settings ?: return@Surface
        val sound = remember(current.sound, current.voice) { Sound(player, current.sound, current.voice) }
        val up = { screen = screen?.let(::parentOf) }
        fun record(exercise: Exercise): (Boolean) -> Unit = { correct ->
            scope.launch {
                store.record(exercise, correct)
                // Every answer also goes into the practice log, which is never cleared.
                log.append(
                    PracticeEntry(
                        time = System.currentTimeMillis(),
                        exercise = exercise,
                        mode = current.inputMode,
                        hit = correct,
                        appVersion = BuildConfig.VERSION_CODE,
                        options = optionSummary(exercise, current),
                    ),
                )
            }
        }
        val update: ((Settings) -> Settings) -> Unit = { transform -> scope.launch { store.updateSettings(transform) } }

        Box(Modifier.safeDrawingPadding()) {
            val s = screen
            when {
                s == null -> HomeScreen(
                    scores = scores,
                    onOpen = { screen = CATEGORY_PREFIX + it.name },
                    onSettings = { screen = SETTINGS_SCREEN },
                )

                s == SETTINGS_SCREEN -> SettingsScreen(
                    settings = current,
                    onChange = update,
                    onStats = { screen = STATS_SCREEN },
                    onBack = up,
                )

                s == STATS_SCREEN -> StatsScreen(
                    log = log,
                    periodStart = periodStart,
                    onResetPeriod = { scope.launch { store.resetScores() } },
                    onBack = { screen = SETTINGS_SCREEN },
                )

                s.startsWith(CATEGORY_PREFIX) -> CategoryScreen(
                    category = Category.valueOf(s.removePrefix(CATEGORY_PREFIX)),
                    scores = scores,
                    onOpen = { screen = EXERCISE_PREFIX + it.name },
                    onBack = up,
                )

                else -> when (val exercise = Exercise.valueOf(s.removePrefix(EXERCISE_PREFIX))) {
                    Exercise.EAR_INTERVAL, Exercise.EAR_QUALITY, Exercise.EAR_CHORD, Exercise.EAR_PITCH ->
                        EarScreen(exercise, current, sound, record(exercise), update, up)
                    Exercise.FRETBOARD -> FretboardScreen(current, sound, record(exercise), update, up)
                    Exercise.INTERVALS -> IntervalScreen(current, sound, record(exercise), update, up)
                    Exercise.SCALE -> ScaleScreen(current, sound, record(exercise), update, up)
                    Exercise.CHORDS -> ChordScreen(current, sound, record(exercise), update, up)
                    Exercise.SHEET -> SheetScreen(current, sound, record(exercise), update, up)
                }
            }
        }
    }
}
