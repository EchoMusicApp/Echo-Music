package com.music.echo.spotify

import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.spotify.models.SpotifySimpleArtist
import echo.music.iad1tya.spotify.models.SpotifyTrack
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpotifyQueueTest {

  private fun createSpotifyTrack(id: String, name: String, artistName: String, durationMs: Int = 180_000): SpotifyTrack {
    return SpotifyTrack(
      id = id,
      name = name,
      artists = listOf(SpotifySimpleArtist(id = "art_$id", name = artistName)),
      durationMs = durationMs
    )
  }

  private fun createSongItem(id: String, title: String, artistName: String, durationSec: Int = 180): SongItem {
    return SongItem(
      id = id,
      title = title,
      artists = listOf(Artist(name = artistName, id = "yt_art_$id")),
      duration = durationSec,
      thumbnail = "https://example.com/$id.jpg"
    )
  }

  @Test
  fun testSpotifyQueuePreservesOrderAndIndex() = runBlocking {
    val track1 = createSpotifyTrack("sp_1", "Song One", "Artist A")
    val track2 = createSpotifyTrack("sp_2", "Song Two", "Artist B")
    val track3 = createSpotifyTrack("sp_3", "Song Three", "Artist C")
    val tracks = listOf(track1, track2, track3)

    val song1 = createSongItem("yt_1", "Song One", "Artist A")
    val song2 = createSongItem("yt_2", "Song Two", "Artist B")
    val song3 = createSongItem("yt_3", "Song Three", "Artist C")

    val mockResolver = object : SpotifyPlaybackResolver() {
      val map = mapOf("sp_1" to song1, "sp_2" to song2, "sp_3" to song3)
      override fun getCached(trackId: String): SongItem? = map[trackId]
      override suspend fun resolveTrack(track: SpotifyTrack, hideExplicit: Boolean): SongItem? = map[track.id]
      override suspend fun resolveTracks(tracks: List<SpotifyTrack>, hideExplicit: Boolean): Map<String, SongItem> =
        tracks.mapNotNull { t -> map[t.id]?.let { t.id to it } }.toMap()
    }

    val preloadMeta = MediaMetadata(
      id = "yt_2",
      title = "Song Two",
      artists = emptyList(),
      duration = 180,
      thumbnailUrl = null
    )

    // User selected track2 (startIndex = 1)
    val queue = SpotifyQueue(
      title = "My Spotify Playlist",
      tracks = tracks,
      startIndex = 1,
      preloadItem = preloadMeta,
      resolver = mockResolver
    )

    val initialStatus = queue.getInitialStatus()

    assertEquals("My Spotify Playlist", initialStatus.title)
    assertEquals(3, initialStatus.items.size)
    assertEquals("yt_1", initialStatus.items[0].mediaId)
    assertEquals("yt_2", initialStatus.items[1].mediaId)
    assertEquals("yt_3", initialStatus.items[2].mediaId)
    // Starting index must point to song2 (index 1)
    assertEquals(1, initialStatus.mediaItemIndex)
  }

  @Test
  fun testSpotifyQueueHandlesUnresolvedTracks() = runBlocking {
    val track1 = createSpotifyTrack("sp_1", "Song One", "Artist A")
    val track2 = createSpotifyTrack("sp_2", "Unresolvable Song", "Unknown Artist")
    val track3 = createSpotifyTrack("sp_3", "Song Three", "Artist C")
    val tracks = listOf(track1, track2, track3)

    val song1 = createSongItem("yt_1", "Song One", "Artist A")
    val song3 = createSongItem("yt_3", "Song Three", "Artist C")

    val mockResolver = object : SpotifyPlaybackResolver() {
      val map = mapOf("sp_1" to song1, "sp_3" to song3)
      override fun getCached(trackId: String): SongItem? = map[trackId]
      override suspend fun resolveTrack(track: SpotifyTrack, hideExplicit: Boolean): SongItem? = map[track.id]
      override suspend fun resolveTracks(tracks: List<SpotifyTrack>, hideExplicit: Boolean): Map<String, SongItem> =
        tracks.mapNotNull { t -> map[t.id]?.let { t.id to it } }.toMap()
    }

    val preloadMeta = MediaMetadata(
      id = "yt_3",
      title = "Song Three",
      artists = emptyList(),
      duration = 180,
      thumbnailUrl = null
    )

    // User selected track3 (startIndex = 2), but track2 failed to resolve
    val queue = SpotifyQueue(
      title = "My Spotify Playlist",
      tracks = tracks,
      startIndex = 2,
      preloadItem = preloadMeta,
      resolver = mockResolver
    )

    val initialStatus = queue.getInitialStatus()

    assertEquals(2, initialStatus.items.size)
    assertEquals("yt_1", initialStatus.items[0].mediaId)
    assertEquals("yt_3", initialStatus.items[1].mediaId)
    // Because track2 was skipped, track3 is now at index 1 in the resolved list
    assertEquals(1, initialStatus.mediaItemIndex)
  }

  @Test
  fun testSpotifyQueueHandlesDuplicateTracks() = runBlocking {
    val track1 = createSpotifyTrack("sp_1", "Song One", "Artist A")
    val track2 = createSpotifyTrack("sp_2", "Song Two", "Artist B")
    val track3 = createSpotifyTrack("sp_1", "Song One", "Artist A") // duplicate!
    val tracks = listOf(track1, track2, track3)

    val song1 = createSongItem("yt_1", "Song One", "Artist A")
    val song2 = createSongItem("yt_2", "Song Two", "Artist B")

    val mockResolver = object : SpotifyPlaybackResolver() {
      val map = mapOf("sp_1" to song1, "sp_2" to song2)
      override fun getCached(trackId: String): SongItem? = map[trackId]
      override suspend fun resolveTrack(track: SpotifyTrack, hideExplicit: Boolean): SongItem? = map[track.id]
      override suspend fun resolveTracks(tracks: List<SpotifyTrack>, hideExplicit: Boolean): Map<String, SongItem> =
        tracks.mapNotNull { t -> map[t.id]?.let { t.id to it } }.toMap()
    }

    val preloadMeta = MediaMetadata(
      id = "yt_1",
      title = "Song One",
      artists = emptyList(),
      duration = 180,
      thumbnailUrl = null
    )

    // User selected the second occurrence (startIndex = 2)
    val queue = SpotifyQueue(
      title = "Duplicate Playlist",
      tracks = tracks,
      startIndex = 2,
      preloadItem = preloadMeta,
      resolver = mockResolver
    )

    val initialStatus = queue.getInitialStatus()

    assertEquals(3, initialStatus.items.size)
    assertEquals("yt_1", initialStatus.items[0].mediaId)
    assertEquals("yt_2", initialStatus.items[1].mediaId)
    assertEquals("yt_1", initialStatus.items[2].mediaId)
    // mediaItemIndex must point to the second occurrence (index 2)
    assertEquals(2, initialStatus.mediaItemIndex)
  }

  @Test
  fun testFindBestMatchPrefersExactMatchOverCoverOrKaraoke() {
    val resolver = SpotifyPlaybackResolver()
    val targetTrack = createSpotifyTrack("sp_hit", "Blinding Lights", "The Weeknd", durationMs = 200_000)

    val cover = createSongItem("cover_id", "Blinding Lights (Cover)", "Random Singer", durationSec = 200)
    val karaoke = createSongItem("karaoke_id", "Blinding Lights [Karaoke Version]", "The Weeknd", durationSec = 200)
    val extendedRemix = createSongItem("remix_id", "Blinding Lights (10 Hour Extended)", "The Weeknd", durationSec = 36000)
    val exactMatch = createSongItem("exact_id", "Blinding Lights", "The Weeknd", durationSec = 200)

    val candidates = listOf(cover, karaoke, extendedRemix, exactMatch)
    val best = resolver.findBestMatch(candidates, targetTrack)

    assertNotNull(best)
    assertEquals("exact_id", best?.id)
  }

  @Test
  fun testSpotifyQueuePaginationAndHasPendingSongs() = runBlocking {
    val allTracks = (1..35).map { createSpotifyTrack("sp_$it", "Song $it", "Artist $it") }
    val mockResolver = object : SpotifyPlaybackResolver() {
      override fun getCached(trackId: String): SongItem? =
        createSongItem("yt_$trackId", "Song $trackId", "Artist $trackId")
      override suspend fun resolveTrack(track: SpotifyTrack, hideExplicit: Boolean): SongItem? =
        createSongItem("yt_${track.id}", "Song ${track.name}", "Artist ${track.id}")
      override suspend fun resolveTracks(tracks: List<SpotifyTrack>, hideExplicit: Boolean): Map<String, SongItem> =
        tracks.associate { it.id to createSongItem("yt_${it.id}", "Song ${it.name}", "Artist ${it.id}") }
    }

    val preloadMeta = MediaMetadata(
      id = "yt_sp_1",
      title = "Song 1",
      artists = emptyList(),
      duration = 180,
      thumbnailUrl = null
    )

    val queue = SpotifyQueue(
      title = "Large Playlist",
      tracks = allTracks,
      startIndex = 0,
      preloadItem = preloadMeta,
      resolver = mockResolver
    )

    // Before getInitialStatus, hasPendingSongs and hasNextPage must be true
    assertEquals(true, queue.hasPendingSongs())
    assertEquals(true, queue.hasNextPage())

    val initialStatus = queue.getInitialStatus()
    assertEquals(20, initialStatus.items.size)
    assertEquals(0, initialStatus.mediaItemIndex)
    assertEquals(true, queue.hasPendingSongs())
    assertEquals(true, queue.hasNextPage())

    // Fetch next page
    val nextPageItems = queue.nextPage()
    assertEquals(15, nextPageItems.size)
    assertEquals(false, queue.hasPendingSongs())
    assertEquals(false, queue.hasNextPage())
  }

  @Test
  fun testSpotifyQueuePreloadItemFallbackGuarantee() = runBlocking {
    val track1 = createSpotifyTrack("sp_1", "Song One", "Artist A")
    val track2 = createSpotifyTrack("sp_2", "Song Two", "Artist B")
    val tracks = listOf(track1, track2)

    val song1 = createSongItem("yt_1", "Song One", "Artist A")

    // Mock resolver returns null for sp_2
    val mockResolver = object : SpotifyPlaybackResolver() {
      val map = mapOf("sp_1" to song1)
      override fun getCached(trackId: String): SongItem? = map[trackId]
      override suspend fun resolveTrack(track: SpotifyTrack, hideExplicit: Boolean): SongItem? = map[track.id]
      override suspend fun resolveTracks(tracks: List<SpotifyTrack>, hideExplicit: Boolean): Map<String, SongItem> =
        tracks.mapNotNull { t -> map[t.id]?.let { t.id to it } }.toMap()
    }

    val preloadMeta = MediaMetadata(
      id = "yt_fallback_2",
      title = "Song Two Fallback",
      artists = emptyList(),
      duration = 180,
      thumbnailUrl = null
    )

    val queue = SpotifyQueue(
      title = "Fallback Playlist",
      tracks = tracks,
      startIndex = 1,
      preloadItem = preloadMeta,
      resolver = mockResolver
    )

    val initialStatus = queue.getInitialStatus()
    assertEquals(2, initialStatus.items.size)
    assertEquals("yt_1", initialStatus.items[0].mediaId)
    // Selected track was sp_2, resolvedMap missed it, but preloadItem fallback was preserved!
    assertEquals("yt_fallback_2", initialStatus.items[1].mediaId)
    assertEquals(1, initialStatus.mediaItemIndex)
  }
}
