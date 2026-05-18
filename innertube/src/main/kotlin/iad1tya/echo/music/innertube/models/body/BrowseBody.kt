




package iad1tya.echo.music.innertube.models.body

import iad1tya.echo.music.innertube.models.Context
import iad1tya.echo.music.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
