package iad1tya.echo.music.utils

object ImageUtils {
    private val W_H_REGEX = Regex("""w\d+-h\d+""")
    private val W_REGEX = Regex("""=w\d+""")
    private val GOOGLE_W_H_REGEX = Regex("""=w\d+-h\d+""")
    private val GOOGLE_S_REGEX = Regex("""=s\d+""")

    fun getHighResThumbnailUrl(url: String?, size: Int = 544): String? {
        if (url == null) return null
        
        val sizeStr = "w$size-h$size"
        val googleSizeStr = "=w$size-h$size"
        val googleSStr = "=s$size"
        val wSizeStr = "=w$size"

        return when {
            url.contains("ytimg.com") -> url
                .replace(Regex("(hqdefault|mqdefault|sddefault|default|maxresdefault)\\.jpg"), "hqdefault.jpg")
                .replace(W_H_REGEX, sizeStr)
            url.contains("lh3.googleusercontent.com") || url.contains("yt3.ggpht.com") || url.contains("googleusercontent.com") ->
                url.replace(GOOGLE_W_H_REGEX, googleSizeStr)
                    .replace(GOOGLE_S_REGEX, googleSStr)
                    .replace(W_REGEX, wSizeStr)
            else -> url.replace(W_H_REGEX, sizeStr)
                .replace(W_REGEX, wSizeStr)
        }
    }
}
