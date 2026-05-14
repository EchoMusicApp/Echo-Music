/*
 * Echo Music Project Original (2026)
 * Aditya (github.com/iad1tya)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */



package iad1tya.echo.music.models

import iad1tya.echo.music.innertube.models.YTItem

data class PlaylistSuggestion(
    val items: List<YTItem>,
    val continuation: String?,
    val currentQueryIndex: Int,
    val totalQueries: Int,
    val query: String,
    val hasMore: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class PlaylistSuggestionPage(
    val items: List<YTItem>,
    val continuation: String?
)

data class PlaylistSuggestionQuery(
    val query: String,
    val priority: Int
)