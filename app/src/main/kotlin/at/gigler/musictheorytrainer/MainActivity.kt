package at.gigler.musictheorytrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import at.gigler.musictheorytrainer.data.AppStore
import at.gigler.musictheorytrainer.ui.App
import at.gigler.musictheorytrainer.ui.TrainerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = AppStore(applicationContext)
        setContent {
            TrainerTheme {
                App(store)
            }
        }
    }
}
