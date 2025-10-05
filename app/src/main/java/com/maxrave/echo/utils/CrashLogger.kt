package iad1tya.echo.music.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Crash Logger utility for saving crash logs to internal storage
 * This is specifically for testing purposes to track app crashes
 */
object CrashLogger {
    
    private const val TAG = "CrashLogger"
    private const val CRASH_LOG_DIR = "crash_logs"
    private const val MAX_LOG_FILES = 10 // Keep only last 10 crash logs
    
    /**
     * Log a crash to internal storage
     */
    fun logCrash(context: Context, throwable: Throwable, additionalInfo: String? = null) {
        try {
            val crashLog = buildCrashLog(throwable, additionalInfo)
            val fileSize = saveCrashLogToFile(context, crashLog)
            Log.e(TAG, "Crash logged to internal storage")
            
            // Log crash analytics
            try {
                AnalyticsHelper.logCrashReportGenerated(
                    reportSize = fileSize,
                    reportLocation = "internal_storage"
                )
            } catch (analyticsException: Exception) {
                Log.e(TAG, "Failed to log crash report analytics: ${analyticsException.message}", analyticsException)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log crash: ${e.message}", e)
        }
    }
    
    /**
     * Build comprehensive crash log
     */
    private fun buildCrashLog(throwable: Throwable, additionalInfo: String?): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val deviceInfo = getDeviceInfo()
        
        return buildString {
            appendLine("=== CRASH LOG ===")
            appendLine("Timestamp: $timestamp")
            appendLine("Device Info: $deviceInfo")
            appendLine()
            appendLine("Exception: ${throwable.javaClass.simpleName}")
            appendLine("Message: ${throwable.message}")
            appendLine()
            appendLine("Stack Trace:")
            appendLine(throwable.stackTraceToString())
            appendLine()
            
            // Add cause if available
            throwable.cause?.let { cause ->
                appendLine("Caused by: ${cause.javaClass.simpleName}")
                appendLine("Cause Message: ${cause.message}")
                appendLine("Cause Stack Trace:")
                appendLine(cause.stackTraceToString())
                appendLine()
            }
            
            // Add additional info if provided
            additionalInfo?.let { info ->
                appendLine("Additional Info:")
                appendLine(info)
                appendLine()
            }
            
            // Add memory info
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            appendLine("Memory Info:")
            appendLine("Used Memory: ${usedMemory / 1024 / 1024} MB")
            appendLine("Max Memory: ${maxMemory / 1024 / 1024} MB")
            appendLine("Free Memory: ${runtime.freeMemory() / 1024 / 1024} MB")
            appendLine()
            
            appendLine("=== END CRASH LOG ===")
        }
    }
    
