package echo.music.iad1tya.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Artwork delivery for Android Auto, Automotive OS and the instrument cluster.
 *
 * Those surfaces live in other processes and load artwork themselves from the
 * icon / album-art URI that Media3 forwards. Android Auto only resolves local
 * `content://` and `android.resource://` URIs there; an `https://` cover URL is
 * ignored. Embedding the bytes in each item worked only for a bounded number of
 * items per page and only when the load finished in time, so covers appeared on
 * one surface (the full-screen player had the bitmap) and not on another (the
 * cluster, the one-third layout, later list pages read the URI).
 *
 * [contentUriFor] maps every remote cover to a signed `content://` URI served by
 * [AutomotiveArtworkProvider], which fetches it through Coil on demand and keeps
 * it in a small file cache. Every surface can then resolve every cover, at any
 * time, at the size it needs.
 */
object AutomotiveArtwork {

  private const val TAG = "AutomotiveArtwork"

  /** Served through the provider: no parcel limit, so allow the large player. */
  private const val PROVIDER_SIZE_PX = 1024

  /** The provider blocks a binder thread of the caller, so bound it. */
  private const val PROVIDER_LOAD_TIMEOUT_MS = 10_000L

  private const val PROVIDER_JPEG_QUALITY = 90

  private const val PATH_ART = "art"
  private const val CACHE_DIR = "automotive_artwork"
  private const val MAX_CACHE_DIR_BYTES = 64L * 1024 * 1024
  private const val TRIMMED_CACHE_DIR_BYTES = 48L * 1024 * 1024

  private const val PREFS = "automotive_artwork"
  private const val PREF_SECRET = "uri_secret"

  /** Mapping is called for every browse item and metadata read; keep it cheap. */
  private val contentUriCache = LruCache<String, Uri>(1024)

  @Volatile private var secret: ByteArray? = null

  fun authority(context: Context): String = "${context.packageName}.artwork"

  /**
   * The provider URI for a remote (`http`/`https`) cover, or [uri] itself for any
   * other scheme (`android.resource`, `content`, `file`) and for null.
   */
  fun contentUriFor(context: Context, uri: Uri?): Uri? {
    if (uri == null) return null
    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") return uri
    val url = uri.toString()
    contentUriCache.get(url)?.let { return it }
    val mapped =
      Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(authority(context))
        .appendPath(PATH_ART)
        .appendPath(Base64.encodeToString(url.toByteArray(), BASE64_FLAGS))
        .appendPath(sign(context, url))
        .build()
    contentUriCache.put(url, mapped)
    return mapped
  }

  /**
   * The remote URL behind a URI built by [contentUriFor], or null when [uri] is
   * not one or its signature does not match. The provider is exported, so the
   * signature is what keeps it from fetching URLs chosen by another app.
   */
  fun originalUrl(context: Context, uri: Uri): String? {
    if (uri.scheme != ContentResolver.SCHEME_CONTENT || uri.authority != authority(context)) return null
    val segments = uri.pathSegments
    if (segments.size != 3 || segments[0] != PATH_ART) return null
    val url =
      try {
        String(Base64.decode(segments[1], BASE64_FLAGS))
      } catch (e: IllegalArgumentException) {
        return null
      }
    if (!url.startsWith("https://") && !url.startsWith("http://")) return null
    val expected = sign(context, url).toByteArray()
    return url.takeIf { MessageDigest.isEqual(expected, segments[2].toByteArray()) }
  }

  fun MediaMetadata.withAutomotiveArtworkUri(context: Context): MediaMetadata {
    val mapped = contentUriFor(context, artworkUri)
    return if (mapped == artworkUri) this else buildUpon().setArtworkUri(mapped).build()
  }

  fun MediaItem.withAutomotiveArtworkUri(context: Context): MediaItem {
    val metadata = mediaMetadata.withAutomotiveArtworkUri(context)
    return if (metadata === mediaMetadata) this else buildUpon().setMediaMetadata(metadata).build()
  }

