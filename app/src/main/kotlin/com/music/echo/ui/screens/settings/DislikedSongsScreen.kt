package echo.music.iad1tya.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.DislikedSongsKey
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.utils.rememberPreference
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DislikedSongsScreen(navController: NavController) {
  val context = LocalContext.current
  val database = LocalDatabase.current
  val dislikedSongs by rememberPreference(key = DislikedSongsKey, defaultValue = emptySet<String>())
  val coroutineScope = rememberCoroutineScope()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Disliked Songs", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = navController::navigateUp) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
      )
    },
  ) { padding ->
    if (dislikedSongs.isEmpty()) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().padding(padding),
      ) {
        Text(
          text = "No disliked songs",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        val list = dislikedSongs.toList()
        items(list.size, key = { list[it] }) { index ->
          val entry = list[index]
          val parts = entry.split("||")
          val id = parts.getOrNull(0).orEmpty()
          val title = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "Unknown Song"
          val artist = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
          val thumbnailUrl = parts.getOrNull(3)?.takeIf { it.isNotBlank() }

          val shape =
            when {
              list.size == 1 -> RoundedCornerShape(20.dp)
              index == 0 ->
                RoundedCornerShape(
                  topStart = 20.dp,
                  topEnd = 20.dp,
                  bottomStart = 4.dp,
                  bottomEnd = 4.dp,
                )
              index == list.size - 1 ->
                RoundedCornerShape(
                  topStart = 4.dp,
                  topEnd = 4.dp,
                  bottomStart = 20.dp,
                  bottomEnd = 20.dp,
                )
              else -> RoundedCornerShape(4.dp)
            }

          Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            shape = shape,
            colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          ) {
            ListItem(
              colors =
                ListItemDefaults.colors(
                  containerColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
              leadingContent = {
                if (!thumbnailUrl.isNullOrBlank()) {
                  AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                  )
                } else {
                  Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).padding(6.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              },
              headlineContent = {
                Text(
                  text = title,
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              supportingContent = {
                Text(
                  text = artist,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              trailingContent = {
                OutlinedButton(
                  onClick = {
                    coroutineScope.launch {
                      context.dataStore.edit { prefs ->
                        val current = prefs[DislikedSongsKey] ?: emptySet()
                        prefs[DislikedSongsKey] = current - entry
                      }
                      val existingSong = database.song(id).firstOrNull()
                      if (existingSong != null) {
                        database.query {
                          update(existingSong.song.copy(hideFromQuickPicks = false))
                        }
                      }
                    }
                  },
                ) {
                  Text("Remove")
                }
              },
            )
          }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
      }
    }
  }
}
