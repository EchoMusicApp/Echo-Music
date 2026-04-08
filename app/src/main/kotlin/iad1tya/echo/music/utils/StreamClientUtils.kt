package iad1tya.echo.music.utils

import com.echo.innertube.models.YouTubeClient

object StreamClientUtils {
    data class OriginReferer(val origin: String?, val referer: String?)

    fun resolveUserAgent(clientParam: String?): String {
        val c = clientParam?.trim().orEmpty()
        return when {
            c.equals("WEB_REMIX", ignoreCase = true) ||
                c.equals("WEB", ignoreCase = true) ||
                c.equals("WEB_CREATOR", ignoreCase = true) -> YouTubeClient.USER_AGENT_WEB

            c.equals("TVHTML5", ignoreCase = true) ||
                c.equals("TVHTML5_SIMPLY_EMBEDDED_PLAYER", ignoreCase = true) ||
                c.equals("TVHTML5_SIMPLY", ignoreCase = true) -> YouTubeClient.TVHTML5.userAgent

            c.startsWith("IOS", ignoreCase = true) -> YouTubeClient.IOS.userAgent

            c.startsWith("ANDROID_VR", ignoreCase = true) -> YouTubeClient.ANDROID_VR_NO_AUTH.userAgent

            c.startsWith("ANDROID_CREATOR", ignoreCase = true) -> YouTubeClient.ANDROID_CREATOR.userAgent

            c.startsWith("ANDROID", ignoreCase = true) -> YouTubeClient.MOBILE.userAgent

            c.startsWith("VISIONOS", ignoreCase = true) -> YouTubeClient.VISIONOS.userAgent

            else -> YouTubeClient.ANDROID_VR_NO_AUTH.userAgent
        }
    }

    fun resolveOriginReferer(clientParam: String?): OriginReferer {
        val c = clientParam?.trim().orEmpty()
        return when {
            c.equals("WEB_REMIX", ignoreCase = true) ||
                c.equals("WEB", ignoreCase = true) ||
                c.equals("WEB_CREATOR", ignoreCase = true) ->
                OriginReferer(YouTubeClient.ORIGIN_YOUTUBE_MUSIC, YouTubeClient.REFERER_YOUTUBE_MUSIC)

            c.equals("TVHTML5", ignoreCase = true) ||
                c.equals("TVHTML5_SIMPLY_EMBEDDED_PLAYER", ignoreCase = true) ||
                c.equals("TVHTML5_SIMPLY", ignoreCase = true) ->
                OriginReferer("https://www.youtube.com", "https://www.youtube.com/tv")

            else -> OriginReferer(null, null)
        }
    }
}
