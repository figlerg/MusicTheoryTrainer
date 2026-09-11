package at.gigler.musictheorytrainer.theory

import org.junit.Assert.assertEquals
import org.junit.Test

class TabTest {

    @Test
    fun `sequence across two strings`() {
        val tab = Tab.renderSequence(
            listOf(FretPosition(0, 0), FretPosition(0, 3), FretPosition(1, 12)),
            Notation.GERMAN,
        )
        val expected = """
            e|------------|
            H|------------|
            G|------------|
            D|------------|
            A|---------12-|
            E|-0---3------|
        """.trimIndent()
        assertEquals(expected, tab)
    }

    @Test
    fun `e major on the low e string`() {
        val tab = Tab.renderSequence(Guitar.majorScaleOnString(PitchClass.E, 0), Notation.GERMAN)
        assertEquals("E|-0---2---4---5---7---9---11--12-|", tab.lines().last())
    }

    @Test
    fun `single digit frets and english string names`() {
        val tab = Tab.renderSequence(listOf(FretPosition(5, 5)), Notation.ENGLISH)
        assertEquals(listOf("e|-5-|", "B|---|", "G|---|", "D|---|", "A|---|", "E|---|"), tab.lines())
    }
}