    /**
     * Get basic device information
     */
    private fun getDeviceInfo(): String {
        return buildString {
            append("Android ${android.os.Build.VERSION.RELEASE}")
            append(" (API ${android.os.Build.VERSION.SDK_INT})")
            append(", Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        }
    }
    
    /**
     * Save crash log to file in internal storage
     */
    private fun saveCrashLogToFile(context: Context, crashLog: String): Long {
        try {
            // Get internal storage directory
            val internalDir = File(context.filesDir, CRASH_LOG_DIR)
            if (!internalDir.exists()) {
                internalDir.mkdirs()
            }
            
            // Create filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "crash_$timestamp.txt"
            val crashFile = File(internalDir, fileName)
            
            // Write crash log to file
            FileWriter(crashFile).use { writer ->
                writer.write(crashLog)
                writer.flush()
            }
            
            Log.d(TAG, "Crash log saved to: ${crashFile.absolutePath}")
            
            // Clean up old crash logs
            cleanupOldCrashLogs(internalDir)
            
            return crashFile.length()
            
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save crash log to file: ${e.message}", e)
            return 0L
        }
    }
    
    /**
     * Clean up old crash logs, keeping only the most recent ones
     */
    private fun cleanupOldCrashLogs(crashDir: File) {
        try {
            val crashFiles = crashDir.listFiles { file ->
                file.isFile && file.name.startsWith("crash_") && file.name.endsWith(".txt")
            } ?: return
            
            if (crashFiles.size > MAX_LOG_FILES) {
                // Sort by modification time (oldest first)
                crashFiles.sortBy { it.lastModified() }
                
                // Delete oldest files
                val filesToDelete = crashFiles.size - MAX_LOG_FILES
                for (i in 0 until filesToDelete) {
                    crashFiles[i].delete()
                    Log.d(TAG, "Deleted old crash log: ${crashFiles[i].name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old crash logs: ${e.message}", e)
        }
    }
    
    /**
     * Get all crash log files
     */
    fun getCrashLogFiles(context: Context): List<File> {
        return try {
            val internalDir = File(context.filesDir, CRASH_LOG_DIR)
            if (!internalDir.exists()) {
                emptyList()
            } else {
                internalDir.listFiles { file ->
                    file.isFile && file.name.startsWith("crash_") && file.name.endsWith(".txt")
                }?.sortedByDescending { it.lastModified() } ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get crash log files: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get crash log directory path for user reference
     */
    fun getCrashLogDirectoryPath(context: Context): String {
        return File(context.filesDir, CRASH_LOG_DIR).absolutePath
    }
    
    /**
     * Clear all crash logs
     */
    fun clearAllCrashLogs(context: Context): Boolean {
        return try {
            val internalDir = File(context.filesDir, CRASH_LOG_DIR)
            if (internalDir.exists()) {
                val crashFiles = internalDir.listFiles { file ->
                    file.isFile && file.name.startsWith("crash_") && file.name.endsWith(".txt")
                }
                crashFiles?.forEach { it.delete() }
                Log.d(TAG, "Cleared all crash logs")
                true
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear crash logs: ${e.message}", e)
            false
        }
    }
    
    /**
     * Export all crash logs to a single file in Downloads folder
     */
    fun exportCrashLogs(context: Context): String? {
        return try {
            val crashFiles = getCrashLogFiles(context)
            if (crashFiles.isEmpty()) {
                Log.w(TAG, "No crash logs to export")
                return null
            }
            
            // Create export filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFileName = "echo_crash_logs_export_$timestamp.txt"
            
            // Get Downloads directory - use app's internal storage to avoid permission issues
            val downloadsDir = File(context.filesDir, "Downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val exportFile = File(downloadsDir, exportFileName)
            
            // Write all crash logs to export file
            FileWriter(exportFile).use { writer ->
                writer.write("=== Echo Music Crash Logs Export ===\n")
                writer.write("Export Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                writer.write("Total Crash Logs: ${crashFiles.size}\n")
                writer.write("App Version: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}\n")
                writer.write("Android Version: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})\n")
                writer.write("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
                writer.write("=".repeat(50) + "\n\n")
                
                crashFiles.forEachIndexed { index, crashFile ->
                    writer.write("--- Crash Log ${index + 1}: ${crashFile.name} ---\n")
                    writer.write("File Size: ${crashFile.length()} bytes\n")
                    writer.write("Last Modified: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(crashFile.lastModified()))}\n")
                    writer.write("-".repeat(30) + "\n")
                    
                    // Read and write crash file content
                    FileInputStream(crashFile).use { inputStream ->
                        inputStream.bufferedReader().use { reader ->
                            writer.write(reader.readText())
                        }
                    }
                    writer.write("\n" + "=".repeat(50) + "\n\n")
                }
            }
            
            Log.i(TAG, "Crash logs exported to: ${exportFile.absolutePath}")
            
            // Log export analytics
            try {
                AnalyticsHelper.logCrashReportExported(
                    exportMethod = "downloads_folder",
                    success = true
                )
            } catch (analyticsException: Exception) {
                Log.e(TAG, "Failed to log export analytics: ${analyticsException.message}", analyticsException)
            }
            
            exportFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export crash logs: ${e.message}", e)
            
            // Log failed export analytics
            try {
                AnalyticsHelper.logCrashReportExported(
                    exportMethod = "downloads_folder",
                    success = false
                )
            } catch (analyticsException: Exception) {
                Log.e(TAG, "Failed to log export failure analytics: ${analyticsException.message}", analyticsException)
            }
            
            null
        }
    }
    
    /**
     * Export a specific crash log file to Downloads folder
     */
    fun exportSingleCrashLog(context: Context, crashFile: File): String? {
        return try {
            if (!crashFile.exists()) {
                Log.w(TAG, "Crash file does not exist: ${crashFile.name}")
                return null
            }
            
            // Create export filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFileName = "echo_crash_log_${crashFile.nameWithoutExtension}_$timestamp.txt"
            
            // Get Downloads directory - use app's internal storage to avoid permission issues
            val downloadsDir = File(context.filesDir, "Downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val exportFile = File(downloadsDir, exportFileName)
            
            // Copy crash file to Downloads
            FileInputStream(crashFile).use { inputStream ->
                FileOutputStream(exportFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            Log.i(TAG, "Crash log exported to: ${exportFile.absolutePath}")
            
            // Log export analytics
            try {
                AnalyticsHelper.logCrashReportExported(
                    exportMethod = "single_file_downloads_folder",
                    success = true
                )
            } catch (analyticsException: Exception) {
                Log.e(TAG, "Failed to log single export analytics: ${analyticsException.message}", analyticsException)
            }
            
            exportFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export crash log: ${e.message}", e)
            
            // Log failed export analytics
            try {
                AnalyticsHelper.logCrashReportExported(
                    exportMethod = "single_file_downloads_folder",
                    success = false
                )
            } catch (analyticsException: Exception) {
                Log.e(TAG, "Failed to log single export failure analytics: ${analyticsException.message}", analyticsException)
            }
            
            null
        }
    }
}
