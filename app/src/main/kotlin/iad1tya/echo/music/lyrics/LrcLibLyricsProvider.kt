package iad1tya.echo.music.lyrics

import android.content.Context
import iad1tya.echo.music.constants.EnableLrcLibKey
import iad1tya.echo.music.lyrics.lrclib.LrcLibService
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

object LrcLibLyricsProvider : LyricsProvider {
    override val name = "LrcLib"
    
    // Explicitly using the base URL from the service implementation plan
    private val service = Retrofit.Builder()
        .baseUrl("https://lrclib.net/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LrcLibService::class.java)

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableLrcLibKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> {
        return try {
            // LrcLib expects duration in seconds (max 3600). 
            // If duration is largely likely milliseconds (e.g. > 10000 which is 2.7h), convert to seconds.
            val durationSeconds = if (duration > 10000) duration / 1000 else duration
            
            Timber.d("LrcLib: Fetching lyrics for $title by $artist (duration: $durationSeconds s)")
            
            // Try explicit get first
            try {
                val dto = service.getLyrics(
                    artistName = artist,
                    trackName = title,
                    duration = durationSeconds
                )
                val lyrics = dto.syncedLyrics?.takeIf { it.isNotEmpty() } 
                    ?: dto.plainLyrics 
                if (!lyrics.isNullOrEmpty()) {
                    Timber.d("LrcLib: Successfully fetched lyrics via GET")
                    return Result.success(lyrics)
                }
            } catch (e: Exception) {
                Timber.w(e, "LrcLib: direct get failed, trying search fallback")
            }

            // Fallback to search if direct get fails (e.g. 404 or strict matching issues)
            val query = "$title $artist"
            val results = service.searchLyrics(query)
            
            // Find best match allowing some fuzziness
            val match = results.firstOrNull { 
                // Simple check: match exact title if possible, or just take first result as search is ranked
                // LrcLib search is usually good.
                // We could filter by duration similarity too.
                val durDiff = kotlin.math.abs(it.duration - durationSeconds)
                durDiff < 10 // within 10 seconds
            } ?: results.firstOrNull()

            val lyrics = match?.syncedLyrics?.takeIf { it.isNotEmpty() }
                ?: match?.plainLyrics
                ?: throw Exception("No lyrics found in search results")

            Timber.d("LrcLib: Successfully fetched lyrics via SEARCH")
            Result.success(lyrics)

        } catch (e: Exception) {
            Timber.e(e, "LrcLib: Failed to fetch lyrics")
            Result.failure(e)
        }
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
       // Optional: Implement getAllLyrics using search endpoint if needed, 
       // but for now we focus on fixing the main getLyrics.
       // The original implementation used getAllLyrics, but the critical path is getLyrics.
       // We can just call getLyrics for one result or implement search if required.
       // For now, let's just default to getLyrics call to satisfy interface or leave empty if not critical.
       // Actually, the interface requires it. Let's try to wrap getLyrics.
       try {
            getLyrics(id, title, artist, duration).onSuccess {
                callback(it)
            }
       } catch (e: Exception) {
           Timber.e(e, "LrcLib: Error in getAllLyrics")
       }
    }
}
