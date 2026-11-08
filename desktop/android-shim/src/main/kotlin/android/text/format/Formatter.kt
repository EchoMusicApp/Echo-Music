package android.text.format

import android.content.Context

object Formatter {
  @JvmStatic fun formatFileSize(context: Context?, sizeBytes: Long): String {
    if (sizeBytes < 1000) return "$sizeBytes B"
    val units = listOf("kB", "MB", "GB", "TB")
    var v = sizeBytes.toDouble(); var i = -1
    while (v >= 1000 && i < units.lastIndex) { v /= 1000; i++ }
    return "%.2f %s".format(v, units[i])
  }
}
