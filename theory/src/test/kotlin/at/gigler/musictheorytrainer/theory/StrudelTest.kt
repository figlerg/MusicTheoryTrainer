package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URLDecoder
import java.util.Base64

class StrudelTest {

    @Test
    fun `share link matches the example in the strudel source`() {
        // website/src/repl/util.mjs: "looking like https://strudel.cc/#ImMzIGUzIg%3D%3D"
        assertEquals("https://strudel.cc/#ImMzIGUzIg%3D%3D", Strudel.shareUrl("\"c3 e3\""))
    }

    @Test
    fun `share link survives umlauts and line breaks`() {
        val code = "// Grüße\n\$: s(\"bd*4\")"
        val hash = Strudel.shareUrl(code).substringAfter('#')
        assertTrue(hash.none { it == '+' || it == '/' || it == '=' })
        assertEquals(code, String(Base64.getDecoder().decode(URLDecoder.decode(hash, "UTF-8")), Charsets.UTF_8))
    }

    @Test
    fun `parses chapters, lessons, paragraphs and code`() {
        val chapters = Strudel.parse(
            """
            # Grundlagen

            Einleitung, erste Zeile
            und zweite Zeile.

            ## Der erste Schlag

            Text vorher.

            ```strudel
            setcpm(120/4)
            ${'$'}: s("bd")
            ```

            # Techno

            ## Kick

            ```strudel
            s("bd*4")
            ```
            """.trimIndent(),
        )
        assertEquals(listOf("Grundlagen", "Techno"), chapters.map { it.title })
        assertEquals(listOf("Einleitung, erste Zeile und zweite Zeile."), chapters[0].intro)
        val first = chapters[0].lessons.single()
        assertEquals("Der erste Schlag", first.title)
        assertEquals(listOf("Text vorher."), first.text)
        assertEquals("setcpm(120/4)\n\$: s(\"bd\")", first.code)
        assertEquals("s(\"bd*4\")", chapters[1].lessons.single().code)
        assertTrue(chapters[1].intro.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a lesson without code is an error`() {
        Strudel.parse("# A\n\n## B\n\nNur Text.\n")
    }

    @Test
    fun `the app's lessons parse into the four chapters`() {
        val chapters = Strudel.parse(File("../app/src/main/assets/strudel.md").readText())
        assertEquals(listOf("Grundlagen", "Techno", "Drum and Bass", "Industrial und Aggrotech"), chapters.map { it.title })
        for (chapter in chapters) {
            assertTrue(chapter.title, chapter.intro.isNotEmpty())
            assertTrue(chapter.title, chapter.lessons.size >= 4)
            chapter.lessons.forEach { assertTrue(it.title, it.code.isNotBlank() && it.text.isNotEmpty()) }
        }
    }
}
