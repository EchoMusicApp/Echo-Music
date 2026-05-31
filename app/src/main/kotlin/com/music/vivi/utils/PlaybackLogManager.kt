

package iad1tya.echo.music.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


enum class PlaybackLogLevel {
    INFO,
    WARNING,
    ERROR,
    DEBUG,
    BOT 
}


data class PlaybackLogEntry(
    val timestamp: String,
    val level: PlaybackLogLevel,
    val message: String,
    val details: String? = null
)


object PlaybackLogManager {
    private const val MAX_LOG_ENTRIES = 500
    
    private val _logs = MutableStateFlow<List<PlaybackLogEntry>>(emptyList())
    val logs: StateFlow<List<PlaybackLogEntry>> = _logs.asStateFlow()
    
    
    fun log(level: PlaybackLogLevel, message: String, details: String? = null) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val entry = PlaybackLogEntry(timestamp, level, message, details)
        
        
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(entry)
        
        
        _logs.value = if (currentLogs.size > MAX_LOG_ENTRIES) {
            currentLogs.takeLast(MAX_LOG_ENTRIES)
        } else {
            currentLogs
        }
    }
    
    
    fun clearLogs() {
        _logs.value = emptyList()
    }
}
