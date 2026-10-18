package echo.music.iad1tya.ui.immersive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.navigation.NavController
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.*
import echo.music.iad1tya.utils.rememberEnumPreference
import echo.music.iad1tya.utils.rememberPreference

@Composable
private fun SectionTitle(text: String) {
  Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
}

@Composable
private fun OptionRow(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
  Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
  Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    options.forEachIndexed { i, name ->
      val sel = i == selected
      Box(
        Modifier.clip(RoundedCornerShape(50))
          .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
          .clickable { onSelect(i) }
          .padding(horizontal = 14.dp, vertical = 8.dp)
      ) {
        Text(name, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
      }
    }
  }
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
  Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
      Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
      Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
    Switch(checked = checked, onCheckedChange = onChange)
  }
}

@Composable
private fun Swatches(selected: Int, onSelect: (Int) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
    LiquidOptions.customSwatches.forEach { c ->
      Box(
        Modifier.size(32.dp)
          .clip(CircleShape)
          .background(Color(c))
          .border(if (c == selected) 3.dp else 1.dp, if (c == selected) Color.White else Color.White.copy(alpha = 0.3f), CircleShape)
          .clickable { onSelect(c) }
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveSettingsScreen(navController: NavController) {
  val (liquidStyle, setLiquidStyle) = rememberPreference(LiquidStyleKey, 0)
  val (material, setMaterial) = rememberPreference(LiquidMaterialKey, 0)
  val (colorMode, setColorMode) = rememberPreference(LiquidColorModeKey, 0)
  val (custom, setCustom) = rememberPreference(LiquidCustomColorKey, LiquidOptions.customSwatches[0])
  val (intensity, setIntensity) = rememberPreference(LiquidIntensityKey, 1)
  val (bg, setBg) = rememberPreference(LiquidBackgroundKey, 0)
  val (quality, setQuality) = rememberPreference(LiquidQualityKey, 1)
  val (layout, setLayout) = rememberPreference(LiquidLayoutKey, 0)
  val (battery, setBattery) = rememberPreference(ImmersiveBatterySaverKey, false)
  val (reduced, setReduced) = rememberPreference(ImmersiveReducedMotionKey, false)

  val (cityStyle, setCityStyle) = rememberPreference(CityThemeStyleKey, 0)
  val (lighting, setLighting) = rememberPreference(CityLightingKey, 1)
  val (cityIntensity, setCityIntensity) = rememberPreference(CityIntensityKey, 1)
  val (cityLayout, setCityLayout) = rememberPreference(CityLayoutKey, 0)
  val (towerArt, setTowerArt) = rememberPreference(CityTowerArtKey, true)

  val (cinemaMode, setCinemaMode) = rememberPreference(CinemaDefaultModeKey, 0)
  val (parallax, setParallax) = rememberPreference(CinemaParallaxKey, true)

  val (cubeOn, setCubeOn) = rememberPreference(MusicCubeEnabledKey, true)
  val (cubeReduced, setCubeReduced) = rememberPreference(MusicCubeReducedMotionKey, false)
  val (cubeGrid, setCubeGrid) = rememberPreference(MusicCubeGridKey, false)
  val (pitch, setPitch) = rememberPreference(MiniPlayerPitchKey, true)

  var playerBg by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.LIQUID_GLASS)

  val motion = rememberMotionSettings()
  val reactive = rememberReactive(playing = true, motion = motion, active = true)
  val palette = rememberAlbumPalette(null)
  val liquidCfg = rememberLiquidConfig(forceCompact = true)
  val cityCfg = rememberCityConfig(forceCompact = true)

  Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    TopAppBar(
      title = { Text("Immersive", fontWeight = FontWeight.Bold) },
      navigationIcon = {
        IconButton(onClick = { navController.navigateUp() }) {
          Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
        }
      },
      windowInsets = WindowInsets.statusBars,
    )
    Column(
      Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).windowInsetsPadding(WindowInsets.navigationBars)
    ) {
      // ---------- Liquid Sound ----------
      SectionTitle("Liquid Sound")
      Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(22.dp))) {
        LiquidSoundBackground(config = liquidCfg, reactive = reactive, palette = palette, thumbnailUrl = null)
        Text(
          text = "Preview · " + reactiveLabel(reactive.live),
          color = Color.White.copy(alpha = 0.75f),
          fontSize = 11.sp,
          modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        )
      }
      Text(
        "Preview uses ambient motion. In the player it reacts to the real audio of the song when available.",
        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
      )
      SwitchRow(
        "Use as player background", "Show Liquid Sound behind the player",
        playerBg == PlayerBackgroundStyle.LIQUID_SOUND,
      ) { playerBg = if (it) PlayerBackgroundStyle.LIQUID_SOUND else PlayerBackgroundStyle.LIQUID_GLASS }
      OptionRow("Style", LiquidOptions.styles, liquidStyle, setLiquidStyle)
      OptionRow("Material", LiquidOptions.materials, material, setMaterial)
      OptionRow("Colors", LiquidOptions.colorModes, colorMode, setColorMode)
      if (colorMode == 2) Swatches(custom, setCustom)
      OptionRow("Animation intensity", LiquidOptions.intensities, intensity, setIntensity)
      OptionRow("Background", LiquidOptions.backgrounds, bg, setBg)
      OptionRow("Quality", LiquidOptions.qualities, quality, setQuality)
      OptionRow("Layout", LiquidOptions.layouts, layout, setLayout)

      // ---------- Beat City ----------
      SectionTitle("Beat City")
      Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(22.dp))) {
        BeatCityView(config = cityCfg, reactive = reactive, palette = palette, thumbnailUrl = null, motion = motion, interactive = false)
      }
      Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
          .clickable { navController.navigate("beat_city") }
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(Modifier.weight(1f)) {
          Text("Explore the city", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
          Text("Swipe, pinch, and tap buildings to open your music", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
      }
      SwitchRow(
        "Use as player background", "Show Beat City behind the player",
        playerBg == PlayerBackgroundStyle.BEAT_CITY,
      ) { playerBg = if (it) PlayerBackgroundStyle.BEAT_CITY else PlayerBackgroundStyle.LIQUID_GLASS }
      OptionRow("Visual theme", CityOptions.themes, cityStyle, setCityStyle)
      OptionRow("Lighting", CityOptions.lighting, lighting, setLighting)
      OptionRow("Animation intensity", CityOptions.intensities, cityIntensity, setCityIntensity)
      OptionRow("Layout", CityOptions.layouts, cityLayout, setCityLayout)
      SwitchRow("Album art on the tower", "Off shows the Pattukal mark instead", towerArt, setTowerArt)

      // ---------- Cinema ----------
      SectionTitle("Cinema mode")
      OptionRow("Opens in", listOf("Artwork", "Lyrics", "Visualizer", "Minimal"), cinemaMode, setCinemaMode)
      SwitchRow("Parallax depth", "Layers shift gently as you touch the screen", parallax, setParallax)

      // ---------- Music Cube ----------
      SectionTitle("Music Cube")
      SwitchRow("Show on Home", "A small cube beside Quick Picks", cubeOn, setCubeOn)
      SwitchRow("Grid layout", "Plain tiles instead of the 3D cube (used automatically with TalkBack)", cubeGrid, setCubeGrid)
      SwitchRow("Reduced motion", "Snap to faces without spring animation", cubeReduced, setCubeReduced)

      // ---------- Shared ----------
      SectionTitle("Mini player & performance")
      SwitchRow("Pitch bars in mini player", "Live level bars, only while music is playing", pitch, setPitch)
      SwitchRow("Battery-friendly mode", "Caps visualisations at about 30 fps", battery, setBattery)
      SwitchRow("Reduced motion", "Slower, calmer visuals; no beat flashes", reduced, setReduced)
      Spacer(Modifier.height(40.dp))
    }
  }
}

