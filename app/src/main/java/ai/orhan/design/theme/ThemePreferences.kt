package ai.orhan.design.theme

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/* ============================================================================
 *  Orhan AI — Theme persistence
 *
 *  One DataStore file, one value: the user's ThemeMode.
 *
 *  ThemeMode itself is declared in Theme.kt (it is already used there by
 *  ThemeMode.isDark()), so this file does NOT redeclare it — that would be a
 *  duplicate declaration and the module would not compile.
 *
 *  Storage format: the enum's name as UTF-8 ("System" / "Light" / "Dark").
 *  Names are used instead of ordinals on purpose — reordering the enum later
 *  must not silently change what a returning user sees.
 * ==========================================================================*/

/** File name inside `context.filesDir/datastore/`. */
private const val THEME_DATASTORE_FILE = "orhan_theme.preferences_pb"

/**
 * Reads and writes [ThemeMode] as its enum name.
 *
 * A corrupt or unreadable file is not fatal: [readFrom] falls back to
 * [ThemeMode.System] so the app still starts, and only a genuine I/O failure
 * is rethrown.
 */
object ThemeModeSerializer : Serializer<ThemeMode> {

    override val defaultValue: ThemeMode = ThemeMode.System

    override suspend fun readFrom(input: InputStream): ThemeMode {
        val raw = try {
            input.readBytes().decodeToString()
        } catch (e: IOException) {
            throw CorruptionException("Cannot read theme mode", e)
        }
        return ThemeMode.values().firstOrNull { it.name == raw } ?: defaultValue
    }

    override suspend fun writeTo(t: ThemeMode, output: OutputStream) {
        output.write(t.name.encodeToByteArray())
    }
}

/**
 * The single DataStore instance for the process.
 *
 * `by dataStore(...)` is a property delegate that guarantees one instance per
 * file name — creating a second DataStore over the same file throws at runtime,
 * so always reach it through this extension.
 */
val Context.themeDataStore: DataStore<ThemeMode> by dataStore(
    fileName = THEME_DATASTORE_FILE,
    serializer = ThemeModeSerializer,
)

/**
 * Repository over the theme DataStore.
 *
 * Deliberately tiny and framework-free apart from DataStore itself, so it can
 * be constructed in a test with an in-memory DataStore and no Android runtime.
 */
class ThemePreferences(
    private val dataStore: DataStore<ThemeMode>,
) {

    /**
     * The persisted mode, re-emitted on every write.
     *
     * `catch` is placed before any operator that could terminate the flow: a
     * disk read error must degrade to the default, not crash the composition.
     */
    val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(ThemeMode.System) else throw error
        }

    /** Persist [mode]. Suspends until the write has been acknowledged. */
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.updateData { mode }
    }
}

/** Convenience constructor for the common case — the app's own DataStore. */
fun Context.themePreferences(): ThemePreferences = ThemePreferences(themeDataStore)
