package com.echo.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicMultiRowListItemRenderer(
    val title: Runs?,
    val subtitle: Runs?,
    val thumbnail: ThumbnailRenderer?,
    val onTap: NavigationEndpoint?,
    val menu: Menu?,
)
