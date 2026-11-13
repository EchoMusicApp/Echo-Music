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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

// Desktop version of the original MusicDatabase.kt. Android migrations are left out: a desktop database starts fresh at version 48.
// shortcut: no migration path yet, add real migrations once the schema changes after this version.
class MusicDatabase(private val delegate: InternalDatabase, private val file: File? = null) : DatabaseDao by delegate.dao {
  // Look-alike of Room's openHelper.writableDatabase (path + version) which backup/restore reads.
  class OpenHelper(file: File?) {
    val writableDatabase = Db(file)
    class Db(file: File?) { val path: String = file?.absolutePath ?: ""; val version: Int = 48 }
  }
  val openHelper = OpenHelper(file)
  val speedDialDao get() = delegate.speedDialDao
  val songPlayStatsDao get() = delegate.songPlayStatsDao
  val recommendationExclusionDao get() = delegate.recommendationExclusionDao
  val tasteProfileDao get() = delegate.tasteProfileDao

  // Same as the original: query { } runs in the background and does not wait.
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  fun query(block: suspend MusicDatabase.() -> Unit) {
    scope.launch { block(this@MusicDatabase) }
  }

  // The original ran this on a blocking executor; here it is a suspend function on the caller's coroutine.

  // Blocking like the Android original (menus call it from click handlers that are not coroutines).
  fun transaction(block: suspend MusicDatabase.() -> Unit) = kotlinx.coroutines.runBlocking(Dispatchers.IO) { withTransaction(block) }

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
@TypeConverters(Converters::class)
abstract class InternalDatabase : RoomDatabase() {
  abstract val dao: DatabaseDao
  abstract val speedDialDao: SpeedDialDao
  abstract val songPlayStatsDao: SongPlayStatsDao
  abstract val recommendationExclusionDao: RecommendationExclusionDao
  abstract val tasteProfileDao: TasteProfileDao

  companion object {
    const val DB_NAME = "song.db"

    // Takes the app Context; read by reflection so this module needs no Android classes (they change what Room generates).
    fun newInstance(context: Any): MusicDatabase =
      newInstance(context.javaClass.getMethod("getFilesDir").invoke(context) as File)

    fun newInstance(dir: File): MusicDatabase {
      val file = File(dir, DB_NAME)
      return MusicDatabase(
        Room.databaseBuilder<InternalDatabase>(file.absolutePath) { InternalDatabase_Impl() }
          .setDriver(BundledSQLiteDriver())
          .setQueryCoroutineContext(Dispatchers.IO)
          .build(),
        file,
      )
    }
  }
}
