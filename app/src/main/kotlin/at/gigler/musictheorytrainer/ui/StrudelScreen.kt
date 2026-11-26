package at.gigler.musictheorytrainer.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import at.gigler.musictheorytrainer.theory.Strudel
import at.gigler.musictheorytrainer.theory.StrudelLesson

private const val LESSONS_ASSET = "strudel.md"

/**
 * A short Strudel course. The examples run on strudel.cc in the browser: the app itself contains no
 * Strudel code and needs no network permission.
 */
@Composable
fun StrudelScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val chapters = remember { Strudel.parse(context.assets.open(LESSONS_ASSET).bufferedReader().use { it.readText() }) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val chapter = chapters[selected.coerceIn(chapters.indices)]
    val list = rememberLazyListState()
    LaunchedEffect(selected) { list.scrollToItem(0) }

    ScreenScaffold(title = "Strudel", onBack = onBack) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chapters.forEachIndexed { index, it ->
                FilterChip(selected = index == selected, onClick = { selected = index }, label = { Text(it.title) })
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = list,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    chapter.intro.forEach { Text(withCode(it), style = MaterialTheme.typography.bodyMedium) }
                }
            }
            items(chapter.lessons, key = { chapter.title + it.title }) { lesson -> LessonCard(lesson) }
            item {
                Text(
                    "Strudel (strudel.cc) ist freie Software unter der AGPL-3.0. Die App enthält keinen Strudel-Code, " +
                        "nur diese Beispiele, die dort laufen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun LessonCard(lesson: StrudelLesson) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(lesson.title, style = MaterialTheme.typography.titleMedium)
            lesson.text.forEach { Text(withCode(it), style = MaterialTheme.typography.bodyMedium) }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Long lines scroll sideways instead of wrapping: line breaks matter in code.
                SelectionContainer(Modifier.horizontalScroll(rememberScrollState()).padding(12.dp)) {
                    Text(lesson.code, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { open(context, lesson.code) }, modifier = Modifier.weight(2f).height(48.dp)) {
                    Text("In Strudel öffnen")
                }
                OutlinedButton(onClick = { copy(context, lesson.code) }, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text("Kopieren")
                }
            }
        }
    }
}

/** `code` in backticks becomes monospace; the backticks themselves are dropped. */
private fun withCode(text: String): AnnotatedString = buildAnnotatedString {
    text.split('`').forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(part) } else append(part)
    }
}

private fun open(context: Context, code: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Strudel.shareUrl(code))))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Kein Browser gefunden", Toast.LENGTH_SHORT).show()
    }
}

private fun copy(context: Context, code: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Strudel", code))
    // From Android 13 on the system confirms copying itself.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "Kopiert", Toast.LENGTH_SHORT).show()
    }
}
