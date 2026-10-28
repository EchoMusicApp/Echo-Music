package com.music.echo.lossless

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface DeezerApiService {

  @GET("search/artist")
  suspend fun searchArtist(
    @Query("q") query: String,
    @Query("limit") limit: Int = 1
  ): DeezerArtistSearchResponse

  @GET("search")
  suspend fun searchTrack(
    @Query("q") query: String,
    @Query("limit") limit: Int = 5
  ): DeezerTrackSearchResponse

  @GET("user/me")
  suspend fun getUserProfile(
    @Header("Cookie") cookie: String
  ): Response<ResponseBody>

  companion object {
    private const val BASE_URL = "https://api.deezer.com/"

    fun create(baseOkHttpClient: OkHttpClient? = null): DeezerApiService {
      val client = (baseOkHttpClient?.newBuilder() ?: OkHttpClient.Builder())
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

      return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DeezerApiService::class.java)
    }

    /**
     * Upgrades a Deezer artist picture URL to the highest resolution (1000x1000).
     */
    fun upgradeToHighResDeezerUrl(url: String): String {
      if (!url.contains("dzcdn.net/images/artist")) return url
      val deezerSizeRegex = Regex("/\\d+x\\d+(-\\d+-\\d+-\\d+)?(\\.[a-zA-Z]+)?$")
      return deezerSizeRegex.replace(url, "/1000x1000$1$2")
    }
  }
}
