package echo.music.iad1tya.ui.screens.ambient

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.MoreVert

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.constants.*
import echo.music.iad1tya.extensions.togglePlayPause
import echo.music.iad1tya.ui.player.InlineLyricsView
import echo.music.iad1tya.ui.screens.ambient.AmbientCanvasBackground
import echo.music.iad1tya.utils.rememberPreference
import kotlin.math.abs

@Composable
@androidx.compose.material3.ExperimentalMaterial3Api

fun AmbientModeScreen(navController: NavController) {
  val context = LocalContext.current
  val playerConnection = LocalPlayerConnection.current ?: return
  val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

  val artScale by rememberPreference(AmbientArtScaleKey, 0.85f)
  val showArt by rememberPreference(AmbientShowArtKey, true)
  val fullScreenArt by rememberPreference(AmbientFullScreenArtKey, false)


  val showTitle by rememberPreference(AmbientShowTitleKey, false)
  val showArtist by rememberPreference(AmbientShowArtistKey, false)
  val showLyrics by rememberPreference(AmbientShowLyricsKey, true)
  val spacing by rememberPreference(AmbientSpacingKey, 32f)


  DisposableEffect(Unit) {
    val activity = context as? Activity
    val originalOrientation =
      activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

    val window = activity?.window
    var windowInsetsController: WindowInsetsControllerCompat? = null
    if (window != null) {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
      windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    onDispose {
      activity?.requestedOrientation = originalOrientation
      if (window != null) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
      }
    }
  }

  BackHandler { navController.popBackStack() }

  
  var showSettingsSheet by remember { mutableStateOf(false) }

  var swipeThresholdX by remember { mutableStateOf(0f) }
  var swipeThresholdY by remember { mutableStateOf(0f) }
  val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager

  Box(
    modifier =
      Modifier.fillMaxSize().pointerInput(Unit) {
        detectDragGestures(
          onDragEnd = {
            // Horizontal swipe logic (Skip Next/Prev)
            if (abs(swipeThresholdX) > 150f && abs(swipeThresholdX) > abs(swipeThresholdY)) {
              if (swipeThresholdX > 0) {
                playerConnection.player.seekToPreviousMediaItem()
              } else {
                playerConnection.player.seekToNext()
              }
            }
            swipeThresholdX = 0f
            swipeThresholdY = 0f
          },
          onDrag = { change, dragAmount ->
            change.consume()
            swipeThresholdX += dragAmount.x
            swipeThresholdY += dragAmount.y

            // Vertical swipe logic (Volume Control)
            if (abs(dragAmount.y) > 10f && abs(dragAmount.y) > abs(dragAmount.x)) {
              val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
              val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
              if (dragAmount.y < 0 && currentVolume < maxVolume) {
                audioManager.adjustStreamVolume(
                  AudioManager.STREAM_MUSIC,
                  AudioManager.ADJUST_RAISE,
                  AudioManager.FLAG_SHOW_UI
                )
              } else if (dragAmount.y > 0 && currentVolume > 0) {
                audioManager.adjustStreamVolume(
                  AudioManager.STREAM_MUSIC,
                  AudioManager.ADJUST_LOWER,
                  AudioManager.FLAG_SHOW_UI
                )
              }
              swipeThresholdY = 0f // Reset to require another swipe chunk to keep raising
            }
          }
        )
      }
  ) {
    AmbientGlowBackground(mediaMetadata = mediaMetadata, modifier = Modifier.fillMaxSize())
    Row(
      modifier = Modifier.fillMaxSize().safeDrawingPadding(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (showArt) {
        // Left Side: Album Art & Info
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .padding(
                start = if (showLyrics) spacing.dp else 0.dp,
                end = if (showLyrics) (spacing / 2).dp else 0.dp,
                top = 32.dp,
                bottom = 32.dp
              ),
          contentAlignment = if (showLyrics) Alignment.CenterEnd else Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier =
                Modifier.fillMaxHeight(artScale)
                  .aspectRatio(1f)
                  .clip(RoundedCornerShape(16.dp))
                  .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { playerConnection.togglePlayPause() })
                  }
            ) {
              AsyncImage(
                model = mediaMetadata?.thumbnailUrl,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
              AmbientCanvasBackground(modifier = Modifier.fillMaxSize())
            }

            if (showTitle || showArtist) {
              Spacer(modifier = Modifier.height(16.dp))
              if (showTitle) {
                Text(
                  text = mediaMetadata?.title ?: "",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              if (showArtist) {
                Text(
                  text = mediaMetadata?.artists?.joinToString { it.name } ?: "",
                  style = MaterialTheme.typography.bodyLarge,
                  color = Color.White.copy(alpha = 0.7f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }

      // Right Side: Lyrics
      if (showLyrics) {
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .padding(
                start = if (showArt) (spacing / 2).dp else 32.dp,
                end = if (showArt) spacing.dp else 32.dp,
                top = 32.dp,
                bottom = 32.dp
              ),
          contentAlignment = if (showArt) Alignment.CenterStart else Alignment.Center
        ) {
          InlineLyricsView(
            mediaMetadata = mediaMetadata,
            showLyrics = true,
            positionProvider = { playerConnection.player.currentPosition }
          )
        }
      }
    }

    // Top Left Controls
    Box(
      modifier = Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(16.dp)
    ) {
      var expandedMenu by remember { mutableStateOf(false) }
      IconButton(onClick = { expandedMenu = true }) {
        Icon(
          imageVector = androidx.compose.material.icons.Icons.Rounded.MoreVert,
          contentDescription = "Menu",
          tint = Color.White.copy(alpha = 0.5f)
        )
      }

      androidx.compose.material3.DropdownMenu(
        expanded = expandedMenu,
        onDismissRequest = { expandedMenu = false }
      ) {
        androidx.compose.material3.DropdownMenuItem(
          text = { Text("Settings") },
          leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
          onClick = {
            expandedMenu = false
            showSettingsSheet = true
          }
        )
        androidx.compose.material3.DropdownMenuItem(
          text = { Text("Back") },
          leadingIcon = { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null) },
          onClick = {
            expandedMenu = false
            navController.popBackStack()
          }
        )
      }
    }

    if (showSettingsSheet) {
      androidx.compose.ui.window.Dialog(
        onDismissRequest = { showSettingsSheet = false },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) { showSettingsSheet = false }
            .padding(16.dp),
          contentAlignment = Alignment.CenterEnd
        ) {
          androidx.compose.material3.Surface(
            modifier = Modifier
              .fillMaxHeight()
              .fillMaxWidth(0.5f)
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
              ) {}, // consume clicks so they don't dismiss
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 3.dp
          ) {
            echo.music.iad1tya.ui.screens.settings.AmbientSettingsContent(
              modifier = Modifier.padding(8.dp)
            )
          }
        }
      }
    }
  }
}
