package com.music.echo.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import echo.music.iad1tya.R
import com.music.innertube.YouTube
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.utils.Wikipedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SongDnaState(
  val isLoading: Boolean = true,
  val trackTitle: String,
  val albumTitle: String? = null,
  val albumThumbnailUrl: String? = null,
  val releaseDate: String? = null,
  val performers: List<String> = emptyList(),
  val songwriters: List<String> = emptyList(),
  val producers: List<String> = emptyList(),
  val recordLabel: String? = null,
  val artistName: String? = null,
  val artistId: String? = null,
  val artistAvatarUrl: String? = null,
  val artistBio: String? = null,
  val artistListenersOrSubs: String? = null,
  val songStory: String? = null,
  val songStoryTitle: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDnaBottomSheet(
  mediaMetadata: MediaMetadata,
  navController: NavController,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()

  var dnaState by remember(mediaMetadata.id) {
    mutableStateOf(
      SongDnaState(
        isLoading = true,
        trackTitle = mediaMetadata.title,
        albumTitle = mediaMetadata.album?.title,
        albumThumbnailUrl = mediaMetadata.thumbnailUrl,
        performers = mediaMetadata.artists.map { it.name },
        artistName = mediaMetadata.artists.firstOrNull()?.name,
        artistId = mediaMetadata.artists.firstOrNull()?.id,
      )
    )
  }

  LaunchedEffect(mediaMetadata.id) {
    withContext(Dispatchers.IO) {
      val artistPrimary = mediaMetadata.artists.firstOrNull()
      val primaryArtistName = artistPrimary?.name ?: ""
      val primaryArtistId = artistPrimary?.id

      var artistAvatar = mediaMetadata.thumbnailUrl
      var artistBio: String? = null
      var artistListeners: String? = null

      // 1. Fetch artist information from YouTube InnerTube
      if (!primaryArtistId.isNullOrBlank()) {
        YouTube.artist(primaryArtistId).onSuccess { page ->
          artistAvatar = page.artist.thumbnail ?: artistAvatar
          artistListeners = page.monthlyListenerCount ?: page.subscriberCountText
          if (!page.description.isNullOrBlank()) {
            artistBio = page.description
          }
        }
      }

      // 2. Fetch YouTube media description for rich official credits
      val parsedCredits = runCatching {
        val mediaInfoResult = YouTube.getMediaInfo(mediaMetadata.id).getOrNull()
        val desc = mediaInfoResult?.description
        parseCreditsFromDescription(desc)
      }.getOrDefault(ParsedCredits())

      // 3. Fallback to Wikipedia for artist biography if not found on YouTube
      if (artistBio.isNullOrBlank() && primaryArtistName.isNotBlank()) {
        val wikiArtist = Wikipedia.fetchArtistBio(primaryArtistName)
        if (wikiArtist != null && !wikiArtist.extract.isNullOrBlank()) {
          artistBio = wikiArtist.extract
          if (artistAvatar == mediaMetadata.thumbnailUrl && wikiArtist.thumbnail?.source != null) {
            artistAvatar = wikiArtist.thumbnail.source
          }
        }
      }

      // 4. Fetch Behind the Music / Song Story from Wikipedia
      val wikiSong = Wikipedia.fetchSongStory(
        songTitle = mediaMetadata.title,
        artistName = primaryArtistName,
        albumTitle = mediaMetadata.album?.title,
      )

      val allPerformers = (mediaMetadata.artists.map { it.name } + parsedCredits.performers).distinct()
      val allWriters = if (parsedCredits.songwriters.isNotEmpty()) {
        parsedCredits.songwriters
      } else {
        mediaMetadata.artists.map { it.name }
      }

      dnaState = SongDnaState(
        isLoading = false,
        trackTitle = mediaMetadata.title,
        albumTitle = mediaMetadata.album?.title,
        albumThumbnailUrl = mediaMetadata.thumbnailUrl,
        releaseDate = parsedCredits.releaseDate,
        performers = allPerformers,
        songwriters = allWriters,
        producers = parsedCredits.producers,
        recordLabel = parsedCredits.recordLabel,
        artistName = primaryArtistName,
        artistId = primaryArtistId,
        artistAvatarUrl = artistAvatar,
        artistBio = artistBio,
        artistListenersOrSubs = artistListeners,
        songStory = wikiSong?.extract,
        songStoryTitle = wikiSong?.title ?: "About the Song",
      )
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .navigationBarsPadding(),
    ) {
      // Top Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(34.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.songdna),
                contentDescription = null,
                modifier = Modifier
                  .size(20.dp)
                  .clip(CircleShape),
              )
            }
          }
          Spacer(Modifier.width(10.dp))
          Text(
            text = "Song DNA",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = (-0.3).sp,
            ),
          )
        }

        IconButton(
          onClick = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
          },
          modifier = Modifier
            .size(36.dp)
            .background(Color.White.copy(alpha = 0.08f), CircleShape),
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f, fill = false),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // 1. Spotify-Style Track Overview Card
        item {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              AsyncImage(
                model = dnaState.albumThumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .size(64.dp)
                  .clip(RoundedCornerShape(12.dp)),
              )

              Column(
                modifier = Modifier
                  .weight(1f)
                  .padding(start = 14.dp),
              ) {
                Text(
                  text = dnaState.trackTitle,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                  text = dnaState.performers.joinToString(", ").ifEmpty { dnaState.artistName.orEmpty() },
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                if (!dnaState.albumTitle.isNullOrBlank() || !dnaState.releaseDate.isNullOrBlank()) {
                  Spacer(Modifier.height(3.dp))
                  Text(
                    text = listOfNotNull(dnaState.albumTitle, dnaState.releaseDate).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }

        // 2. Spotify-Style About the Artist Card
        item {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "ABOUT THE ARTIST",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
              )
              Spacer(Modifier.height(12.dp))

              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
              ) {
                AsyncImage(
                  model = dnaState.artistAvatarUrl,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                )

                Column(
                  modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                ) {
                  Text(
                    text = dnaState.artistName ?: "Artist",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  if (!dnaState.artistListenersOrSubs.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                      text = dnaState.artistListenersOrSubs.orEmpty(),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }

                if (!dnaState.artistId.isNullOrBlank()) {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable {
                      scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onDismiss()
                        navController.navigate("artist/${dnaState.artistId}")
                      }
                    },
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                      Text(
                        text = "View Artist",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                      )
                      Spacer(Modifier.width(3.dp))
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp),
                      )
                    }
                  }
                }
              }

              Spacer(Modifier.height(12.dp))

              var expandedBio by remember { mutableStateOf(false) }
              val bioText = dnaState.artistBio
              if (!bioText.isNullOrBlank()) {
                Text(
                  text = bioText,
                  style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = if (expandedBio) Int.MAX_VALUE else 4,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.animateContentSize(),
                )
                if (bioText.length > 200) {
                  Spacer(Modifier.height(6.dp))
                  Text(
                    text = if (expandedBio) "Show less" else "Read more",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { expandedBio = !expandedBio },
                  )
                }
              } else if (dnaState.isLoading) {
                Text(
                  text = "Loading artist insights...",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
              } else {
                Text(
                  text = "Biography is not available for this artist yet.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
              }
            }
          }
        }

        // 3. Spotify-Style Credits Card
        item {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "CREDITS",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
              )
              Spacer(Modifier.height(12.dp))

              // Performed By
              CreditRow(
                role = "Performed by",
                names = dnaState.performers.ifEmpty { listOfNotNull(dnaState.artistName) },
                badge = "Artist",
              )

              // Written By
              if (dnaState.songwriters.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                CreditRow(
                  role = "Written by",
                  names = dnaState.songwriters,
                  badge = "Composer / Lyricist",
                )
              }

              // Produced By
              if (dnaState.producers.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                CreditRow(
                  role = "Produced by",
                  names = dnaState.producers,
                  badge = "Producer",
                )
              }

              // Record Label / Source
              if (!dnaState.recordLabel.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                CreditRow(
                  role = "Source",
                  names = listOf(dnaState.recordLabel.orEmpty()),
                  badge = "Record Label",
                )
              }
            }
          }
        }

        // 4. Behind the Music / Track Story Card (when available)
        if (!dnaState.songStory.isNullOrBlank()) {
          item {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "BEHIND THE MUSIC",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                  ),
                  color = MaterialTheme.colorScheme.secondary,
                )
                if (!dnaState.songStoryTitle.isNullOrBlank()) {
                  Spacer(Modifier.height(6.dp))
                  Text(
                    text = dnaState.songStoryTitle.orEmpty(),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                  text = dnaState.songStory.orEmpty(),
                  style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        item {
          Spacer(Modifier.height(16.dp))
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreditRow(
  role: String,
  names: List<String>,
  badge: String,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = role,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.08f),
      ) {
        Text(
          text = badge,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
      }
    }
    Spacer(Modifier.height(4.dp))
    Text(
      text = names.joinToString(", "),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

private data class ParsedCredits(
  val songwriters: List<String> = emptyList(),
  val producers: List<String> = emptyList(),
  val performers: List<String> = emptyList(),
  val recordLabel: String? = null,
  val releaseDate: String? = null,
)

private fun parseCreditsFromDescription(description: String?): ParsedCredits {
  if (description.isNullOrBlank()) return ParsedCredits()
  val songwriters = mutableListOf<String>()
  val producers = mutableListOf<String>()
  val performers = mutableListOf<String>()
  var label: String? = null
  var releaseDate: String? = null

  val lines = description.lines()
  for (line in lines) {
    val trimmed = line.trim()
    when {
      trimmed.startsWith("Provided to YouTube by", ignoreCase = true) -> {
        label = trimmed.removePrefix("Provided to YouTube by").trim()
      }
      trimmed.startsWith("Released on:", ignoreCase = true) -> {
        releaseDate = trimmed.removePrefix("Released on:").trim()
      }
      trimmed.startsWith("Composer:", ignoreCase = true) ||
        trimmed.startsWith("Composer, Producer:", ignoreCase = true) ||
        trimmed.startsWith("Composer, Lyricist:", ignoreCase = true) ||
        trimmed.startsWith("Lyricist:", ignoreCase = true) ||
        trimmed.startsWith("Author:", ignoreCase = true) ||
        trimmed.startsWith("Songwriter:", ignoreCase = true) ||
        trimmed.startsWith("Music Director:", ignoreCase = true) -> {
        val names = trimmed.substringAfter(":").trim().split("·", ",", "/", ";").map { it.trim() }.filter { it.isNotBlank() }
        songwriters.addAll(names)
        if (trimmed.startsWith("Composer, Producer:", ignoreCase = true)) {
          producers.addAll(names)
        }
      }
      trimmed.startsWith("Producer:", ignoreCase = true) ||
        trimmed.startsWith("Produced by:", ignoreCase = true) ||
        trimmed.startsWith("Arranger:", ignoreCase = true) -> {
        val names = trimmed.substringAfter(":").trim().split("·", ",", "/", ";").map { it.trim() }.filter { it.isNotBlank() }
        producers.addAll(names)
      }
      trimmed.startsWith("Associated Performer:", ignoreCase = true) ||
        trimmed.startsWith("Vocals:", ignoreCase = true) -> {
        val names = trimmed.substringAfter(":").trim().split("·", ",", "/", ";").map { it.trim() }.filter { it.isNotBlank() }
        performers.addAll(names)
      }
      trimmed.startsWith("℗") -> {
        if (label == null) {
          label = trimmed.trim()
        }
      }
    }
  }
  return ParsedCredits(
    songwriters = songwriters.distinct(),
    producers = producers.distinct(),
    performers = performers.distinct(),
    recordLabel = label,
    releaseDate = releaseDate,
  )
}