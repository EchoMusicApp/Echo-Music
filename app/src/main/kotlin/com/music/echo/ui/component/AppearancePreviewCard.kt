package echo.music.iad1tya.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.PlayerBackgroundStyle
import echo.music.iad1tya.constants.PlayerButtonsStyle
import echo.music.iad1tya.constants.SliderStyle
import echo.music.iad1tya.ui.theme.PlayerSliderColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun playerBackgroundStyleLabel(style: PlayerBackgroundStyle): String {
  return when (style) {
    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
    PlayerBackgroundStyle.GLOW_ANIMATED -> stringResource(R.string.glow_animated)
    PlayerBackgroundStyle.APPLE_MUSIC -> stringResource(R.string.apple_music)
    PlayerBackgroundStyle.LIVE_MESH -> stringResource(R.string.live_mesh)
    PlayerBackgroundStyle.LIQUID_GLASS -> stringResource(R.string.player_background_liquid_glass)
  }
}

@Composable
fun playerButtonsStyleLabel(style: PlayerButtonsStyle): String {
  return when (style) {
    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
    PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
  }
}

/**
 * Reusable mock component accurately representing Echo Music's expanded Now Playing screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FullPlayerPreview(
  backgroundStyle: PlayerBackgroundStyle,
  buttonStyle: PlayerButtonsStyle,
  sliderStyle: SliderStyle,
  isSquiggly: Boolean,
  wavyPlayPause: Boolean,
  thumbnailCornerRadius: Dp,
  cropAlbumArt: Boolean,
  fontFamily: FontFamily,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val isDark = colorScheme.surface.luminance() < 0.5f

  val sliderColors =
    PlayerSliderColors.getSliderColors(
      activeColor = colorScheme.primary,
      playerBackground = backgroundStyle,
      useDarkTheme = isDark
    )

  val (playPauseContainerColor, playPauseContentColor) =
    when (buttonStyle) {
      PlayerButtonsStyle.DEFAULT -> {
        if (isDark) Pair(Color.White, Color.Black)
        else Pair(colorScheme.primary, colorScheme.onPrimary)
      }
      PlayerButtonsStyle.PRIMARY ->
        Pair(colorScheme.primary, colorScheme.onPrimary)
      PlayerButtonsStyle.TERTIARY ->
        Pair(colorScheme.tertiary, colorScheme.onTertiary)
    }

  val (sideButtonContainerColor, sideButtonContentColor) =
    when (buttonStyle) {
      PlayerButtonsStyle.DEFAULT ->
        Pair(Color.White.copy(alpha = 0.15f), if (isDark) Color.White else colorScheme.onSurface)
      PlayerButtonsStyle.PRIMARY ->
        Pair(colorScheme.primaryContainer, colorScheme.onPrimaryContainer)
      PlayerButtonsStyle.TERTIARY ->
        Pair(colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer)
    }

  val playPauseShape =
    if (wavyPlayPause) {
      remember { PreviewWavyShape(sides = 9, indent = 0.16f, rotationDegrees = 0f) }
    } else {
      CircleShape
    }

  val backgroundModifier = remember(backgroundStyle, colorScheme) {
    when (backgroundStyle) {
      PlayerBackgroundStyle.DEFAULT -> Modifier.background(colorScheme.surfaceContainer)
      PlayerBackgroundStyle.GRADIENT -> Modifier.background(
        Brush.verticalGradient(
          colors = listOf(
            colorScheme.primaryContainer.copy(alpha = 0.5f),
            colorScheme.surfaceContainer
          )
        )
      )
      PlayerBackgroundStyle.BLUR -> Modifier.background(
        Brush.verticalGradient(
          colors = listOf(
            colorScheme.primary.copy(alpha = 0.35f),
            colorScheme.surfaceContainerHigh
          )
        )
      )
      PlayerBackgroundStyle.GLOW_ANIMATED -> Modifier.background(
        Brush.radialGradient(
          colors = listOf(
            colorScheme.primary.copy(alpha = 0.45f),
            colorScheme.surfaceContainerHigh
          )
        )
      )
      PlayerBackgroundStyle.APPLE_MUSIC -> Modifier.background(
        Brush.verticalGradient(
          colors = listOf(
            colorScheme.secondaryContainer.copy(alpha = 0.6f),
            colorScheme.surfaceContainer
          )
        )
      )
      PlayerBackgroundStyle.LIVE_MESH -> Modifier.background(
        Brush.linearGradient(
          colors = listOf(
            colorScheme.primary.copy(alpha = 0.4f),
            colorScheme.tertiary.copy(alpha = 0.3f),
            colorScheme.surfaceContainerHigh
          )
        )
      )
      PlayerBackgroundStyle.LIQUID_GLASS -> Modifier
        .background(colorScheme.surfaceContainer.copy(alpha = 0.7f))
        .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    }
  }

  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .then(backgroundModifier)
    ) {
      // In Apple Music background style, top artwork with gradient fade
      if (backgroundStyle == PlayerBackgroundStyle.APPLE_MUSIC) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.60f)
        ) {
          Image(
            painter = painterResource(R.drawable.preview_artwork),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(
                    Color.Transparent,
                    colorScheme.surfaceContainer.copy(alpha = 0.5f),
                    colorScheme.surfaceContainer
                  )
                )
              )
          )
        }
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 10.dp, vertical = 8.dp)
      ) {
        // Top Pill: "Song"
        Surface(
          shape = CircleShape,
          color = colorScheme.onSurface.copy(alpha = 0.12f),
          border = BorderStroke(0.5.dp, colorScheme.onSurface.copy(alpha = 0.2f)),
          modifier = Modifier.padding(bottom = 6.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Icon(
              painter = painterResource(R.drawable.music_note),
              contentDescription = null,
              modifier = Modifier.size(8.dp),
              tint = colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Song",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
              color = colorScheme.onSurface
            )
          }
        }

        // Album Art (centered square when not Apple Music)
        if (backgroundStyle != PlayerBackgroundStyle.APPLE_MUSIC) {
          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(RoundedCornerShape(thumbnailCornerRadius))
              .background(colorScheme.surfaceVariant)
              .border(
                0.5.dp,
                colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(thumbnailCornerRadius)
              ),
            contentAlignment = Alignment.Center
          ) {
            Image(
              painter = painterResource(R.drawable.preview_artwork),
              contentDescription = null,
              contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
              modifier = Modifier.fillMaxSize()
            )
          }
        } else {
          Spacer(modifier = Modifier.height(48.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Title & Artist
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.Start
        ) {
          Text(
            text = stringResource(R.string.preview_song_title),
            fontFamily = fontFamily,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = colorScheme.onSurface
          )
          Text(
            text = stringResource(R.string.preview_song_artist),
            fontFamily = fontFamily,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress Slider
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
          contentAlignment = Alignment.Center
        ) {
          when {
            sliderStyle == SliderStyle.WAVY && isSquiggly -> {
              SquigglySlider(
                value = 0.42f,
                onValueChange = {},
                valueRange = 0f..1f,
                colors = sliderColors,
                isPlaying = false,
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(18.dp)
              )
            }
            sliderStyle == SliderStyle.WAVY && !isSquiggly -> {
              WavySlider(
                value = 0.42f,
                onValueChange = {},
                valueRange = 0f..1f,
                colors = sliderColors,
                isPlaying = false,
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(18.dp)
              )
            }
            sliderStyle == SliderStyle.SLIM -> {
              Slider(
                value = 0.42f,
                onValueChange = {},
                valueRange = 0f..1f,
                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                track = { sliderState ->
                  PlayerSliderTrack(sliderState = sliderState, colors = sliderColors, trackHeight = 4.dp)
                },
                colors = sliderColors,
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(18.dp)
              )
            }
            else -> {
              Slider(
                value = 0.42f,
                onValueChange = {},
                valueRange = 0f..1f,
                colors = sliderColors,
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(18.dp)
              )
            }
          }
        }

        // Duration row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "0:12",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
            color = colorScheme.onSurfaceVariant
          )
          Text(
            text = "3:08",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
            color = colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Playback buttons
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(24.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(sideButtonContainerColor),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              painter = painterResource(R.drawable.skip_previous),
              contentDescription = null,
              tint = sideButtonContentColor,
              modifier = Modifier.size(14.dp)
            )
          }

          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(playPauseShape)
              .background(playPauseContainerColor),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              painter = painterResource(R.drawable.play),
              contentDescription = null,
              tint = playPauseContentColor,
              modifier = Modifier.size(16.dp)
            )
          }

          Box(
            modifier = Modifier
              .size(24.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(sideButtonContainerColor),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              painter = painterResource(R.drawable.skip_next),
              contentDescription = null,
              tint = sideButtonContentColor,
              modifier = Modifier.size(14.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom row icons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          Icon(
            painter = painterResource(R.drawable.lyrics),
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(11.dp)
          )
          Icon(
            painter = painterResource(R.drawable.favorite),
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(11.dp)
          )
          Icon(
            painter = painterResource(R.drawable.more_horiz),
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(11.dp)
          )
        }
      }
    }
  }
}

/**
 * Contextual preview for Mini-Player background styles.
 */
