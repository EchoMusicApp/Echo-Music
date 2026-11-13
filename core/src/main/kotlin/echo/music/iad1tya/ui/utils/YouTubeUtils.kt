package echo.music.iad1tya.ui.utils

fun String.resize(
  width: Int? = null,
  height: Int? = null,
): String {
  if (width == null && height == null) return this

  if (this.contains("i.ytimg.com")) {
    // maxresdefault does not exist for every video; YouTubeThumbnailFallbackInterceptor
    // retries those with hqdefault, so the large variant is safe to request here.
    val targetQuality = if (width != null && width >= 1200) "maxresdefault.jpg" else "hqdefault.jpg"
    return this.replace(
      Regex("(default|mqdefault|hqdefault|sddefault|maxresdefault)\\.jpg"),
      targetQuality,
    )
  }

  if (this.contains("googleusercontent.com") && this.contains("=w")) {
    val baseUrl = this.split("=w")[0]
    val size = if ((width ?: 0) >= 1000 || (height ?: 0) >= 1000) 1200 else 500
    return "$baseUrl=w$size-h$size"
  }

  if (this.contains("yt3.ggpht.com")) {
    // Only the "=s…" suffix carries the size; image ids can contain "-s" themselves.
    val baseUrl = this.substringBefore('=')
    return "$baseUrl=s${width ?: height}"
  }

  "https://lh\\d\\.googleusercontent\\.com/.*".toRegex().matchEntire(this)?.let {
    val size = if ((width ?: 0) >= 1000 || (height ?: 0) >= 1000) 1200 else 500
    return "${this.split("=")[0]}=w$size-h$size"
  }

  return this
}

private val ytimgVariantRegex =
  Regex("(default|mqdefault|hqdefault|sddefault|maxresdefault)\\.jpg$")

/**
 * The always-available variant of a YouTube video thumbnail whose resolution may not exist
 * (maxresdefault / sddefault), or null when [url] is not one. Older builds stored maxresdefault
 * URLs in the database, so image loaders retry with it.
 */
fun ytimgFallbackUrl(url: String): String? {
  if (!url.contains("ytimg.com")) return null
  val path = url.substringBefore('?')
  if (!path.endsWith("/maxresdefault.jpg") && !path.endsWith("/sddefault.jpg")) return null
  return ytimgVariantRegex.replace(path, "hqdefault.jpg")
}