  /**
   * The cached cover file for [url], fetching and storing it first when needed.
   * Blocking: called from [AutomotiveArtworkProvider.openFile] on a binder thread.
   */
  fun artworkFile(context: Context, url: String): File? {
    val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
    val file = File(dir, sha256Hex(url) + ".jpg")
    if (file.length() > 0) {
      file.setLastModified(System.currentTimeMillis())
      return file
    }
    val data =
      runBlocking {
        fetchJpeg(context, url, PROVIDER_SIZE_PX, PROVIDER_JPEG_QUALITY, PROVIDER_LOAD_TIMEOUT_MS)
      } ?: return null
    // Write then rename, so a concurrent reader never sees a partial file.
    val tmp = File(dir, "${file.name}.${Thread.currentThread().id}.tmp")
    try {
      tmp.writeBytes(data)
      if (!tmp.renameTo(file)) tmp.delete()
    } catch (e: Exception) {
      Timber.tag(TAG).w(e, "Could not store artwork")
      tmp.delete()
    }
    trimCacheDir(dir)
    return file.takeIf { it.length() > 0 }
  }

  private suspend fun fetchJpeg(
    context: Context,
    url: String,
    sizePx: Int,
    quality: Int,
    timeoutMs: Long,
  ): ByteArray? =
    withTimeoutOrNull(timeoutMs) {
      try {
        val request =
          ImageRequest.Builder(context)
            .data(Uri.parse(url))
            .size(sizePx, sizePx)
            .allowHardware(false)
            .build()
        var result = context.imageLoader.execute(request)
        if (result is ErrorResult) {
          // Offline or flaky network: a cover Coil already has on disk still works.
          result =
            context.imageLoader.execute(
              request.newBuilder().networkCachePolicy(CachePolicy.READ_ONLY).build()
            )
        }
        when (result) {
          // Bitmap.compress can take a long time; keep it off the caller's thread.
          is SuccessResult ->
            withContext(Dispatchers.Default) { compress(result.image.toBitmap(), quality) }
          is ErrorResult -> {
            Timber.tag(TAG).w(result.throwable, "Artwork load failed for %s", url)
            null
          }
        }
      } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        Timber.tag(TAG).w(e, "Artwork load failed for %s", url)
        null
      }
    }

  private fun compress(bitmap: Bitmap, quality: Int): ByteArray? =
    try {
      ByteArrayOutputStream().use { out ->
        if (bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) out.toByteArray() else null
      }
    } catch (e: Exception) {
      Timber.tag(TAG).w(e, "Artwork compression failed")
      null
    }

  private fun trimCacheDir(dir: File) {
    val files = dir.listFiles()?.filter { it.isFile } ?: return
    var total = files.sumOf { it.length() }
    if (total <= MAX_CACHE_DIR_BYTES) return
    for (file in files.sortedBy { it.lastModified() }) {
      if (total <= TRIMMED_CACHE_DIR_BYTES) break
      val size = file.length()
      if (file.delete()) total -= size
    }
  }

  /**
   * Per-install key, persisted so URIs stay valid across process restarts:
   * the head unit may keep showing (and re-requesting) items it browsed earlier.
   */
  private fun secret(context: Context): ByteArray =
    secret
      ?: synchronized(this) {
        secret
          ?: run {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.getString(PREF_SECRET, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
              ?: ByteArray(32).also {
                SecureRandom().nextBytes(it)
                prefs.edit().putString(PREF_SECRET, Base64.encodeToString(it, Base64.NO_WRAP)).commit()
              }
          }.also { secret = it }
      }

  private fun sign(context: Context, url: String): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret(context), "HmacSHA256"))
    return mac.doFinal(url.toByteArray()).take(16).toHex()
  }

  private fun sha256Hex(value: String): String =
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).toList().toHex()

  private fun List<Byte>.toHex(): String = joinToString("") { "%02x".format(it) }

  private const val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
}
