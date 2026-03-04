package iad1tya.echo.music.utils.scanners

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.db.entities.AlbumEntity
import iad1tya.echo.music.db.entities.ArtistEntity
import iad1tya.echo.music.db.entities.Song
import iad1tya.echo.music.db.entities.SongAlbumMap
import iad1tya.echo.music.db.entities.SongArtistMap
import iad1tya.echo.music.db.entities.SongEntity
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

data class SongTempData(
    val song: Song,
)

class LocalMediaScanner(val context: Context, val database: MusicDatabase) {
    private val TAG = "LocalMediaScanner"

    val scannerProgress = MutableStateFlow(0f)
    val isScanning = MutableStateFlow(false)

    fun generateSongId() = "LOC" + UUID.randomUUID().toString().replace("-", "")
    fun generateArtistId() = "ART" + UUID.randomUUID().toString().replace("-", "")
    fun generateAlbumId() = "ALB" + UUID.randomUUID().toString().replace("-", "")

    suspend fun scan(
        scanPaths: List<String> = emptyList(),
        excludedPaths: List<String> = emptyList(),
        sensitivity: Int = 1,
        strictExt: Boolean = false,
        useFilenameAsTitle: Boolean = false
    ) {
        if (isScanning.value) return
        isScanning.value = true
        scannerProgress.value = 0f

        Log.i(TAG, "Starting Local Media Scan. Paths: $scanPaths, Excluded: $excludedPaths, Sensitivity: $sensitivity, StrictExt: $strictExt, UseFilename: $useFilenameAsTitle")

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA, // Path
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )

        val tempSongs = ArrayList<SongTempData>()
        // Expanded format support matching Auxio/TagLib capabilities
        val supportedExtensions = setOf(
            // Common formats
            "mp3", "flac", "m4a", "wav", "ogg", "opus", "aac",
            // Lossless
            "alac", "ape", "wv", "tta",
            // Windows Media
            "wma", "asf",
            // Matroska/WebM
            "mka", "webm", "mkv",
            // Module formats
            "mod", "s3m", "xm", "it",
            // AIFF
            "aif", "aiff", "aifc",
            // Speex
            "spx",
            // Musepack
            "mpc", "mp+", "mpp",
            // WavPack
            "wv",
            // Other
            "3gp", "3gpp", "mp4", "m4b", "m4r", "m4p",
            "ac3", "amr", "awb", "dts", "dsf", "dff",
            "mid", "midi", "smf",
        )

        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val pathCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateAddedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (c.moveToNext()) {
                val mediaStoreId = c.getLong(idCol)
                val path = c.getString(pathCol)
                
                // Debug log
                // Log.d(TAG, "Checking path: $path") 

                // Filter paths
                if (scanPaths.isNotEmpty()) {
                    if (scanPaths.none { path.startsWith(it) }) {
                        Log.v(TAG, "Skipping $path - does not start with any of $scanPaths")
                        continue
                    }
                }
                if (excludedPaths.any { path.startsWith(it) }) {
                    Log.v(TAG, "Skipping $path - excluded")
                    continue
                }
                
                // Strict extension filter
                if (strictExt) {
                    val ext = path.substringAfterLast('.', "").lowercase()
                    if (ext !in supportedExtensions) continue
                }

                var title = c.getString(titleCol)
                val duration = c.getInt(durationCol) / 1000 // ms to seconds
                val artistName = c.getString(artistCol) ?: "<Unknown>"
                val albumName = c.getString(albumCol) ?: "<Unknown>"
                val albumId = c.getLong(albumIdCol)
                val dateAdded = c.getLong(dateAddedCol)
                val year = c.getInt(yearCol)
                val fileName = c.getString(nameCol) ?: path.substringAfterLast('/')

                if (useFilenameAsTitle || title.isNullOrBlank()) {
                    title = fileName.substringBeforeLast('.')
                }

                val songId = generateSongId()
                
                val artistEntity = ArtistEntity(
                    id = "", // placeholder
                    name = artistName,
                    isLocal = true
                )
                
                val albumEntity = AlbumEntity(
                    id = "", // placeholder
                    title = albumName,
                    year = year,
                    songCount = 0,
                    duration = 0,
                    isLocal = true
                )

                // Store content:// URI for reliable playback on all Android versions
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    mediaStoreId
                ).toString()

