/*
 * Echo Music Project Original (2026)
 * Aditya (github.com/iad1tya)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package com.my.kizzy.gateway.entities.presence

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Assets(
    @SerialName("large_image")
    val largeImage: String?,
    @SerialName("small_image")
    val smallImage: String?,
    @SerialName("large_text")
    val largeText: String? = null,
    @SerialName("small_text")
    val smallText: String? = null,
)