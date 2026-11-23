package echo.music.iad1tya.utils

import coil3.intercept.Interceptor
import coil3.network.HttpException
import coil3.request.ErrorResult
import coil3.request.ImageResult
import echo.music.iad1tya.ui.utils.ytimgFallbackUrl

/**
 * Retries a missing YouTube video thumbnail with the next larger variant that exists.
 *
 * `maxresdefault` / `sddefault` do not exist for many videos. Older builds stored those URLs in the
 * database and in queued media items, so without this the affected songs never showed a cover (in
 * the app, the notification or on Android Auto). Every request goes through the singleton loader,
 * so this covers all of them.
 *
 * The retry walks the ladder from [ytimgFallbackUrl] instead of jumping straight to `hqdefault`:
 * `hq720` keeps the 16:9 frame and the sharpness, `hqdefault` is the last resort and is visibly
 * soft on a phone screen.
 */
class YouTubeThumbnailFallbackInterceptor : Interceptor {
  override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
    val original = chain.request.data.toString()
    var current = original
    var result = chain.proceed()
    var attempts = 0
    while (attempts < MAX_FALLBACK_STEPS && isNotFound(result)) {
      val fallback = ytimgFallbackUrl(current) ?: break
      val request =
        chain.request
          .newBuilder()
          .data(fallback)
          // Cache the variant under its own URL. Keying it to the original is what pinned a
          // 480x360 fallback to the 1200px request: every later load hit that entry from the disk
          // cache and never retried the large variant, so a cover could stay soft for good.
          .diskCacheKey(fallback)
          .build()
      result = chain.withRequest(request).proceed()
      current = fallback
      attempts += 1
    }
    return result
  }

  /** Only a missing variant (404) is fixed by another URL; a network error or timeout is not. */
  private fun isNotFound(result: ImageResult): Boolean =
    result is ErrorResult &&
      (result.throwable as? HttpException)?.response?.code == HTTP_NOT_FOUND

  private companion object {
    const val HTTP_NOT_FOUND = 404
    /** maxresdefault -> hq720 -> hqdefault. */
    const val MAX_FALLBACK_STEPS = 2
  }
}