                // Album art URI from MediaStore
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                val songEntity = SongEntity(
                    id = songId,
                    title = title,
                    duration = duration,
                    thumbnailUrl = albumArtUri,
                    isLocal = true,
                    localPath = contentUri,
                    inLibrary = LocalDateTime.ofInstant(Instant.ofEpochSecond(dateAdded), ZoneOffset.UTC),
                )

                tempSongs.add(SongTempData(
                    Song(
                        song = songEntity,
                        artists = listOf(artistEntity),
                        album = albumEntity
                    )
                ))
            }
        }
        
        Log.i(TAG, "Found ${tempSongs.size} songs in MediaStore after filtering")

        syncDB(tempSongs)

        isScanning.value = false
        Log.i(TAG, "Finished Local Media Scan")
    }

    private suspend fun syncDB(newSongs: List<SongTempData>) {
        val total = newSongs.size
        var processed = 0
        
        // Fetch existing local songs to update or ignore
        val existingSongs = database.allLocalSongs().toMutableList()
        
        // Remove duplicates from previous scans (keep only the first occurrence)
        val seen = mutableSetOf<String>()
        val duplicateIds = mutableListOf<String>()
        for (song in existingSongs) {
            val key = "${song.song.title}|${song.song.duration}|${song.artists.firstOrNull()?.name}"
            if (!seen.add(key)) {
                duplicateIds.add(song.id)
            }
        }
        if (duplicateIds.isNotEmpty()) {
            Log.i(TAG, "Removing ${duplicateIds.size} duplicate local songs")
            duplicateIds.forEach { id -> database.deleteSongById(id) }
            existingSongs.removeAll { it.id in duplicateIds }
        }

        newSongs.forEach { temp ->
            processed++
            scannerProgress.value = processed.toFloat() / total.toFloat()

            // Match by title + duration + artist to avoid duplicates across path format changes
            val existing = existingSongs.find {
                it.song.title == temp.song.song.title &&
                it.song.duration == temp.song.song.duration &&
                it.artists.firstOrNull()?.name == temp.song.artists.firstOrNull()?.name
            }
            
            if (existing != null) {
                // Already exists — update localPath and thumbnailUrl if changed
                if (existing.song.localPath != temp.song.song.localPath ||
                    existing.song.thumbnailUrl != temp.song.song.thumbnailUrl) {
                    database.updateLocalSongMetadata(
                        existing.id,
                        existing.song.inLibrary,
                        temp.song.song.localPath!!,
                        temp.song.song.thumbnailUrl
                    )
                }
            } else {
                // New Song
                database.transaction {
                    // 1. Handle Artist
                    val artistName = temp.song.artists.first().name // Assuming single artist from MediaStore
                    var artistId = ""
                    val dbArtist = database.getArtistByName(artistName)
                    if (dbArtist != null) {
                        artistId = dbArtist.id
                    } else {
                        artistId = generateArtistId()
                        val newArtist = temp.song.artists.first().copy(id = artistId, lastUpdateTime = LocalDateTime.now())
                        insert(newArtist)
                    }

                    // 2. Handle Album
                    val albumTitle = temp.song.album?.title ?: "<Unknown>"
                    var albumId = ""
                    val dbAlbum = database.getAlbumByName(albumTitle)
                    if (dbAlbum != null) {
                        albumId = dbAlbum.id
                        // Update album stats?
                    } else {
                        albumId = generateAlbumId()
                        val newAlbum = temp.song.album!!.copy(id = albumId, lastUpdateTime = LocalDateTime.now())
                        insert(newAlbum)
                    }

                    // 3. Insert Song
                    // Use the ID generated during scan
                    val songToInsert = temp.song.song.copy(
                        albumId = albumId,
                        albumName = albumTitle
                    )
                    insert(songToInsert)

                    // 4. Link Artist
                    insert(SongArtistMap(songToInsert.id, artistId, 0))

                    // 5. Link Album
                    insert(SongAlbumMap(songToInsert.id, albumId, 0)) // Index 0
                }
            }
        }
    }
}
