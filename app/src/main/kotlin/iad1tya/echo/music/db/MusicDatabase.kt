package iad1tya.echo.music.db

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import iad1tya.echo.music.db.entities.AlbumArtistMap
import iad1tya.echo.music.db.entities.AlbumEntity
import iad1tya.echo.music.db.entities.ArtistEntity
import iad1tya.echo.music.db.entities.AccountEntity
import iad1tya.echo.music.db.entities.Event
import iad1tya.echo.music.db.entities.FormatEntity
import iad1tya.echo.music.db.entities.LyricsEntity
import iad1tya.echo.music.db.entities.PlaylistEntity
import iad1tya.echo.music.db.entities.PlayCountEntity
import iad1tya.echo.music.db.entities.PlaylistSongMap
import iad1tya.echo.music.db.entities.PlaylistSongMapPreview
import iad1tya.echo.music.db.entities.RelatedSongMap
import iad1tya.echo.music.db.entities.SearchHistory
import iad1tya.echo.music.db.entities.SetVideoIdEntity
import iad1tya.echo.music.db.entities.SongAlbumMap
import iad1tya.echo.music.db.entities.SongArtistMap
import iad1tya.echo.music.db.entities.SongEntity
import iad1tya.echo.music.db.entities.SortedSongAlbumMap
import iad1tya.echo.music.db.entities.SortedSongArtistMap
import iad1tya.echo.music.extensions.toSQLiteQuery
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date

class MusicDatabase(
    internal val delegate: InternalDatabase,
) : DatabaseDao by delegate.dao {
    val openHelper: SupportSQLiteOpenHelper
        get() = delegate.openHelper
    
    val accountDao: AccountDao
        get() = delegate.accountDao

    fun query(block: MusicDatabase.() -> Unit) =
        with(delegate) {
            queryExecutor.execute {
                block(this@MusicDatabase)
            }
        }

    fun transaction(block: MusicDatabase.() -> Unit) =
        with(delegate) {
            transactionExecutor.execute {
                runInTransaction {
                    block(this@MusicDatabase)
                }
            }
        }

    fun close() = delegate.close()
}

