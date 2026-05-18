




package iad1tya.echo.music.innertube.pages

import iad1tya.echo.music.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
