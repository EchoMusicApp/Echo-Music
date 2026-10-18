package echo.music.iad1tya.echomusic.updater.downloadmanager

import android.content.Context
import android.os.Environment
import java.io.File

/** Removes downloaded update APKs so old installers never pile up on the phone. */
object UpdateApkCleaner {
  private const val PREFS = "pattukal_update_cleanup"
  private const val KEY_VERSION = "last_version_code"

  fun clear(context: Context) {
    runCatching {
      val dirs =
        listOfNotNull(
          File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "echo_updates"),
          File(context.cacheDir, "echo_updates")
        )
      dirs.forEach { dir ->
        dir.listFiles()?.forEach { f ->
          if (f.isFile && (f.name.endsWith(".apk", true) || f.name.endsWith(".zip", true))) f.delete()
        }
      }
    }
  }

  /** Called on app start: the first launch of a new version means the update installed, so its APK goes. */
  fun clearAfterInstall(context: Context) {
    runCatching {
      val code =
        androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(
          context.packageManager.getPackageInfo(context.packageName, 0)
        )
      val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      if (prefs.getLong(KEY_VERSION, -1L) != code) {
        clear(context)
        prefs.edit().putLong(KEY_VERSION, code).apply()
      }
    }
  }
}