@Database(
    entities = [
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
        AccountEntity::class
    ],
    views = [
        SortedSongArtistMap::class,
        SortedSongAlbumMap::class,
        PlaylistSongMapPreview::class,
    ],
    version = 27,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class InternalDatabase : RoomDatabase() {
    abstract val dao: DatabaseDao
    abstract val accountDao: AccountDao

    companion object {
        const val DB_NAME = "song.db"

        fun newInstance(context: Context): MusicDatabase =
            MusicDatabase(
                delegate =
                Room
                    .databaseBuilder(context, InternalDatabase::class.java, DB_NAME)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_26, MIGRATION_25_26, MIGRATION_26_27)
                    // Safety net: any version gap not covered by explicit migrations falls
                    // back to a fresh database instead of crashing the app.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build(),
            )
    }
}

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            data class OldSongEntity(
                val id: String,
                val title: String,
                val duration: Int = -1, // in seconds
                val thumbnailUrl: String? = null,
                val albumId: String? = null,
                val albumName: String? = null,
                val liked: Boolean = false,
                val totalPlayTime: Long = 0, // in milliseconds
                val downloadState: Int = 0,
                val createDate: LocalDateTime = LocalDateTime.now(),
                val modifyDate: LocalDateTime = LocalDateTime.now(),
            )

            val converters = Converters()
            val artistMap = mutableMapOf<Int, String>()
            val artists = mutableListOf<ArtistEntity>()
            db.query("SELECT * FROM artist".toSQLiteQuery()).use { cursor ->
                while (cursor.moveToNext()) {
                    val oldId = cursor.getInt(0)
                    val newId = ArtistEntity.generateArtistId()
                    artistMap[oldId] = newId
                    artists.add(
                        ArtistEntity(
                            id = newId,
                            name = cursor.getString(1),
                        ),
                    )
                }
            }

            val playlistMap = mutableMapOf<Int, String>()
            val playlists = mutableListOf<PlaylistEntity>()
            db.query("SELECT * FROM playlist".toSQLiteQuery()).use { cursor ->
                while (cursor.moveToNext()) {
                    val oldId = cursor.getInt(0)
                    val newId = PlaylistEntity.generatePlaylistId()
                    playlistMap[oldId] = newId
                    playlists.add(
                        PlaylistEntity(
                            id = newId,
                            name = cursor.getString(1),
                        ),
                    )
                }
            }
            val playlistSongMaps = mutableListOf<PlaylistSongMap>()
            db.query("SELECT * FROM playlist_song".toSQLiteQuery()).use { cursor ->
                while (cursor.moveToNext()) {
                    playlistSongMaps.add(
                        PlaylistSongMap(
                            playlistId = playlistMap[cursor.getInt(1)]!!,
                            songId = cursor.getString(2),
                            position = cursor.getInt(3),
                        ),
                    )
                }
            }
            // ensure we have continuous playlist song position
            playlistSongMaps.sortBy { it.position }
            val playlistSongCount = mutableMapOf<String, Int>()
            playlistSongMaps.map { map ->
                if (map.playlistId !in playlistSongCount) playlistSongCount[map.playlistId] = 0
                map.copy(position = playlistSongCount[map.playlistId]!!).also {
                    playlistSongCount[map.playlistId] = playlistSongCount[map.playlistId]!! + 1
                }
            }
            val songs = mutableListOf<OldSongEntity>()
            val songArtistMaps = mutableListOf<SongArtistMap>()
            db.query("SELECT * FROM song".toSQLiteQuery()).use { cursor ->
                while (cursor.moveToNext()) {
                    val songId = cursor.getString(0)
                    songs.add(
                        OldSongEntity(
                            id = songId,
                            title = cursor.getString(1),
                            duration = cursor.getInt(3),
                            liked = cursor.getInt(4) == 1,
                            createDate = Instant.ofEpochMilli(Date(cursor.getLong(8)).time)
                                .atZone(ZoneOffset.UTC).toLocalDateTime(),
                            modifyDate = Instant.ofEpochMilli(Date(cursor.getLong(9)).time)
                                .atZone(ZoneOffset.UTC).toLocalDateTime(),
                        ),
                    )
                    songArtistMaps.add(
                        SongArtistMap(
                            songId = songId,
                            artistId = artistMap[cursor.getInt(2)]!!,
                            position = 0,
                        ),
                    )
                }
            }
            db.execSQL("DROP TABLE IF EXISTS song")
            db.execSQL("DROP TABLE IF EXISTS artist")
            db.execSQL("DROP TABLE IF EXISTS playlist")
            db.execSQL("DROP TABLE IF EXISTS playlist_song")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `song` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `duration` INTEGER NOT NULL, `thumbnailUrl` TEXT, `albumId` TEXT, `albumName` TEXT, `liked` INTEGER NOT NULL, `totalPlayTime` INTEGER NOT NULL, `isTrash` INTEGER NOT NULL, `download_state` INTEGER NOT NULL, `create_date` INTEGER NOT NULL, `modify_date` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `artist` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `thumbnailUrl` TEXT, `bannerUrl` TEXT, `description` TEXT, `createDate` INTEGER NOT NULL, `lastUpdateTime` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `album` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `year` INTEGER, `thumbnailUrl` TEXT, `songCount` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `createDate` INTEGER NOT NULL, `lastUpdateTime` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `playlist` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `author` TEXT, `authorId` TEXT, `year` INTEGER, `thumbnailUrl` TEXT, `createDate` INTEGER NOT NULL, `lastUpdateTime` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `song_artist_map` (`songId` TEXT NOT NULL, `artistId` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`songId`, `artistId`), FOREIGN KEY(`songId`) REFERENCES `song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`artistId`) REFERENCES `artist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_artist_map_songId` ON `song_artist_map` (`songId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_artist_map_artistId` ON `song_artist_map` (`artistId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `song_album_map` (`songId` TEXT NOT NULL, `albumId` TEXT NOT NULL, `index` INTEGER, PRIMARY KEY(`songId`, `albumId`), FOREIGN KEY(`songId`) REFERENCES `song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`albumId`) REFERENCES `album`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_album_map_songId` ON `song_album_map` (`songId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_album_map_albumId` ON `song_album_map` (`albumId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `album_artist_map` (`albumId` TEXT NOT NULL, `artistId` TEXT NOT NULL, `order` INTEGER NOT NULL, PRIMARY KEY(`albumId`, `artistId`), FOREIGN KEY(`albumId`) REFERENCES `album`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`artistId`) REFERENCES `artist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_artist_map_albumId` ON `album_artist_map` (`albumId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_artist_map_artistId` ON `album_artist_map` (`artistId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `playlist_song_map` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` TEXT NOT NULL, `songId` TEXT NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`playlistId`) REFERENCES `playlist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`songId`) REFERENCES `song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_song_map_playlistId` ON `playlist_song_map` (`playlistId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_song_map_songId` ON `playlist_song_map` (`songId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `download` (`id` INTEGER NOT NULL, `songId` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `search_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `query` TEXT NOT NULL)",
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query` ON `search_history` (`query`)")
            db.execSQL("CREATE VIEW `sorted_song_artist_map` AS SELECT * FROM song_artist_map ORDER BY position")
            db.execSQL(
                "CREATE VIEW `playlist_song_map_preview` AS SELECT * FROM playlist_song_map WHERE position <= 3 ORDER BY position",
            )
            artists.forEach { artist ->
                db.insert(
                    "artist",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "id" to artist.id,
                        "name" to artist.name,
                        "createDate" to converters.dateToTimestamp(artist.lastUpdateTime),
                        "lastUpdateTime" to converters.dateToTimestamp(artist.lastUpdateTime),
                    ),
                )
            }
            songs.forEach { song ->
                db.insert(
                    "song",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "id" to song.id,
                        "title" to song.title,
                        "duration" to song.duration,
                        "liked" to song.liked,
                        "totalPlayTime" to song.totalPlayTime,
                        "isTrash" to false,
                        "download_state" to song.downloadState,
                        "create_date" to converters.dateToTimestamp(song.createDate),
                        "modify_date" to converters.dateToTimestamp(song.modifyDate),
                    ),
                )
            }
            songArtistMaps.forEach { songArtistMap ->
                db.insert(
                    "song_artist_map",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "songId" to songArtistMap.songId,
                        "artistId" to songArtistMap.artistId,
                        "position" to songArtistMap.position,
                    ),
                )
            }
            playlists.forEach { playlist ->
                db.insert(
                    "playlist",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "id" to playlist.id,
                        "name" to playlist.name,
                        "createDate" to converters.dateToTimestamp(LocalDateTime.now()),
                        "lastUpdateTime" to converters.dateToTimestamp(LocalDateTime.now()),
                    ),
                )
            }
            playlistSongMaps.forEach { playlistSongMap ->
                db.insert(
                    "playlist_song_map",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "playlistId" to playlistSongMap.playlistId,
                        "songId" to playlistSongMap.songId,
                        "position" to playlistSongMap.position,
                    ),
                )
            }
        }
    }

/**
 * Comprehensive migration from v2 schema (produced by MIGRATION_1_2) to v26.
 *
 * Covers users who have been on the app since early versions (v1/v2) and are
 * upgrading directly to v26. It:
 *   - Recreates `song`, `artist`, `album`, `playlist` tables with the v26 schema,
 *     preserving all data that existed in v2 for each table.
 *   - Drops the legacy `download` table.
 *   - Creates all tables added after v2 (format, lyrics, event, related_song_map,
 *     set_video_id, playCount, account).
 *   - Recreates all views.
 */
val MIGRATION_2_26 =
    object : Migration(2, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Drop old views first — they reference tables being recreated.
            db.execSQL("DROP VIEW IF EXISTS `sorted_song_artist_map`")
            db.execSQL("DROP VIEW IF EXISTS `sorted_song_album_map`")
            db.execSQL("DROP VIEW IF EXISTS `playlist_song_map_preview`")

            // ── song ──────────────────────────────────────────────────────────
            db.execSQL(
                """CREATE TABLE `song_new` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `duration` INTEGER NOT NULL,
                    `thumbnailUrl` TEXT,
                    `albumId` TEXT,
                    `albumName` TEXT,
                    `explicit` INTEGER NOT NULL DEFAULT 0,
                    `year` INTEGER,
                    `date` INTEGER,
                    `dateModified` INTEGER,
                    `liked` INTEGER NOT NULL,
                    `likedDate` INTEGER,
                    `totalPlayTime` INTEGER NOT NULL,
                    `inLibrary` INTEGER,
                    `dateDownload` INTEGER,
                    `isLocal` INTEGER NOT NULL DEFAULT 0,
                    `localPath` TEXT,
                    `libraryAddToken` TEXT,
                    `libraryRemoveToken` TEXT,
                    `romanizeLyrics` INTEGER NOT NULL DEFAULT 1,
                    `isDownloaded` INTEGER NOT NULL DEFAULT 0,
                    `isUploaded` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`))"""
            )
            db.execSQL(
                """INSERT INTO `song_new`
                    (id, title, duration, thumbnailUrl, albumId, albumName, liked, totalPlayTime)
                   SELECT id, title, duration, thumbnailUrl, albumId, albumName, liked, totalPlayTime
                   FROM `song`"""
            )
            db.execSQL("DROP TABLE `song`")
            db.execSQL("ALTER TABLE `song_new` RENAME TO `song`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_albumId` ON `song` (`albumId`)")

            // ── artist ────────────────────────────────────────────────────────
            db.execSQL(
                """CREATE TABLE `artist_new` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `thumbnailUrl` TEXT,
                    `channelId` TEXT,
                    `lastUpdateTime` INTEGER NOT NULL,
                    `bookmarkedAt` INTEGER,
                    `isLocal` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`))"""
            )
            db.execSQL(
                """INSERT INTO `artist_new` (id, name, thumbnailUrl, lastUpdateTime)
                   SELECT id, name, thumbnailUrl, lastUpdateTime FROM `artist`"""
            )
            db.execSQL("DROP TABLE `artist`")
            db.execSQL("ALTER TABLE `artist_new` RENAME TO `artist`")

            // ── album ─────────────────────────────────────────────────────────
            db.execSQL(
                """CREATE TABLE `album_new` (
                    `id` TEXT NOT NULL,
                    `playlistId` TEXT,
                    `title` TEXT NOT NULL,
                    `year` INTEGER,
                    `thumbnailUrl` TEXT,
                    `themeColor` INTEGER,
                    `songCount` INTEGER NOT NULL,
                    `duration` INTEGER NOT NULL,
                    `explicit` INTEGER NOT NULL DEFAULT 0,
                    `lastUpdateTime` INTEGER NOT NULL,
                    `bookmarkedAt` INTEGER,
                    `likedDate` INTEGER,
                    `inLibrary` INTEGER,
                    `isLocal` INTEGER NOT NULL DEFAULT 0,
                    `isUploaded` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`))"""
            )
            db.execSQL(
                """INSERT INTO `album_new` (id, title, year, thumbnailUrl, songCount, duration, lastUpdateTime)
                   SELECT id, title, year, thumbnailUrl, songCount, duration, lastUpdateTime
                   FROM `album`"""
            )
            db.execSQL("DROP TABLE `album`")
            db.execSQL("ALTER TABLE `album_new` RENAME TO `album`")

            // ── playlist ──────────────────────────────────────────────────────
            db.execSQL(
                """CREATE TABLE `playlist_new` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `browseId` TEXT,
                    `createdAt` INTEGER,
                    `lastUpdateTime` INTEGER,
                    `isEditable` INTEGER NOT NULL DEFAULT 1,
                    `bookmarkedAt` INTEGER,
                    `remoteSongCount` INTEGER,
                    `playEndpointParams` TEXT,
                    `thumbnailUrl` TEXT,
                    `shuffleEndpointParams` TEXT,
                    `radioEndpointParams` TEXT,
                    `isLocal` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`))"""
            )
            db.execSQL(
                """INSERT INTO `playlist_new` (id, name)
                   SELECT id, name FROM `playlist`"""
            )
            db.execSQL("DROP TABLE `playlist`")
            db.execSQL("ALTER TABLE `playlist_new` RENAME TO `playlist`")

            // ── drop legacy download table ────────────────────────────────────
            db.execSQL("DROP TABLE IF EXISTS `download`")

            // ── new tables added after v2 ─────────────────────────────────────
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `format` (
                    `id` TEXT NOT NULL,
                    `itag` INTEGER NOT NULL,
                    `mimeType` TEXT NOT NULL,
                    `codecs` TEXT NOT NULL,
                    `bitrate` INTEGER NOT NULL,
                    `sampleRate` INTEGER,
                    `contentLength` INTEGER NOT NULL,
                    `loudnessDb` REAL,
                    `playbackUrl` TEXT,
                    PRIMARY KEY(`id`))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `lyrics` (
                    `id` TEXT NOT NULL,
                    `lyrics` TEXT NOT NULL,
                    PRIMARY KEY(`id`))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `event` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `songId` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `playTime` INTEGER NOT NULL,
                    FOREIGN KEY(`songId`) REFERENCES `song`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE)"""
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_event_songId` ON `event` (`songId`)")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `related_song_map` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `songId` TEXT NOT NULL,
                    `relatedSongId` TEXT NOT NULL,
                    FOREIGN KEY(`songId`) REFERENCES `song`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`relatedSongId`) REFERENCES `song`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE)"""
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_related_song_map_songId` ON `related_song_map` (`songId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_related_song_map_relatedSongId` ON `related_song_map` (`relatedSongId`)")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `set_video_id` (
                    `videoId` TEXT NOT NULL,
                    `setVideoId` TEXT,
                    PRIMARY KEY(`videoId`))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `playCount` (
                    `song` TEXT NOT NULL,
                    `year` INTEGER NOT NULL,
                    `month` INTEGER NOT NULL,
                    `count` INTEGER NOT NULL,
                    PRIMARY KEY(`song`, `year`, `month`))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `account` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `email` TEXT NOT NULL,
                    `channelHandle` TEXT NOT NULL,
                    `thumbnailUrl` TEXT,
                    `innerTubeCookie` TEXT NOT NULL,
                    `visitorData` TEXT NOT NULL,
                    `dataSyncId` TEXT NOT NULL,
                    `isActive` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `lastUsedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`))"""
            )

            // ── recreate views ────────────────────────────────────────────────
            db.execSQL("CREATE VIEW `sorted_song_artist_map` AS SELECT * FROM song_artist_map ORDER BY position")
            db.execSQL("CREATE VIEW `sorted_song_album_map` AS SELECT * FROM song_album_map ORDER BY `index`")
            db.execSQL("CREATE VIEW `playlist_song_map_preview` AS SELECT * FROM playlist_song_map WHERE position <= 3 ORDER BY position")
        }
    }

