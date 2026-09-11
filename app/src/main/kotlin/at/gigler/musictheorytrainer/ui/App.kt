package at.gigler.musictheorytrainer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import at.gigler.musictheorytrainer.audio.Sound
import at.gigler.musictheorytrainer.audio.TonePlayer
import at.gigler.musictheorytrainer.data.AppStore
import at.gigler.musictheorytrainer.data.Exercise
import at.gigler.musictheorytrainer.data.Settings
import kotlinx.coroutines.launch

/** null is the home screen, [SETTINGS_SCREEN] the settings, anything else an exercise. */
private const val SETTINGS_SCREEN = "SETTINGS"

@Composable
fun App(store: AppStore) {
    val settings by store.settings.collectAsState(initial = null)
    val scores by store.scores.collectAsState(initial = emptyMap())
    val player = remember { TonePlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    val scope = rememberCoroutineScope()
    var screen by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = screen != null) { screen = null }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // DataStore answers within milliseconds; showing nothing until then avoids flashing defaults.
        val current = settings ?: return@Surface
        val sound = remember(current.sound) { Sound(player, current.sound) }
        val home = { screen = null }
        fun record(exercise: Exercise): (Boolean) -> Unit = { correct -> scope.launch { store.record(exercise, correct) } }
        val update: ((Settings) -> Settings) -> Unit = { transform -> scope.launch { store.updateSettings(transform) } }

        Box(Modifier.safeDrawingPadding()) {
            when (val s = screen) {
                null -> HomeScreen(scores = scores, onOpen = { screen = it.name }, onSettings = { screen = SETTINGS_SCREEN })
                SETTINGS_SCREEN -> SettingsScreen(
                    settings = current,
                    onChange = update,
                    onResetScores = { scope.launch { store.resetScores() } },
                    onBack = home,
                )
                else -> when (val exercise = Exercise.valueOf(s)) {
                    Exercise.FRETBOARD -> FretboardScreen(current, sound, record(exercise), update, home)
                    Exercise.INTERVALS -> IntervalScreen(current, sound, record(exercise), update, home)
                    Exercise.SCALE -> ScaleScreen(current, sound, record(exercise), update, home)
                    Exercise.CHORDS -> ChordScreen(current, sound, record(exercise), update, home)
                    Exercise.SHEET -> SheetScreen(current, sound, record(exercise), update, home)
                }
            }
        }
    }
}