/** Full-screen, interactive Beat City: swipe to explore, pinch to zoom, tap buildings to open music. */
@Composable
fun BeatCityScreen(navController: NavController) {
  val playerConnection = echo.music.iad1tya.LocalPlayerConnection.current
  val isPlaying by (playerConnection?.isPlaying ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
  val meta by (playerConnection?.mediaMetadata ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState()
  val motion = rememberMotionSettings()
  val reactive = rememberReactive(playing = isPlaying, motion = motion, active = true)
  val palette = rememberAlbumPalette(meta?.thumbnailUrl)
  val config = rememberCityConfig(forceCompact = false)
  Box(Modifier.fillMaxSize().background(Color.Black)) {
    BeatCityView(
      config = config,
      reactive = reactive,
      palette = palette,
      thumbnailUrl = meta?.thumbnailUrl,
      motion = motion,
      interactive = true,
      onDestination = { navigateCityDestination(navController, it) },
    )
    Box(
      Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(12.dp).size(44.dp).clip(CircleShape)
        .background(Color.Black.copy(alpha = 0.5f)).clickable { navController.navigateUp() },
      contentAlignment = Alignment.Center,
    ) {
      Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back", tint = Color.White)
    }
    Text(
      text = (if (reactive.live) "Live audio" else "Ambient motion") + " · tap a lit building to open music",
      color = Color.White.copy(alpha = 0.7f),
      fontSize = 11.sp,
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).windowInsetsPadding(WindowInsets.navigationBars),
    )
  }
}