val MIGRATION_25_26 =
    object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE song ADD COLUMN localPath TEXT DEFAULT NULL")
        }
    }

val MIGRATION_26_27 =
    object : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE lyrics ADD COLUMN provider TEXT NOT NULL DEFAULT 'Unknown'")
        }
    }

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "isTrash"),
    DeleteColumn(tableName = "playlist", columnName = "author"),
    DeleteColumn(tableName = "playlist", columnName = "authorId"),
    DeleteColumn(tableName = "playlist", columnName = "year"),
    DeleteColumn(tableName = "playlist", columnName = "thumbnailUrl"),
    DeleteColumn(tableName = "playlist", columnName = "createDate"),
    DeleteColumn(tableName = "playlist", columnName = "lastUpdateTime"),
)
@RenameColumn.Entries(
    RenameColumn(
        tableName = "song",
        fromColumnName = "download_state",
        toColumnName = "downloadState"
    ),
    RenameColumn(tableName = "song", fromColumnName = "create_date", toColumnName = "createDate"),
    RenameColumn(tableName = "song", fromColumnName = "modify_date", toColumnName = "modifyDate"),
)
class Migration5To6 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.query("SELECT id FROM playlist WHERE id NOT LIKE 'LP%'").use { cursor ->
            while (cursor.moveToNext()) {
                db.execSQL(
                    "UPDATE playlist SET browseID = '${cursor.getString(0)}' WHERE id = '${
                        cursor.getString(
                            0
                        )
                    }'"
                )
            }
        }
    }
}

