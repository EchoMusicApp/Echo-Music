package com.echo.innertube.models.body

import com.echo.innertube.models.Context
import com.echo.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
