package com.echo.innertube.pages

import com.echo.innertube.models.YTItem
import com.echo.innertube.models.filterExplicit
import com.echo.innertube.models.filterVideoSongs
import com.echo.innertube.models.filterYoutubeShorts

data class BrowseResult(
    val title: String?,
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

    fun filterVideoSongs(enabled: Boolean = false) =
        if (enabled) {
            copy(
                items = items.mapNotNull {
                    it.copy(
                        items = it.items.filterVideoSongs(true).ifEmpty { return@mapNotNull null },
                    )
                },
            )
        } else {
            this
        }

    fun filterYoutubeShorts(enabled: Boolean = false) =
        if (enabled) {
            copy(
                items = items.mapNotNull {
                    it.copy(
                        items = it.items.filterYoutubeShorts(true).ifEmpty { return@mapNotNull null },
                    )
                },
            )
        } else {
            this
        }
}
