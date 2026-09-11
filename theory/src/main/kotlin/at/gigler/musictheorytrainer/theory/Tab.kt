package at.gigler.musictheorytrainer.theory

object Tab {
    /**
     * ASCII tab, high e on top. Each column holds the positions played at the same time.
     *
     * ```
     * e|--------|
     * …
     * E|-0---3--|
     * ```
     */
    fun render(columns: List<List<FretPosition>>, notation: Notation): String {
        val width = columns.flatten().maxOfOrNull { it.fret.toString().length } ?: 1
        return (Guitar.STRING_COUNT - 1 downTo 0).joinToString("\n") { string ->
            val cells = columns.joinToString("") { column ->
                val fret = column.firstOrNull { it.string == string }?.fret
                "-" + (fret?.toString() ?: "").padEnd(width, '-') + "-"
            }
            "${Guitar.stringName(string, notation)}|$cells|"
        }
    }

    fun renderSequence(positions: List<FretPosition>, notation: Notation): String =
        render(positions.map { listOf(it) }, notation)
}
