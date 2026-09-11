package at.gigler.musictheorytrainer.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Material has no "success" color, so correct answers get their own green. */
@Immutable
data class FeedbackColors(val correct: Color, val onCorrect: Color)

private val LightFeedback = FeedbackColors(correct = Color(0xFF2E7D32), onCorrect = Color.White)
private val DarkFeedback = FeedbackColors(correct = Color(0xFF81C784), onCorrect = Color(0xFF0B2E0F))

val LocalFeedbackColors = staticCompositionLocalOf { LightFeedback }

@Composable
fun TrainerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    CompositionLocalProvider(LocalFeedbackColors provides if (dark) DarkFeedback else LightFeedback) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
