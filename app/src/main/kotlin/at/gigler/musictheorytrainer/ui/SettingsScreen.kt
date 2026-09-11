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
import androidx.compose.material3.HorizontalDivider
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
import at.gigler.musictheorytrainer.data.SheetSource
import at.gigler.musictheorytrainer.data.StringSet
import at.gigler.musictheorytrainer.theory.IntervalDifficulty
import at.gigler.musictheorytrainer.theory.KeyChoice
import at.gigler.musictheorytrainer.theory.KeyOrder
import at.gigler.musictheorytrainer.theory.Notation
import at.gigler.musictheorytrainer.theory.StaffRange

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
            Section("Eingabe") {
                Segmented(
                    InputMode.entries,
                    settings.inputMode,
                    {
                        when (it) {
                            InputMode.TEXT -> "Text"
                            InputMode.KEYS -> "Tasten"
                            InputMode.GUITAR -> "Gitarre"
                        }
                    },
                    { value -> onChange { it.copy(inputMode = value) } },
                )
            }
            SwitchRow("Tab-Ansicht", settings.showTab) { value -> onChange { it.copy(showTab = value) } }
            SwitchRow("Ton abspielen", settings.sound) { value -> onChange { it.copy(sound = value) } }

            Group("Griffbrett-Töne")
            Section("Saiten") {
                Segmented(
                    StringSet.entries,
                    settings.stringSet,
                    { if (it == StringSet.LOW_E_AND_A) "E + A" else "Alle 6" },
                    { value -> onChange { it.copy(stringSet = value) } },
                )
            }

            Group("Intervalle")
            Section("Schwierigkeit") {
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

            Group("Tonleitern und Akkorde")
            Section("Tonarten") {
                Segmented(
                    KeyChoice.entries,
                    settings.keyChoice,
                    {
                        when (it) {
                            KeyChoice.MAJOR -> "Dur"
                            KeyChoice.MINOR -> "Moll"
                            KeyChoice.BOTH -> "Beide"
                        }
                    },
                    { value -> onChange { it.copy(keyChoice = value) } },
                )
            }
            Section("Reihenfolge") {
                Segmented(
                    KeyOrder.entries,
                    settings.keyOrder,
                    { if (it == KeyOrder.CIRCLE_OF_FIFTHS) "Quintenzirkel" else "Zufall" },
                    { value -> onChange { it.copy(keyOrder = value) } },
                )
            }
            SwitchRow("Akkorde: Grundton vorgegeben", settings.chordRootGiven) { value -> onChange { it.copy(chordRootGiven = value) } }
            SwitchRow("Griffe: Grundton muss tiefster Ton sein", settings.gripRootInBass) { value -> onChange { it.copy(gripRootInBass = value) } }

            Group("Notenlesen")
            Section("Tonumfang (Zufall)") {
                Segmented(
                    StaffRange.entries,
                    settings.sheetRange,
                    { if (it == StaffRange.IN_STAFF) "Im System" else "Mit Hilfslinien" },
                    { value -> onChange { it.copy(sheetRange = value) } },
                )
            }
            SwitchRow("Gitarre: exakte Oktave", settings.sheetExactOctave) { value -> onChange { it.copy(sheetExactOctave = value) } }

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
private fun Group(title: String) {
    HorizontalDivider(Modifier.padding(top = 24.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
    content()
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(top = 8.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
