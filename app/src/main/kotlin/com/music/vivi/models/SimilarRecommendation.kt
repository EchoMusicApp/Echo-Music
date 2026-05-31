

package iad1tya.echo.music.models

import com.music.innertube.models.YTItem
import iad1tya.echo.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
