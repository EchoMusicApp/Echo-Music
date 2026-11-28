@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package echo.music.iad1tya.echomusic.updater

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

// Desktop stand-in for the Android updater: the settings are kept as they were, but there is no APK download/install,
// so checkForUpdate always reports "could not check" and the update screen is a note.
const val PREFS_NAME = "settings"
const val KEY_AUTO_UPDATE_CHECK = "auto_update_check"
const val KEY_LAST_CHECKED_TIME = "last_checked_time"
const val KEY_BETA_UPDATES = "beta_updates"
const val KEY_UPDATE_AVAILABLE = "update_available"

fun getUpdateAvailableState(context: Context): Boolean {
  val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  return sharedPrefs.getBoolean(KEY_UPDATE_AVAILABLE, false)
}

fun saveUpdateAvailableState(context: Context, available: Boolean) {
  val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  sharedPrefs.edit().putBoolean(KEY_UPDATE_AVAILABLE, available).apply()
}

fun getAutoUpdateCheckSetting(context: Context): Boolean {
  val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  return sharedPrefs.getBoolean(KEY_AUTO_UPDATE_CHECK, true)
}

fun saveAutoUpdateCheckSetting(context: Context, enabled: Boolean) {
  val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  sharedPrefs.edit().putBoolean(KEY_AUTO_UPDATE_CHECK, enabled).apply()
}

const val KEY_UPDATE_NOTIFICATIONS = "update_notifications"

fun getUpdateNotificationsSetting(context: Context): Boolean {
  val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  return sharedPrefs.getBoolean(KEY_UPDATE_NOTIFICATIONS, true)
}

fun saveUpdateNotificationsSetting(context: Context, enabled: Boolean) {
  val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  sharedPrefs.edit().putBoolean(KEY_UPDATE_NOTIFICATIONS, enabled).apply()
}

fun saveLastCheckedTime(context: Context, timestamp: String) {
  val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  sharedPrefs.edit().putString(KEY_LAST_CHECKED_TIME, timestamp).apply()
}

fun getLastCheckedTime(context: Context): String {
  val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  return sharedPrefs.getString(KEY_LAST_CHECKED_TIME, "") ?: ""
}

fun getBetaUpdatesSetting(context: Context): Boolean {
  val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  return sharedPrefs.getBoolean(KEY_BETA_UPDATES, false)
}

fun saveBetaUpdatesSetting(context: Context, enabled: Boolean) {
  val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  sharedPrefs.edit().putBoolean(KEY_BETA_UPDATES, enabled).apply()
}


const val KEY_LAST_SEEN_CHANGELOG_VERSION = "last_seen_changelog_version"
fun getLastSeenChangelogVersion(context: Context): String {
  val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  return sharedPrefs.getString(KEY_LAST_SEEN_CHANGELOG_VERSION, "") ?: ""
}

fun saveLastSeenChangelogVersion(context: Context, version: String) {
  val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  sharedPrefs.edit().putString(KEY_LAST_SEEN_CHANGELOG_VERSION, version).apply()
}


data class WhatsNewInfo(val changelog: List<ChangelogSection>, val description: String?)

suspend fun fetchChangelogForVersion(currentVersion: String): WhatsNewInfo? = null

@Composable
fun WhatsNewDialog(version: String, info: WhatsNewInfo, onDismiss: () -> Unit) { onDismiss() }

suspend fun checkForUpdate(
  context: Context,
  onSuccess: (tag: String, isAvailable: Boolean, changelog: List<ChangelogSection>, size: String, date: String, description: String?, imageUrl: String?, apkUrl: String?) -> Unit,
  onError: () -> Unit,
) = onError()


@Composable
fun UpdateScreen(navController: NavHostController) {
  Scaffold(topBar = { TopAppBar(title = { Text("Updates") }) }) { pad ->
    Column(Modifier.padding(pad).padding(16.dp)) { Text("Updates for the desktop app are installed by replacing the app, not from inside it.") }
  }
}
