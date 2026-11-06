package echo.music.iad1tya.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import echo.music.iad1tya.db.daos.RecommendationExclusionDao
import echo.music.iad1tya.db.daos.SongPlayStatsDao
import echo.music.iad1tya.db.daos.SpeedDialDao
import echo.music.iad1tya.db.daos.TasteProfileDao
import echo.music.iad1tya.db.entities.AlbumArtistMap
import echo.music.iad1tya.db.entities.AlbumEntity
import echo.music.iad1tya.db.entities.ArtistEntity
import echo.music.iad1tya.db.entities.BeatInfoEntity
import echo.music.iad1tya.db.entities.Event
import echo.music.iad1tya.db.entities.FormatEntity
import echo.music.iad1tya.db.entities.LyricsEntity
import echo.music.iad1tya.db.entities.PlayCountEntity
import echo.music.iad1tya.db.entities.PlaylistEntity
import echo.music.iad1tya.db.entities.PlaylistSongMap
import echo.music.iad1tya.db.entities.PlaylistSongMapPreview
import echo.music.iad1tya.db.entities.RecognitionHistory
import echo.music.iad1tya.db.entities.RecommendationExclusionEntity
import echo.music.iad1tya.db.entities.RelatedSongMap
import echo.music.iad1tya.db.entities.SearchHistory
import echo.music.iad1tya.db.entities.SetVideoIdEntity
import echo.music.iad1tya.db.entities.SongAlbumMap
import echo.music.iad1tya.db.entities.SongArtistMap
import echo.music.iad1tya.db.entities.SongEntity
import echo.music.iad1tya.db.entities.SongPlayStatsEntity
import echo.music.iad1tya.db.entities.SortedSongAlbumMap
import echo.music.iad1tya.db.entities.SortedSongArtistMap
import echo.music.iad1tya.db.entities.SpeedDialItem
import echo.music.iad1tya.db.entities.TasteProfileEntity
import kotlinx.coroutines.Dispatchers
import java.io.File

// Desktop version of the original MusicDatabase.kt. Android migrations are left out: a desktop database starts fresh at version 48.
// shortcut: no migration path yet, add real migrations once the schema changes after this version.
class MusicDatabase(private val delegate: InternalDatabase) : DatabaseDao by delegate.dao {
  val speedDialDao get() = delegate.speedDialDao
  val songPlayStatsDao get() = delegate.songPlayStatsDao
  val recommendationExclusionDao get() = delegate.recommendationExclusionDao
  val tasteProfileDao get() = delegate.tasteProfileDao

  suspend fun withTransaction(block: suspend MusicDatabase.() -> Unit) =
    delegate.useWriterConnection { it.immediateTransaction { block(this@MusicDatabase) } }

  fun close() = delegate.close()
}

@Database(
  entities =
    [
      SongEntity::class,
      ArtistEntity::class,
      AlbumEntity::class,
      PlaylistEntity::class,
      SongArtistMap::class,
      SongAlbumMap::class,
      AlbumArtistMap::class,
      PlaylistSongMap::class,
      SearchHistory::class,
      FormatEntity::class,
      LyricsEntity::class,
      Event::class,
      RelatedSongMap::class,
      SetVideoIdEntity::class,
      PlayCountEntity::class,
      RecognitionHistory::class,
      SpeedDialItem::class,
      BeatInfoEntity::class,
      SongPlayStatsEntity::class,
      RecommendationExclusionEntity::class,
      TasteProfileEntity::class,
    ],
  views =
    [
      SortedSongArtistMap::class,
      SortedSongAlbumMap::class,
      PlaylistSongMapPreview::class,
    ],
  version = 48,
  exportSchema = false,
)
@TypeConverters
abstract class InternalDatabase : RoomDatabase() {
  abstract val dao: DatabaseDao
  abstract val speedDialDao: SpeedDialDao
  abstract val songPlayStatsDao: SongPlayStatsDao
  abstract val recommendationExclusionDao: RecommendationExclusionDao
  abstract val tasteProfileDao: TasteProfileDao

  companion object {
    const val DB_NAME = "song.db"

    fun newInstance(dir: File): MusicDatabase =
      MusicDatabase(
        Room.databaseBuilder<InternalDatabase>(File(dir, DB_NAME).absolutePath) { InternalDatabase_Impl() }
          .setDriver(BundledSQLiteDriver())
          .setQueryCoroutineContext(Dispatchers.IO)
          .build()
      )
  }
}
