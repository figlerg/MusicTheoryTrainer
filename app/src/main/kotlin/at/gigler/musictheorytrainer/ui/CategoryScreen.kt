package at.gigler.musictheorytrainer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.data.Category
import at.gigler.musictheorytrainer.data.Exercise
import at.gigler.musictheorytrainer.data.Score

@Composable
fun CategoryScreen(
    category: Category,
    scores: Map<Exercise, Score>,
    onOpen: (Exercise) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(category.title, onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(
                category.hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            val exercises = Exercise.of(category)
            if (exercises.isEmpty()) {
                Text("Kommt bald.", style = MaterialTheme.typography.bodyLarge)
            }
            for (exercise in exercises) {
                val score = scores[exercise] ?: Score()
                ScoreCard(
                    title = exercise.title,
                    subtitle = if (score.total == 0) exercise.hint else "${score.correct} von ${score.total} richtig",
                    score = score,
                    onClick = { onOpen(exercise) },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
