package iad1tya.echo.music.api

import retrofit2.http.Body
import retrofit2.http.POST

data class NeuroengineRequest(
    val trackId: String,
    val durationMs: Long,
    val skipped: Boolean
)

data class SuggestedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val matchReason: String
)

data class NeuroengineResponse(
    val suggestions: List<SuggestedTrack>,
    val tasteProfileUpdate: TasteProfileDto?
)

data class TasteProfileDto(
    val genres: String,
    val confidence: Float,
    val patternsFound: Int,
    val modelVersion: String
)

interface FlowNeuroengineApi {
    @POST("/v1/brain/analyze")
    suspend fun analyzeEvent(@Body request: NeuroengineRequest): NeuroengineResponse
}
