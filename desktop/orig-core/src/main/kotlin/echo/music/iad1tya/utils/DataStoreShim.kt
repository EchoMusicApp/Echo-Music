package echo.music.iad1tya.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import echo.music.iad1tya.extensions.toEnum
import java.io.File
import kotlin.properties.ReadOnlyProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// Desktop version of `val Context.dataStore` from the original DataStore.kt (file-backed preferences).
private val store: DataStore<Preferences> by lazy {
    PreferenceDataStoreFactory.create(produceFile = { File(Context().filesDir, "settings.preferences_pb") })
}
val Context.dataStore: DataStore<Preferences> get() = store

// ---- copied unchanged from the original DataStore.kt ----
operator fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): T? =
  runBlocking(Dispatchers.IO) {
    (try {
      data.first()[key]
    } catch (e: Exception) {
      null
    })
  }

fun <T> DataStore<Preferences>.get(
  key: Preferences.Key<T>,
  defaultValue: T,
): T =
  runBlocking(Dispatchers.IO) {
    (try {
      data.first()[key]
    } catch (e: Exception) {
      null
    }) ?: defaultValue
  }

fun <T> preference(
  context: Context,
  key: Preferences.Key<T>,
  defaultValue: T,
) =
  ReadOnlyProperty<Any?, T> { _, _ ->
    (try {
      context.dataStore[key]
    } catch (e: Exception) {
      null
    }) ?: defaultValue
  }

inline fun <reified T : Enum<T>> enumPreference(
  context: Context,
  key: Preferences.Key<String>,
  defaultValue: T,
) =
  ReadOnlyProperty<Any?, T> { _, _ ->
    (try {
        context.dataStore[key]
      } catch (e: Exception) {
        null
      })
      .toEnum(defaultValue)
  }

// The original clears Android WebView cookies after Spotify login; nothing to clear on desktop yet.
suspend fun clearWebAuthSession(context: Context) {}
