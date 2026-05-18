




package iad1tya.echo.music.innertube.pages

import iad1tya.echo.music.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
