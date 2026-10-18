package echo.music.iad1tya.utils

import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import kotlinx.coroutines.delay

/**
 * Makes song artwork load reliably. When a YouTube thumbnail 404s (maxres / sd versions don't exist for
 * many songs) it quietly walks down to the next size that does, and any other failure gets one retry
 * after a short pause, which covers flaky mobile networks.
 */
class ThumbnailFallbackInterceptor : Interceptor {
  override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
    val first = chain.proceed()
    if (first !is ErrorResult) return first
    val url = chain.request.data as? String ?: return first
    if (!url.startsWith("http")) return first

    val tries = ArrayList<String>()
    val m = Regex("(maxresdefault|sddefault|hqdefault|mqdefault)(\\.jpg|\\.webp)").find(url)
    if (m != null) {
      val order = listOf("maxresdefault", "sddefault", "hqdefault", "mqdefault")
      for (q in order.drop(order.indexOf(m.groupValues[1]) + 1)) tries += url.replace(m.value, q + m.groupValues[2])
      // the .webp variant is sometimes missing where the .jpg exists
      if (m.groupValues[2] == ".webp") tries += url.replace("/vi_webp/", "/vi/").replace(".webp", ".jpg")
    }
    // transient failure: try the very same URL once more
    tries += url

    for ((i, candidate) in tries.withIndex()) {
      if (candidate == url && i == tries.lastIndex) delay(400)
      val retry = chain.withRequest(chain.request.newBuilder().data(candidate).build()).proceed()
      if (retry is SuccessResult) return retry
    }
    return first
  }
}
