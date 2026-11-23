package echo.music.iad1tya.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.*
import echo.music.iad1tya.ui.component.*
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.utils.rememberEnumPreference

@Composable
fun AmbientSettingsContent(modifier: Modifier = Modifier) {
  val scrollState = rememberScrollState()

  var showArt by rememberPreference(AmbientShowArtKey, true)
  var artScale by rememberPreference(AmbientArtScaleKey, 0.85f)
  var showTitle by rememberPreference(AmbientShowTitleKey, false)
  var showArtist by rememberPreference(AmbientShowArtistKey, false)
  var showLyrics by rememberPreference(AmbientShowLyricsKey, true)
  var spacing by rememberPreference(AmbientSpacingKey, 32f)

  var lyricsTextSize by rememberPreference(LyricsTextSizeKey, 24f)
  var lyricsLineSpacing by rememberPreference(LyricsLineSpacingKey, 1.3f)
  var lyricsAnimationStyle by rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.echomusic_1)

  Column(modifier = modifier.fillMaxWidth().verticalScroll(scrollState)) {
    Material3SettingsGroup(
      scrollState = scrollState,
      title = "Display Options",
      items =
        listOf(
          Material3SettingsItem(
            title = { Text("Show Music Art") },
            description = { Text("Display the album artwork") },
            icon = painterResource(R.drawable.image),
            trailingContent = {
              Switch(checked = showArt, onCheckedChange = { showArt = it })
            },
            onClick = { showArt = !showArt }
          )
        ) + (if (showArt) listOf(
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
          )
        ) else emptyList()) + listOf(
          Material3SettingsItem(
            title = { Text("Spacing Between Art and Lyrics") },
            description = { Text("${spacing.toInt()} dp") },
            icon = painterResource(R.drawable.image),
            trailingContent = {
              Slider(
                value = spacing,
                onValueChange = { spacing = it },
                valueRange = 0f..100f,
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

    if (showLyrics) {
      Spacer(modifier = Modifier.height(16.dp))
      Material3SettingsGroup(
        scrollState = scrollState,
        title = "Lyrics Customization",
        items =
          listOf(
            Material3SettingsItem(
              title = { Text("Lyrics Font Size") },
              description = { Text("${lyricsTextSize.toInt()} sp") },
              icon = painterResource(R.drawable.lyrics),
              trailingContent = {
                Slider(
                  value = lyricsTextSize,
                  onValueChange = { lyricsTextSize = it },
                  valueRange = 12f..48f,
                  modifier = Modifier.width(120.dp)
                )
              },
              onClick = {}
            ),
            Material3SettingsItem(
              title = { Text("Lyrics Line Spacing") },
              description = { Text(String.format("%.1f", lyricsLineSpacing)) },
              icon = painterResource(R.drawable.lyrics),
              trailingContent = {
                Slider(
                  value = lyricsLineSpacing,
                  onValueChange = { lyricsLineSpacing = it },
                  valueRange = 0.5f..2.5f,
                  modifier = Modifier.width(120.dp)
                )
              },
              onClick = {}
            ),
            Material3SettingsItem(
              title = { Text("Lyrics Animation Style") },
              description = { Text(lyricsAnimationStyle.name) },
              icon = painterResource(R.drawable.music_note),
              trailingContent = {
                Button(onClick = {
                  val values = LyricsAnimationStyle.values()
                  lyricsAnimationStyle = values[(lyricsAnimationStyle.ordinal + 1) % values.size]
                }) {
                  Text("Next")
                }
              },
              onClick = {
                  val values = LyricsAnimationStyle.values()
                  lyricsAnimationStyle = values[(lyricsAnimationStyle.ordinal + 1) % values.size]
              }
            )
          )
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbientSettingsScreen(navController: NavController) {
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
    AmbientSettingsContent(modifier = Modifier.fillMaxSize().padding(innerPadding))
  }
}
