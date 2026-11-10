package echo.music.iad1tya.utils

import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil3.ImageLoader
import coil3.Uri
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.toAndroidUri

class LocalAlbumArtFetcher(
  private val albumId: Long,
  private val options: Options,
) : Fetcher {
  override suspend fun fetch(): FetchResult? {
    val albumUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
    val bitmap =
      runCatching {
          options.context.contentResolver.loadThumbnail(albumUri, Size(SIZE, SIZE), null)
        }
        .getOrNull() ?: return null
    return ImageFetchResult(
      image = bitmap.asImage(),
      isSampled = false,
      dataSource = DataSource.DISK,
    )
  }

  class Factory : Fetcher.Factory<Uri> {
    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
      val uri = data.toAndroidUri()
      if (uri.scheme != "content" || uri.authority != MediaStore.AUTHORITY) return null
      val segments = uri.pathSegments
      if (segments.size < 2 || segments[segments.size - 2] != ALBUM_ART_SEGMENT) return null
      val albumId = segments.last().toLongOrNull() ?: return null
      return LocalAlbumArtFetcher(albumId, options)
    }
  }

  private companion object {
    const val SIZE = 1024
    const val ALBUM_ART_SEGMENT = "albumart"
  }
}
