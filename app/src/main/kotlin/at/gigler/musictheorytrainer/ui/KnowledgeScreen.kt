package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.data.Settings
import at.gigler.musictheorytrainer.theory.Grips
import at.gigler.musictheorytrainer.theory.Guitar
import at.gigler.musictheorytrainer.theory.Notation
import at.gigler.musictheorytrainer.theory.NoteNames
import at.gigler.musictheorytrainer.theory.Piano
import at.gigler.musictheorytrainer.theory.PitchClass
import at.gigler.musictheorytrainer.theory.Recorder
import at.gigler.musictheorytrainer.theory.RecorderSystem
import at.gigler.musictheorytrainer.theory.Spelling

private enum class Instrument(val title: String) { RECORDER("Blockflöte"), PIANO("Klavier"), GUITAR("Gitarre") }

/**
 * What a beginner should know about each instrument, as a page to look things up. Only the
 * standard basics; the exercises themselves stay free of explanations.
 */
@Composable
fun KnowledgeScreen(settings: Settings, onBack: () -> Unit) {
    var instrument by rememberSaveable { mutableStateOf(Instrument.RECORDER) }
    ScreenScaffold(title = "Grundwissen", onBack = onBack) {
        Segmented(Instrument.entries, instrument, { it.title }, { instrument = it })
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (instrument) {
                Instrument.RECORDER -> RecorderBasics(settings.recorderSystem, settings.notation)
                Instrument.PIANO -> PianoBasics(settings.notation)
                Instrument.GUITAR -> GuitarBasics(settings.notation)
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
}

@Composable
private fun Point(text: String) {
    Row {
        Text("•", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ColumnScope.RecorderBasics(system: RecorderSystem, notation: Notation) {
    Heading("Das Instrument")
    Point("Die übliche Schulflöte ist die Sopranflöte in C. Ihre Noten stehen im Violinschlüssel, sie klingt eine Oktave höher als notiert.")
    Point("Acht Löcher: 0 ist das Daumenloch auf der Rückseite, 1 bis 3 deckt die linke Hand (Zeige-, Mittel-, Ringfinger), 4 bis 7 die rechte Hand (Zeige-, Mittel-, Ringfinger, kleiner Finger).")
    Point("Die unteren beiden Löcher sind oft Doppellöcher. Für die Griffe hier deckst du sie immer ganz ab.")

    Heading("Halten und anblasen")
    Point("Linke Hand oben am Mundstück, rechte Hand unten. Der rechte Daumen stützt die Flöte von hinten.")
    Point("Die Löcher mit den flachen Fingerkuppen ganz abdecken. Schon ein kleiner Spalt lässt den Ton quietschen.")
    Point("Das Mundstück nur leicht zwischen die Lippen nehmen, nicht zwischen die Zähne.")
    Point("Sanft blasen, wie warmer Atem. Zu viel Luft überbläst den Ton, er springt nach oben und wird schrill.")
    Point("Jeden Ton mit der Zunge beginnen, als würdest du „dü“ sagen.")
    Point("Je tiefer der Ton, desto sanfter blasen. c' und d' sind am schwersten, weil dafür fast alle Löcher dicht sein müssen.")

    Heading("Der Anfang")
    Point("Die meisten Flötenschulen beginnen mit h', a' und g'. Dafür braucht es nur die linke Hand.")
    Point("Für die hohen Töne ab e'' bleibt das Daumenloch halb offen: den Daumen beugen, so dass der Nagel einen kleinen Spalt freilässt.")

    Heading("Barock oder deutsch")
    Point("Es gibt zwei Griffsysteme. Sie unterscheiden sich vor allem beim f'. Heute ist barock üblich, deutsche Flöten waren lange in Schulen verbreitet.")
    Point("Oft steht die Griffweise auf der Flöte. Sonst: f' mit 0 1 2 3 4 greifen. Klingt das sauber, ist es eine deutsche Flöte, sonst eine barocke.")
    Point("Die Griffweise stellst du in den Optionen jeder Übung ein.")

    Heading("Grifftabelle (${if (system == RecorderSystem.BAROQUE) "barock" else "deutsch"})")
    for (note in Recorder.notes(system)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(note.name(notation), style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(64.dp))
            RecorderFingeringView(note.fingering, Modifier.width(220.dp), captions = false, holeGap = 2.dp)
        }
    }
    Text(
        "Schwarz: zu, weiß: offen, halb: Daumenloch halb offen.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ColumnScope.PianoBasics(notation: Notation) {
    val whites = Piano.whiteKeys(Piano.TWO_OCTAVES).toSet()
    PianoKeyboard(
        Piano.TWO_OCTAVES,
        Modifier.fillMaxWidth().height(150.dp),
        marks = mapOf(Piano.MIDDLE_C to MarkStyle.ROOT),
        label = { if (it in whites) NoteNames.name(PitchClass.of(it), notation, Spelling.SHARP) else null },
    )
    Text(
        "Markiert: das eingestrichene c', das mittlere C.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Heading("Die Tastatur")
    Point("Alle 12 Tasten wiederholt sich dasselbe Muster: 7 weiße und 5 schwarze Tasten sind eine Oktave.")
    Point("Die schwarzen Tasten liegen in Zweier- und Dreiergruppen. Das C liegt direkt links von der Zweiergruppe, das F direkt links von der Dreiergruppe.")
    Point("Weiße Tasten: C D E F G A ${if (notation == Notation.GERMAN) "H" else "B"}. Zwischen E und F und zwischen ${if (notation == Notation.GERMAN) "H" else "B"} und C gibt es keine schwarze Taste, dort ist der Schritt nur ein Halbton.")
    Point("Jede schwarze Taste hat zwei Namen, zum Beispiel ${NoteNames.bothNames(PitchClass.of(1), notation)} oder ${NoteNames.bothNames(PitchClass.of(10), notation)}.")
    Point("Ein Klavier hat 88 Tasten. Das mittlere C (c') ist das vierte C von links, etwa in der Mitte.")

    Heading("Finger")
    Point("Die Finger sind an beiden Händen nummeriert: Daumen 1, Zeigefinger 2, Mittelfinger 3, Ringfinger 4, kleiner Finger 5.")
    Point("Finger leicht gebogen, als würdest du einen Ball halten. Handgelenk locker.")
    Point("C-Dur aufwärts, rechte Hand: 1 2 3 1 2 3 4 5. Nach dem E setzt der Daumen unter.")
    Point("C-Dur aufwärts, linke Hand: 5 4 3 2 1 3 2 1. Nach dem G greift der Mittelfinger über den Daumen.")

    Heading("Noten")
    Point("Die rechte Hand liest meist den Violinschlüssel, die linke den Bassschlüssel.")
    Point("Das c' steht auf einer Hilfslinie genau zwischen den beiden Systemen: unter dem Violin- und über dem Bassschlüssel.")
    val treble = if (notation == Notation.GERMAN) "E G H D F" else "E G B D F"
    val bass = if (notation == Notation.GERMAN) "G H D F A" else "G B D F A"
    Point("Linien von unten im Violinschlüssel: $treble. Im Bassschlüssel: $bass.")
}

@Composable
private fun ColumnScope.GuitarBasics(notation: Notation) {
    Heading("Saiten und Bünde")
    Point("Sechs Saiten, von tief nach hoch: ${Guitar.ALL_STRINGS.joinToString(" ") { Guitar.stringName(it, notation) }}. Die tiefe E-Saite ist die 6., die hohe e-Saite die 1.")
    Point("Jeder Bund ist ein Halbton. Am 12. Bund ist die Oktave der leeren Saite.")
    Point("Knapp hinter dem Bundstäbchen greifen, nicht darauf und nicht in der Mitte zwischen zwei Bünden.")
    Point("Gitarrennoten klingen eine Oktave tiefer als notiert.")

    Heading("Stimmen")
    Point("Der 5. Bund einer Saite klingt wie die nächsthöhere leere Saite. Ausnahme: auf der G-Saite ist es der 4. Bund, er gibt die ${Guitar.stringName(4, notation)}-Saite.")

    Heading("Finger")
    Point("Greifhand: Zeigefinger 1, Mittelfinger 2, Ringfinger 3, kleiner Finger 4. Der Daumen bleibt hinter dem Hals.")
    Point("Anschlagshand in Noten für klassische Gitarre: p Daumen, i Zeigefinger, m Mittelfinger, a Ringfinger.")

    Heading("Tabs lesen")
    Point("Jede Linie ist eine Saite, die oberste Linie die hohe e-Saite. Die Zahl sagt, in welchem Bund du greifst, 0 heißt leere Saite.")

    Heading("Die ersten Akkorde")
    Point("Schreibweise von der tiefen E- zur hohen e-Saite: Zahl = Bund, 0 = leer, x = nicht anschlagen.")
    for (chord in Grips.FIRST_CHORDS) {
        Row(Modifier.fillMaxWidth()) {
            Text(chord.symbol(notation), style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(64.dp))
            Text(Grips.standard(chord).shorthand(), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
        }
    }
    Spacer(Modifier.height(4.dp))
}
