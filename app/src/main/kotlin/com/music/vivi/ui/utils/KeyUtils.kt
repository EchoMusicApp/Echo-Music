

package iad1tya.echo.music.ui.utils

import java.util.concurrent.atomic.AtomicLong


object KeyUtils {
    private val counter = AtomicLong(0)
    
    
    fun generateUniqueKey(baseId: String, prefix: String = ""): String {
        val uniqueId = counter.incrementAndGet()
        return if (prefix.isNotEmpty()) {
            "${prefix}_${baseId}_$uniqueId"
        } else {
            "${baseId}_$uniqueId"
        }
    }
    
    
    fun generateIndexedKey(baseId: String, index: Int, prefix: String = ""): String {
        val uniqueId = counter.incrementAndGet()
        return if (prefix.isNotEmpty()) {
            "${prefix}_${baseId}_${index}_$uniqueId"
        } else {
            "${baseId}_${index}_$uniqueId"
        }
    }
    
    
    fun generateTimestampKey(baseId: String, prefix: String = ""): String {
        val timestamp = System.currentTimeMillis()
        val uniqueId = counter.incrementAndGet()
        return if (prefix.isNotEmpty()) {
            "${prefix}_${baseId}_${timestamp}_$uniqueId"
        } else {
            "${baseId}_${timestamp}_$uniqueId"
        }
    }
}
