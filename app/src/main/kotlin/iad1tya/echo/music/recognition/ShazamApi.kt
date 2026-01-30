package iad1tya.echo.music.recognition

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ShazamAPI {
    @POST("discovery/v5/en/US/android/-/tag/{uuidDNS}/{uuidURL}")
    suspend fun discovery(
        @Body body: ShazamRequestBody,
        @Path("uuidDNS") uuidDNS: String,
        @Path("uuidURL") uuidURL: String,
        @Header("User-Agent") userAgent: String,
        @Header("Content-Language") contentLanguage: String = "en_US",
        @Header("Content-Type") contentType: String = "application/json",
        @Query("sync") sync: String = "true",
        @Query("webv3") webv3: String = "true",
        @Query("sampling") sampling: String = "true",
        @Query("connected") connected: String = "",
        @Query("shazamapiversion") shazamapiversion: String = "v3",
        @Query("sharehub") sharehub: String = "true",
        @Query("video") video: String = "v3",
    ): Response<ShazamResponse>
}
