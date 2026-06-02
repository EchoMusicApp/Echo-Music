

package iad1tya.echo.music.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import iad1tya.echo.music.MainActivity
import iad1tya.echo.music.R
import iad1tya.echo.music.db.InternalDatabase
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.db.entities.ArtistEntity
import iad1tya.echo.music.db.entities.Song
import iad1tya.echo.music.db.entities.SongEntity
import iad1tya.echo.music.extensions.div
import iad1tya.echo.music.extensions.tryOrNull
import iad1tya.echo.music.extensions.zipInputStream
import iad1tya.echo.music.extensions.zipOutputStream
import iad1tya.echo.music.playback.MusicService
import iad1tya.echo.music.playback.MusicService.Companion.PERSISTENT_QUEUE_FILE
import iad1tya.echo.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import javax.inject.Inject
import kotlin.system.exitProcess

data class CsvImportState(
    val previewRows: List<List<String>> = emptyList(),
    val artistColumnIndex: Int = 0,
    val titleColumnIndex: Int = 1,
    val urlColumnIndex: Int = -1,
    val hasHeader: Boolean = true,
)

data class ConvertedSongLog(
    val title: String,
    val artists: String,
)

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    val database: MusicDatabase,
) : ViewModel() {
    fun backup(context: Context, uri: Uri) {
        runCatching {
            context.applicationContext.contentResolver.openOutputStream(uri)?.use {
                it.buffered().zipOutputStream().use { outputStream ->
                    (context.filesDir / "datastore" / SETTINGS_FILENAME).inputStream().buffered()
                        .use { inputStream ->
                            outputStream.putNextEntry(ZipEntry(SETTINGS_FILENAME))
                            inputStream.copyTo(outputStream)
                        }
                    runBlocking(Dispatchers.IO) {
                        database.checkpoint()
                    }
                    FileInputStream(database.openHelper.writableDatabase.path).use { inputStream ->
                        outputStream.putNextEntry(ZipEntry(InternalDatabase.DB_NAME))
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }.onSuccess {
            Toast.makeText(context, R.string.backup_create_success, Toast.LENGTH_SHORT).show()
        }.onFailure {
            reportException(it)
            Toast.makeText(context, R.string.backup_create_failed, Toast.LENGTH_SHORT).show()
        }
    }

    fun restore(context: Context, uri: Uri) {
        runCatching {
            Timber.tag("RESTORE").i("Starting restore from URI: $uri")
            context.applicationContext.contentResolver.openInputStream(uri)?.use { raw ->
                raw.zipInputStream().use { inputStream ->
                    var entry = tryOrNull { inputStream.nextEntry } 
                    var foundAny = false
                    while (entry != null) {
                        Timber.tag("RESTORE").i("Found zip entry: ${entry.name}")
                        when (entry.name) {
                            SETTINGS_FILENAME -> {
                                Timber.tag("RESTORE").i("Restoring settings to datastore")
                                foundAny = true
                                (context.filesDir / "datastore" / SETTINGS_FILENAME).outputStream()
                                    .use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                            }
                            InternalDatabase.DB_NAME -> {
                                Timber.tag("RESTORE").i("Restoring DB (entry = ${entry.name})")
                                foundAny = true
                                
                                val tempFile = java.io.File(context.cacheDir, "temp_restore.db")
                                java.io.FileOutputStream(tempFile).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                                
                                var backupVersion = 0
                                runCatching {
                                    java.io.RandomAccessFile(tempFile, "r").use { raf ->
                                        raf.seek(60)
                                        backupVersion = raf.readInt()
                                    }
                                }
                                
                                if (backupVersion > 35) {
                                    Timber.tag("RESTORE").e("Backup version ($backupVersion) > current version (35)")
                                    kotlinx.coroutines.runBlocking(Dispatchers.Main) {
                                        Toast.makeText(context, context.getString(R.string.restore_failed) + ": Backup is from a newer app version.", Toast.LENGTH_LONG).show()
                                    }
                                    tempFile.delete()
                                    return
                                }
                                
                                try {
                                    val dbPath = database.openHelper.writableDatabase.path
                                    runBlocking(Dispatchers.IO) { database.checkpoint() }
                                    database.close()
                                    Timber.tag("RESTORE").i("Overwriting DB at path: $dbPath")
                                    tempFile.copyTo(java.io.File(dbPath), overwrite = true)
                                    tempFile.delete()
                                    Timber.tag("RESTORE").i("DB overwrite complete")
                                } catch (e: Exception) {
                                    Timber.tag("RESTORE").e(e, "Due to new architecture this backup can't be restored")
                                    kotlinx.coroutines.runBlocking(Dispatchers.Main) {
                                        Toast.makeText(context, "Due to new architecture, this backup can't be restored", Toast.LENGTH_LONG).show()
                                    }
                                    tempFile.delete()
                                    return
                                }
                            }
                            else -> {
                                Timber.tag("RESTORE").i("Skipping unexpected entry: ${entry.name}")
                            }
                        }
                        entry = tryOrNull { inputStream.nextEntry } 
                    }
                    if (!foundAny) {
                        Timber.tag("RESTORE").w("No expected entries found in archive")
                    }
                }
            } ?: run {
                Timber.tag("RESTORE").e("Could not open input stream for uri: $uri")
            }

            context.stopService(Intent(context, MusicService::class.java))
            context.filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            context.startActivity(Intent(context, MainActivity::class.java))
            exitProcess(0)
        }.onFailure {
            reportException(it)
            Timber.tag("RESTORE").e(it, "Due to new architecture this backup can't be restored")
            Toast.makeText(context, "Due to new architecture, this backup can't be restored", Toast.LENGTH_SHORT).show()
        }
    }

    fun previewCsvFile(context: Context, uri: Uri): CsvImportState {
        val previewRows = mutableListOf<List<String>>()
        val csvState: CsvImportState
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()
                val rowsToPreview = lines.take(6).map { parseCsvLine(it) }
                previewRows.addAll(rowsToPreview)

                val hasHeader = lines.isNotEmpty() && lines[0].contains(",")
                csvState = CsvImportState(
                    previewRows = previewRows,
                    hasHeader = hasHeader,
                )
                return csvState
            }
        }.onFailure {
            reportException(it)
            Toast.makeText(context, "Failed to preview CSV file", Toast.LENGTH_SHORT).show()
        }
        return CsvImportState()
    }

    fun importPlaylistFromCsv(
        context: Context,
        uri: Uri,
        columnMapping: CsvImportState,
        onProgress: (Int) -> Unit = {},
        onLogUpdate: (List<ConvertedSongLog>) -> Unit = {},
    ): ArrayList<Song> {
        val songs = arrayListOf<Song>()
        val recentLogs = mutableListOf<ConvertedSongLog>()

        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()
                val startIndex = if (columnMapping.hasHeader) 1 else 0
                val totalLines = lines.size - startIndex

                lines.drop(startIndex).forEachIndexed { index, line ->
                    val parts = parseCsvLine(line)

                    if (parts.isNotEmpty()) {
                        if (columnMapping.artistColumnIndex < parts.size && columnMapping.titleColumnIndex < parts.size) {
                            val title = parts[columnMapping.titleColumnIndex].trim()
                            val artistStr = parts[columnMapping.artistColumnIndex].trim()
                            val url = if (columnMapping.urlColumnIndex >= 0 && columnMapping.urlColumnIndex < parts.size) {
                                parts[columnMapping.urlColumnIndex].trim()
                            } else {
                                ""
                            }

                            if (title.isNotEmpty() && artistStr.isNotEmpty()) {
                                val artists = artistStr.split(";", ",").map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .map { ArtistEntity(id = "", name = it) }

                                val mockSong = Song(
                                    song = SongEntity(
                                        id = "",
                                        title = title,
                                    ),
                                    artists = artists,
                                )
                                songs.add(mockSong)

                                
                                val logEntry = ConvertedSongLog(
                                    title = title,
                                    artists = artists.joinToString(", ") { it.name },
                                )
                                recentLogs.add(0, logEntry)
                                if (recentLogs.size > 3) {
                                    recentLogs.removeAt(recentLogs.size - 1)
                                }
                                onLogUpdate(recentLogs.toList())
                            }
                        }
                    }

                    
                    val progress = ((index + 1) * 100) / totalLines
                    onProgress(progress.coerceIn(0, 99))
                }
            }
        }.onFailure {
            reportException(it)
            Timber.tag("CSV_IMPORT").e(it, "CSV import failed")
            Toast.makeText(
                context,
                "Failed to import CSV file",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (songs.isEmpty()) {
            Toast.makeText(
                context,
                "No songs found. Invalid file, or perhaps no song matches were found.",
                Toast.LENGTH_SHORT
            ).show()
        }
        return songs
    }

    fun importPlaylistFromCsv(context: Context, uri: Uri): ArrayList<Song> {
        
        return importPlaylistFromCsv(context, uri, CsvImportState())
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result.map { it.trim().trim('"') }
    }

    fun loadM3UOnline(
        context: Context,
        uri: Uri,
    ): ArrayList<Song> {
        val songs = ArrayList<Song>()

        runCatching {
            context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()
                if (lines.first().startsWith("#EXTM3U")) {
                    lines.forEachIndexed { _, rawLine ->
                        if (rawLine.startsWith("#EXTINF:")) {
                            
                            val artists =
                                rawLine.substringAfter("#EXTINF:").substringAfter(',').substringBefore(" - ").split(';')
                            val title = rawLine.substringAfter("#EXTINF:").substringAfter(',').substringAfter(" - ")

                            val mockSong = Song(
                                song = SongEntity(
                                    id = "",
                                    title = title,
                                ),
                                artists = artists.map { ArtistEntity("", it) },
                            )
                            songs.add(mockSong)

                        }
                    }
                }
            }
        }

        if (songs.isEmpty()) {
            Toast.makeText(
                context,
                "No songs found. Invalid file, or perhaps no song matches were found.",
                Toast.LENGTH_SHORT
            ).show()
        }
        return songs
    }

    companion object {
        const val SETTINGS_FILENAME = "settings.preferences_pb"
    }
}
