package at.gigler.musictheorytrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import at.gigler.musictheorytrainer.audio.TonePlayer
import at.gigler.musictheorytrainer.data.AppStore
import at.gigler.musictheorytrainer.ui.App
import at.gigler.musictheorytrainer.ui.TrainerTheme

class MainActivity : ComponentActivity() {
    private val player = TonePlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = AppStore(applicationContext)
        setContent {
            TrainerTheme {
                App(store, player)
            }
        }
    }

    /** Nothing keeps playing once the app is in the background. */
    override fun onStop() {
        super.onStop()
        player.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