class Migration6To7 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.query("SELECT id, createDate FROM song").use { cursor ->
            while (cursor.moveToNext()) {
                db.execSQL(
                    "UPDATE song SET inLibrary = ${cursor.getLong(1)} WHERE id = '${
                        cursor.getString(
                            0
                        )
                    }'"
                )
            }
        }
    }
}

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "createDate"),
    DeleteColumn(tableName = "song", columnName = "modifyDate"),
)
class Migration7To8 : AutoMigrationSpec

@DeleteTable.Entries(
    DeleteTable(tableName = "download"),
)
class Migration9To10 : AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "downloadState"),
    DeleteColumn(tableName = "artist", columnName = "bannerUrl"),
    DeleteColumn(tableName = "artist", columnName = "description"),
    DeleteColumn(tableName = "artist", columnName = "createDate"),
)
class Migration10To11 : AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "album", columnName = "createDate"),
)
class Migration11To12 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE album SET bookmarkedAt = lastUpdateTime")
        db.query("SELECT DISTINCT albumId, albumName FROM song").use { cursor ->
            while (cursor.moveToNext()) {
                val albumId = cursor.getString(0)
                val albumName = cursor.getString(1)
                db.insert(
                    table = "album",
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE,
                    values =
                    contentValuesOf(
                        "id" to albumId,
                        "title" to albumName,
                        "songCount" to 0,
                        "duration" to 0,
                        "lastUpdateTime" to 0,
                    ),
                )
            }
        }
        db.query("CREATE INDEX IF NOT EXISTS `index_song_albumId` ON `song` (`albumId`)")
    }
}

