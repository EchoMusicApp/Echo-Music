package echo.music.iad1tya.export

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import androidx.core.content.getSystemService
import androidx.datastore.preferences.core.edit
import androidx.documentfile.provider.DocumentFile
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.music.innertube.YouTube
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.constants.AudioQuality
import echo.music.iad1tya.constants.EnableExportAsMp3Key
import echo.music.iad1tya.constants.ExportConflictStrategyKey
import echo.music.iad1tya.constants.ExportDirectoryUriKey
import echo.music.iad1tya.constants.ExportMetadataKey
import echo.music.iad1tya.constants.ExportProgressKey
import echo.music.iad1tya.constants.ExportedSongIdsKey
import echo.music.iad1tya.constants.ExportingSongIdsKey
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.playback.DownloadUtil
import echo.music.iad1tya.utils.InnerTubeXResolver
import echo.music.iad1tya.utils.PlayerClient
import echo.music.iad1tya.utils.YTPlayerUtils
import echo.music.iad1tya.utils.dataStore
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

@Singleton
class ExportEngine
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val database: MusicDatabase,
  private val downloadUtil: DownloadUtil,
) {
  private val LOG_TAG = "EchoExport"
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val httpClient = OkHttpClient()

  private val lastProgressWriteTime = ConcurrentHashMap<String, Long>()
  private val lastProgressWritePercent = ConcurrentHashMap<String, Int>()

  private val _batchState = MutableStateFlow(ExportBatchState())
  val batchState: StateFlow<ExportBatchState> = _batchState.asStateFlow()

  suspend fun getEligibleSongs(): List<ExportableSong> =
    withContext(Dispatchers.IO) {
      try {
        val downloadedSongs = database.downloadedSongsByNameAsc().first()
        val allLocalSongs =
          try {
            database.getSongsByIds(database.localSongIds())
          } catch (e: Exception) {
            emptyList()
          }

        val exportedIds = getExportedSongIds()
        val exportingIds = getExportingSongIds()

        val combinedSongs = (downloadedSongs + allLocalSongs).distinctBy { it.song.id }

        combinedSongs.map { song ->
          val isDownloaded =
            song.song.isDownloaded ||
              DownloadUtil.hasDownload(song.id, downloadUtil.downloads.value)
          val isLocal =
            song.song.isLocal || song.id.startsWith("content://") || song.id.startsWith("file://")

          val status =
            when {
              exportingIds.contains(song.id) -> ExportStatus.EXPORTING
              exportedIds.contains(song.id) -> ExportStatus.COMPLETED
              else -> ExportStatus.PENDING
            }

          val estimatedSize = calculateSongSize(song)

          ExportableSong(
            id = song.id,
            title = song.song.title,
            artist = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" },
            album = song.song.albumName ?: song.album?.title ?: "",
            durationSeconds = song.song.duration,
            sizeBytes = estimatedSize,
            thumbnailUrl = song.thumbnailUrl ?: song.album?.thumbnailUrl,
            isLocal = isLocal,
            isDownloaded = isDownloaded,
            status = status,
            stage =
              if (status == ExportStatus.COMPLETED) ExportStage.COMPLETED else ExportStage.QUEUED,
            song = song,
          )
        }
      } catch (e: Exception) {
        Timber.tag(LOG_TAG).e(e, "Error querying eligible export songs")
        emptyList()
      }
    }

  suspend fun exportBatch(
    songIds: List<String>,
    targetDirectoryUri: String? = null,
    conflictStrategyOverride: ExportConflictStrategy? = null,
    enableMp3Override: Boolean? = null,
    preserveMetadataOverride: Boolean? = null,
    onProgress: (suspend (ExportBatchState) -> Unit)? = null,
  ): ExportBatchState =
    withContext(Dispatchers.IO) {
      if (_batchState.value.isExporting) {
        Timber.tag(LOG_TAG)
          .w("exportBatch called while an export is already running. Ignoring duplicate request.")
        return@withContext _batchState.value
      }

      val prefs = context.dataStore.data.first()
      val destinationUri =
        targetDirectoryUri
          ?: prefs[ExportDirectoryUriKey]
          ?: error("No export directory configured")

      if (destinationUri.isBlank()) {
        error("Export directory URI is blank")
      }

      val conflictStrategy =
        conflictStrategyOverride
          ?: ExportConflictStrategy.fromString(prefs[ExportConflictStrategyKey])
      val enableMp3 = enableMp3Override ?: (prefs[EnableExportAsMp3Key] ?: false)
      val preserveMetadata = preserveMetadataOverride ?: (prefs[ExportMetadataKey] ?: true)

      val eligibleSongs = getEligibleSongs().associateBy { it.id }
      val targetSongs = songIds.mapNotNull { eligibleSongs[it] }

      if (targetSongs.isEmpty()) {
        val emptyState = ExportBatchState(isFinished = true, currentStage = ExportStage.COMPLETED)
        _batchState.value = emptyState
        return@withContext emptyState
      }

      _batchState.update {
        ExportBatchState(
          isExporting = true,
          totalCount = targetSongs.size,
          currentStage = ExportStage.QUEUED,
          isFinished = false,
          isCancelled = false,
        )
      }

      var completed = 0
      var failed = 0
      var skipped = 0
      var totalExportedBytes = 0L
      var batchMaxPercent = 0
      val failedIds = mutableListOf<String>()

      Timber.tag(LOG_TAG)
        .i(
          "[Export Batch] Started for %d songs to target directory: %s",
          targetSongs.size,
          destinationUri,
        )

      for ((index, song) in targetSongs.withIndex()) {
        currentCoroutineContext().ensureActive()
        if (_batchState.value.isCancelled) {
          Timber.tag(LOG_TAG).w("[Export Batch] Cancelled by user")
          break
        }

        val baseSongPercent = ((index * 100) / targetSongs.size).coerceIn(0, 99)
        batchMaxPercent = maxOf(batchMaxPercent, baseSongPercent)

        _batchState.update { state ->
          state.copy(
            currentSongId = song.id,
            currentSongTitle = song.title,
            currentStage = ExportStage.RESOLVING,
            progressPercent = batchMaxPercent,
          )
        }
        onProgress?.invoke(_batchState.value)

        val resultStatus =
          exportSingleSong(
            song = song,
            targetDirectoryUri = destinationUri,
            conflictStrategy = conflictStrategy,
            enableMp3 = enableMp3,
            preserveMetadata = preserveMetadata,
            onProgressUpdate = { stage, songPercent ->
              val songProgressWeight = 100f / targetSongs.size
              val currentSongContribution = (songPercent * songProgressWeight / 100f).toInt()
              val rawBatchPercent = ((index * 100) / targetSongs.size) + currentSongContribution
              batchMaxPercent = maxOf(batchMaxPercent, rawBatchPercent.coerceIn(0, 99))

              _batchState.update { state ->
                state.copy(
                  currentStage = stage,
                  progressPercent = batchMaxPercent,
                )
              }
              onProgress?.invoke(_batchState.value)
            },
          )

        when (resultStatus) {
          ExportStatus.COMPLETED -> {
            completed++
            totalExportedBytes += song.sizeBytes
          }
          ExportStatus.SKIPPED -> {
            skipped++
          }
          ExportStatus.FAILED -> {
            failed++
            failedIds.add(song.id)
          }
          else -> {}
        }

        _batchState.update { state ->
          state.copy(
            completedCount = completed,
            failedCount = failed,
            skippedCount = skipped,
            exportedSizeBytes = totalExportedBytes,
            failedSongIds = failedIds.toList(),
          )
        }
        onProgress?.invoke(_batchState.value)
      }

      val finalStage =
        when {
          _batchState.value.isCancelled -> ExportStage.CANCELLED
          failed > 0 && completed == 0 && skipped == 0 -> ExportStage.FAILED
          failed > 0 -> ExportStage.FAILED
          else -> ExportStage.COMPLETED
        }
      val finalState =
        _batchState.value.copy(
          isExporting = false,
          currentSongId = null,
          currentSongTitle = null,
          currentStage = finalStage,
          progressPercent =
            if (finalStage == ExportStage.COMPLETED) 100 else batchMaxPercent.coerceAtMost(99),
          isFinished = true,
        )
      _batchState.value = finalState
      onProgress?.invoke(finalState)
      Timber.tag(LOG_TAG)
        .i(
          "[Export Batch] Finished: completed=%d, failed=%d, skipped=%d, totalBytes=%d",
          completed,
          failed,
          skipped,
          totalExportedBytes,
        )
      return@withContext finalState
    }

  fun cancelExport() {
    Timber.tag(LOG_TAG).w("[Export Batch] Cancellation requested by user")
    _batchState.update { it.copy(isCancelled = true, currentStage = ExportStage.CANCELLED) }
  }

  suspend fun exportSingleSong(
    song: ExportableSong,
    targetDirectoryUri: String,
    conflictStrategy: ExportConflictStrategy,
    enableMp3: Boolean,
    preserveMetadata: Boolean,
    onProgressUpdate: (suspend (stage: ExportStage, percent: Int) -> Unit)? = null,
  ): ExportStatus {
    val exportId = UUID.randomUUID().toString().take(8)
    val songId = song.id
    val safeTitle = sanitizeTitle(song.title.ifBlank { songId })
    val tempFiles = mutableListOf<File>()
    var maxPercent = 0

    suspend fun reportProgress(stage: ExportStage, percent: Int) {
      val monotonicPercent = maxOf(maxPercent, percent.coerceIn(0, 100))
      maxPercent = monotonicPercent
      onProgressUpdate?.invoke(stage, monotonicPercent)
      updateExportProgress(songId, monotonicPercent)
    }

    Timber.tag(LOG_TAG)
      .i(
        "[Export %s | ID:%s] Pipeline started: title='%s', isLocal=%b, enableMp3=%b, targetUri='%s'",
        songId,
        exportId,
        song.title,
        song.isLocal,
        enableMp3,
        targetDirectoryUri,
      )

    try {
      addExportingSongId(songId)

      // Stage 1: RESOLVING (0% to 5%)
      reportProgress(ExportStage.RESOLVING, 2)

      val isLocal = song.isLocal || songId.startsWith("content://") || songId.startsWith("file://")

      var detectedMimeType: String
      var detectedExtension: String

      reportProgress(ExportStage.RESOLVING, 5)

      // Stage 2: DOWNLOADING (5% to 75%)
      reportProgress(ExportStage.DOWNLOADING, 5)

      val tempSourceFile =
        File.createTempFile("export_source_${exportId}_", ".tmp", context.cacheDir)
          .also(tempFiles::add)

      if (isLocal) {
        Timber.tag(LOG_TAG)
          .d("[Export %s | ID:%s] [DOWNLOADING] Reading local audio content...", songId, exportId)
        val readSuccess = copyLocalContentToTemp(songId, tempSourceFile)
        if (!readSuccess || tempSourceFile.length() == 0L) {
          error("Failed to read local song source: $songId")
        }
        val sourceFormat = detectAudioFormat(tempSourceFile, getMimeTypeFromUri(songId))
        detectedMimeType = sourceFormat.first
        detectedExtension = sourceFormat.second
        Timber.tag(LOG_TAG)
          .d(
            "[Export %s | ID:%s] [DOWNLOADING] Local content read complete, size=%d bytes, detectedExt=%s",
            songId,
            exportId,
            tempSourceFile.length(),
            detectedExtension,
          )
        reportProgress(ExportStage.DOWNLOADING, 75)
      } else {
        Timber.tag(LOG_TAG)
          .d(
            "[Export %s | ID:%s] [DOWNLOADING] Requesting YouTube playback data...",
            songId,
            exportId,
          )
        val connectivityManager =
          context.getSystemService<ConnectivityManager>() ?: error("No connectivity manager")

        val playbackData =
          YTPlayerUtils.playerResponseForPlayback(
              videoId = songId,
              audioQuality = AudioQuality.AUTO,
              connectivityManager = connectivityManager,
            )
            .getOrThrow()

        val rawMime = playbackData.format.mimeType.split(";")[0].trim()
        detectedMimeType = if (rawMime.isNotBlank()) rawMime else "audio/mp4"
        detectedExtension =
          ExportAudioFormats.fromMime(detectedMimeType)?.extension
            ?: error("Unsupported resolved audio MIME type: $detectedMimeType")
        Timber.tag(LOG_TAG)
          .d(
            "[Export %s | ID:%s] [RESOLVING] Stream format resolved: mimeType='%s', containerExt='%s'",
            songId,
            exportId,
            detectedMimeType,
            detectedExtension,
          )

        currentCoroutineContext().ensureActive()

        Timber.tag(LOG_TAG)
          .d("[Export %s | ID:%s] [DOWNLOADING] Streaming audio bytes...", songId, exportId)
        downloadStream(songId, playbackData, tempSourceFile) { rawPercent ->
          val mapped = (5 + (rawPercent * 0.70)).toInt().coerceIn(5, 75)
          reportProgress(ExportStage.DOWNLOADING, mapped)
        }
        val sourceFormat = detectAudioFormat(tempSourceFile, detectedMimeType)
        detectedMimeType = sourceFormat.first
        detectedExtension = sourceFormat.second
        Timber.tag(LOG_TAG)
          .d(
            "[Export %s | ID:%s] [DOWNLOADING] Stream download complete, size=%d bytes",
            songId,
            exportId,
            tempSourceFile.length(),
          )
      }

      currentCoroutineContext().ensureActive()

      // Stage 3: VERIFYING_SOURCE (75% to 77%)
      reportProgress(ExportStage.VERIFYING_SOURCE, 75)
      check(tempSourceFile.exists() && tempSourceFile.length() > 0L) {
        "Downloaded temporary source file is missing or empty"
      }
      check(hasAudioContainerSignature(tempSourceFile)) {
        "Downloaded source is not a recognized audio container"
      }
      check(hasDecodableAudio(tempSourceFile)) {
        "Downloaded source does not contain decodable audio"
      }
      Timber.tag(LOG_TAG)
        .d(
          "[Export %s | ID:%s] [VERIFYING_SOURCE] Source verified: size=%d bytes",
          songId,
          exportId,
          tempSourceFile.length(),
        )
      reportProgress(ExportStage.VERIFYING_SOURCE, 77)

      // Stage 4: CONVERTING & METADATA (77% to 90%)
      reportProgress(ExportStage.CONVERTING, 77)

      val tempMp3File =
        File.createTempFile("export_res_${exportId}_", ".mp3", context.cacheDir)
          .also(tempFiles::add)
      val tempArtworkFile =
        File.createTempFile("export_cover_${exportId}_", ".jpg", context.cacheDir)
          .also(tempFiles::add)

      Timber.tag(LOG_TAG)
        .d(
          "[Export %s | ID:%s] [CONVERTING] Fetching artwork and year metadata...",
          songId,
          exportId,
        )
      val artworkDownloaded =
        if (preserveMetadata) downloadArtwork(song.thumbnailUrl.orEmpty(), tempArtworkFile)
        else false

      val year =
        if (!isLocal && preserveMetadata) fetchSongYear(songId)
        else if (preserveMetadata) song.song.song.year else null
      Timber.tag(LOG_TAG)
        .d(
          "[Export %s | ID:%s] [CONVERTING] Metadata ready: artworkDownloaded=%b, year=%s",
          songId,
          exportId,
          artworkDownloaded,
          year,
        )

      if (enableMp3) {
        Timber.tag(LOG_TAG)
          .d("[Export %s | ID:%s] [CONVERTING] Starting FFmpeg MP3 encoding...", songId, exportId)
        convertToMp3(
          songId = songId,
          sourceFile = tempSourceFile,
          outputFile = tempMp3File,
          songTitle = if (preserveMetadata) song.title else "",
          songArtist = if (preserveMetadata) song.artist else "",
          songAlbum = if (preserveMetadata) song.album else "",
          year = year,
          artworkFile = if (artworkDownloaded) tempArtworkFile else null,
          durationSeconds = song.durationSeconds,
          onFfmpegProgress = { ffmpegPercent ->
            val mapped = (77 + (ffmpegPercent * 0.13)).toInt().coerceIn(77, 90)
            reportProgress(ExportStage.CONVERTING, mapped)
          },
        )
        Timber.tag(LOG_TAG)
          .d(
            "[Export %s | ID:%s] [CONVERTING] FFmpeg encoding complete. mp3Size=%d bytes",
            songId,
            exportId,
            tempMp3File.length(),
          )
      }

      reportProgress(ExportStage.CONVERTING, 90)
      currentCoroutineContext().ensureActive()

      // Stage 5: VERIFYING_OUTPUT (90% to 92%)
      reportProgress(ExportStage.VERIFYING_OUTPUT, 90)
      val outputFile = if (enableMp3) tempMp3File else tempSourceFile
      check(outputFile.exists() && outputFile.length() > 0L) {
        "Temporary output file is missing or zero bytes after conversion/copy"
      }
      if (enableMp3) {
        check(isValidMp3File(outputFile)) {
          "Generated MP3 file failed header validation"
        }
      }
      Timber.tag(LOG_TAG)
        .d(
          "[Export %s | ID:%s] [VERIFYING_OUTPUT] Output file verified: size=%d bytes",
          songId,
          exportId,
          tempMp3File.length(),
        )
      reportProgress(ExportStage.VERIFYING_OUTPUT, 92)

      // Stage 6: SAVING OUTPUT VIA SAF (92% to 98%)
      reportProgress(ExportStage.SAVING, 92)
      Timber.tag(LOG_TAG)
        .d(
          "[Export %s | ID:%s] [SAVING] Writing output file to targetUri=%s...",
          songId,
          exportId,
          targetDirectoryUri,
        )

      val finalExtension = if (enableMp3) "mp3" else detectedExtension
      val finalMimeType = if (enableMp3) "audio/mpeg" else detectedMimeType

      val status =
        writeOutputFile(
          safeTitle = safeTitle,
          extension = finalExtension,
          mimeType = finalMimeType,
          targetDirectoryUri = targetDirectoryUri,
          sourceFile = outputFile,
          conflictStrategy = conflictStrategy,
          onCopyProgress = { copyPercent ->
            val mapped = (92 + (copyPercent * 0.06)).toInt().coerceIn(92, 98)
            reportProgress(ExportStage.SAVING, mapped)
          },
          onDestinationVerified = { reportProgress(ExportStage.VERIFYING_DESTINATION, 98) },
        )

      if (status == ExportStatus.SKIPPED) {
        Timber.tag(LOG_TAG)
          .i(
            "[Export %s | ID:%s] Export skipped because file already exists (conflictStrategy=SKIP)",
            songId,
            exportId,
          )
        return ExportStatus.SKIPPED
      }

      // Stage 7: FINALIZING (98% to 99%)
      reportProgress(ExportStage.FINALIZING, 98)
      Timber.tag(LOG_TAG)
        .d(
          "[Export %s | ID:%s] [FINALIZING] SAF stream closed & file verified...",
          songId,
          exportId,
        )
      currentCoroutineContext().ensureActive()

      // Stage 8: COMPLETED (100%)
      reportProgress(ExportStage.COMPLETED, 100)
      addExportedSongId(songId)

      Timber.tag(LOG_TAG).i("[Export %s | ID:%s] [COMPLETED] Export succeeded!", songId, exportId)
      return ExportStatus.COMPLETED
    } catch (e: Exception) {
      if (e is CancellationException) {
        Timber.tag(LOG_TAG).w("[Export %s | ID:%s] Pipeline cancelled", songId, exportId)
        reportProgress(ExportStage.CANCELLED, maxPercent)
        throw e
      }
      Timber.tag(LOG_TAG)
        .e(e, "[Export %s | ID:%s] Pipeline failed: %s", songId, exportId, e.message)
      reportProgress(ExportStage.FAILED, maxPercent)
      return ExportStatus.FAILED
    } finally {
      tempFiles.forEach { file -> runCatching { file.delete() } }
      withContext(NonCancellable) {
        clearExportProgress(songId)
        removeExportingSongId(songId)
      }
    }
  }

  private fun copyLocalContentToTemp(songId: String, destFile: File): Boolean {
    return runCatching {
        val uri = Uri.parse(songId)
        val input =
          if (uri.scheme == "content" || uri.scheme == "file") {
            context.contentResolver.openInputStream(uri)
          } else {
            File(songId).inputStream()
          } ?: return false

        input.use { inStream ->
          destFile.outputStream().use { outStream ->
            inStream.copyTo(outStream)
            outStream.flush()
          }
        }
        destFile.length() > 0L
      }
      .getOrDefault(false)
  }

  private suspend fun fetchSongYear(songId: String): Int? = runCatching {
    withTimeoutOrNull(3000) {
      YouTube.getMediaInfo(songId).getOrNull()?.uploadDate?.let {
        Regex("(19|20)\\d{2}").find(it)?.value?.toIntOrNull()
      }
    }
  }
    .getOrNull()

  @OptIn(InternalCoroutinesApi::class)
  private suspend fun downloadStream(
    songId: String,
    playbackData: YTPlayerUtils.PlaybackData,
    destFile: File,
    onProgress: suspend (Int) -> Unit = {},
  ) {
    val totalLength = playbackData.format.contentLength?.takeIf { it > 0L } ?: -1L

    val buildRequest = { data: YTPlayerUtils.PlaybackData, useRangeHeader: Boolean ->
      val builder = Request.Builder().url(data.streamUrl)
      val allHeaders = mutableMapOf<String, String>()
      data.headers?.let { allHeaders.putAll(it) }
      InnerTubeXResolver.headersFor(data.streamUrl)?.let { allHeaders.putAll(it) }
      PlayerClient.forStreamUrl(data.streamUrl).mediaHeaders().let { allHeaders.putAll(it) }
      allHeaders.forEach { (name, value) -> builder.header(name, value) }

      if (useRangeHeader && totalLength > 0L) {
        builder.header("Range", "bytes=0-${totalLength - 1}")
      }
      builder.build()
    }

    var request = buildRequest(playbackData, true)
    var call = httpClient.newCall(request)
    var response = runCatching { call.execute() }.getOrNull()

    // Fallback 1: Try without Range header if Range header triggered 403/401
    if (
      response != null && !response.isSuccessful && (response.code == 403 || response.code == 401)
    ) {
      response.close()
      request = buildRequest(playbackData, false)
      call = httpClient.newCall(request)
      response = runCatching { call.execute() }.getOrNull()
    }

    // Fallback 2: Re-fetch fresh playback response if still 403/401/network failure
    if (response == null || !response.isSuccessful) {
      val responseCode = response?.code ?: -1
      response?.close()
      if (responseCode == 403 || responseCode == 401 || responseCode == -1) {
        Timber.tag(LOG_TAG)
          .w(
            "[Export %s] Stream download returned code %d. Fetching fresh playback URL...",
            songId,
            responseCode,
          )
        val connectivityManager =
          context.getSystemService<ConnectivityManager>() ?: error("No connectivity manager")
        val freshPlaybackData =
          YTPlayerUtils.playerResponseForPlayback(
              videoId = songId,
              audioQuality = AudioQuality.AUTO,
              connectivityManager = connectivityManager,
            )
            .getOrNull()

        if (freshPlaybackData != null && freshPlaybackData.streamUrl.isNotBlank()) {
          request = buildRequest(freshPlaybackData, true)
          call = httpClient.newCall(request)
          response = runCatching { call.execute() }.getOrNull()
          if (
            response != null &&
              !response.isSuccessful &&
              (response.code == 403 || response.code == 401)
          ) {
            response.close()
            request = buildRequest(freshPlaybackData, false)
            call = httpClient.newCall(request)
            response = runCatching { call.execute() }.getOrNull()
          }
        }
      }
    }

    val finalResponse = response ?: error("Stream request failed with network error")
    val downloadJob = currentCoroutineContext()[Job]
    val cancellationHandle =
      downloadJob?.invokeOnCompletion(onCancelling = true) { cause ->
        if (cause is CancellationException) call.cancel()
      }

    try {
      finalResponse.use { resp ->
        if (!resp.isSuccessful) error("Stream request failed with code ${resp.code}")
        val body = resp.body
        val totalBytes = body.contentLength().takeIf { it > 0 } ?: totalLength
        var bytesWritten = 0L
        var lastReportedPercent = -1

        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        body.byteStream().use { input ->
          destFile.outputStream().use { output ->
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
              currentCoroutineContext().ensureActive()
              output.write(buffer, 0, read)
              bytesWritten += read
              val percent =
                if (totalBytes > 0L) {
                  ((bytesWritten * 100) / totalBytes).toInt().coerceIn(0, 100)
                } else {
                  -1
                }
              if (percent >= 0 && percent >= lastReportedPercent + 2) {
                lastReportedPercent = percent
                onProgress(percent)
              }
            }
            output.flush()
          }
        }
        if (totalBytes > 0 && bytesWritten < totalBytes) {
          error("Incomplete export source: wrote $bytesWritten of $totalBytes bytes")
        }
      }
    } finally {
      cancellationHandle?.dispose()
    }
  }

  @OptIn(InternalCoroutinesApi::class)
  private suspend fun downloadArtwork(artworkUrl: String, destFile: File): Boolean {
    if (artworkUrl.isBlank()) return false
    return runCatching {
        withTimeoutOrNull(5000) {
          val call = httpClient.newCall(Request.Builder().url(artworkUrl).build())
          val cancellationHandle =
            currentCoroutineContext()[Job]?.invokeOnCompletion(onCancelling = true) { cause ->
              if (cause is CancellationException) call.cancel()
            }
          try {
            call.execute().use { response ->
              if (!response.isSuccessful) return@use false
              response.body.byteStream().use { input ->
                destFile.outputStream().use { output ->
                  input.copyTo(output)
                  output.flush()
                }
              }
              destFile.length() > 2048L
            }
          } finally {
            cancellationHandle?.dispose()
          }
        } ?: false
      }
      .getOrDefault(false)
  }

  private suspend fun convertToMp3(
    songId: String,
    sourceFile: File,
    outputFile: File,
    songTitle: String,
    songArtist: String,
    songAlbum: String,
    year: Int?,
    artworkFile: File?,
    durationSeconds: Int,
    onFfmpegProgress: (suspend (percent: Int) -> Unit)? = null,
  ) {
    val durationMs = durationSeconds * 1000L
    val hasValidArtwork =
      artworkFile != null && artworkFile.exists() && artworkFile.length() > 2048L
    val coverToUse = if (hasValidArtwork) artworkFile else null

    Timber.tag(LOG_TAG)
      .d(
        "[Export %s] [FFmpeg] Command setup. Source: %s, Output: %s, Artwork: %s",
        songId,
        sourceFile.absolutePath,
        outputFile.absolutePath,
        coverToUse?.absolutePath ?: "None",
      )

    val successWithArtwork =
      executeFfmpegSession(
        songId,
        sourceFile,
        outputFile,
        songTitle,
        songArtist,
        songAlbum,
        year,
        coverToUse,
        durationMs,
        onFfmpegProgress,
      )

    if (successWithArtwork && isValidMp3File(outputFile)) {
      Timber.tag(LOG_TAG)
        .i(
          "[Export %s] [FFmpeg] Conversion with artwork succeeded. Size: %d bytes",
          songId,
          outputFile.length(),
        )
      return
    }

    if (coverToUse != null) {
      Timber.tag(LOG_TAG)
        .w(
          "[Export %s] [FFmpeg] Conversion with artwork failed or timed out. Retrying without artwork...",
          songId,
        )
      val tempFallbackFile =
        File.createTempFile("export_result_fallback_", ".mp3", context.cacheDir)
      try {
        val successWithoutArtwork =
          executeFfmpegSession(
            songId = songId,
            sourceFile = sourceFile,
            outputFile = tempFallbackFile,
            songTitle = songTitle,
            songArtist = songArtist,
            songAlbum = songAlbum,
            year = year,
            artworkFile = null,
            durationMs = durationMs,
            onFfmpegProgress = onFfmpegProgress,
          )

        if (successWithoutArtwork && isValidMp3File(tempFallbackFile)) {
          Timber.tag(LOG_TAG)
            .i(
              "[Export %s] [FFmpeg] Fallback conversion without artwork succeeded. Size: %d bytes",
              songId,
              tempFallbackFile.length(),
            )
          tempFallbackFile.copyTo(outputFile, overwrite = true)
          return
        }
      } finally {
        tempFallbackFile.delete()
      }
    }

    error("FFmpeg MP3 conversion failed for songId=$songId. Output file could not be generated.")
  }

  private suspend fun executeFfmpegSession(
    songId: String,
    sourceFile: File,
    outputFile: File,
    songTitle: String,
    songArtist: String,
    songAlbum: String,
    year: Int?,
    artworkFile: File?,
    durationMs: Long,
    onFfmpegProgress: (suspend (percent: Int) -> Unit)? = null,
  ): Boolean {
    val terminalResult = CompletableDeferred<Boolean>()
    val callbackScope = CoroutineScope(currentCoroutineContext())
    val progressJobs = ConcurrentHashMap.newKeySet<Job>()
    val args =
      buildFfmpegArguments(
        inputPath = sourceFile.absolutePath,
        outputPath = outputFile.absolutePath,
        title = songTitle,
        artist = songArtist,
        album = songAlbum,
        year = year,
        coverPath = artworkFile?.absolutePath,
      )

    val startTime = System.currentTimeMillis()
    var lastPercent = -1

    val session =
      FFmpegKit.executeWithArgumentsAsync(
        args,
        { completedSession ->
          val duration = System.currentTimeMillis() - startTime
          val returnCode = completedSession.returnCode
          val isSuccess =
            returnCode != null &&
              ReturnCode.isSuccess(returnCode) &&
              outputFile.exists() &&
              outputFile.length() > 0L

          Timber.tag(LOG_TAG)
            .d(
              "[Export %s] [FFmpeg Session %d] Finished in %d ms. ReturnCode: %s, Success: %b, OutputSize: %d bytes",
              songId,
              completedSession.sessionId,
              duration,
              returnCode,
              isSuccess,
              outputFile.length(),
            )

          if (!isSuccess && completedSession.output != null) {
            Timber.tag(LOG_TAG)
              .e(
                "[Export %s] [FFmpeg Session %d] Output log: %s",
                songId,
                completedSession.sessionId,
                completedSession.output,
              )
          }

          terminalResult.complete(isSuccess)
        },
        { log ->
          if (log != null && log.message.isNotBlank()) {
            Timber.tag(LOG_TAG).v("[Export %s] [FFmpeg Log] %s", songId, log.message)
          }
        },
        { statistics ->
          if (statistics != null) {
            val timeMs = statistics.time
            val percent =
              if (durationMs > 0) {
                ((timeMs * 100) / durationMs).toInt().coerceIn(0, 99)
              } else {
                0
              }

            if (percent >= lastPercent + 2) {
              lastPercent = percent
              onFfmpegProgress?.let { callback ->
                val job = callbackScope.launch { callback(percent) }
                progressJobs += job
                job.invokeOnCompletion { progressJobs -= job }
              }
            }
          }
        },
      )

    Timber.tag(LOG_TAG)
      .d("[Export %s] [FFmpeg Session %d] Started asynchronously", songId, session.sessionId)

    return try {
      val success = withTimeout(120_000L) { terminalResult.await() }
      progressJobs.toList().joinAll()
      success
    } catch (e: TimeoutCancellationException) {
      Timber.tag(LOG_TAG)
        .w(
          "[Export %s] [FFmpeg Session %d] Timed out; waiting for native termination",
          songId,
          session.sessionId,
        )
      FFmpegKit.cancel(session.sessionId)
      withContext(NonCancellable) { terminalResult.await() }
      withContext(NonCancellable) { progressJobs.toList().joinAll() }
      false
    } catch (e: CancellationException) {
      Timber.tag(LOG_TAG)
        .w(
          "[Export %s] [FFmpeg Session %d] Cancelled; waiting for native termination",
          songId,
          session.sessionId,
        )
      FFmpegKit.cancel(session.sessionId)
      withContext(NonCancellable) { terminalResult.await() }
      withContext(NonCancellable) { progressJobs.toList().joinAll() }
      throw e
    }
  }

  private suspend fun isValidMp3File(file: File): Boolean {
    if (!file.exists() || file.length() < 100L) return false
    val hasMp3Header = runCatching {
      file.inputStream().use { input ->
        val header = ByteArray(3)
        val read = input.read(header)
        if (read < 3) return false
        val isId3 =
          header[0] == 'I'.code.toByte() &&
            header[1] == 'D'.code.toByte() &&
            header[2] == '3'.code.toByte()
        val isSync = (header[0].toInt() and 0xFF) == 0xFF && ((header[1].toInt() and 0xE0) == 0xE0)
        isId3 || isSync
      }
    }
      .getOrDefault(false)
    if (!hasMp3Header) return false

    return hasDecodableAudio(file)
  }

  private suspend fun hasDecodableAudio(file: File): Boolean {
    if (!file.exists() || file.length() < 100L) return false
    val header = readHeader(file)
    if (ExportAudioFormats.isTextOrErrorPayload(header)) return false

    val terminalResult = CompletableDeferred<Boolean>()
    val session =
      FFmpegKit.executeWithArgumentsAsync(
        arrayOf(
          "-v",
          "error",
          "-i",
          file.absolutePath,
          "-map",
          "0:a:0?",
          "-f",
          "null",
          "-",
        ),
        { completed ->
          terminalResult.complete(completed.returnCode?.let(ReturnCode::isSuccess) == true)
        },
      )
    return try {
      withTimeout(30_000L) { terminalResult.await() }
    } catch (e: TimeoutCancellationException) {
      FFmpegKit.cancel(session.sessionId)
      withContext(NonCancellable) { terminalResult.await() }
      false
    } catch (e: CancellationException) {
      FFmpegKit.cancel(session.sessionId)
      withContext(NonCancellable) { terminalResult.await() }
      throw e
    }
  }

  private suspend fun writeOutputFile(
    safeTitle: String,
    extension: String,
    mimeType: String,
    targetDirectoryUri: String,
    sourceFile: File,
    conflictStrategy: ExportConflictStrategy,
    onCopyProgress: (suspend (percent: Int) -> Unit)? = null,
    onDestinationVerified: (suspend () -> Unit)? = null,
  ): ExportStatus {
    val uri = Uri.parse(targetDirectoryUri)
    val sourceSizeBytes = sourceFile.length()
    check(sourceFile.exists() && sourceSizeBytes > 0L) {
      "Source audio file is missing or zero bytes before writing"
    }

    val sourceSha256 = computeSha256(sourceFile) ?: error("Unable to checksum export source")

    if (uri.scheme == "file" || uri.scheme == null) {
      val folder = File(uri.path ?: error("Invalid export directory"))
      if (!folder.exists() && !folder.mkdirs()) error("Unable to create export directory")

      var targetFileName = "$safeTitle.$extension"
      var targetFile = File(folder, targetFileName)
      val replacingExisting =
        targetFile.exists() && conflictStrategy == ExportConflictStrategy.REPLACE
      val existingBackup =
        if (replacingExisting)
          File.createTempFile("export_backup_", ".$extension", folder).also { it.delete() }
        else null

      if (targetFile.exists()) {
        when (conflictStrategy) {
          ExportConflictStrategy.SKIP -> return ExportStatus.SKIPPED
          ExportConflictStrategy.REPLACE -> {
            // Retain the current file until the replacement has been verified.
          }
          ExportConflictStrategy.RENAME -> {
            var counter = 1
            while (targetFile.exists() && counter <= 500) {
              targetFileName = "$safeTitle ($counter).$extension"
              targetFile = File(folder, targetFileName)
              counter++
            }
          }
        }
      }

      val stagedFile = File.createTempFile("export_staged_", ".$extension", folder)
      try {
        stagedFile.outputStream().use { copyExport(sourceFile, it, onCopyProgress) }
        currentCoroutineContext().ensureActive()

        val isStagedValid =
          verifyDestinationStream(
            inputStreamProvider = { stagedFile.inputStream() },
            expectedSizeBytes = sourceSizeBytes,
            expectedSha256 = sourceSha256,
          )
        check(isStagedValid) { "Staged file verification failed for $targetFileName" }

        if (replacingExisting)
          check(targetFile.renameTo(existingBackup!!)) {
            "Unable to preserve existing export before replacement"
          }
        try {
          Files.move(
            stagedFile.toPath(),
            targetFile.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
          )
        } catch (_: Exception) {
          Files.move(stagedFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        check(targetFile.exists() && targetFile.length() == sourceSizeBytes) {
          "Exported target file size mismatch: expected $sourceSizeBytes, got ${targetFile.length()}"
        }
        check(
          verifyDestinationStream({ targetFile.inputStream() }, sourceSizeBytes, sourceSha256)
        ) {
          "Final destination checksum mismatch for $targetFileName"
        }
        onDestinationVerified?.invoke()
        Timber.tag(LOG_TAG)
          .i(
            "[FILE] Export verified and saved: filename=%s, size=%d bytes",
            targetFileName,
            targetFile.length(),
          )
        return ExportStatus.COMPLETED
      } catch (e: Exception) {
        stagedFile.delete()
        if (existingBackup?.exists() == true) {
          targetFile.delete()
          existingBackup.renameTo(targetFile)
        }
        throw e
      } finally {
        if (targetFile.exists()) existingBackup?.delete()
      }
    } else {
      check(
        context.contentResolver.persistedUriPermissions.any {
          it.uri == uri && it.isWritePermission
        }
      ) {
        "Export folder access was revoked. Select the folder again in Settings."
      }
      val destinationDir =
        DocumentFile.fromTreeUri(context, uri) ?: error("Export directory unavailable")
      check(destinationDir.exists() && destinationDir.isDirectory && destinationDir.canWrite()) {
        "Export folder is unavailable or not writable"
      }

      var targetFileName = "$safeTitle.$extension"
      val existingFile = destinationDir.findFile(targetFileName)

      if (existingFile != null) {
        when (conflictStrategy) {
          ExportConflictStrategy.SKIP -> return ExportStatus.SKIPPED
          ExportConflictStrategy.REPLACE -> {
            // Retain the current document until the replacement has been verified.
          }
          ExportConflictStrategy.RENAME -> {
            var counter = 1
            while (destinationDir.findFile(targetFileName) != null && counter <= 500) {
              targetFileName = "$safeTitle ($counter).$extension"
              counter++
            }
          }
        }
      }

      val stagedFileName = ".$safeTitle.${UUID.randomUUID()}.part"
      val stagedDocument =
        destinationDir.createFile(mimeType, stagedFileName)
          ?: error("Unable to create staged output file: $stagedFileName")
      var finalizedDocument: DocumentFile? = null
      var existingBackupName: String? = null

      try {
        val output =
          context.contentResolver.openOutputStream(stagedDocument.uri, "w")
            ?: error("Export staged file is not writable")
        output.use { copyExport(sourceFile, it, onCopyProgress) }
        currentCoroutineContext().ensureActive()

        val isVerified =
          verifyDestinationStream(
            inputStreamProvider = { context.contentResolver.openInputStream(stagedDocument.uri) },
            expectedSizeBytes = sourceSizeBytes,
            expectedSha256 = sourceSha256,
          )

        check(isVerified) {
          "SAF destination bit-exact byte/checksum verification failed for $targetFileName"
        }

        val existingSafTarget = destinationDir.findFile(targetFileName)
        if (existingSafTarget != null && conflictStrategy == ExportConflictStrategy.REPLACE) {
          val backupName = ".$safeTitle.${UUID.randomUUID()}.backup"
          existingBackupName = backupName
          check(existingSafTarget.renameTo(backupName)) {
            "Unable to preserve existing export before replacing $targetFileName"
          }
        }
        val renamed = runCatching {
          stagedDocument.renameTo(targetFileName)
        }
          .getOrDefault(false)

        val finalDocument =
          if (renamed) {
            stagedDocument
          } else {
            error(
              "SAF provider cannot finalize staged export $targetFileName because it does not support renaming"
            )
          }
        finalizedDocument = finalDocument

        check(
          verifyDestinationStream(
            inputStreamProvider = { context.contentResolver.openInputStream(finalDocument.uri) },
            expectedSizeBytes = sourceSizeBytes,
            expectedSha256 = sourceSha256,
          )
        ) {
          "Final SAF destination verification failed for $targetFileName"
        }
        onDestinationVerified?.invoke()
        existingBackupName?.let { backupName ->
          check(destinationDir.findFile(backupName)?.delete() == true) {
            "Export completed but the previous backup could not be removed"
          }
        }
        Timber.tag(LOG_TAG)
          .i(
            "[SAF] Successfully verified and saved SAF destination file: filename=%s, uri=%s, size=%d bytes",
            targetFileName,
            finalDocument.uri,
            sourceSizeBytes,
          )
        return ExportStatus.COMPLETED
      } catch (e: Exception) {
        if (finalizedDocument == null) stagedDocument.delete() else finalizedDocument.delete()
        existingBackupName?.let { backupName ->
          destinationDir.findFile(backupName)?.renameTo(targetFileName)
        }
        throw e
      }
    }
  }

  private fun verifyDestinationStream(
    inputStreamProvider: () -> java.io.InputStream?,
    expectedSizeBytes: Long,
    expectedSha256: String?,
  ): Boolean {
    return runCatching {
        var readBytes = 0L
        val digest = MessageDigest.getInstance("SHA-256")

        inputStreamProvider()?.use { input ->
          val buffer = ByteArray(8192)
          var read: Int
          while (input.read(buffer).also { read = it } != -1) {
            readBytes += read
            digest.update(buffer, 0, read)
          }
        } ?: return false

        if (readBytes != expectedSizeBytes || readBytes <= 0L) {
          Timber.tag(LOG_TAG)
            .e(
              "[SAF Verification Failed] Byte count mismatch: expected=%d, read=%d",
              expectedSizeBytes,
              readBytes,
            )
          return false
        }

        val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
          Timber.tag(LOG_TAG)
            .e(
              "[SAF Verification Failed] Checksum mismatch: expected=%s, actual=%s",
              expectedSha256,
              actualSha256,
            )
          return false
        }

        true
      }
      .getOrDefault(false)
  }

  private fun computeSha256(file: File): String? {
    return runCatching {
      val digest = MessageDigest.getInstance("SHA-256")
      file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
          digest.update(buffer, 0, read)
        }
      }
      digest.digest().joinToString("") { "%02x".format(it) }
    }
      .getOrNull()
  }

  private suspend fun copyExport(
    sourceFile: File,
    output: OutputStream,
    onProgress: (suspend (percent: Int) -> Unit)? = null,
  ) {
    val totalBytes = sourceFile.length().coerceAtLeast(1L)
    var bytesWritten = 0L
    var lastReportedPercent = -1

    sourceFile.inputStream().use { input ->
      val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
      while (true) {
        currentCoroutineContext().ensureActive()
        val read = input.read(buffer)
        if (read == -1) break
        output.write(buffer, 0, read)
        bytesWritten += read
        val percent = ((bytesWritten * 100) / totalBytes).toInt().coerceIn(0, 100)
        if (percent >= lastReportedPercent + 5) {
          lastReportedPercent = percent
          onProgress?.invoke(percent)
        }
      }
      output.flush()
    }
  }

  private fun calculateSongSize(song: Song): Long {
    val formatLength = song.format?.contentLength
    if (formatLength != null && formatLength > 0L) {
      return formatLength
    }
    if (song.song.isLocal || song.id.startsWith("content://") || song.id.startsWith("file://")) {
      runCatching {
        val uri = Uri.parse(song.id)
        if (uri.scheme == "content") {
          context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            return pfd.statSize
          }
        } else if (uri.scheme == "file" || uri.path != null) {
          val file = File(uri.path ?: song.id)
          if (file.exists()) return file.length()
        }
      }
    }
    val durationSec = song.song.duration.coerceAtLeast(1)
    return durationSec * 40_000L
  }

  private fun sanitizeTitle(title: String): String =
    title.replace(Regex("[\\\\/:*?\"<>|]"), "_").replace(Regex("\\s+"), " ").trim().ifBlank {
      "song_${System.currentTimeMillis()}"
    }

  private fun detectAudioFormat(file: File, declaredMime: String): Pair<String, String> =
    ExportAudioFormats.fromHeader(readHeader(file), declaredMime)?.let {
      it.mimeType to it.extension
    } ?: error("Unrecognized audio container")

  private fun hasAudioContainerSignature(file: File): Boolean = runCatching {
    ExportAudioFormats.hasContainerSignature(readHeader(file))
  }
    .getOrDefault(false)

  private fun readHeader(file: File): ByteArray =
    file.inputStream().use { input ->
      ByteArray(16).also { buffer ->
        var offset = 0
        while (offset < buffer.size) {
          val count = input.read(buffer, offset, buffer.size - offset)
          if (count < 0) break
          offset += count
        }
      }
    }

  private fun getMimeTypeFromUri(uriString: String): String {
    return runCatching {
      if (uriString.startsWith("content://")) {
        context.contentResolver.getType(Uri.parse(uriString))
      } else {
        null
      }
    }
      .getOrNull() ?: "audio/mpeg"
  }

  private suspend fun getExportedSongIds(): Set<String> {
    val prefs = context.dataStore.data.first()
    return prefs[ExportedSongIdsKey]
      .orEmpty()
      .split(',')
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .toSet()
  }

  private suspend fun getExportingSongIds(): Set<String> {
    val prefs = context.dataStore.data.first()
    return prefs[ExportingSongIdsKey]
      .orEmpty()
      .split(',')
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .toSet()
  }

  private suspend fun addExportedSongId(songId: String) {
    context.dataStore.edit { preferences ->
      val current =
        preferences[ExportedSongIdsKey]
          .orEmpty()
          .split(',')
          .map { it.trim() }
          .filter { it.isNotBlank() }
      val updated = listOf(songId) + current.filterNot { it == songId }
      preferences[ExportedSongIdsKey] = updated.take(1000).joinToString(",")
    }
  }

  private suspend fun addExportingSongId(songId: String) {
    context.dataStore.edit { preferences ->
      val current =
        preferences[ExportingSongIdsKey]
          .orEmpty()
          .split(',')
          .map { it.trim() }
          .filter { it.isNotBlank() }
      val updated = listOf(songId) + current.filterNot { it == songId }
      preferences[ExportingSongIdsKey] = updated.take(1000).joinToString(",")
    }
  }

  private suspend fun removeExportingSongId(songId: String) {
    context.dataStore.edit { preferences ->
      val current =
        preferences[ExportingSongIdsKey]
          .orEmpty()
          .split(',')
          .map { it.trim() }
          .filter { it.isNotBlank() }
      preferences[ExportingSongIdsKey] = current.filterNot { it == songId }.joinToString(",")
    }
  }

  private suspend fun updateExportProgress(songId: String, percent: Int) {
    val now = System.currentTimeMillis()
    val lastTime = lastProgressWriteTime[songId] ?: 0L
    val lastPercent = lastProgressWritePercent[songId] ?: -1

    if (percent < 100 && (now - lastTime) < 1000L && kotlin.math.abs(percent - lastPercent) < 3) {
      return
    }

    lastProgressWriteTime[songId] = now
    lastProgressWritePercent[songId] = percent

    context.dataStore.edit { preferences ->
      val current =
        preferences[ExportProgressKey]
          .orEmpty()
          .split(',')
          .filter { it.isNotBlank() }
          .associate {
            val parts = it.split(':')
            parts[0] to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
          }
          .toMutableMap()
      current[songId] = percent
      preferences[ExportProgressKey] = current.map { "${it.key}:${it.value}" }.joinToString(",")
    }
  }

  private suspend fun clearExportProgress(songId: String) {
    lastProgressWriteTime.remove(songId)
    lastProgressWritePercent.remove(songId)
    context.dataStore.edit { preferences ->
      val current =
        preferences[ExportProgressKey]
          .orEmpty()
          .split(',')
          .filter { it.isNotBlank() }
          .associate {
            val parts = it.split(':')
            parts[0] to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
          }
          .toMutableMap()
      current.remove(songId)
      preferences[ExportProgressKey] = current.map { "${it.key}:${it.value}" }.joinToString(",")
    }
  }

  companion object {
    private fun buildFfmpegArguments(
      inputPath: String,
      outputPath: String,
      title: String,
      artist: String,
      album: String,
      year: Int?,
      coverPath: String?,
    ): Array<String> {
      val args = mutableListOf("-y", "-i", inputPath)
      if (coverPath != null) {
        args.addAll(
          listOf(
            "-i",
            coverPath,
            "-map",
            "0:a:0?",
            "-map",
            "1:v:0?",
            "-c:a",
            "libmp3lame",
            "-b:a",
            "320k",
            "-c:v",
            "copy",
            "-disposition:v",
            "attached_pic",
            "-id3v2_version",
            "3",
          )
        )
      } else {
        args.addAll(
          listOf(
            "-map",
            "0:a:0?",
            "-vn",
            "-c:a",
            "libmp3lame",
            "-b:a",
            "320k",
            "-id3v2_version",
            "3",
          )
        )
      }
      if (title.isNotBlank()) args.addAll(listOf("-metadata", "title=$title"))
      if (artist.isNotBlank()) args.addAll(listOf("-metadata", "artist=$artist"))
      if (album.isNotBlank()) args.addAll(listOf("-metadata", "album=$album"))

      if (year != null) {
        args.addAll(listOf("-metadata", "date=$year", "-metadata", "year=$year"))
      }
      if (coverPath != null) {
        args.addAll(
          listOf("-metadata:s:v", "title=Album cover", "-metadata:s:v", "comment=Cover (front)")
        )
      }
      args.add(outputPath)
      return args.toTypedArray()
    }
  }
}
