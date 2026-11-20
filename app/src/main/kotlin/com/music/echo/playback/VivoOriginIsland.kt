package echo.music.iad1tya.playback

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession

/**
 * Metadata and notification extras integration for vivo / iQOO OriginOS Origin Island
 * (Atomic Island / 原子岛).
 *
 * OriginOS reads `notification.superx.*` bundled into the standard Android notification extras
 * to present interactive playback status capsules on the punch-hole island and status bar.
 * Non-vivo devices safely ignore these extra keys, ensuring zero regression across all platforms.
 */
object VivoOriginIsland {
  const val EXTRA_SUPERX_OPERATION = "notification.superx.operation"
  const val EXTRA_SUPERX_SHOW_NOTIFY = "notification.superx.showNotify"
  const val EXTRA_SUPERX_TEMPLATE = "notification.superx.template"
  const val EXTRA_SUPERX_SCENE = "notification.superx.scene"
  const val EXTRA_SUPERX_CLICK_RESP = "notification.superx.clickResp"
  const val EXTRA_SUPERX_BASE_INFOS = "notification.superx.baseInfos"

  const val EXTRA_BASE_INFO_TITLE = "notification.superx.baseInfos.title"
  const val EXTRA_BASE_INFO_CONTENT = "notification.superx.baseInfos.content"
  const val EXTRA_BASE_INFO_ICON = "notification.superx.baseInfos.icon"

  const val OPERATION_CREATE = 0
  const val OPERATION_UPDATE = 1
  const val OPERATION_END = 2

  const val TEMPLATE_CAPSULE = 1
  const val SCENE_MEDIA_PLAYBACK = "MEDIA_PLAYBACK"

  /**
   * Whether Origin Island integration is active. Defaults to device capability (vivo/iQOO)
   * and updated in-memory via [echo.music.iad1tya.playback.MusicService] DataStore observation
   * so notification builds never block the main thread.
   */
  @Volatile
  var isEnabled: Boolean = isVivoDevice()

  /**
   * Tracks the last mediaId seen so track transitions trigger [OPERATION_CREATE],
   * while ongoing updates use [OPERATION_UPDATE], and paused/idle states use [OPERATION_END].
   */
  @Volatile
  private var lastMediaId: String? = null

  /**
   * Returns true if the host device manufacturer or brand indicates a vivo or iQOO device.
   */
  fun isVivoDevice(): Boolean {
    val manufacturer = Build.MANUFACTURER.orEmpty()
    val brand = Build.BRAND.orEmpty()
    return manufacturer.contains("vivo", ignoreCase = true) ||
      brand.contains("vivo", ignoreCase = true) ||
      brand.contains("iqoo", ignoreCase = true)
  }

  /**
   * Enriches [notification] with Origin Island extras if enabled.
   *
   * Fully guarded against exceptions: any error during extraction is safely swallowed
   * so standard media notification and audio playback are never interrupted.
   */
  @OptIn(UnstableApi::class)
  fun apply(
    notification: Notification,
    mediaSession: MediaSession,
  ) {
    if (!isEnabled) return

    try {
      val extras = notification.extras ?: Bundle().also { notification.extras = it }
      val player = mediaSession.player
      val isPlaying = player.playWhenReady &&
        player.playbackState != Player.STATE_IDLE &&
        player.playbackState != Player.STATE_ENDED

      val currentMediaId = player.currentMediaItem?.mediaId
      val isNewTrack = currentMediaId != null && currentMediaId != lastMediaId

      val operation = when {
        !isPlaying -> {
          // Playback paused or stopped: reset tracked ID so next resume starts with CREATE
          lastMediaId = null
          OPERATION_END
        }
        isNewTrack -> {
          lastMediaId = currentMediaId
          OPERATION_CREATE
        }
        else -> OPERATION_UPDATE
      }

      extras.putInt(EXTRA_SUPERX_OPERATION, operation)
      extras.putBoolean(EXTRA_SUPERX_SHOW_NOTIFY, true)
      extras.putInt(EXTRA_SUPERX_TEMPLATE, TEMPLATE_CAPSULE)
      extras.putString(EXTRA_SUPERX_SCENE, SCENE_MEDIA_PLAYBACK)

      val clickIntent = notification.contentIntent ?: mediaSession.sessionActivity
      if (clickIntent != null) {
        extras.putParcelable(EXTRA_SUPERX_CLICK_RESP, clickIntent)
      }

      val metadata = player.mediaMetadata
      val title = metadata.title?.toString()
        ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        ?: ""
      val artist = metadata.artist?.toString()
        ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        ?: ""

      val baseInfo = Bundle().apply {
        putCharSequence(EXTRA_BASE_INFO_TITLE, title)
        putCharSequence(EXTRA_BASE_INFO_CONTENT, artist)

        try {
          @Suppress("DEPRECATION")
          val iconParcelable: Parcelable? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              extras.getParcelable(Notification.EXTRA_LARGE_ICON, Parcelable::class.java)
            } else {
              extras.getParcelable(Notification.EXTRA_LARGE_ICON)
            } ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
              notification.largeIcon
            } else {
              null
            }

          if (iconParcelable != null) {
            putParcelable(EXTRA_BASE_INFO_ICON, iconParcelable)
          }
        } catch (_: Throwable) {
          // Ignore icon parceling issues safely
        }
      }
      extras.putBundle(EXTRA_SUPERX_BASE_INFOS, baseInfo)
    } catch (_: Throwable) {
      // Never let vendor-specific extras affect notification delivery or playback.
    }
  }

  /**
   * Resets internal tracking state (useful for tests).
   */
  internal fun reset() {
    lastMediaId = null
    isEnabled = isVivoDevice()
  }
}
