




package iad1tya.echo.music.models

import iad1tya.echo.music.innertube.models.YTItem
import iad1tya.echo.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
