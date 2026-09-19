package at.gigler.musictheorytrainer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PracticeStatsTest {

    private val start = 1_700_000_000_000L

    private fun entry(offsetSeconds: Long, exercise: Exercise = Exercise.FRETBOARD, hit: Boolean = true, mode: InputMode = InputMode.TEXT) =
        PracticeEntry(start + offsetSeconds * 1000, exercise, mode, hit, appVersion = 4, options = "Saiten=012345")

    @Test
    fun `a line survives encoding and decoding`() {
        val original = entry(0, Exercise.SHEET, hit = false, mode = InputMode.GUITAR)
        assertEquals(original, PracticeEntry.decode(original.encode()))
    }

    @Test
    fun `unreadable lines are skipped, unknown extra fields are kept out of the way`() {
        assertNull(PracticeEntry.decode(""))
        assertNull(PracticeEntry.decode("not a line"))
        val withExtra = "${start}\tFRETBOARD\tTEXT\t1\t4\tSaiten=01\tsomething new"
        assertEquals(Exercise.FRETBOARD, PracticeEntry.decode(withExtra)?.exercise)
    }

    @Test
    fun `practice time counts the gaps between answers, not the breaks`() {
        val entries = listOf(entry(0), entry(10), entry(25), entry(5000))
        val summary = PracticeStats.summarise(entries)
        assertEquals(4, summary.total.attempts)
        // 5 s for the first answer, then 10 s and 15 s measured, and 5 s for the one after the break.
        assertEquals(5_000L + 10_000L + 15_000L + 5_000L, summary.total.millis)
    }

    @Test
    fun `switching exercise starts a fresh measurement`() {
        val entries = listOf(entry(0), entry(10, Exercise.SCALE), entry(20, Exercise.SCALE))
        val summary = PracticeStats.summarise(entries)
        assertEquals(PracticeStats.SINGLE_ANSWER_MILLIS, summary.byExercise.getValue(Exercise.FRETBOARD).millis)
        assertEquals(PracticeStats.SINGLE_ANSWER_MILLIS + 10_000L, summary.byExercise.getValue(Exercise.SCALE).millis)
    }

    @Test
    fun `hit rate per exercise and input mode`() {
        val entries = listOf(
            entry(0, hit = true),
            entry(10, hit = false),
            entry(20, Exercise.SHEET, hit = true, mode = InputMode.GUITAR),
        )
        val summary = PracticeStats.summarise(entries)
        assertEquals(50, summary.byExercise.getValue(Exercise.FRETBOARD).percent)
        assertEquals(100, summary.byExercise.getValue(Exercise.SHEET).percent)
        assertEquals(1, summary.byMode.getValue(InputMode.GUITAR).attempts)
        assertEquals(67, summary.total.percent)
    }

    @Test
    fun `a period only counts newer answers but keeps the timing of the older ones`() {
        val entries = listOf(entry(0), entry(10), entry(20))
        val summary = PracticeStats.summarise(entries, since = start + 15_000)
        assertEquals(1, summary.total.attempts)
        assertEquals(10_000L, summary.total.millis)
    }

    @Test
    fun `answers are grouped into local days`() {
        val entries = listOf(entry(0), entry(60 * 60 * 30), entry(60 * 60 * 30 + 30))
        val summary = PracticeStats.summarise(entries)
        assertEquals(2, summary.activeDays)
        assertEquals(1, summary.byDay.first().second.attempts)
        assertEquals(2, summary.byDay.last().second.attempts)
    }
}
