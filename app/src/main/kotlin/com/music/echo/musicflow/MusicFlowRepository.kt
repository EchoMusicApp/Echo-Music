package echo.music.iad1tya.musicflow

import android.content.Context
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.filterBlockedArtists
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.constants.DataSaverEnabledKey
import echo.music.iad1tya.constants.HideExplicitKey
import echo.music.iad1tya.constants.HideVideoSongsKey
import echo.music.iad1tya.constants.MusicFlowHideRecentKey
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.utils.dataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

class MusicFlowOfflineException(cause: Throwable?) : Exception(cause)

data class MusicFlowSignals(
  val filter: MusicFlowCandidateFilter,
  val likedArtists: Set<String>,
)

interface MusicFlowCursor {
  val hasMore: Boolean

  suspend fun next(): List<MusicFlowItem>
}

interface MusicFlowFeedSource {
  suspend fun signals(): MusicFlowSignals

  fun open(nowPlaying: SongItem?): MusicFlowCursor

  suspend fun offlineCandidates(): List<MusicFlowItem>
}

@Singleton
class MusicFlowRepository
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val database: MusicDatabase,
) : MusicFlowFeedSource {

  override suspend fun signals(): MusicFlowSignals =
    withContext(Dispatchers.IO) {
      val hideRecent = context.dataStore.data.first()[MusicFlowHideRecentKey] ?: true
      val liked = database.likedSongsByCreateDateAsc().first()
      MusicFlowSignals(
        filter =
          MusicFlowCandidateFilter(
            excludedTrackKeys =
              database.recommendationExclusionDao.getAll().map { it.trackKey }.toSet(),
            hiddenSongIds = database.hiddenFromQuickPicksSongIds().first().toSet(),
            recentSongIds =
              if (hideRecent) {
                database.recentlyPlayedSongs(RECENT_EXCLUSION_LIMIT).first().map { it.id }.toSet()
              } else {
                emptySet()
              },
          ),
        likedArtists = liked.flatMap { song -> song.artists.map { it.id } }.toSet(),
      )
    }

  override fun open(nowPlaying: SongItem?): MusicFlowCursor = RadioCursor(nowPlaying)

  override suspend fun offlineCandidates(): List<MusicFlowItem> =
    withContext(Dispatchers.IO) {
      database
        .downloadedSongsByCreateDateAsc()
        .first()
        .shuffled()
        .map { it.toMusicFlowSongItem() }
        .applyContentFilters()
        .map { MusicFlowItem(it, MusicFlowSource.OFFLINE) }
    }

  private suspend fun List<SongItem>.applyContentFilters(): List<SongItem> {
    val preferences = context.dataStore.data.first()
    val hideVideos =
      (preferences[HideVideoSongsKey] ?: false) || (preferences[DataSaverEnabledKey] ?: false)
    return filterExplicit(preferences[HideExplicitKey] ?: false)
      .filterVideoSongs(hideVideos)
      .filterBlockedArtists()
  }

  private data class Seed(val song: SongItem, val source: MusicFlowSource)

  private inner class RadioCursor(private val nowPlaying: SongItem?) : MusicFlowCursor {
    private var seeds: ArrayDeque<Seed>? = null
    private var tailEndpoint: WatchEndpoint? = null
    private var tailContinuation: String? = null
    private var tailSource = MusicFlowSource.CATALOG
    private var exhausted = false

    override val hasMore: Boolean
      get() = !exhausted

    override suspend fun next(): List<MusicFlowItem> =
      withContext(Dispatchers.IO) {
        val queue = seeds ?: loadSeeds().also { seeds = it }
        val batch = mutableListOf<List<MusicFlowItem>>()
        var networkFailure: Throwable? = null

        repeat(SEEDS_PER_BATCH) {
          val seed = queue.removeFirstOrNull() ?: return@repeat
          runCatching {
            radio(
              WatchEndpoint(videoId = seed.song.id, playlistId = "RDAMVM${seed.song.id}"),
              null,
            )
          }
            .onSuccess { (songs, endpoint, continuation) ->
              batch += songs.map { MusicFlowItem(it, seed.source) }
              tailEndpoint = endpoint
              tailContinuation = continuation
              tailSource = seed.source
            }
            .onFailure {
              if (it is CancellationException) throw it
              networkFailure = it
              Timber.tag(TAG).w(it, "Radio fetch failed for a Music Flow seed")
            }
        }

        if (batch.isEmpty()) {
          val endpoint = tailEndpoint
          val continuation = tailContinuation
          if (endpoint != null && continuation != null) {
            runCatching { radio(endpoint, continuation) }
              .onSuccess { (songs, nextEndpoint, nextContinuation) ->
                batch += songs.map { MusicFlowItem(it, tailSource) }
                tailEndpoint = nextEndpoint
                tailContinuation = nextContinuation
              }
              .onFailure {
                if (it is CancellationException) throw it
                networkFailure = it
                tailContinuation = null
              }
          }
        }

        if (batch.isEmpty() && networkFailure != null) {
          throw MusicFlowOfflineException(networkFailure)
        }
        if (queue.isEmpty() && tailContinuation == null) exhausted = true
        interleave(batch)
      }

    private suspend fun radio(
      endpoint: WatchEndpoint,
      continuation: String?,
    ): Triple<List<SongItem>, WatchEndpoint, String?> {
      val result = YouTube.next(endpoint, continuation).getOrThrow()
      return Triple(result.items.applyContentFilters(), result.endpoint, result.continuation)
    }

    private suspend fun loadSeeds(): ArrayDeque<Seed> {
      val recent =
        database.recentlyPlayedSongs(SEED_LIMIT).first().map { it.seed(MusicFlowSource.HISTORY) }
      val liked =
        database
          .likedSongsByCreateDateAsc()
          .first()
          .takeLast(LIKED_POOL)
          .shuffled()
          .take(SEED_LIMIT)
          .map { it.seed(MusicFlowSource.LIKED) }
      val quickPicks =
        database.quickPicks().first().take(SEED_LIMIT).map { it.seed(MusicFlowSource.QUICK_PICKS) }
      val personal =
        listOfNotNull(nowPlaying?.let { Seed(it, MusicFlowSource.NOW_PLAYING) }) +
          interleave(listOf(recent, liked, quickPicks))
      val seeds = personal.ifEmpty { catalogSeeds() }
      return ArrayDeque(seeds.distinctBy { it.song.id })
    }

    private suspend fun catalogSeeds(): List<Seed> = runCatching {
      YouTube.home().getOrThrow()
    }
      .getOrNull()
      ?.sections
      ?.flatMap { section -> section.items.filterIsInstance<SongItem>() }
      ?.applyContentFilters()
      ?.take(SEED_LIMIT)
      ?.map { Seed(it, MusicFlowSource.CATALOG) }
      .orEmpty()

    private fun Song.seed(source: MusicFlowSource) = Seed(toMusicFlowSongItem(), source)
  }

  private companion object {
    const val TAG = "MusicFlowRepository"
    const val SEEDS_PER_BATCH = 2
    const val SEED_LIMIT = 6
    const val LIKED_POOL = 40
    const val RECENT_EXCLUSION_LIMIT = 30
  }
}

internal fun <T> interleave(sources: List<List<T>>): List<T> = buildList {
  for (index in 0 until (sources.maxOfOrNull { it.size } ?: 0)) {
    sources.forEach { source -> source.getOrNull(index)?.let { add(it) } }
  }
}
