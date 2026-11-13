package echo.music.iad1tya.utils

import coil3.intercept.Interceptor
import coil3.network.HttpException
import coil3.request.ErrorResult
import coil3.request.ImageResult
import echo.music.iad1tya.ui.utils.ytimgFallbackUrl

/**
 * Retries a missing YouTube video thumbnail with the always-available variant.
 *
 * maxresdefault / sddefault do not exist for many videos. Older builds stored those URLs in the
 * database and in queued media items, so without this the affected songs never showed a cover (in
 * the app, the notification or on Android Auto). Every request goes through the singleton loader,
 * so this covers all of them.
 */
class YouTubeThumbnailFallbackInterceptor : Interceptor {
  override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
    val result = chain.proceed()
    if (result !is ErrorResult) return result
    // Only a missing variant (404) is fixed by another URL; a network error or
    // timeout would just fail a second time.
    if ((result.throwable as? HttpException)?.response?.code != HTTP_NOT_FOUND) return result
    val original = chain.request.data.toString()
    val fallback = ytimgFallbackUrl(original) ?: return result
    val request =
      chain.request
        .newBuilder()
        .data(fallback)
        // Store the fallback under the original URL too, so later requests for
        // the missing variant hit the disk cache instead of repeating the 404.
        .diskCacheKey(original)
        .build()
    return chain.withRequest(request).proceed()
  }

  private companion object {
    const val HTTP_NOT_FOUND = 404
  }
}
