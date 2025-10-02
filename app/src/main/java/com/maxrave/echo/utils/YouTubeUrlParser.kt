package iad1tya.echo.music.utils

import android.net.Uri
import android.util.Log
import java.util.regex.Pattern

/**
 * Utility class for parsing YouTube URLs and extracting video IDs, playlist IDs, and other metadata
 */
object YouTubeUrlParser {
    
    private const val TAG = "YouTubeUrlParser"
    
    // YouTube URL patterns
    private val YOUTUBE_VIDEO_PATTERNS = listOf(
        Pattern.compile("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11})"),
        Pattern.compile("youtube\\.com/v/([a-zA-Z0-9_-]{11})"),
        Pattern.compile("music\\.youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})")
    )
    
    private val YOUTUBE_PLAYLIST_PATTERN = Pattern.compile("(?:youtube\\.com|music\\.youtube\\.com).*[?&]list=([a-zA-Z0-9_-]+)")
    
    /**
     * Data class to hold parsed YouTube URL information
     */
    data class YouTubeUrlInfo(
        val videoId: String? = null,
        val playlistId: String? = null,
        val channelId: String? = null,
        val timestamp: Int? = null,
        val isYouTubeMusic: Boolean = false,
        val isPlaylist: Boolean = false,
        val isChannel: Boolean = false,
        val originalUrl: String
    )
    
    /**
     * Parse a YouTube URL and extract relevant information
     */
    fun parseUrl(url: String): YouTubeUrlInfo? {
        try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return null
            
            Log.d(TAG, "Parsing URL: $url")
            Log.d(TAG, "Host: $host, Path: ${uri.path}, Query: ${uri.query}")
            
            // Check if it's a YouTube domain
            if (!isYouTubeUrl(host)) {
                Log.d(TAG, "Not a YouTube URL")
                return null
            }
            
            val isYouTubeMusic = host.contains("music.youtube.com")
            val path = uri.path ?: ""
            val pathSegments = uri.pathSegments
            
            var videoId: String? = null
            var playlistId: String? = null
            var channelId: String? = null
            var timestamp: Int? = null
            
            // Extract video ID
            videoId = extractVideoId(url, uri)
            
            // Extract playlist ID
            playlistId = uri.getQueryParameter("list")
            
            // Extract timestamp (t parameter)
            uri.getQueryParameter("t")?.let { t ->
                timestamp = parseTimestamp(t)
            }
            
            // Extract channel ID for channel URLs
            when {
                path.startsWith("/channel/") -> {
                    channelId = pathSegments?.getOrNull(1)
                }
                path.startsWith("/c/") || path.startsWith("/user/") -> {
                    channelId = pathSegments?.getOrNull(1)
                }
            }
            
            val result = YouTubeUrlInfo(
                videoId = videoId,
                playlistId = playlistId,
                channelId = channelId,
                timestamp = timestamp,
                isYouTubeMusic = isYouTubeMusic,
                isPlaylist = !playlistId.isNullOrEmpty(),
                isChannel = !channelId.isNullOrEmpty(),
                originalUrl = url
            )
            
            Log.d(TAG, "Parsed result: $result")
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing URL: $url", e)
            return null
        }
    }
    
    /**
     * Extract video ID from various YouTube URL formats
     */
    private fun extractVideoId(url: String, uri: Uri): String? {
        // Try query parameter first (most common)
        uri.getQueryParameter("v")?.let { return it }
        
        // Try path segments for youtu.be links
        if (uri.host?.contains("youtu.be") == true) {
            return uri.pathSegments?.firstOrNull()
        }
        
        // Try regex patterns
        for (pattern in YOUTUBE_VIDEO_PATTERNS) {
            val matcher = pattern.matcher(url)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        
        return null
    }
    
    /**
     * Parse timestamp from various formats (123, 1m23s, 1:23, etc.)
     */
    private fun parseTimestamp(timestamp: String): Int? {
        try {
            // Handle numeric seconds
            timestamp.toIntOrNull()?.let { return it }
            
            // Handle time format like "1m23s"
            val timePattern = Pattern.compile("(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?")
            val matcher = timePattern.matcher(timestamp)
            if (matcher.matches()) {
                val hours = matcher.group(1)?.toIntOrNull() ?: 0
                val minutes = matcher.group(2)?.toIntOrNull() ?: 0
                val seconds = matcher.group(3)?.toIntOrNull() ?: 0
                return hours * 3600 + minutes * 60 + seconds
            }
            
            // Handle time format like "1:23" or "1:23:45"
            val timeParts = timestamp.split(":")
            when (timeParts.size) {
                2 -> {
                    val minutes = timeParts[0].toIntOrNull() ?: 0
                    val seconds = timeParts[1].toIntOrNull() ?: 0
                    return minutes * 60 + seconds
                }
                3 -> {
                    val hours = timeParts[0].toIntOrNull() ?: 0
                    val minutes = timeParts[1].toIntOrNull() ?: 0
                    val seconds = timeParts[2].toIntOrNull() ?: 0
                    return hours * 3600 + minutes * 60 + seconds
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing timestamp: $timestamp", e)
        }
        return null
    }
    
    /**
     * Check if the host is a YouTube domain
     */
    private fun isYouTubeUrl(host: String): Boolean {
        return host in listOf(
            "youtube.com",
            "www.youtube.com",
            "m.youtube.com",
            "music.youtube.com",
            "youtu.be"
        )
    }
    
    /**
     * Extract YouTube URL from shared text (handles cases where URL is embedded in text)
     */
    fun extractYouTubeUrlFromText(text: String): String? {
        val urlPattern = Pattern.compile(
            "(https?://)?(www\\.)?(youtube\\.com|youtu\\.be|music\\.youtube\\.com)[^\\s]*",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = urlPattern.matcher(text)
        
        return if (matcher.find()) {
            var url = matcher.group()
            if (!url.startsWith("http")) {
                url = "https://$url"
            }
            url
        } else null
    }
    
    /**
     * Generate a user-friendly description of what will be played
     */
    fun getPlaybackDescription(urlInfo: YouTubeUrlInfo): String {
        return when {
            urlInfo.isPlaylist && urlInfo.videoId != null -> 
                "Playing song from ${if (urlInfo.isYouTubeMusic) "YouTube Music" else "YouTube"} playlist"
            urlInfo.isPlaylist -> 
                "Playing ${if (urlInfo.isYouTubeMusic) "YouTube Music" else "YouTube"} playlist"
            urlInfo.videoId != null -> 
                "Playing ${if (urlInfo.isYouTubeMusic) "YouTube Music" else "YouTube"} ${if (urlInfo.isYouTubeMusic) "song" else "video"}"
            urlInfo.isChannel -> 
                "Opening ${if (urlInfo.isYouTubeMusic) "YouTube Music" else "YouTube"} channel"
            else -> 
                "Opening ${if (urlInfo.isYouTubeMusic) "YouTube Music" else "YouTube"} content"
        }
    }
}