@Composable
fun MiniPlayerPreview(
  backgroundStyle: PlayerBackgroundStyle,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme

  val backgroundBrushOrColor = remember(backgroundStyle, colorScheme) {
    when (backgroundStyle) {
      PlayerBackgroundStyle.DEFAULT -> Modifier.background(colorScheme.surfaceContainerHighest)
      PlayerBackgroundStyle.GRADIENT -> Modifier.background(
        Brush.horizontalGradient(
          colors = listOf(
            colorScheme.primaryContainer.copy(alpha = 0.6f),
            colorScheme.surfaceContainerHighest
          )
        )
      )
      PlayerBackgroundStyle.BLUR -> Modifier.background(
        Brush.horizontalGradient(
          colors = listOf(
            colorScheme.primary.copy(alpha = 0.4f),
            colorScheme.surfaceContainerHighest
          )
        )
      )
      PlayerBackgroundStyle.GLOW_ANIMATED -> Modifier.background(
        Brush.radialGradient(
          colors = listOf(
            colorScheme.primary.copy(alpha = 0.5f),
            colorScheme.surfaceContainerHighest
          )
        )
      )
      PlayerBackgroundStyle.APPLE_MUSIC -> Modifier.background(
        Brush.horizontalGradient(
          colors = listOf(
            colorScheme.secondaryContainer.copy(alpha = 0.7f),
            colorScheme.surfaceContainerHighest
          )
        )
      )
      PlayerBackgroundStyle.LIVE_MESH -> Modifier.background(
        Brush.linearGradient(
          colors = listOf(
            colorScheme.primary.copy(alpha = 0.45f),
            colorScheme.tertiary.copy(alpha = 0.35f),
            colorScheme.surfaceContainerHighest
          )
        )
      )
      PlayerBackgroundStyle.LIQUID_GLASS -> Modifier
        .background(colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
        .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    }
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .then(backgroundBrushOrColor)
      .padding(horizontal = 10.dp, vertical = 8.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Image(
        painter = painterResource(R.drawable.preview_artwork),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(36.dp)
          .clip(RoundedCornerShape(6.dp))
      )

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.preview_song_title),
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = colorScheme.onSurface
        )
        Text(
          text = stringResource(R.string.preview_song_artist),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = colorScheme.onSurfaceVariant
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          painter = painterResource(R.drawable.play),
          contentDescription = null,
          tint = colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
        Icon(
          painter = painterResource(R.drawable.skip_next),
          contentDescription = null,
          tint = colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

/**
 * Contextual selector dialog for Player Background Styles displaying full-player visual cards.
 */
@Composable
fun PlayerBackgroundStyleModal(
  current: PlayerBackgroundStyle,
  availableStyles: List<PlayerBackgroundStyle>,
  onSelect: (PlayerBackgroundStyle) -> Unit,
  onDismiss: () -> Unit,
  buttonStyle: PlayerButtonsStyle,
  sliderStyle: SliderStyle,
  isSquiggly: Boolean,
  wavyPlayPause: Boolean,
  thumbnailCornerRadius: Dp,
  cropAlbumArt: Boolean,
  fontFamily: FontFamily,
) {
  DefaultDialog(
    onDismiss = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.player_background_style),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
    },
    buttons = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(android.R.string.cancel))
      }
    }
  ) {
    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 500.dp)
        .padding(top = 8.dp)
    ) {
      items(availableStyles) { style ->
        val isSelected = style == current
        Card(
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
          ),
          colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
          ),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              onSelect(style)
              onDismiss()
            }
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
          ) {
            FullPlayerPreview(
              backgroundStyle = style,
              buttonStyle = buttonStyle,
              sliderStyle = sliderStyle,
              isSquiggly = isSquiggly,
              wavyPlayPause = wavyPlayPause,
              thumbnailCornerRadius = thumbnailCornerRadius,
              cropAlbumArt = cropAlbumArt,
              fontFamily = fontFamily,
              modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
              if (isSelected) {
                Icon(
                  painter = painterResource(R.drawable.check),
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
              }
              Text(
                text = playerBackgroundStyleLabel(style),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Contextual selector dialog for Player Button Colors displaying full-player visual cards.
 */
@Composable
fun PlayerButtonsStyleModal(
  current: PlayerButtonsStyle,
  availableStyles: List<PlayerButtonsStyle>,
  onSelect: (PlayerButtonsStyle) -> Unit,
  onDismiss: () -> Unit,
  backgroundStyle: PlayerBackgroundStyle,
  sliderStyle: SliderStyle,
  isSquiggly: Boolean,
  wavyPlayPause: Boolean,
  thumbnailCornerRadius: Dp,
  cropAlbumArt: Boolean,
  fontFamily: FontFamily,
) {
  DefaultDialog(
    onDismiss = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.player_buttons_style),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
    },
    buttons = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(android.R.string.cancel))
      }
    }
  ) {
    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 500.dp)
        .padding(top = 8.dp)
    ) {
      items(availableStyles) { style ->
        val isSelected = style == current
        Card(
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
          ),
          colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
          ),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              onSelect(style)
              onDismiss()
            }
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
          ) {
            FullPlayerPreview(
              backgroundStyle = backgroundStyle,
              buttonStyle = style,
              sliderStyle = sliderStyle,
              isSquiggly = isSquiggly,
              wavyPlayPause = wavyPlayPause,
              thumbnailCornerRadius = thumbnailCornerRadius,
              cropAlbumArt = cropAlbumArt,
              fontFamily = fontFamily,
              modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
              if (isSelected) {
                Icon(
                  painter = painterResource(R.drawable.check),
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
              }
              Text(
                text = playerButtonsStyleLabel(style),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Contextual selector dialog for Mini-Player background styles.
 */
@Composable
fun MiniPlayerBackgroundStyleModal(
  current: PlayerBackgroundStyle,
  availableStyles: List<PlayerBackgroundStyle>,
  onSelect: (PlayerBackgroundStyle) -> Unit,
  onDismiss: () -> Unit,
) {
  DefaultDialog(
    onDismiss = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.miniplayer_background_style),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
    },
    buttons = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(android.R.string.cancel))
      }
    }
  ) {
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 480.dp)
        .padding(top = 8.dp)
    ) {
      items(availableStyles) { style ->
        val isSelected = style == current
        Card(
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
          ),
          colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
          ),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              onSelect(style)
              onDismiss()
            }
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            MiniPlayerPreview(
              backgroundStyle = style,
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 4.dp)
            ) {
              RadioButton(
                selected = isSelected,
                onClick = null
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = playerBackgroundStyleLabel(style),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }
  }
}

private data class PreviewWavyShape(
  val sides: Int = 9,
  val indent: Float = 0.16f,
  val rotationDegrees: Float = 0f,
) : Shape {
  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density
  ): Outline {
    val path = Path()
    val maxRadiusX = size.width / 2f
    val maxRadiusY = size.height / 2f
    val cx = size.width / 2f
    val cy = size.height / 2f

    val steps = 120
    val rotationRad = rotationDegrees * Math.PI / 180.0
    for (i in 0..steps) {
      val angle = i * Math.PI * 2 / steps
      val bumpAngle = angle - rotationRad
      val r = 1f - indent + indent * cos(sides * bumpAngle)
      val x = cx + maxRadiusX * r * cos(angle)
      val y = cy + maxRadiusY * r * sin(angle)
      if (i == 0) path.moveTo(x.toFloat(), y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat())
    }
    path.close()
    return Outline.Generic(path)
  }
}
