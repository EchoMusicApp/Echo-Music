package echo.music.iad1tya.utils

import com.music.innertube.models.filterBlockedArtists
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import echo.music.iad1tya.db.entities.Song

fun filterQuickPickSongs(
  songs: List<Song>,
  excludedSongIds: Set<String>,
  blockedArtists: Set<String>,
  hideExplicit: Boolean,
  hideVideoSongs: Boolean,
): List<Song> {
  val visible = songs.filterNot { it.song.hideFromQuickPicks || it.id in excludedSongIds }
  val allowedIds = visible.map { it.toCarouselSong() }
    .filterBlockedArtists(blockedArtists)
    .filterExplicit(hideExplicit)
    .filterVideoSongs(hideVideoSongs)
    .mapTo(mutableSetOf()) { it.id }
  return visible.filter { it.id in allowedIds }.distinctBy { it.id }
}
