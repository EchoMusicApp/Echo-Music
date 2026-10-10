package echo.music.iad1tya.ui.menu

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.runtime.LaunchedEffect
import echo.music.iad1tya.ui.component.DefaultDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.music.innertube.YouTube
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.LocalDownloadUtil
import echo.music.iad1tya.LocalListenTogetherManager
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.EnableExportAsMp3Key
import echo.music.iad1tya.constants.ExportDirectoryUriKey
import echo.music.iad1tya.constants.ExportedSongIdsKey
import echo.music.iad1tya.constants.ExportingSongIdsKey
import echo.music.iad1tya.constants.ListItemHeight
import echo.music.iad1tya.constants.VarispeedKey
import echo.music.iad1tya.ui.menu.SpeedDialog
import echo.music.iad1tya.extensions.toggleRepeatMode
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.ExoDownloadService
import echo.music.iad1tya.ui.component.BottomSheetState
import echo.music.iad1tya.ui.component.ListDialog
import echo.music.iad1tya.ui.component.Material3MenuGroup
import echo.music.iad1tya.ui.component.Material3MenuItemData
import echo.music.iad1tya.ui.component.NewAction
import echo.music.iad1tya.ui.component.NewActionGrid
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.viewmodels.CachePlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun OldPlayerMenu(
  mediaMetadata: MediaMetadata?,
  navController: NavController,
  playerBottomSheetState: BottomSheetState,
  onShowDetailsDialog: () -> Unit,
  onDismiss: () -> Unit,
) {
  mediaMetadata ?: return
  val context = LocalContext.current
  val database = LocalDatabase.current
  val playerConnection = LocalPlayerConnection.current ?: return
  val coroutineScope = rememberCoroutineScope()
  val playerVolume = playerConnection.service.playerVolume.collectAsState()

  val castHandler =
    remember(playerConnection) {
      try {
        playerConnection.service.castConnectionHandler
      } catch (e: Exception) {
        null
      }
    }
  val varispeedMode by rememberPreference(VarispeedKey, defaultValue = false)
  var showSpeedDialog by rememberSaveable { mutableStateOf(false) }

  val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }
  val castVolume by
    castHandler?.castVolume?.collectAsState() ?: remember { mutableFloatStateOf(1f) }
  val castDeviceName by
    castHandler?.castDeviceName?.collectAsState() ?: remember { mutableStateOf<String?>(null) }

  val download by
    LocalDownloadUtil.current.getDownload(mediaMetadata.id).collectAsState(initial = null)

  val listenTogetherManager = LocalListenTogetherManager.current
  val isListenTogetherGuest by
    listenTogetherManager?.guestPlaybackRestricted?.collectAsState(initial = false)
      ?: remember { mutableStateOf(false) }

  val currentSong by playerConnection.currentSong.collectAsState(initial = null)
  val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
  val repeatMode by playerConnection.repeatMode.collectAsState()
  val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

  val artists = remember(mediaMetadata.artists) { mediaMetadata.artists.filter { it.id != null } }

  val ringtoneViewModel = echo.music.iad1tya.LocalRingtoneViewModel.current

  val (enableExportAsMp3) = rememberPreference(key = EnableExportAsMp3Key, defaultValue = false)
  val (exportDirectoryUri) = rememberPreference(key = ExportDirectoryUriKey, defaultValue = "")
  val (exportingSongIds) = rememberPreference(key = ExportingSongIdsKey, defaultValue = "")
  val (exportedSongIds) = rememberPreference(key = ExportedSongIdsKey, defaultValue = "")
  val (showLyricsOnPlayer, onShowLyricsOnPlayerChange) =
    rememberPreference(
      key = echo.music.iad1tya.constants.ShowLyricsOnPlayerKey,
      defaultValue = false
    )

  val isExporting =
    remember(exportingSongIds, mediaMetadata.id) {
      exportingSongIds.split(",").contains(mediaMetadata.id)
    }
  val isExported =
    remember(exportedSongIds, mediaMetadata.id) {
      exportedSongIds.split(",").contains(mediaMetadata.id)
    }

  var showReExportDialog by
    androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
  if (showReExportDialog) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { showReExportDialog = false },
      title = { androidx.compose.material3.Text("Re-export") },
      text = { androidx.compose.material3.Text("Wanna re-export it again?") },
      confirmButton = {
        androidx.compose.material3.Button(
          onClick = {
            showReExportDialog = false
            if (exportDirectoryUri.isBlank()) {
              android.widget.Toast.makeText(
                  context,
                  context.getString(R.string.export_directory_not_set),
                  android.widget.Toast.LENGTH_SHORT
                )
                .show()
              onDismiss()
            } else {
              onDismiss()
              echo.music.iad1tya.playback.AudioExportService.start(
                context = context,
                songId = mediaMetadata.id,
                songTitle = mediaMetadata.title,
                songArtist = artists.joinToString(", ") { it.name },
                songAlbum = mediaMetadata.album?.title ?: "",
                artworkUrl = mediaMetadata.thumbnailUrl ?: "",
                targetDirectoryUri = exportDirectoryUri
              )
            }
          }
        ) {
          androidx.compose.material3.Text("Yes")
        }
      },
      dismissButton = {
        androidx.compose.material3.OutlinedButton(onClick = { showReExportDialog = false }) {
          androidx.compose.material3.Text("No")
        }
      }
    )
  }

  var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
  var showListenTogetherDialog by rememberSaveable { mutableStateOf(false) }
  var showSelectArtistDialog by rememberSaveable { mutableStateOf(false) }
  var showPitchTempoDialog by rememberSaveable { mutableStateOf(false) }
  var refetchIconDegree by remember { mutableFloatStateOf(0f) }
  val cacheViewModel = hiltViewModel<CachePlaylistViewModel>()
  val rotationAnimation by
    animateFloatAsState(
      targetValue = refetchIconDegree,
      animationSpec = tween(durationMillis = 800, easing = LinearEasing),
      label = ""
    )

  AddToPlaylistDialog(
    isVisible = showChoosePlaylistDialog,
    onGetSong = { playlist ->
      database.transaction { insert(mediaMetadata) }
      coroutineScope.launch(Dispatchers.IO) {
        playlist.playlist.browseId?.let { YouTube.addToPlaylist(it, mediaMetadata.id) }
      }
      onDismiss()
      listOf(mediaMetadata.id)
    },
    onDismiss = { showChoosePlaylistDialog = false }
  )

  ListenTogetherDialog(
    visible = showListenTogetherDialog,
    mediaMetadata = mediaMetadata,
    onDismiss = { showListenTogetherDialog = false }
  )

  if (showSelectArtistDialog) {
    ListDialog(onDismiss = { showSelectArtistDialog = false }) {
      items(artists) { artist ->
        Box(
          contentAlignment = Alignment.CenterStart,
          modifier =
            Modifier.fillParentMaxWidth()
              .height(ListItemHeight)
              .clickable {
                navController.navigate("artist/${artist.id}")
                showSelectArtistDialog = false
                playerBottomSheetState.collapseSoft()
                onDismiss()
              }
              .padding(horizontal = 24.dp),
        ) {
          Text(
            text = artist.name,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }

  var showSongDnaDialog by rememberSaveable { mutableStateOf(false) }
  var dnaAlbum by rememberSaveable { mutableStateOf("Fetching...") }
  var dnaMeaning by rememberSaveable { mutableStateOf("Fetching info from Wikipedia...") }
  var dnaBio by rememberSaveable { mutableStateOf("Fetching biography...") }
  var dnaFetched by rememberSaveable { mutableStateOf(false) }

  if (showSongDnaDialog) {
    LaunchedEffect(mediaMetadata.id) {
      if (!dnaFetched) {
         kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val artistName = mediaMetadata.artists.firstOrNull()?.name ?: ""
                val songTitle = mediaMetadata.title
                val query = java.net.URLEncoder.encode("$artistName $songTitle", "UTF-8")
                val searchUrl = java.net.URL("https://genius.com/api/search/multi?q=$query")
                val searchJson = searchUrl.readText()
                val searchRoot = org.json.JSONObject(searchJson).getJSONObject("response")
                val sections = searchRoot.getJSONArray("sections")
                var songId = -1
                for (i in 0 until sections.length()) {
                    val sec = sections.getJSONObject(i)
                    if (sec.getString("type") == "top_hit" || sec.getString("type") == "song") {
                        val hits = sec.getJSONArray("hits")
                        if (hits.length() > 0) {
                            songId = hits.getJSONObject(0).getJSONObject("result").getInt("id")
                            break
                        }
                    }
                }
                if (songId == -1) {
                    dnaAlbum = mediaMetadata.album?.title ?: "Unknown"
                    dnaMeaning = "Meaning not found."
                    dnaBio = "Biography not found."
                } else {
                    val songUrl = java.net.URL("https://genius.com/api/songs/$songId?text_format=plain")
                    val songJson = songUrl.readText()
                    val songObj = org.json.JSONObject(songJson).getJSONObject("response").getJSONObject("song")
                    
                    if (songObj.has("album") && !songObj.isNull("album")) {
                        dnaAlbum = songObj.getJSONObject("album").getString("name")
                    } else {
                        dnaAlbum = mediaMetadata.album?.title ?: "Unknown"
                    }
                    
                    dnaMeaning = "Info not found." // Reset so Wikipedia can override it
                    
                    var artistId = -1
                    if (songObj.has("primary_artist") && !songObj.isNull("primary_artist")) {
                        artistId = songObj.getJSONObject("primary_artist").getInt("id")
                    }
                    
                    if (artistId != -1) {
                        val artistUrl = java.net.URL("https://genius.com/api/artists/$artistId?text_format=plain")
                        val artistJson = artistUrl.readText()
                        val artistObj = org.json.JSONObject(artistJson).getJSONObject("response").getJSONObject("artist")
                        if (artistObj.has("description") && !artistObj.isNull("description")) {
                            dnaBio = artistObj.getJSONObject("description").getString("plain")
                            if (dnaBio == "?") dnaBio = "Biography not found."
                        } else {
                            dnaBio = "Biography not found."
                        }
                    } else {
                        dnaBio = "Biography not found."
                    }
                }
                
                // --- WIKIPEDIA FALLBACKS ---
                
                // 1. Wikipedia exclusively for About the Song
                if (true) {
                    dnaMeaning = "Info not found." 
                    try {
                        val wpQuery = java.net.URLEncoder.encode(songTitle + " " + dnaAlbum, "UTF-8")
                        val wpSearchUrl = java.net.URL("https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$wpQuery&utf8=&format=json")
                        val wpSearchRoot = org.json.JSONObject(wpSearchUrl.readText()).getJSONObject("query")
                        val wpSearchList = wpSearchRoot.getJSONArray("search")
                        if (wpSearchList.length() > 0) {
                            val wpTitle = wpSearchList.getJSONObject(0).getString("title")
                            val wpTitleEnc = java.net.URLEncoder.encode(wpTitle, "UTF-8")
                            val wpExtractUrl = java.net.URL("https://en.wikipedia.org/w/api.php?action=query&prop=extracts&exsentences=4&exlimit=1&titles=$wpTitleEnc&explaintext=1&format=json")
                            val wpExtractRoot = org.json.JSONObject(wpExtractUrl.readText()).getJSONObject("query").getJSONObject("pages")
                            val firstKey = wpExtractRoot.keys().next()
                            val extract = wpExtractRoot.getJSONObject(firstKey).optString("extract", "")
                            if (extract.isNotBlank() && !extract.contains("may refer to")) {
                                dnaMeaning = extract
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                
                // 2. Wikipedia fallback for Artist Bio
                if (dnaBio == "Biography not found." && artistName.isNotBlank()) {
                    try {
                        val wpQuery = java.net.URLEncoder.encode(artistName, "UTF-8")
                        val wpSearchUrl = java.net.URL("https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$wpQuery&utf8=&format=json")
                        val wpSearchRoot = org.json.JSONObject(wpSearchUrl.readText()).getJSONObject("query")
                        val wpSearchList = wpSearchRoot.getJSONArray("search")
                        if (wpSearchList.length() > 0) {
                            val wpTitle = wpSearchList.getJSONObject(0).getString("title")
                            val wpTitleEnc = java.net.URLEncoder.encode(wpTitle, "UTF-8")
                            val wpExtractUrl = java.net.URL("https://en.wikipedia.org/w/api.php?action=query&prop=extracts&exsentences=4&exlimit=1&titles=$wpTitleEnc&explaintext=1&format=json")
                            val wpExtractRoot = org.json.JSONObject(wpExtractUrl.readText()).getJSONObject("query").getJSONObject("pages")
                            val firstKey = wpExtractRoot.keys().next()
                            val extract = wpExtractRoot.getJSONObject(firstKey).optString("extract", "")
                            if (extract.isNotBlank() && !extract.contains("may refer to")) {
                                dnaBio = extract
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                dnaAlbum = mediaMetadata.album?.title ?: "Unknown"
                dnaMeaning = "Failed to fetch meaning."
                dnaBio = "Failed to fetch biography."
            }
            dnaFetched = true
         }
      }
    }

    DefaultDialog(
      onDismiss = { showSongDnaDialog = false },
      icon = {
        androidx.compose.foundation.Image(
          painter = painterResource(R.drawable.songdna),
          contentDescription = null,
          modifier = Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape)
        )
      },
      title = {
        Text("Song DNA", fontWeight = FontWeight.Bold)
      },
      buttons = {
        TextButton(onClick = { showSongDnaDialog = false }) { Text(stringResource(android.R.string.ok)) }
      }
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        item {
            Column {
                Text("Movie / Album", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(dnaAlbum, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Column {
                Text("About the Song", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(dnaMeaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Column {
                Text("About the Artist", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(dnaBio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
      }
    }
  }

  if (showSpeedDialog) {
    SpeedDialog(
      onDismiss = { showSpeedDialog = false },
    )
  }

  if (showPitchTempoDialog) {
    TempoPitchDialog(onDismiss = { showPitchTempoDialog = false })
  }

  if (isCasting && castDeviceName != null) {
    Column(
      modifier =
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 6.dp),
    ) {
      Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
      ) {
        Icon(
          painter = painterResource(R.drawable.cast),
          contentDescription = null,
          modifier = Modifier.size(24.dp),
          tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = stringResource(R.string.casting_to, castDeviceName ?: ""),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary
        )
      }
    }
  }

  LazyColumn(
    contentPadding =
      PaddingValues(
        start = 0.dp,
        top = 0.dp,
        end = 0.dp,
        bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
      ),
  ) {
    item {
      val startingRadioText = stringResource(R.string.starting_radio)
      NewActionGrid(
        actions =
          listOfNotNull(
            if (!isListenTogetherGuest) {
              NewAction(
                icon = {
                  Icon(
                    painter = painterResource(R.drawable.radio),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                },
                text = stringResource(R.string.start_an_radio),
                onClick = {
                  Toast.makeText(context, startingRadioText, Toast.LENGTH_SHORT).show()
                  playerConnection.startRadioSeamlessly()
                  onDismiss()
                }
              )
            } else null,
            NewAction(
              icon = {
                Icon(
                  painter = painterResource(R.drawable.playlist_add),
                  contentDescription = null,
                  modifier = Modifier.size(32.dp),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              },
              text = stringResource(R.string.add_to_an_playlist),
              onClick = { showChoosePlaylistDialog = true }
            ),
            NewAction(
              icon = {
                Icon(
                  painter = painterResource(R.drawable.share),
                  contentDescription = null,
                  modifier = Modifier.size(32.dp),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              },
              text = stringResource(R.string.share),
              onClick = {
                val intent =
                  android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(
                      android.content.Intent.EXTRA_TEXT,
                      "https://share.echomusic.fun/watch?v=${mediaMetadata.id}"
                    )
                  }
                context.startActivity(android.content.Intent.createChooser(intent, null))
                onDismiss()
              }
            )
          ),
        columns = if (isListenTogetherGuest) 2 else 3,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
      )
    }

    item { Spacer(modifier = Modifier.height(8.dp)) }

    item {
      Material3MenuGroup(
        items =
          buildList {
            val infiniteTransition = rememberInfiniteTransition()
            val animatedColor by infiniteTransition.animateColor(
                initialValue = MaterialTheme.colorScheme.primary,
                targetValue = MaterialTheme.colorScheme.tertiary,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dnaColor"
            )

            add(
              Material3MenuItemData(
                title = { Text(text = "Song DNA", color = animatedColor, fontWeight = FontWeight.Bold) },
                icon = {
                  androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.songdna),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape)
                  )
                },
                onClick = {
                  showSongDnaDialog = true
                }
              )
            )
            add(
              Material3MenuItemData(
                customComposable = { echo.music.iad1tya.ui.component.CastButton(asMenuItem = true) }
              )
            )

            add(
              Material3MenuItemData(
                title = { Text(text = "Ambient Mode") },
                icon = {
                  Icon(
                    painter = painterResource(R.drawable.fullscreen),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                  )
                },
                onClick = {
                  navController.navigate("ambient_mode")
                  playerBottomSheetState.collapseSoft()
                  onDismiss()
                }
              )
            )

            add(
              Material3MenuItemData(
                title = { Text(text = if (showLyricsOnPlayer) "Hide Lyrics" else "Show Lyrics") },
                icon = {
                  Icon(
                    painter = painterResource(R.drawable.lyrics),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                  )
                },
                onClick = {
                  onShowLyricsOnPlayerChange(!showLyricsOnPlayer)
                  onDismiss()
                }
              )
            )

            if (!isListenTogetherGuest) {
              add(
                Material3MenuItemData(
                  title = { Text(stringResource(R.string.shuffle)) },
                  icon = {
                    Icon(
                      painter = painterResource(R.drawable.shuffle),
                      contentDescription = null,
                      modifier = Modifier.size(24.dp),
                      tint =
                        if (shuffleModeEnabled) MaterialTheme.colorScheme.primary
                        else androidx.compose.material3.LocalContentColor.current
                    )
                  },
                  onClick = {
                    playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                    onDismiss()
                  }
                )
              )
            }

            when (download?.state) {
              Download.STATE_COMPLETED -> {
                add(
                  Material3MenuItemData(
                    title = { Text(stringResource(R.string.remove_download)) },
                    icon = {
                      Icon(
                        painter = painterResource(R.drawable.offline),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                      )
                    },
                    onClick = {
                      DownloadService.sendRemoveDownload(
                        context,
                        ExoDownloadService::class.java,
                        mediaMetadata.id,
                        false
                      )
                      onDismiss()
                    }
                  )
                )
              }
              Download.STATE_QUEUED,
              Download.STATE_DOWNLOADING -> {
                add(
                  Material3MenuItemData(
                    title = { Text(stringResource(R.string.downloading)) },
                    icon = {
                      CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    },
                    onClick = {
                      DownloadService.sendRemoveDownload(
                        context,
                        ExoDownloadService::class.java,
                        mediaMetadata.id,
                        false
                      )
                      onDismiss()
                    }
                  )
                )
              }
              else -> {
                add(
                  Material3MenuItemData(
                    title = { Text(stringResource(R.string.action_download)) },
                    icon = {
                      Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                      )
                    },
                    onClick = {
                      database.transaction { insert(mediaMetadata) }
                      val downloadRequest =
                        DownloadRequest.Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                          .setCustomCacheKey(mediaMetadata.id)
                          .setData(mediaMetadata.title.toByteArray())
                          .build()
                      DownloadService.sendAddDownload(
                        context,
                        ExoDownloadService::class.java,
                        downloadRequest,
                        false
                      )
                      onDismiss()
                    }
                  )
                )
              }
            }

            if (enableExportAsMp3) {
              when {
                isExporting ->
                  add(
                    Material3MenuItemData(
                      title = { Text(text = stringResource(R.string.exporting)) },
                      icon = {
                        CircularProgressIndicator(
                          modifier = Modifier.size(24.dp),
                          strokeWidth = 2.dp
                        )
                      },
                      onClick = {}
                    )
                  )
                isExported ->
                  add(
                    Material3MenuItemData(
                      title = { Text(text = stringResource(R.string.action_exported)) },
                      icon = {
                        Icon(
                          painter = painterResource(R.drawable.folder_managed),
                          contentDescription = null,
                          modifier = Modifier.size(24.dp)
                        )
                      },
                      onClick = { showReExportDialog = true }
                    )
                  )
                else ->
                  add(
                    Material3MenuItemData(
                      title = { Text(text = stringResource(R.string.action_export)) },
                      icon = {
                        Icon(
                          painter = painterResource(R.drawable.file_export),
                          contentDescription = null,
                          modifier = Modifier.size(24.dp)
                        )
                      },
                      onClick = {
                        if (exportDirectoryUri.isBlank()) {
                          android.widget.Toast.makeText(
                              context,
                              context.getString(R.string.export_directory_not_set),
                              android.widget.Toast.LENGTH_SHORT
                            )
                            .show()
                          onDismiss()
                        } else {
                          onDismiss()
                          echo.music.iad1tya.playback.AudioExportService.start(
                            context = context,
                            songId = mediaMetadata.id,
                            songTitle = mediaMetadata.title,
                            songArtist = artists.joinToString(", ") { it.name },
                            songAlbum = mediaMetadata.album?.title ?: "",
                            artworkUrl = mediaMetadata.thumbnailUrl ?: "",
                            targetDirectoryUri = exportDirectoryUri
                          )
                        }
                      }
                    )
                  )
              }
            }

            val isLiked = currentSong?.song?.liked == true
            add(
              Material3MenuItemData(
                title = { Text(stringResource(if (isLiked) R.string.liked else R.string.like)) },
                icon = {
                  Icon(
                    painter =
                      painterResource(
                        if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                      ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint =
                      if (isLiked) MaterialTheme.colorScheme.error
                      else androidx.compose.material3.LocalContentColor.current
                  )
                },
                onClick = {
                  playerConnection.toggleLike()
                  onDismiss()
                }
              )
            )

            if (!isListenTogetherGuest) {
              add(
                Material3MenuItemData(
                  title = { Text(stringResource(R.string.repeat)) },
                  icon = {
                    Icon(
                      painter =
                        painterResource(
                          when (repeatMode) {
                            Player.REPEAT_MODE_OFF,
                            Player.REPEAT_MODE_ALL -> R.drawable.repeat
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> R.drawable.repeat
                          }
                        ),
                      contentDescription = null,
                      modifier = Modifier.size(24.dp),
                      tint =
                        if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                        else androidx.compose.material3.LocalContentColor.current
                    )
                  },
                  onClick = { playerConnection.player.toggleRepeatMode() }
                )
              )
            }

            add(
              Material3MenuItemData(
                title = { Text(text = stringResource(R.string.refetch)) },
                description = { Text(text = stringResource(R.string.refetch_desc)) },
                icon = {
                  Icon(
                    painter = painterResource(R.drawable.sync),
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation)
                  )
                },
                onClick = {
                  refetchIconDegree -= 360
                  cacheViewModel.removeSongFromCache(mediaMetadata.id)
                  androidx.media3.exoplayer.offline.DownloadService.sendRemoveDownload(
                    context,
                    echo.music.iad1tya.playback.ExoDownloadService::class.java,
                    mediaMetadata.id,
                    false
                  )
                  val intent =
                    android.content
                      .Intent(context, echo.music.iad1tya.playback.MusicService::class.java)
                      .apply {
                        action = "echo.music.iad1tya.ACTION_CLEAR_SONG_CACHE"
                        putExtra("songId", mediaMetadata.id)
                      }
                  context.startService(intent)
                  coroutineScope.launch(Dispatchers.IO) {
                    database.query { deleteFormat(mediaMetadata.id) }
                    YouTube.queue(listOf(mediaMetadata.id)).onSuccess {
                      val newSong = it.firstOrNull()
                      if (newSong != null) {
                        database.transaction {
                          val songToUpdate = librarySong
                          if (songToUpdate != null) {
                            update(songToUpdate, newSong.toMediaMetadata())
                          } else {
                            insert(newSong.toMediaMetadata())
                          }
                        }
                      }
                    }
                  }
                }
              )
            )
          }
      )
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }

    item {
      Material3MenuGroup(
        items =
          buildList {
            if (artists.isNotEmpty()) {
              add(
                Material3MenuItemData(
                  title = { Text(text = stringResource(R.string.view_artist)) },
                  description = {
                    Text(
                      text = mediaMetadata.artists.joinToString { it.name },
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  },
                  icon = {
                    Icon(
                      painter = painterResource(R.drawable.artist),
                      contentDescription = null,
                      modifier = Modifier.size(24.dp)
                    )
                  },
                  onClick = {
                    if (mediaMetadata.artists.size == 1) {
                      navController.navigate("artist/${mediaMetadata.artists[0].id}")
                      playerBottomSheetState.collapseSoft()
                      onDismiss()
                    } else {
                      showSelectArtistDialog = true
                    }
                  }
                )
              )
            }

            val isInLibrary = librarySong?.song?.inLibrary != null
            add(
              Material3MenuItemData(
                title = {
                  Text(
                    text =
                      stringResource(
                        if (isInLibrary) R.string.remove_from_library else R.string.add_to_library
                      )
                  )
                },
                icon = {
                  Icon(
                    painter =
                      painterResource(
                        if (isInLibrary) R.drawable.library_add_check else R.drawable.library_add
                      ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                  )
                },
                onClick = {
                  playerConnection.toggleLibrary()
                  onDismiss()
                }
              )
            )
          }
      )
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }

    item {
      Material3MenuGroup(
        items =
          listOf(
            Material3MenuItemData(
              title = { Text(text = "Set as Ringtone") },
              icon = {
                Icon(
                  painter = painterResource(R.drawable.notification),
                  contentDescription = null,
                  modifier = Modifier.size(24.dp)
                )
              },
              onClick = {
                if (ringtoneViewModel.hasSettingsPermission(context)) {
                  ringtoneViewModel.showTrimmer(
                    mediaMetadata.id,
                    mediaMetadata.title,
                    mediaMetadata.artists.joinToString { it.name },
                    mediaMetadata.duration
                  )
                } else {
                  ringtoneViewModel.requestSettingsPermission(context)
                }
                onDismiss()
              }
            )
          )
      )
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }

    item {
      Material3MenuGroup(
        items =
          buildList {
            add(
              Material3MenuItemData(
                title = { Text(text = stringResource(R.string.listen_together)) },
                icon = {
                  Icon(
                    painter = painterResource(R.drawable.group),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                  )
                },
                onClick = { showListenTogetherDialog = true }
              )
            )
            if (isListenTogetherGuest) {
              add(
                Material3MenuItemData(
                  title = { Text(text = stringResource(R.string.resync)) },
                  icon = {
                    Icon(
                      painter = painterResource(R.drawable.replay),
                      contentDescription = null,
                      modifier = Modifier.size(24.dp)
                    )
                  },
                  onClick = {
                    listenTogetherManager?.requestSync()
                    onDismiss()
                  }
                )
              )
            }
          }
      )
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }

    item {
      Material3MenuGroup(
        items =
          buildList {
            add(
              Material3MenuItemData(
                title = { Text(text = stringResource(R.string.details)) },
                description = { Text(text = stringResource(R.string.details_desc)) },
                icon = {
                  Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                  )
                },
                onClick = {
                  onShowDetailsDialog()
                  onDismiss()
                }
              )
            )

            add(
              Material3MenuItemData(
                title = { Text(text = stringResource(R.string.equalizer)) },
                description = { Text(text = stringResource(R.string.equalizer_desc)) },
                icon = {
                  Icon(
                    painter = painterResource(R.drawable.equalizer),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                  )
                },
                onClick = {
                  navController.navigate("equalizer")
                  onDismiss()
                }
              )
            )

            add(
              Material3MenuItemData(
                title = { Text(text = stringResource(R.string.advanced)) },
                description = { Text(text = stringResource(R.string.advanced_desc)) },
                icon = {
                  Icon(
                    painter = painterResource(R.drawable.tune),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                  )
                },
                onClick = { if (!varispeedMode) showPitchTempoDialog = true else showSpeedDialog = true }
              )
            )
          }
      )
    }
  }
}
