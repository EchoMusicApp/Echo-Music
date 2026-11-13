package echo.music.iad1tya.ui.component

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.InnerTubeCookieKey
import echo.music.iad1tya.db.entities.PlaylistEntity
import echo.music.iad1tya.extensions.isSyncEnabled
import echo.music.iad1tya.utils.rememberPreference
import java.time.LocalDateTime
import java.util.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CreatePlaylistDialog(
  onDismiss: () -> Unit,
  initialTextFieldValue: String? = null,
  allowSyncing: Boolean = true,
  isLocal: Boolean = false,
  onPlaylistCreated: ((String) -> Unit)? = null,
) {
  val database = LocalDatabase.current
  val coroutineScope = rememberCoroutineScope()
  var syncedPlaylist by remember { mutableStateOf(false) }
  val context = LocalContext.current

  val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
  val isSignedIn = innerTubeCookie.isNotEmpty()

  TextFieldDialog(
    title = { Text(text = stringResource(R.string.create_playlist)) },
    initialTextFieldValue = TextFieldValue(initialTextFieldValue ?: ""),
    onDismiss = onDismiss,
    onDone = { playlistName ->
      coroutineScope.launch(Dispatchers.IO) {
        val browseId =
          if (syncedPlaylist && isSignedIn) {
            YouTube.createPlaylist(playlistName)
          } else if (syncedPlaylist) {
            Logger.getLogger("CreatePlaylistDialog").warning("Not signed in")
            return@launch
          } else null

        val playlistEntity =
          PlaylistEntity(
            name = playlistName,
            browseId = browseId,
            bookmarkedAt = LocalDateTime.now(),
            isEditable = true,
            isLocal = isLocal,
          )

        database.query { insert(playlistEntity) }

        withContext(Dispatchers.Main) { onPlaylistCreated?.invoke(playlistEntity.id) }
      }
    },
    extraContent = {
      if (allowSyncing) {
        Spacer(modifier = Modifier.padding(top = 16.dp))
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clickable {
                val isYtmSyncEnabled = context.isSyncEnabled()
                if (!isSignedIn && !syncedPlaylist) {
                  Toast.makeText(
                      context,
                      context.getString(R.string.not_logged_in_youtube),
                      Toast.LENGTH_SHORT,
                    )
                    .show()
                } else if (!isYtmSyncEnabled) {
                  Toast.makeText(
                      context,
                      context.getString(R.string.sync_disabled),
                      Toast.LENGTH_SHORT,
                    )
                    .show()
                } else {
                  syncedPlaylist = !syncedPlaylist
                }
              }
              .padding(vertical = 8.dp),
          verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
              text = stringResource(R.string.sync_playlist),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(R.string.allows_for_sync_witch_youtube),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = syncedPlaylist,
            onCheckedChange = {
              val isYtmSyncEnabled = context.isSyncEnabled()
              if (!isSignedIn && !syncedPlaylist) {
                Toast.makeText(
                    context,
                    context.getString(R.string.not_logged_in_youtube),
                    Toast.LENGTH_SHORT,
                  )
                  .show()
              } else if (!isYtmSyncEnabled) {
                Toast.makeText(
                    context,
                    context.getString(R.string.sync_disabled),
                    Toast.LENGTH_SHORT,
                  )
                  .show()
              } else {
                syncedPlaylist = !syncedPlaylist
              }
            },
          )
        }
      }
    },
  )
}
