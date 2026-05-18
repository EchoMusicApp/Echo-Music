




package iad1tya.echo.music.innertube.pages

import iad1tya.echo.music.innertube.models.YTItem
import iad1tya.echo.music.innertube.models.filterExplicit
import iad1tya.echo.music.innertube.models.filterVideo

data class BrowseResult(
    val title: String?,
    val thumbnail: String? = null,
    val items: List<Item>,
) {
    data class Item(
        val title: String?,
        val items: List<YTItem>,
    )

    fun filterExplicit(enabled: Boolean = true) =
        if (enabled) {
            copy(
                items =
                    items.mapNotNull {
                        it.copy(
                            items =
                                it.items
                                    .filterExplicit()
                                    .ifEmpty { return@mapNotNull null },
                        )
                    },
            )
        } else {
            this
        }

    fun filterVideo(enabled: Boolean = true) =
        if (enabled) {
            copy(
                items =
                    items.mapNotNull {
                        it.copy(
                            items =
                                it.items
                                    .filterVideo()
                                    .ifEmpty { return@mapNotNull null },
                        )
                    },
            )
        } else {
            this
        }
}
