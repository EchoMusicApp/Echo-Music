package echo.music.iad1tya.ui.utils

fun String.resize(
  width: Int? = null,
  height: Int? = null,
): String {
  if (width == null && height == null) return this

  if (this.contains("i.ytimg.com")) {
    // maxresdefault does not exist for every video; YouTubeThumbnailFallbackInterceptor
    // retries those along ytimgFallbackUrl's ladder (hq720, then hqdefault), so the
    // large variant is safe to request here.
    val targetQuality = if (width != null && width >= 1200) "maxresdefault.jpg" else "hqdefault.jpg"
    return this.replace(
      Regex("(default|mqdefault|hqdefault|sddefault|maxresdefault|hq720)\\.jpg"),
      targetQuality
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

/**
 * The next variant to try when a YouTube video thumbnail is missing, or null when [url] is not one.
 *
 * The ladder goes through `hq720` before `hqdefault` on purpose: `hq720` is 1280x720, the same 16:9
 * frame as `maxresdefault`, while `hqdefault` is 480x360 with the 16:9 picture letterboxed inside a
 * 4:3 canvas. Jumping straight to `hqdefault` is what made a cover that starts sharp turn soft and
 * look stretched a moment later, for every video that has no `maxresdefault`.
 */
fun ytimgFallbackUrl(url: String): String? {
  if (!url.contains("ytimg.com")) return null
  val path = url.substringBefore('?')
  val next = when {
    path.endsWith("/maxresdefault.jpg") || path.endsWith("/sddefault.jpg") -> "hq720.jpg"
    path.endsWith("/hq720.jpg") -> "hqdefault.jpg"
    else -> return null
  }
  return path.substringBeforeLast('/') + "/" + next
}
