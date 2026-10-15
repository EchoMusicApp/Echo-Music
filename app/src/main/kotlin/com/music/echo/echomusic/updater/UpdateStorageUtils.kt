package echo.music.iad1tya.echomusic.updater

import android.content.Context
import android.os.Environment
import java.io.File

fun getDownloadedApksDir(context: Context): File {
  return File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "echo_updates")
}

fun getDownloadedApkCount(context: Context): Int {
  val dir = getDownloadedApksDir(context)
  if (!dir.exists() || !dir.isDirectory) return 0
  return dir
    .listFiles { file -> file.isFile && file.name.endsWith(".apk", ignoreCase = true) }
    ?.size ?: 0
}

fun clearDownloadedApks(context: Context): Boolean {
  val dir = getDownloadedApksDir(context)
  if (!dir.exists() || !dir.isDirectory) return true
  var allDeleted = true
  dir
    .listFiles { file -> file.isFile && file.name.endsWith(".apk", ignoreCase = true) }
    ?.forEach { file ->
      if (!file.delete()) {
        allDeleted = false
      }
    }
  return allDeleted
}

/**
 * Automatically clear old downloaded APKs.
 * More aggressive than before: keeps only the newest APK and removes anything older than 6 hours.
 * This prevents "package is invalid" / install crashes after updates.
 */
fun autoClearOldApks(context: Context) {
  val dir = getDownloadedApksDir(context)
  if (!dir.exists() || !dir.isDirectory) return

  val apkFiles = dir
    .listFiles { file ->
      file.isFile && file.name.endsWith(".apk", ignoreCase = true)
    }
    ?.sortedByDescending { it.lastModified() }
    ?: return

  if (apkFiles.isEmpty()) return

  // Keep only the newest APK, delete everything else
  apkFiles.drop(1).forEach { it.delete() }

  // Also delete the newest one if it is older than 6 hours
  val sixHoursAgo = System.currentTimeMillis() - (6 * 60 * 60 * 1000)
  val newest = apkFiles.first()
  if (newest.lastModified() < sixHoursAgo) {
    newest.delete()
  }
}
