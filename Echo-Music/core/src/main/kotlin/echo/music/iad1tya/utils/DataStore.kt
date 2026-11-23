package echo.music.iad1tya.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import echo.music.iad1tya.extensions.toEnum
import kotlin.properties.ReadOnlyProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

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
