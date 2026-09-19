package at.gigler.musictheorytrainer.data

/** The four areas on the home screen. */
enum class Category(val title: String, val hint: String) {
    HEAR("Hören", "Intervalle und Akkorde am Klang erkennen"),
    NOTES("Noten", "Vom Blatt lesen"),
    FRETBOARD("Griffbrett", "Töne und Griffe auf der Gitarre"),
    THEORY("Theorie", "Rechnen und bauen"),
}

enum class Exercise(val category: Category, val title: String, val hint: String) {
    FRETBOARD(Category.FRETBOARD, "Griffbrett-Töne", "Ton nennen oder Stelle finden"),
    CHORDS(Category.FRETBOARD, "Akkorde", "Dreiklänge aus Tönen oder als Griff"),
    SHEET(Category.NOTES, "Notenlesen", "Noten im Violinschlüssel benennen"),
    SHEET_SPEED(Category.NOTES, "Notenlesen auf Zeit", "So viele Noten wie möglich, Tempo zählt"),
    INTERVALS(Category.THEORY, "Intervalle", "Halbtöne auf- und abwärts rechnen"),
    SCALE(Category.THEORY, "Tonleitern", "Dur und Moll Ton für Ton bauen"),
    EAR_INTERVAL(Category.HEAR, "Intervalle hören", "Zwei Töne: welcher Abstand?"),
    EAR_QUALITY(Category.HEAR, "Dur oder Moll", "Dreiklang hören und einordnen"),
    EAR_CHORD(Category.HEAR, "Akkordtyp hören", "Dur, Moll, vermindert, übermäßig"),
    EAR_PITCH(Category.HEAR, "Ton erraten", "Einen einzelnen Ton benennen"),
    ;

    companion object {
        fun of(category: Category): List<Exercise> = entries.filter { it.category == category }
    }
}
