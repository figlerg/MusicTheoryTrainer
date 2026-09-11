package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.data.Exercise
import at.gigler.musictheorytrainer.data.Score

@Composable
fun HomeScreen(scores: Map<Exercise, Score>, onOpen: (Exercise) -> Unit, onSettings: () -> Unit) {
    ScreenScaffold(title = "Musiktheorie", onBack = null) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(4.dp))
            for (exercise in Exercise.entries) {
                ExerciseCard(exercise.title, scores[exercise] ?: Score(), onClick = { onOpen(exercise) })
                Spacer(Modifier.height(10.dp))
            }
        }
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(56.dp)) {
            Text("Einstellungen")
        }
    }
}

@Composable
private fun ExerciseCard(title: String, score: Score, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (score.total == 0) "noch keine Versuche" else "${score.correct} von ${score.total} richtig",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                score.percent?.let { "$it %" } ?: "–",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
