package echo.music.iad1tya.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.constants.MUSIC_LANGUAGE_SKIPPED
import echo.music.iad1tya.constants.MusicLanguageOption
import echo.music.iad1tya.constants.MusicLanguageOptions
import echo.music.iad1tya.constants.MusicLanguagesKey
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/** Codes saved in [MusicLanguagesKey], without the "skipped" marker. */
fun parseMusicLanguages(raw: String): List<MusicLanguageOption> =
  raw.split(',').mapNotNull { code -> MusicLanguageOptions.firstOrNull { it.code == code.trim() } }

/** Multi-select language chooser. Used for the first-launch popup and from the AI hub settings. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MusicLanguageDialog(
  initial: List<String>,
  onSave: (List<String>) -> Unit,
  onSkip: (() -> Unit)?,
  onDismiss: () -> Unit,
) {
  var selected by remember { mutableStateOf(initial.toSet()) }
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(28.dp),
    title = { Text("Choose your music languages") },
    text = {
      Column {
        Text(
          "Pick the languages you listen to. Your first Home rows will show songs in these languages. The app itself stays in English.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          MusicLanguageOptions.forEach { option ->
            val on = option.code in selected
            Column(
              Modifier.clip(RoundedCornerShape(18.dp))
                .background(
                  if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                  else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                )
                .border(
                  1.dp,
                  if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                  RoundedCornerShape(18.dp),
                )
                .clickable { selected = if (on) selected - option.code else selected + option.code }
                .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
              Text(option.label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
              Text(
                option.industry,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        enabled = selected.isNotEmpty(),
        onClick = { onSave(MusicLanguageOptions.map { it.code }.filter { it in selected }) },
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = { if (onSkip != null) onSkip() else onDismiss() }) {
        Text(if (onSkip != null) "Skip" else "Cancel")
      }
    },
  )
}

/** Shows the first-launch language popup once for new users. Saving or skipping both end the prompt. */
@Composable
fun FirstLaunchMusicLanguagePrompt() {
  val (raw, setRaw) = rememberPreference(MusicLanguagesKey, defaultValue = "")
  var dismissedThisSession by remember { mutableStateOf(false) }
  if (raw.isEmpty() && !dismissedThisSession) {
    MusicLanguageDialog(
      initial = emptyList(),
      onSave = { setRaw(it.joinToString(",")) },
      onSkip = { setRaw(MUSIC_LANGUAGE_SKIPPED) },
      onDismiss = { dismissedThisSession = true },
    )
  }
}

private suspend fun fetchLanguageSongs(query: String): List<SongItem> =
  withContext(Dispatchers.IO) {
    YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty().filterIsInstance<SongItem>()
  }

/**
 * Two rows of songs in the user's chosen languages for the top of Home. Row one is the first language,
 * row two is the second language (or more songs of the first one). [refreshKey] reshuffles on refresh.
 */
@Composable
fun LanguageSongRows(refreshKey: Any?) {
  val (raw, _) = rememberPreference(MusicLanguagesKey, defaultValue = "")
  val languages = remember(raw) { parseMusicLanguages(raw) }
  if (languages.isEmpty()) return
  val playerConnection = LocalPlayerConnection.current ?: return

  val first = languages.first()
  val second = languages.getOrNull(1)
  val rows by
    produceState<List<Pair<String, List<SongItem>>>>(initialValue = emptyList(), raw, refreshKey) {
      val suffixes = listOf("new", "hits", "top", "popular", "latest", "trending").shuffled()
      value =
        coroutineScope {
          listOf(
              async {
                "${first.label} songs" to fetchLanguageSongs("${first.query} ${suffixes[0]}").shuffled().take(14)
              },
              async {
                val l = second ?: first
                (if (second != null) "${l.label} songs" else "More ${l.label}") to
                  fetchLanguageSongs("${l.query} ${suffixes[1]}").shuffled().take(14)
              },
            )
            .awaitAll()
            .filter { it.second.isNotEmpty() }
        }
    }

  rows.forEach { (title, songs) ->
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
      Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
      )
      LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(songs, key = { "lang_${title}_${it.id}" }) { song ->
          Column(
            Modifier.width(140.dp)
              .clip(RoundedCornerShape(16.dp))
              .clickable {
                playerConnection.playQueue(
                  YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), song.toMediaMetadata())
                )
              }
          ) {
            Box(Modifier.size(140.dp).clip(RoundedCornerShape(16.dp))) {
              AsyncImage(
                model = song.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(140.dp),
              )
            }
            Text(
              song.title,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(top = 6.dp),
            )
            Text(
              song.artists.joinToString { it.name },
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
