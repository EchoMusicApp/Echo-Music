package iad1tya.echo.music.lyrics.lrclib

import retrofit2.http.GET
import retrofit2.http.Query

interface LrcLibService {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("artist_name") artistName: String,
        @Query("track_name") trackName: String,
        @Query("duration") duration: Int,
    ): LrcLibDto
    @GET("api/search")
    suspend fun searchLyrics(
        @Query("q") query: String,
    ): List<LrcLibDto>
}
