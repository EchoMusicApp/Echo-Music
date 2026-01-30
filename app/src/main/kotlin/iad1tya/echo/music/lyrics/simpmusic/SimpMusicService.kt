package iad1tya.echo.music.lyrics.simpmusic

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SimpMusicService {
    @GET("v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5
    ): SimpMusicApiResponse

    @GET("v1/{videoId}")
    suspend fun getLyrics(
        @Path("videoId") videoId: String
    ): SimpMusicApiResponse
}
