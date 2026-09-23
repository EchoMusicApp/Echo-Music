package echo.music.iad1tya.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.*
import echo.music.iad1tya.ui.component.*
import echo.music.iad1tya.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbientSettingsScreen(navController: NavController) {
  val scrollState = rememberScrollState()

  var artScale by rememberPreference(AmbientArtScaleKey, 0.85f)
  var showTitle by rememberPreference(AmbientShowTitleKey, false)
  var showArtist by rememberPreference(AmbientShowArtistKey, false)
  var showLyrics by rememberPreference(AmbientShowLyricsKey, true)

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Ambient Mode Settings") },
        navigationIcon = {
          IconButton(onClick = navController::navigateUp) {
            Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Material3SettingsGroup(
        scrollState = scrollState,
        title = "Display Options",
        items = listOf(
          Material3SettingsItem(
            title = { Text("Music Art Size") },
            description = { Text("${(artScale * 100).toInt()}%") },
            icon = painterResource(R.drawable.image),
            trailingContent = {
              Slider(
                value = artScale,
                onValueChange = { artScale = it },
                valueRange = 0.3f..1.0f,
                modifier = Modifier.width(120.dp)
              )
            },
            onClick = {}
          ),
          Material3SettingsItem(
            title = { Text("Show Song Name") },
            description = { Text("Display the current song title") },
            icon = painterResource(R.drawable.music_note),
            trailingContent = {
              Switch(checked = showTitle, onCheckedChange = { showTitle = it })
            },
            onClick = { showTitle = !showTitle }
          ),
          Material3SettingsItem(
            title = { Text("Show Artist Name") },
            description = { Text("Display the current artist name") },
            icon = painterResource(R.drawable.person),
            trailingContent = {
              Switch(checked = showArtist, onCheckedChange = { showArtist = it })
            },
            onClick = { showArtist = !showArtist }
          ),
          Material3SettingsItem(
            title = { Text("Show Lyrics") },
            description = { Text("Display synchronized lyrics if available") },
            icon = painterResource(R.drawable.lyrics),
            trailingContent = {
              Switch(checked = showLyrics, onCheckedChange = { showLyrics = it })
            },
            onClick = { showLyrics = !showLyrics }
          )
        )
      )
    }
  }
}
