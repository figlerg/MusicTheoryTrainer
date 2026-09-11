package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.data.InputMode
import at.gigler.musictheorytrainer.data.Settings
import at.gigler.musictheorytrainer.data.StringSet
import at.gigler.musictheorytrainer.theory.IntervalDifficulty
import at.gigler.musictheorytrainer.theory.Notation

@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    onResetScores: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }

    ScreenScaffold("Einstellungen", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Section("Notation") {
                Segmented(
                    Notation.entries,
                    settings.notation,
                    { if (it == Notation.GERMAN) "Deutsch (H, B)" else "Englisch (B, Bb)" },
                    { value -> onChange { it.copy(notation = value) } },
                )
            }
            Section("Standard-Eingabe") {
                Segmented(
                    InputMode.entries,
                    settings.inputMode,
                    { if (it == InputMode.TEXT) "Text" else "Tasten" },
                    { value -> onChange { it.copy(inputMode = value) } },
                )
            }
            Section("Griffbrett-Saiten") {
                Segmented(
                    StringSet.entries,
                    settings.stringSet,
                    { if (it == StringSet.LOW_E_AND_A) "E + A" else "Alle 6" },
                    { value -> onChange { it.copy(stringSet = value) } },
                )
            }
            Section("Intervall-Schwierigkeit") {
                Segmented(
                    IntervalDifficulty.entries,
                    settings.intervalDifficulty,
                    {
                        when (it) {
                            IntervalDifficulty.EASY -> "Halb/Ganz"
                            IntervalDifficulty.MEDIUM -> "bis 5"
                            IntervalDifficulty.HARD -> "bis 11"
                        }
                    },
                    { value -> onChange { it.copy(intervalDifficulty = value) } },
                )
            }
            SwitchRow("Tab-Ansicht", settings.showTab) { value -> onChange { it.copy(showTab = value) } }
            SwitchRow("Ton abspielen", settings.sound) { value -> onChange { it.copy(sound = value) } }
            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = { confirmReset = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Trefferquoten zurücksetzen")
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Trefferquoten zurücksetzen?") },
            confirmButton = {
                TextButton(onClick = {
                    onResetScores()
                    confirmReset = false
                }) { Text("Zurücksetzen") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
    content()
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(top = 12.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
