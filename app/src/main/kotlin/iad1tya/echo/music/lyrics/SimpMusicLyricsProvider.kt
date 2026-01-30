package iad1tya.echo.music.lyrics.simpmusic

import android.content.Context
import iad1tya.echo.music.constants.EnableSimpMusicKey
import iad1tya.echo.music.lyrics.LyricsProvider
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

object SimpMusicLyricsProvider : LyricsProvider {
    override val name = "SimpMusic"

    private const val BASE_URL = "https://api-lyrics.simpmusic.org/"

    private val service: SimpMusicService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SimpMusicService::class.java)
    }

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableSimpMusicKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int
    ): Result<String> {
        return try {
            Timber.d("SimpMusic: Searching for $title by $artist")
            val query = "$title $artist"
            val searchResponse = service.search(query, limit = 1)
            
            val firstMatch = searchResponse.data?.firstOrNull() 
                ?: run {
                    Timber.d("SimpMusic: No matches found")
                    return Result.failure(Exception("No lyrics found"))
                }

            Timber.d("SimpMusic: Found match ${firstMatch.videoId}")
            val videoId = firstMatch.videoId
            val lyricsResponse = service.getLyrics(videoId)

            val lyricItem = lyricsResponse.data?.firstOrNull()
                ?: run {
                     Timber.d("SimpMusic: No lyrics data in getLyrics response")
                     return Result.failure(Exception("No lyrics data"))
                }

            val lyrics = lyricItem.syncedLyrics?.takeIf { it.isNotEmpty() }
                ?: lyricItem.plainLyric
                ?: run {
                    Timber.d("SimpMusic: Lyrics content empty")
                    return Result.failure(Exception("Lyrics content is empty"))
                }

            Timber.d("SimpMusic: Successfully fetched lyrics")
            Result.success(lyrics)
        } catch (e: Exception) {
            Timber.e(e, "SimpMusic: Error fetching lyrics")
            Result.failure(e)
        }
    }
}
