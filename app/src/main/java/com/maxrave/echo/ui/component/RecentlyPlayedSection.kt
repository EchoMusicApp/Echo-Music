package iad1tya.echo.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import android.util.Log
import iad1tya.echo.music.R
import iad1tya.echo.music.data.db.entities.AlbumEntity
import iad1tya.echo.music.data.db.entities.ArtistEntity
import iad1tya.echo.music.data.db.entities.PlaylistEntity
import iad1tya.echo.music.data.db.entities.PodcastsEntity
import iad1tya.echo.music.data.db.entities.SongEntity
import iad1tya.echo.music.data.type.RecentlyType
import iad1tya.echo.music.extension.toMediaItem
import iad1tya.echo.music.ui.navigation.destination.list.AlbumDestination
import iad1tya.echo.music.ui.navigation.destination.list.ArtistDestination
import iad1tya.echo.music.ui.navigation.destination.list.PlaylistDestination
import iad1tya.echo.music.ui.theme.typo

@Composable
fun RecentlyPlayedSection(
    recentlyPlayed: List<RecentlyType>,
    navController: NavController,
    onSongClick: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (recentlyPlayed.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = "Recently Played",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Recently Played",
                style = typo.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Recently played items in horizontal scroll
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            items(recentlyPlayed.distinctBy { 
                when (it) {
                    is SongEntity -> it.videoId
                    is PlaylistEntity -> it.id
                    is AlbumEntity -> it.browseId
                    is ArtistEntity -> it.channelId
                    is PodcastsEntity -> it.podcastId
                    else -> it.toString()
                }
            }.take(10)) { item -> // Show 10 unique items horizontally
                RecentlyPlayedItem(
                    item = item,
                    onClick = {
                        when (item) {
                            is SongEntity -> {
                                onSongClick(item)
                            }
                            is PlaylistEntity -> {
                                navController.navigate(PlaylistDestination(item.id))
                            }
                            is AlbumEntity -> {
                                navController.navigate(AlbumDestination(item.browseId))
                            }
                            is ArtistEntity -> {
                                navController.navigate(ArtistDestination(item.channelId))
                            }
                            is PodcastsEntity -> {
                                navController.navigate(PlaylistDestination(item.podcastId))
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RecentlyPlayedItem(
    item: RecentlyType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(140.dp) // Fixed width for horizontal scroll
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Thumbnail - Square aspect ratio
        Box(
            modifier = Modifier
                .size(140.dp) // Fixed square size
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = when (item) {
                    is SongEntity -> item.thumbnails
                    is PlaylistEntity -> item.thumbnails
                    is AlbumEntity -> item.thumbnails
                    is ArtistEntity -> item.thumbnails
                    is PodcastsEntity -> item.thumbnail
                    else -> null
                },
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title - Centered and cleaner
        Text(
            text = when (item) {
                is SongEntity -> item.title
                is PlaylistEntity -> item.title
                is AlbumEntity -> item.title
                is ArtistEntity -> item.name
                is PodcastsEntity -> item.title
                else -> "Unknown"
            },
            style = typo.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Subtitle - Centered and more subtle
        Text(
            text = when (item) {
                is SongEntity -> {
                    val artists = item.artistName?.joinToString(", ") ?: "Unknown Artist"
                    if (artists.length > 20) {
                        artists.take(17) + "..."
                    } else {
                        artists
                    }
                }
                is PlaylistEntity -> "${item.trackCount} tracks"
                is AlbumEntity -> {
                    val artists = item.artistName?.joinToString(", ") ?: "Unknown Artist"
                    if (artists.length > 20) {
                        artists.take(17) + "..."
                    } else {
                        artists
                    }
                }
                is ArtistEntity -> "Artist"
                is PodcastsEntity -> "${item.listEpisodes.size} episodes"
                else -> ""
            },
            style = typo.bodySmall.copy(
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
