/*
 * Echo Music Project Original (2026)
 * Aditya (github.com/iad1tya)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package iad1tya.echo.music.kugou.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchLyricsResponse(
    val status: Int,
    val info: String,
    val errcode: Int,
    val errmsg: String,
    val expire: Int,
    val candidates: List<Candidate>,
) {
    @Serializable
    data class Candidate(
        val id: Long,
        @SerialName("product_from")
        val productFrom: String, // Consider choosing '官方推荐歌词'
        val duration: Long,
        val accesskey: String,
    )
}
