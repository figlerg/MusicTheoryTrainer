package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.data.InputMode
import at.gigler.musictheorytrainer.theory.EnteredNote
import at.gigler.musictheorytrainer.theory.Guitar
import at.gigler.musictheorytrainer.theory.Notation
import at.gigler.musictheorytrainer.theory.NoteNames
import at.gigler.musictheorytrainer.theory.PitchClass
import at.gigler.musictheorytrainer.theory.Spelling

enum class InputState {
    /** Waiting for an answer (also after a wrong one: trying again is just answering again). */
    ACCEPTING,

    /** Answer given, the screen moves on by itself. */
    LOCKED,

    /** Solution shown, the user moves on with "Weiter". */
    CONTINUE,
}

/**
 * Free text, piano keys or a fretboard in first position. [mode] is the saved setting, so a
 * switch here sticks across questions, screens and restarts. Where the fretboard would give the
 * answer away ([allowGuitar] = false) the keys stand in for it.
 */
@Composable
fun NoteInput(
    notation: Notation,
    mode: InputMode,
    onModeChange: (InputMode) -> Unit,
    state: InputState,
    onNote: (EnteredNote) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    allowGuitar: Boolean = true,
    guitarMarks: List<FretMark> = emptyList(),
    onUnreadable: () -> Unit = {},
) {
    val effective = if (mode == InputMode.GUITAR && !allowGuitar) InputMode.KEYS else mode
    Column(modifier.fillMaxWidth()) {
        ModeSwitch(effective, allowGuitar, onModeChange)
        when (effective) {
            InputMode.TEXT -> TextNoteInput(notation, state, onNote, onContinue, onUnreadable)
            InputMode.KEYS -> ContinueOr(state, onContinue, KEY_HEIGHT * 2 + KEY_GAP) {
                PianoKeys(notation, enabled = state == InputState.ACCEPTING) { onNote(EnteredNote(it)) }
            }
            InputMode.GUITAR -> ContinueOr(state, onContinue, GUITAR_KEYS_HEIGHT) {
                GuitarKeys(
                    notation = notation,
                    enabled = state == InputState.ACCEPTING,
                    marks = guitarMarks,
                    onTap = { onNote(EnteredNote(Guitar.pitchAt(it), position = it)) },
                )
            }
        }
    }
}

@Composable
private fun ModeSwitch(current: InputMode, allowGuitar: Boolean, onModeChange: (InputMode) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        for (mode in InputMode.entries) {
            if (mode == InputMode.GUITAR && !allowGuitar) continue
            val selected = mode == current
            TextButton(
                onClick = { onModeChange(mode) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    when (mode) {
                        InputMode.TEXT -> "Text"
                        InputMode.KEYS -> "Tasten"
                        InputMode.GUITAR -> "Gitarre"
                    },
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

/** Keeps the input's height and puts a big "Weiter" where the thumb already is. */
@Composable
private fun ContinueOr(state: InputState, onContinue: () -> Unit, height: Dp, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().height(height)) {
        if (state == InputState.CONTINUE) {
            Button(onClick = onContinue, modifier = Modifier.fillMaxSize()) {
                Text("Weiter", style = MaterialTheme.typography.titleLarge)
            }
        } else {
            content()
        }
    }
}

@Composable
private fun ColumnScope.TextNoteInput(
    notation: Notation,
    state: InputState,
    onNote: (EnteredNote) -> Unit,
    onContinue: () -> Unit,
    onUnreadable: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var unreadable by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun submit() {
        when (state) {
            InputState.LOCKED -> return
            InputState.CONTINUE -> onContinue()
            InputState.ACCEPTING -> {
                val note = EnteredNote.fromText(text, notation)
                if (note == null) {
                    if (text.isNotBlank()) {
                        unreadable = true
                        onUnreadable()
                    }
                } else {
                    text = ""
                    onNote(note)
                }
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        // The field stays enabled between questions so the soft keyboard does not close.
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                unreadable = false
            },
            modifier = Modifier.weight(1f).focusRequester(focus),
            singleLine = true,
            isError = unreadable,
            placeholder = { Text("Ton") },
            supportingText = { if (unreadable) Text("Unbekannter Ton") },
            textStyle = MaterialTheme.typography.headlineSmall,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = ::submit,
            enabled = state != InputState.LOCKED,
            modifier = Modifier.height(56.dp),
        ) {
            Text(if (state == InputState.CONTINUE) "Weiter" else "OK")
        }
    }
}

private val KEY_HEIGHT = 56.dp
private val KEY_GAP = 6.dp

/**
 * Two rows laid out like a piano: black keys sit between the white keys they belong to.
 * Weights are in units of one white key (7 in total per row).
 */
@Composable
private fun PianoKeys(notation: Notation, enabled: Boolean, onNote: (PitchClass) -> Unit) {
    val whites = listOf(0, 2, 4, 5, 7, 9, 11).map(PitchClass::of)
    Column(Modifier.alpha(if (enabled) 1f else 0.5f), verticalArrangement = Arrangement.spacedBy(KEY_GAP)) {
        Row(Modifier.fillMaxWidth().height(KEY_HEIGHT)) {
            Gap(0.55f)
            BlackKey(1, notation, enabled, onNote)
            Gap(0.1f)
            BlackKey(3, notation, enabled, onNote)
            Gap(1.1f)
            BlackKey(6, notation, enabled, onNote)
            Gap(0.1f)
            BlackKey(8, notation, enabled, onNote)
            Gap(0.1f)
            BlackKey(10, notation, enabled, onNote)
            Gap(0.55f)
        }
        Row(Modifier.fillMaxWidth().height(KEY_HEIGHT), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            whites.forEach { pitch ->
                Key(
                    onClick = { onNote(pitch) },
                    enabled = enabled,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        NoteNames.name(pitch, notation, Spelling.SHARP),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.Gap(weight: Float) = Spacer(Modifier.weight(weight))

@Composable
private fun RowScope.BlackKey(semitone: Int, notation: Notation, enabled: Boolean, onNote: (PitchClass) -> Unit) {
    val pitch = PitchClass.of(semitone)
    Key(
        onClick = { onNote(pitch) },
        enabled = enabled,
        color = Color(0xFF1C1B1F),
        contentColor = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.weight(0.9f),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(NoteNames.name(pitch, notation, Spelling.SHARP), style = MaterialTheme.typography.titleSmall)
            Text(NoteNames.name(pitch, notation, Spelling.FLAT), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun Key(
    onClick: () -> Unit,
    enabled: Boolean,
    color: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        color = color,
        contentColor = contentColor,
        border = border,
        modifier = modifier.fillMaxHeight(),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
