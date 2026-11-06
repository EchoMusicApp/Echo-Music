package echo.music.iad1tya.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File

// Desktop version of `val Context.dataStore` from the original DataStore.kt (file-backed preferences).
private val store: DataStore<Preferences> by lazy {
    PreferenceDataStoreFactory.create(produceFile = { File(Context().filesDir, "settings.preferences_pb") })
}
val Context.dataStore: DataStore<Preferences> get() = store

// The original clears Android WebView cookies after Spotify login; nothing to clear on desktop yet.
suspend fun clearWebAuthSession(context: Context) {}
