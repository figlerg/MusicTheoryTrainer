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
import at.gigler.musictheorytrainer.theory.KeyChoice
import at.gigler.musictheorytrainer.theory.KeyOrder
import at.gigler.musictheorytrainer.theory.Notation
import at.gigler.musictheorytrainer.theory.StaffRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class InputMode { TEXT, KEYS, GUITAR }

enum class SheetSource { MELODIES, RANDOM }

data class Settings(
    val notation: Notation = Notation.GERMAN,
    val inputMode: InputMode = InputMode.TEXT,
    val strings: Set<Int> = Guitar.ALL_STRINGS.toSet(),
    val intervalDifficulty: IntervalDifficulty = IntervalDifficulty.EASY,
    val fretboardReverse: Boolean = false,
    val showTab: Boolean = true,
    val sound: Boolean = true,
    val keyChoice: KeyChoice = KeyChoice.MAJOR,
    val keyOrder: KeyOrder = KeyOrder.RANDOM,
    val chordRootGiven: Boolean = false,
    val chordGrip: Boolean = false,
    val gripRootInBass: Boolean = true,
    val sheetSource: SheetSource = SheetSource.MELODIES,
    val sheetRange: StaffRange = StaffRange.IN_STAFF,
    val sheetExactOctave: Boolean = true,
    val hideStringNames: Boolean = false,
    val hideKeyLabels: Boolean = false,
) {
    /** Always at least one string, low to high. */
    val stringList: List<Int> get() = strings.sorted().ifEmpty { listOf(0) }
}

data class Score(val correct: Int = 0, val total: Int = 0) {
    val percent: Int? get() = if (total == 0) null else (correct * 100 + total / 2) / total

    operator fun plus(other: Score) = Score(correct + other.correct, total + other.total)
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
            prefs[STRINGS] = s.stringList.joinToString(",")
            prefs[INTERVAL_DIFFICULTY] = s.intervalDifficulty.name
            prefs[FRETBOARD_REVERSE] = s.fretboardReverse
            prefs[SHOW_TAB] = s.showTab
            prefs[SOUND] = s.sound
            prefs[KEY_CHOICE] = s.keyChoice.name
            prefs[KEY_ORDER] = s.keyOrder.name
            prefs[CHORD_ROOT_GIVEN] = s.chordRootGiven
            prefs[CHORD_GRIP] = s.chordGrip
            prefs[GRIP_ROOT_IN_BASS] = s.gripRootInBass
            prefs[SHEET_SOURCE] = s.sheetSource.name
            prefs[SHEET_RANGE] = s.sheetRange.name
            prefs[SHEET_EXACT_OCTAVE] = s.sheetExactOctave
            prefs[HIDE_STRING_NAMES] = s.hideStringNames
            prefs[HIDE_KEY_LABELS] = s.hideKeyLabels
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
        val d = Settings()
        return Settings(
            notation = enumOr(this[NOTATION], d.notation),
            inputMode = enumOr(this[INPUT_MODE], d.inputMode),
            strings = this[STRINGS]?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
                ?.filter { it in Guitar.ALL_STRINGS }?.toSet()?.ifEmpty { null } ?: d.strings,
            intervalDifficulty = enumOr(this[INTERVAL_DIFFICULTY], d.intervalDifficulty),
            fretboardReverse = this[FRETBOARD_REVERSE] ?: d.fretboardReverse,
            showTab = this[SHOW_TAB] ?: d.showTab,
            sound = this[SOUND] ?: d.sound,
            keyChoice = enumOr(this[KEY_CHOICE], d.keyChoice),
            keyOrder = enumOr(this[KEY_ORDER], d.keyOrder),
            chordRootGiven = this[CHORD_ROOT_GIVEN] ?: d.chordRootGiven,
            chordGrip = this[CHORD_GRIP] ?: d.chordGrip,
            gripRootInBass = this[GRIP_ROOT_IN_BASS] ?: d.gripRootInBass,
            sheetSource = enumOr(this[SHEET_SOURCE], d.sheetSource),
            sheetRange = enumOr(this[SHEET_RANGE], d.sheetRange),
            sheetExactOctave = this[SHEET_EXACT_OCTAVE] ?: d.sheetExactOctave,
            hideStringNames = this[HIDE_STRING_NAMES] ?: d.hideStringNames,
            hideKeyLabels = this[HIDE_KEY_LABELS] ?: d.hideKeyLabels,
        )
    }

    private companion object {
        val NOTATION = stringPreferencesKey("notation")
        val INPUT_MODE = stringPreferencesKey("input_mode")
        val STRINGS = stringPreferencesKey("strings")
        val INTERVAL_DIFFICULTY = stringPreferencesKey("interval_difficulty")
        val FRETBOARD_REVERSE = booleanPreferencesKey("fretboard_reverse")
        val SHOW_TAB = booleanPreferencesKey("show_tab")
        val SOUND = booleanPreferencesKey("sound")
        val KEY_CHOICE = stringPreferencesKey("key_choice")
        val KEY_ORDER = stringPreferencesKey("key_order")
        val CHORD_ROOT_GIVEN = booleanPreferencesKey("chord_root_given")
        val CHORD_GRIP = booleanPreferencesKey("chord_grip")
        val GRIP_ROOT_IN_BASS = booleanPreferencesKey("grip_root_in_bass")
        val SHEET_SOURCE = stringPreferencesKey("sheet_source")
        val SHEET_RANGE = stringPreferencesKey("sheet_range")
        val SHEET_EXACT_OCTAVE = booleanPreferencesKey("sheet_exact_octave")
        val HIDE_STRING_NAMES = booleanPreferencesKey("hide_string_names")
        val HIDE_KEY_LABELS = booleanPreferencesKey("hide_key_labels")

        fun correctKey(exercise: Exercise) = intPreferencesKey("score_${exercise.name.lowercase()}_correct")
        fun totalKey(exercise: Exercise) = intPreferencesKey("score_${exercise.name.lowercase()}_total")

        inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
            enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}
