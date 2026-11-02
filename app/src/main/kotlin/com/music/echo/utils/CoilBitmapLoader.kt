package echo.music.iad1tya.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.core.graphics.createBitmap
import androidx.media3.common.util.BitmapLoader
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Session artwork loader.
 *
 * Media3 publishes the legacy session metadata (read by Android Auto, the
 * instrument cluster and the notification) immediately on every track change:
 * when this loader's future is not complete yet, that first update goes out
 * without a bitmap and a second one follows once the cover arrives. The car's
 * cluster does not reliably pick up the second update, so the cover was often
 * missing there while the head unit (which re-reads it) showed it.
 *
 * Covers are therefore kept in [bitmapCache] and served as already-completed
 * futures, and [prefetch] warms the cache for upcoming queue items, so the very
 * first metadata update for a track already carries its cover.
 */
class CoilBitmapLoader(
  private val context: Context,
  private val scope: CoroutineScope,
) : BitmapLoader {

  override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

  /**
   * Preferred decode size for artwork handed to clients.
   *
   * Automotive renders the same item at several sizes (full-screen card, reduced
   * player, dashboard thumbnail) and asks for the biggest one, so keeping this
   * above the largest common request avoids a re-fetch for smaller surfaces.
   */
  private val targetSize = 1024

  /**
   * A stuck request would leave Android Auto and the cluster on the app icon, so
   * fail after a bounded wait and let the caller fall back (or retry on the next
   * transition).
   */
  private val timeoutMs = 6_000L

  /** Decoded covers by source URL; a handful of 1024px covers (~4 MB each). */
  private val bitmapCache =
    object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
      override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

  /** Loads in flight, so a prefetch and the session's own request share one fetch. */
  private val inFlight = ConcurrentHashMap<String, ListenableFuture<Bitmap>>()

  private fun createFallbackBitmap(): Bitmap = createBitmap(64, 64)

  private fun Bitmap.copyIfNeeded(): Bitmap {
    return if (isRecycled) {
      createFallbackBitmap()
    } else {
      try {
        copy(Bitmap.Config.ARGB_8888, false) ?: createFallbackBitmap()
      } catch (e: Exception) {
        createFallbackBitmap()
      }
    }
  }

  /**
   * Decodes synchronously: embedded covers are small, and an already-completed
   * future lets Media3 include the bitmap in the first metadata update. An
   * undecodable image fails the future instead of returning a blank bitmap,
   * which every surface would show as an empty cover.
   */
  override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
    try {
      BitmapFactory.decodeByteArray(data, 0, data.size)?.let { Futures.immediateFuture(it) }
        ?: Futures.immediateFailedFuture(IOException("Undecodable artwork data"))
    } catch (e: Exception) {
      Timber.tag(TAG).w(e, "Failed to decode bitmap data")
      Futures.immediateFailedFuture(e)
    }

  /** Warms the cache for [uri] (an item about to play); failures are ignored. */
  fun prefetch(uri: Uri?) {
    if (uri == null) return
    loadBitmap(uri)
  }

  override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
    // The session exposes covers as AutomotiveArtworkProvider URIs; load the
    // remote URL directly instead of round-tripping through our own provider.
    val source = AutomotiveArtwork.originalUrl(context, uri)?.let(Uri::parse) ?: uri
    val key = source.toString()
    bitmapCache.get(key)?.let { return Futures.immediateFuture(it) }
    inFlight[key]?.let { return it }
    val future = scope.future(Dispatchers.IO) { fetch(source) }
    inFlight[key] = future
    future.addListener(
      {
        inFlight.remove(key, future)
        if (!future.isCancelled) {
          try {
            bitmapCache.put(key, Futures.getDone(future))
          } catch (e: Exception) {
            Timber.tag(TAG).w("Artwork load failed for $key: ${e.message}")
          }
        }
      },
      Runnable::run,
    )
    return future
  }

  private suspend fun fetch(uri: Uri): Bitmap {
    val request =
      ImageRequest.Builder(context)
        .data(uri)
        .size(targetSize)
        .allowHardware(false)
        .build()

    var result =
      withTimeoutOrNull(timeoutMs) { context.imageLoader.execute(request) }
        ?: throw IOException("Timed out loading artwork from $uri")

    if (result is ErrorResult) {
      val cacheRequest =
        request.newBuilder().networkCachePolicy(coil3.request.CachePolicy.READ_ONLY).build()
      result =
        withTimeoutOrNull(timeoutMs) { context.imageLoader.execute(cacheRequest) }
          ?: throw IOException("Timed out loading artwork from $uri")
    }

    return when (result) {
      // Surface the failure as an exception instead of a blank 64x64: callers
      // (notably Media3's artwork plumbing) treat a failed future as "no
      // artwork yet" and can retry, while a blank bitmap is cached and
      // displayed as a broken cover for the rest of the session.
      is ErrorResult -> throw result.throwable
      is SuccessResult ->
        try {
          result.image.toBitmap().copyIfNeeded()
        } catch (e: Exception) {
          Timber.tag(TAG).w(e, "Failed to convert image to bitmap")
          createFallbackBitmap()
        }
    }
  }

  private companion object {
    const val TAG = "CoilBitmapLoader"
  }
}
