/*
 * EchoMusic (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Adapted from Vivi Music (vivimusic Project (C) 2026, GPL-3.0)
 * https://github.com/vivizzz007/vivi-music
 */

package echo.music.iad1tya.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class SpotifySession(
  val spDc: String,
  val spKey: String? = null,
  val accessToken: String? = null,
  val expiresAt: Long = 0L,
  val accountName: String? = null,
  val accountAvatarUrl: String? = null,
) {
  val isAuthenticated: Boolean
    get() = spDc.isNotBlank() && !accessToken.isNullOrBlank()
}
