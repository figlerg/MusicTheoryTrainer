package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.data.Options
import at.gigler.musictheorytrainer.data.Settings

@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    onStats: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold("Einstellungen", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Options.general.forEach { OptionRow(it, settings, onChange) }
            Text(
                "Alles Übungsspezifische stellst du in der Übung selbst unter Optionen ein.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp),
            )
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onStats, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Statistik und Übungszeit")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
