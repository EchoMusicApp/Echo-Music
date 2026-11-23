package echo.music.iad1tya.utils

import com.music.innertube.models.Album
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.db.entities.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroCarouselSongsTest {
  private fun song(id: String, artwork: String = id) =
    SongItem(id = id, title = id, artists = emptyList(), thumbnail = "https://art/$artwork")

  @Test
  fun mixesAllSourcesWithTheMostRecentSongFirst() {
    val result = selectHeroCarouselSongs(
      recentlyPlayed = (1..30).map { song("recent-$it") },
      library = (1..30).map { song("library-$it") },
      recommendations = (1..30).map { song("radio-$it") },
    )

    assertEquals(20, result.size)
    assertEquals("recent-1", result.first().id)
    assertTrue(result.any { it.id.startsWith("library-") })
    assertTrue(result.any { it.id.startsWith("radio-") })
  }

  @Test
  fun keepsRecommendationsThatAreNotInTheLibrary() {
    val result = selectHeroCarouselSongs(emptyList(), emptyList(), listOf(song("new-song")))

    assertEquals(listOf("new-song"), result.map { it.id })
  }

  @Test
  fun accountSongsAreIncludedEvenWithALargeLocalLibrary() {
    val result = selectHeroCarouselSongs(
      emptyList(), (1..50).map { song("local-$it") }, emptyList(),
      account = listOf(song("account-only")), limit = 4,
    )

    assertTrue(result.any { it.id == "account-only" })
  }

  @Test
  fun removesDuplicateTracksAcrossSourcesAndPrefersRecentMetadata() {
    val recent = song("shared").copy(title = "Recent title")
    val result = selectHeroCarouselSongs(
      listOf(recent, recent), listOf(song("shared")), listOf(song("shared"), song("new")),
    )

    assertEquals(listOf("shared", "new"), result.map { it.id })
    assertEquals("Recent title", result.first().title)
  }

  @Test
  fun prioritizesDifferentCoversEvenWhenThumbnailSizesDiffer() {
    val first = song("first", "shared").copy(thumbnail = "https://art/shared=w300-h300")
    val repeated = song("repeated", "shared").copy(thumbnail = "https://art/shared=w600-h600")
    val result = selectHeroCarouselSongs(
      listOf(first, repeated, song("different")), emptyList(), emptyList(), limit = 2,
    )

    assertEquals(listOf("first", "different"), result.map { it.id })
  }

  @Test
  fun spreadsTracksFromTheSameAlbumAndFillsSparseFeeds() {
    val album = Album(name = "Album", id = "album-id")
    val first = song("first").copy(album = album)
    val second = song("second").copy(album = album)
    val result = selectHeroCarouselSongs(listOf(first, second, song("new")), emptyList(), emptyList())

    assertEquals(listOf("first", "new", "second"), result.map { it.id })
  }

  @Test
  fun recognizesSharedCoversWhenOnlyOneSourceHasAlbumMetadata() {
    val recent = song("recent", "shared")
    val account = song("account", "shared").copy(album = Album(name = "Album", id = "album"))
    val result = selectHeroCarouselSongs(
      listOf(recent), emptyList(), listOf(song("new")), account = listOf(account), limit = 2,
    )

    assertEquals(listOf("recent", "new"), result.map { it.id })
  }

  @Test
  fun appliesContentPreferencesToEverySource() {
    val explicit = song("explicit").copy(explicit = true)
    val video = song("video").copy(musicVideoType = "MUSIC_VIDEO_TYPE_OMV")
    val allowed = song("allowed")
    val result = selectHeroCarouselSongs(
      listOf(explicit), listOf(video), listOf(explicit, video, allowed),
      account = listOf(explicit, video),
      hideExplicit = true, hideVideoSongs = true,
    )

    assertEquals(listOf(allowed), result)
    assertEquals(3, selectHeroCarouselSongs(listOf(explicit), listOf(video), listOf(allowed)).size)
  }

  @Test
  fun toleratesEmptySourcesAndNonPositiveLimits() {
    assertTrue(selectHeroCarouselSongs(emptyList(), emptyList(), emptyList()).isEmpty())
    assertTrue(selectHeroCarouselSongs(listOf(song("recent")), emptyList(), emptyList(), limit = 0).isEmpty())
    assertTrue(selectHeroCarouselSongs(listOf(song("recent")), emptyList(), emptyList(), limit = -1).isEmpty())
  }

  @Test
  fun hiddenSongsCannotReappearFromAnySource() {
    val hidden = song("hidden")
    val allowed = song("allowed")
    val result = selectHeroCarouselSongs(
      listOf(hidden), listOf(hidden), listOf(hidden, allowed),
      account = listOf(hidden), excludedSongIds = setOf(hidden.id),
    )

    assertEquals(listOf(allowed), result)
  }

  @Test
  fun blockedArtistsAreExcludedFromRecentSavedAndRecommendedSongs() {
    val blocked = song("blocked").copy(artists = listOf(Artist("Blocked artist", "blocked-id")))
    val allowed = song("allowed")
    val result = selectHeroCarouselSongs(
      listOf(blocked), listOf(blocked), listOf(blocked, allowed), account = listOf(blocked),
      blockedArtists = setOf("blocked-id||Blocked artist"),
    )

    assertEquals(listOf(allowed), result)
  }

  @Test
  fun preservesVideoAndExplicitFlagsWhenMappingLocalSongs() {
    val mapped = Song(
      SongEntity(id = "local", title = "Local", explicit = true, isVideo = true), emptyList(),
    ).toCarouselSong()

    assertTrue(mapped.explicit)
    assertTrue(mapped.isVideoSong)
    assertEquals("", mapped.thumbnail)
  }
}
