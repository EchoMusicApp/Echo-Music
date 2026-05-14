/*
 * Echo Music Project Original (2026)
 * Aditya (github.com/iad1tya)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package iad1tya.echo.music.kugou.models

import kotlinx.serialization.Serializable

@Serializable
data class DownloadLyricsResponse(
    val content: String,
)
