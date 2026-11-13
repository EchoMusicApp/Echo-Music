package echo.music.iad1tya.utils

import android.content.Context
import android.net.Uri

/** Desktop has no ringtones: every call says "not possible". */
object RingtoneHelper {
  suspend fun getStreamUrl(context: Context, songId: String): String? = null
  suspend fun downloadAndTrimAsRingtone(context: Context, songId: String, title: String, artist: String, startMs: Long, endMs: Long, onProgress: (Float, String) -> Unit, onComplete: (Boolean, String, Uri?) -> Unit) =
    onComplete(false, "Ringtones are not available on desktop", null)
  fun openRingtoneSettings(context: Context, ringtoneUri: Uri? = null) {}
  fun hasSettingsPermission(context: Context): Boolean = false
  fun requestSettingsPermission(context: Context) {}
}

enum class AppIconType(val value: Int) {
  DEFAULT(0), LEGACY(1), STATIC(2), CAT(3), CRAZY_BLUE(4), POOKIE(5), SKY(6), ECHO_CAT(7),
  BILLIE_EILISH(13), SABRINA_CARPENTER(14), SABRINA_CARPENTER_2(15), CRAFT(16), SADIE(17)
}

/** Changing the launcher icon is Android-only. */
object IconUtils { fun setIcon(context: Context, iconType: AppIconType) {} }
