package at.gigler.musictheorytrainer.theory

import java.net.URLEncoder
import java.util.Base64

/** One step of a Strudel lesson: some text, then the code to try. */
data class StrudelLesson(val title: String, val text: List<String>, val code: String)

/** A topic such as the basics or one genre, with its introduction. */
data class StrudelChapter(val title: String, val intro: List<String>, val lessons: List<StrudelLesson>)

/**
 * The lessons live in one Markdown file that the app shows and a script checks against Strudel
 * itself (tools/strudel-check). No Strudel code ships with the app: examples open on strudel.cc.
 */
object Strudel {
    const val SITE = "https://strudel.cc/"

    /**
     * strudel.cc reads code from the URL fragment: Base64 of the UTF-8 text, then URI-encoded
     * (code2hash in @strudel/core). The String overload of encode keeps this working below API 33.
     */
    fun shareUrl(code: String): String =
        SITE + "#" + URLEncoder.encode(Base64.getEncoder().encodeToString(code.toByteArray(Charsets.UTF_8)), "UTF-8")

    /** "# Chapter", "## Lesson", paragraphs separated by blank lines, and one ```strudel block per lesson. */
    fun parse(markdown: String): List<StrudelChapter> {
        val chapters = mutableListOf<StrudelChapter>()
        var chapterTitle: String? = null
        var intro = mutableListOf<String>()
        var lessons = mutableListOf<StrudelLesson>()
        var lessonTitle: String? = null
        var text = mutableListOf<String>()
        var code: String? = null
        val paragraph = StringBuilder()

        fun endParagraph() {
            if (paragraph.isEmpty()) return
            (if (lessonTitle == null) intro else text) += paragraph.toString()
            paragraph.clear()
        }

        fun endLesson() {
            endParagraph()
            val title = lessonTitle ?: return
            lessons += StrudelLesson(title, text, requireNotNull(code) { "Lesson \"$title\" has no strudel block" })
            lessonTitle = null
            text = mutableListOf()
            code = null
        }

        fun endChapter() {
            endLesson()
            val title = chapterTitle ?: return
            chapters += StrudelChapter(title, intro, lessons)
            intro = mutableListOf()
            lessons = mutableListOf()
        }

        val lines = markdown.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.startsWith("## ") -> {
                    endLesson()
                    lessonTitle = line.removePrefix("## ").trim()
                }
                line.startsWith("# ") -> {
                    endChapter()
                    chapterTitle = line.removePrefix("# ").trim()
                }
                line.trim() == "```strudel" -> {
                    endParagraph()
                    val body = mutableListOf<String>()
                    i++
                    while (i < lines.size && lines[i].trim() != "```") body += lines[i++]
                    code = body.joinToString("\n")
                }
                line.isBlank() -> endParagraph()
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(line.trim())
                }
            }
            i++
        }
        endChapter()
        return chapters
    }
}