class Migration12To13 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
    }
}

class Migration13To14 : AutoMigrationSpec {
    @SuppressLint("Range")
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE playlist SET createdAt = '${Converters().dateToTimestamp(LocalDateTime.now())}'")
        db.execSQL(
            "UPDATE playlist SET lastUpdateTime = '${
                Converters().dateToTimestamp(
                    LocalDateTime.now()
                )
            }'"
        )
    }
}

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "isLocal"),
    DeleteColumn(tableName = "song", columnName = "localPath"),
    DeleteColumn(tableName = "artist", columnName = "isLocal"),
    DeleteColumn(tableName = "playlist", columnName = "isLocal"),
)
class Migration16To17 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE playlist SET bookmarkedAt = lastUpdateTime")
        db.execSQL("UPDATE playlist SET isEditable = 1 WHERE browseId IS NOT NULL")
    }
}

class Migration18To19 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        // Add explicit column
        db.execSQL("UPDATE song SET explicit = 0 WHERE explicit IS NULL")
    }
}

class Migration19To20 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        // Add explicit column
        db.execSQL("UPDATE song SET explicit = 0 WHERE explicit IS NULL")
    }
}

@DeleteColumn.Entries(
    DeleteColumn(
        tableName = "song",
        columnName = "artistName"
    )
)
class Migration20To21 : AutoMigrationSpec

class Migration21To22 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        // Add LibraryTokens
        db.execSQL("ALTER TABLE song ADD COLUMN libraryAddToken TEXT DEFAULT ''")
        db.execSQL("ALTER TABLE song ADD COLUMN libraryRemoveToken TEXT DEFAULT ''")

        // Add romanizeLyrics column
        db.execSQL("ALTER TABLE song ADD COLUMN romanizeLyrics INTEGER NOT NULL DEFAULT 1")

        // Add isDownloaded column
        db.execSQL("ALTER TABLE song ADD COLUMN isDownloaded INTEGER NOT NULL DEFAULT 0")
    }
}

class Migration22To23: AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        // Add isUploaded column
        db.execSQL("ALTER TABLE song ADD COLUMN isUploaded INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE album ADD COLUMN isUploaded INTEGER NOT NULL DEFAULT 0")
    }
}

class Migration25To26 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        // Add localPath column
        db.execSQL("ALTER TABLE song ADD COLUMN localPath TEXT DEFAULT NULL")
    }
}