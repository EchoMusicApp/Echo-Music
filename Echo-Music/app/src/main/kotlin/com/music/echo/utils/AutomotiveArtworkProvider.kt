package echo.music.iad1tya.utils

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.ParcelFileDescriptor
import java.io.FileNotFoundException
import timber.log.Timber

/**
 * Serves the covers referenced by [AutomotiveArtwork.contentUriFor] to Android
 * Auto, Automotive OS and the instrument cluster, which only load local artwork
 * URIs. Exported because those run in other processes; only URIs signed by this
 * install are served, read-only.
 */
class AutomotiveArtworkProvider : ContentProvider() {

  override fun onCreate(): Boolean = true

  override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
    if (mode != "r") throw SecurityException("Artwork is read-only")
    val context = context ?: throw FileNotFoundException(uri.toString())
    val url = AutomotiveArtwork.originalUrl(context, uri)
    if (url == null) {
      Timber.tag(TAG).w("Rejected unsigned artwork request from %s", callerName())
      throw FileNotFoundException("Unknown artwork")
    }
    val file = AutomotiveArtwork.artworkFile(context, url)
    if (file == null) {
      Timber.tag(TAG).w("Artwork unavailable for %s: %s", callerName(), url)
      throw FileNotFoundException("Artwork unavailable")
    }
    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
  }

  private fun callerName(): String? = context?.packageManager?.getNameForUid(Binder.getCallingUid())

  override fun getType(uri: Uri): String = "image/jpeg"

  override fun query(
    uri: Uri,
    projection: Array<out String>?,
    selection: String?,
    selectionArgs: Array<out String>?,
    sortOrder: String?,
  ): Cursor? = null

  override fun insert(uri: Uri, values: ContentValues?): Uri? = null

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

  override fun update(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<out String>?,
  ): Int = 0

  private companion object {
    const val TAG = "AutomotiveArtwork"
  }
}
