package echo.music.iad1tya.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import echo.music.iad1tya.BuildConfig
import echo.music.iad1tya.ui.screens.CrashActivity
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess
import timber.log.Timber

class CrashHandler private constructor(private val applicationContext: Context) :
  Thread.UncaughtExceptionHandler {

  private val defaultHandler: Thread.UncaughtExceptionHandler? =
    Thread.getDefaultUncaughtExceptionHandler()

  override fun uncaughtException(thread: Thread, throwable: Throwable) {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        throwable is android.app.ForegroundServiceStartNotAllowedException
    ) {
      Timber.e(throwable, "Suppressed ForegroundServiceStartNotAllowedException in CrashHandler")
      if (thread == android.os.Looper.getMainLooper().thread) {
        while (true) {
          try {
            android.os.Looper.loop()
          } catch (e: Throwable) {
            if (
              Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is android.app.ForegroundServiceStartNotAllowedException
            ) {
              Timber.e(
                e,
                "Suppressed another ForegroundServiceStartNotAllowedException in CrashHandler",
              )
            } else {
              uncaughtException(thread, e)
              break
            }
          }
        }
        return
      }
      return
    }

    if (thread != android.os.Looper.getMainLooper().thread && isNetworkFailure(throwable)) {
      // A connection dropped mid-request on a background thread (e.g. Wi-Fi/data switch, or the
      // system cutting background network). The request has already failed; that thread or
      // coroutine is finished and the rest of the app is unaffected, so don't kill the process.
      Timber.w(throwable, "Suppressed uncaught network failure on thread ${thread.name}")
      return
    }

    try {
      val crashLog = buildCrashLog(throwable)
      Timber.e(throwable, "App crashed")

      val intent =
        Intent(applicationContext, CrashActivity::class.java).apply {
          putExtra(EXTRA_CRASH_LOG, crashLog)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
      applicationContext.startActivity(intent)

      android.os.Process.killProcess(android.os.Process.myPid())
      exitProcess(1)
    } catch (e: Exception) {

      Timber.e(e, "Error handling crash")
      defaultHandler?.uncaughtException(thread, throwable)
    }
  }

  /**
   * True if [throwable] or one of its causes is a connection-level network error. Walks the whole
   * cause chain, stopping only when a throwable repeats, so cyclic chains can't loop forever.
   */
  private fun isNetworkFailure(throwable: Throwable): Boolean {
    val visited = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Throwable, Boolean>())
    var current: Throwable? = throwable
    while (current != null && visited.add(current)) {
      if (
        current is java.net.SocketException ||
          current is java.net.SocketTimeoutException ||
          current is java.net.UnknownHostException ||
          current is javax.net.ssl.SSLException
      ) {
        return true
      }
      current = current.cause
    }
    return false
  }

  private fun buildCrashLog(throwable: Throwable): String {
    val stackTrace =
      StringWriter().apply { throwable.printStackTrace(PrintWriter(this)) }.toString()

    return buildString {
      appendLine("echomusic Crash Report")
      appendLine("=".repeat(50))
      appendLine()
      appendLine("Manufacturer: ${Build.MANUFACTURER}")
      appendLine("Device: ${Build.MODEL}")
      appendLine("Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
      appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
      appendLine()
      appendLine("=".repeat(50))
      appendLine("Stacktrace:")
      appendLine("=".repeat(50))
      appendLine()
      append(stackTrace)
    }
  }

  companion object {
    const val EXTRA_CRASH_LOG = "crash_log"

    fun install(context: Context) {
      val handler = CrashHandler(context.applicationContext)
      Thread.setDefaultUncaughtExceptionHandler(handler)
      Timber.d("CrashHandler installed")
    }
  }
}
