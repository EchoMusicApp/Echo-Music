package echo.music.iad1tya.playback

import android.content.Context
import com.music.innertube.models.Album
import com.music.innertube.models.Artist
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.pages.HomePage
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * Persists the Android Auto Home shelves across process restarts.
 *
 * YouTube personalizes Home on every request, so a freshly started process got a different set of
 * shelves each time the car connected: "Mixtape per te" present on one drive, replaced by "Nuove
 * uscite" on the next. Restoring the last merged snapshot (and merging every fetch into it) keeps
 * the full set of categories, in the same order, from one connection to the next.
 *
 * The snapshot is tagged with the account it was built for and is ignored for any other account, so
 * one account's personalized shelves never reach another.
 *
 * Only the fields the Auto tree uses are stored (songs and playlists), so the innertube models do
 * not need to be serializable.
 */
class AutoHomeSnapshotStore(context: Context) {

  data class Snapshot(val sections: List<HomePage.Section>, val lastSeenMs: Map<String, Long>)

  private val file = File(context.filesDir, FILE_NAME)

  /** The stored snapshot, or null when there is none or it belongs to another account. */
  fun load(account: String): Snapshot? =
    try {
      if (!file.exists()) {
        null
      } else {
        val root = JSONObject(file.readText())
        if (root.optString("account") != account) return null
        val sections = mutableListOf<HomePage.Section>()
        val lastSeen = mutableMapOf<String, Long>()
        val array = root.getJSONArray("sections")
        for (i in 0 until array.length()) {
          val json = array.getJSONObject(i)
          val section =
            HomePage.Section(
              title = json.getString("title"),
              label = json.optStringOrNull("label"),
              thumbnail = json.optStringOrNull("thumbnail"),
              endpoint = null,
              items =
                json.getJSONArray("items").let { items ->
                  (0 until items.length()).mapNotNull { readItem(items.getJSONObject(it)) }
                },
            )
          sections += section
          lastSeen[json.getString("key")] = json.optLong("lastSeen", 0L)
        }
        Snapshot(sections, lastSeen)
      }
    } catch (e: Exception) {
      Timber.tag(TAG).w(e, "Could not read Auto Home snapshot")
      null
    }

  fun save(
    account: String,
    sections: List<HomePage.Section>,
    keyOf: (HomePage.Section) -> String,
    lastSeenMs: Map<String, Long>,
  ) {
    try {
      val array = JSONArray()
      sections.forEach { section ->
        val key = keyOf(section)
        array.put(
          JSONObject()
            .put("key", key)
            .put("title", section.title)
            .putOpt("label", section.label)
            .putOpt("thumbnail", section.thumbnail)
            .put("lastSeen", lastSeenMs[key] ?: 0L)
            .put(
              "items",
              JSONArray().apply { section.items.mapNotNull(::writeItem).forEach(::put) },
            )
        )
      }
      val tmp = File(file.parentFile, "$FILE_NAME.tmp")
      tmp.writeText(JSONObject().put("account", account).put("sections", array).toString())
      if (!tmp.renameTo(file)) tmp.delete()
    } catch (e: Exception) {
      Timber.tag(TAG).w(e, "Could not save Auto Home snapshot")
    }
  }

  private fun writeItem(item: YTItem): JSONObject? =
    when (item) {
      is SongItem ->
        JSONObject()
          .put("type", "song")
          .put("id", item.id)
          .put("title", item.title)
          .put("artists", JSONArray().apply { item.artists.forEach { put(writeArtist(it)) } })
          .putOpt("album", item.album?.let { JSONObject().put("name", it.name).put("id", it.id) })
          .putOpt("duration", item.duration)
          .putOpt("musicVideoType", item.musicVideoType)
          .put("thumbnail", item.thumbnail)
          .put("explicit", item.explicit)
          .putOpt("setVideoId", item.setVideoId)
      is PlaylistItem ->
        JSONObject()
          .put("type", "playlist")
          .put("id", item.id)
          .put("title", item.title)
          .putOpt("author", item.author?.let(::writeArtist))
          .putOpt("songCountText", item.songCountText)
          .putOpt("thumbnail", item.thumbnail)
      // The Auto tree renders only songs and playlists.
      else -> null
    }

  private fun readItem(json: JSONObject): YTItem? =
    when (json.optString("type")) {
      "song" ->
        SongItem(
          id = json.getString("id"),
          title = json.getString("title"),
          artists =
            json.getJSONArray("artists").let { a ->
              (0 until a.length()).map { readArtist(a.getJSONObject(it)) }
            },
          album =
            json.optJSONObject("album")?.let {
              Album(name = it.getString("name"), id = it.getString("id"))
            },
          duration = if (json.has("duration")) json.getInt("duration") else null,
          musicVideoType = json.optStringOrNull("musicVideoType"),
          thumbnail = json.getString("thumbnail"),
          explicit = json.optBoolean("explicit", false),
          setVideoId = json.optStringOrNull("setVideoId"),
        )
      "playlist" ->
        PlaylistItem(
          id = json.getString("id"),
          title = json.getString("title"),
          author = json.optJSONObject("author")?.let(::readArtist),
          songCountText = json.optStringOrNull("songCountText"),
          thumbnail = json.optStringOrNull("thumbnail"),
          playEndpoint = null,
          shuffleEndpoint = null,
          radioEndpoint = null,
        )
      else -> null
    }

  private fun writeArtist(artist: Artist): JSONObject =
    JSONObject().put("name", artist.name).putOpt("id", artist.id)

  private fun readArtist(json: JSONObject): Artist =
    Artist(name = json.getString("name"), id = json.optStringOrNull("id"))

  private fun JSONObject.optStringOrNull(name: String): String? =
    if (has(name) && !isNull(name)) getString(name) else null

  private companion object {
    const val TAG = "AutoHomeSnapshot"
    const val FILE_NAME = "auto_home_snapshot.json"
  }
}
