package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.data.Exercise
import at.gigler.musictheorytrainer.data.Option
import at.gigler.musictheorytrainer.data.Options
import at.gigler.musictheorytrainer.data.Settings
import at.gigler.musictheorytrainer.theory.Guitar

/** Longer than this many characters in total and the segments would truncate. */
private const val SEGMENT_BUDGET = 26

/** An exercise screen. Its parameters sit behind one button in the header, next to the title. */
@Composable
fun ExerciseScaffold(
    exercise: Exercise,
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    ScreenScaffold(
        title = exercise.title,
        onBack = onBack,
        action = { TextButton(onClick = { open = true }) { Text("Optionen") } },
        content = content,
    )
    if (open) OptionsSheet(exercise, settings, onChange) { open = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionsSheet(
    exercise: Exercise,
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
        ) {
            Text(exercise.title, style = MaterialTheme.typography.titleLarge)
            Options.of(exercise).forEach { OptionRow(it, settings, onChange) }
            HorizontalDivider(Modifier.padding(top = 24.dp))
            Text("Allgemein", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Options.general.forEach { OptionRow(it, settings, onChange) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionRow(option: Option, settings: Settings, onChange: ((Settings) -> Settings) -> Unit) {
    when (option) {
        is Option.Choice -> {
            OptionLabel(option)
            val selected = option.selected(settings)
            // Segments would cut long names off, so those become a list instead.
            if (option.labels.sumOf { it.length } > SEGMENT_BUDGET) {
                Column(Modifier.fillMaxWidth().selectableGroup()) {
                    option.labels.forEachIndexed { index, label ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = index == selected,
                                    role = Role.RadioButton,
                                    onClick = { onChange { option.select(it, index) } },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = index == selected, onClick = null)
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            } else {
                Segmented(
                    option.labels.indices.toList(),
                    selected,
                    { option.labels[it] },
                    { index -> onChange { option.select(it, index) } },
                )
            }
        }

        is Option.Switch -> {
            val checked = option.checked(settings)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(top = 12.dp)
                    .toggleable(
                        value = checked,
                        role = Role.Switch,
                        onValueChange = { value -> onChange { option.set(it, value) } },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(option.title, style = MaterialTheme.typography.bodyLarge)
                    if (option.hint.isNotEmpty()) {
                        Text(
                            option.hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(checked = checked, onCheckedChange = null)
            }
        }

        is Option.Strings -> {
            OptionLabel(option)
            val selected = option.selected(settings)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Guitar.ALL_STRINGS.forEach { string ->
                    FilterChip(
                        selected = string in selected,
                        onClick = {
                            val next = if (string in selected) selected - string else selected + string
                            onChange { option.set(it, next) }
                        },
                        label = { Text(Guitar.stringName(string, settings.notation)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionLabel(option: Option) {
    Text(
        option.title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
    )
    if (option.hint.isNotEmpty()) {
        Text(
            option.hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}
