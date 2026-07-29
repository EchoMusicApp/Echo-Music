

package iad1tya.echo.music.utils

import iad1tya.echo.music.constants.CrashScreenEnabledKey
import android.content.Context
import android.content.Intent
import android.os.Build
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.ui.screens.CrashActivity
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class CrashHandler private constructor(
    private val applicationContext: Context
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

   override fun uncaughtException(thread: Thread, throwable: Throwable){
    try{
        Timber.e(throwable, "App Crashed")

        val crashScreenEnabled = applicationContext.dataStore.get(CrashScreenEnabledKey,true)

        if(crashScreenEnabled){
            val crashLog = buildCrashLog(throwable)
            val intent = Intent(applicationContext,CrashActivity::class.java).apply{
                putExtra(EXTRA_CRASH_LOG, crashLog)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

            }
            applicationContext.startActivity(intent)
        }
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(1)
    } catch(e:Exception){
        Timber.e(e, "Error Handling Crash")
        defaultHandler?.uncaughtException(thread,throwable)
    }
   }
    
    private fun buildCrashLog(throwable: Throwable): String {
        val stackTrace = StringWriter().apply {
            throwable.printStackTrace(PrintWriter(this))
        }.toString()

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
