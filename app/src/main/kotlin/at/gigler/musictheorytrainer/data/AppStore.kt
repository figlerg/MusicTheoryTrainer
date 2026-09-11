package at.gigler.musictheorytrainer.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import at.gigler.musictheorytrainer.theory.Guitar
import at.gigler.musictheorytrainer.theory.IntervalDifficulty
import at.gigler.musictheorytrainer.theory.Notation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class InputMode { TEXT, KEYS }

enum class StringSet(val strings: List<Int>) {
    LOW_E_AND_A(listOf(0, 1)),
    ALL(Guitar.ALL_STRINGS),
}

enum class Exercise { FRETBOARD, INTERVALS, SCALE }

data class Settings(
    val notation: Notation = Notation.GERMAN,
    val inputMode: InputMode = InputMode.TEXT,
    val stringSet: StringSet = StringSet.LOW_E_AND_A,
    val intervalDifficulty: IntervalDifficulty = IntervalDifficulty.EASY,
    val fretboardReverse: Boolean = false,
    val showTab: Boolean = true,
    val sound: Boolean = true,
)

data class Score(val correct: Int = 0, val total: Int = 0) {
    val percent: Int? get() = if (total == 0) null else (correct * 100 + total / 2) / total
}

private val Context.dataStore by preferencesDataStore(name = "trainer")

/** Settings and hit rates, stored locally with DataStore. */
class AppStore(context: Context) {
    private val dataStore = context.dataStore

    val settings: Flow<Settings> = dataStore.data.map { it.toSettings() }

    val scores: Flow<Map<Exercise, Score>> = dataStore.data.map { prefs ->
        Exercise.entries.associateWith { Score(prefs[correctKey(it)] ?: 0, prefs[totalKey(it)] ?: 0) }
    }

    suspend fun updateSettings(transform: (Settings) -> Settings) {
        dataStore.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[NOTATION] = s.notation.name
            prefs[INPUT_MODE] = s.inputMode.name
            prefs[STRING_SET] = s.stringSet.name
            prefs[INTERVAL_DIFFICULTY] = s.intervalDifficulty.name
            prefs[FRETBOARD_REVERSE] = s.fretboardReverse
            prefs[SHOW_TAB] = s.showTab
            prefs[SOUND] = s.sound
        }
    }

    suspend fun record(exercise: Exercise, correct: Boolean) {
        dataStore.edit { prefs ->
            prefs[totalKey(exercise)] = (prefs[totalKey(exercise)] ?: 0) + 1
            if (correct) prefs[correctKey(exercise)] = (prefs[correctKey(exercise)] ?: 0) + 1
        }
    }

    suspend fun resetScores() {
        dataStore.edit { prefs ->
            Exercise.entries.forEach {
                prefs.remove(correctKey(it))
                prefs.remove(totalKey(it))
            }
        }
    }

    private fun Preferences.toSettings(): Settings {
        val defaults = Settings()
        return Settings(
            notation = enumOr(this[NOTATION], defaults.notation),
            inputMode = enumOr(this[INPUT_MODE], defaults.inputMode),
            stringSet = enumOr(this[STRING_SET], defaults.stringSet),
            intervalDifficulty = enumOr(this[INTERVAL_DIFFICULTY], defaults.intervalDifficulty),
            fretboardReverse = this[FRETBOARD_REVERSE] ?: defaults.fretboardReverse,
            showTab = this[SHOW_TAB] ?: defaults.showTab,
            sound = this[SOUND] ?: defaults.sound,
        )
    }

    private companion object {
        val NOTATION = stringPreferencesKey("notation")
        val INPUT_MODE = stringPreferencesKey("input_mode")
        val STRING_SET = stringPreferencesKey("string_set")
        val INTERVAL_DIFFICULTY = stringPreferencesKey("interval_difficulty")
        val FRETBOARD_REVERSE = booleanPreferencesKey("fretboard_reverse")
        val SHOW_TAB = booleanPreferencesKey("show_tab")
        val SOUND = booleanPreferencesKey("sound")

        fun correctKey(exercise: Exercise) = intPreferencesKey("score_${exercise.name.lowercase()}_correct")
        fun totalKey(exercise: Exercise) = intPreferencesKey("score_${exercise.name.lowercase()}_total")

        inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
            enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}
