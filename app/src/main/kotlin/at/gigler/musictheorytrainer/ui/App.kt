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

private enum class Screen { HOME, FRETBOARD, INTERVALS, SCALE, SETTINGS }

@Composable
fun App(store: AppStore) {
    val settings by store.settings.collectAsState(initial = null)
    val scores by store.scores.collectAsState(initial = emptyMap())
    val player = remember { TonePlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    val scope = rememberCoroutineScope()
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // DataStore answers within milliseconds; showing nothing until then avoids flashing defaults.
        val current = settings ?: return@Surface
        val sound = remember(current.sound) { Sound(player, current.sound) }
        val home = { screen = Screen.HOME }
        fun record(exercise: Exercise): (Boolean) -> Unit = { correct -> scope.launch { store.record(exercise, correct) } }
        fun update(transform: (Settings) -> Settings) {
            scope.launch { store.updateSettings(transform) }
        }

        Box(Modifier.safeDrawingPadding()) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    scores = scores,
                    onOpen = {
                        screen = when (it) {
                            Exercise.FRETBOARD -> Screen.FRETBOARD
                            Exercise.INTERVALS -> Screen.INTERVALS
                            Exercise.SCALE -> Screen.SCALE
                        }
                    },
                    onSettings = { screen = Screen.SETTINGS },
                )
                Screen.FRETBOARD -> FretboardScreen(
                    settings = current,
                    sound = sound,
                    onResult = record(Exercise.FRETBOARD),
                    onReverseChange = { reverse -> update { it.copy(fretboardReverse = reverse) } },
                    onBack = home,
                )
                Screen.INTERVALS -> IntervalScreen(current, sound, record(Exercise.INTERVALS), home)
                Screen.SCALE -> ScaleScreen(current, sound, record(Exercise.SCALE), home)
                Screen.SETTINGS -> SettingsScreen(
                    settings = current,
                    onChange = ::update,
                    onResetScores = { scope.launch { store.resetScores() } },
                    onBack = home,
                )
            }
        }
    }
}
