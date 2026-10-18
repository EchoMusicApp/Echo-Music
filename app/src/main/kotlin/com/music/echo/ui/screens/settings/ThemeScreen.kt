@file:OptIn(
  ExperimentalMaterial3ExpressiveApi::class,
  ExperimentalLayoutApi::class,
  ExperimentalMaterial3Api::class
)

package echo.music.iad1tya.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.DarkModeKey
import echo.music.iad1tya.constants.DynamicThemeKey
import echo.music.iad1tya.constants.PureBlackKey
import echo.music.iad1tya.constants.PureBlackMiniPlayerKey
import echo.music.iad1tya.constants.SelectedThemeColorKey
import echo.music.iad1tya.ui.theme.DefaultThemeColor
import echo.music.iad1tya.utils.rememberEnumPreference
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.constants.NatureThemeKey
import echo.music.iad1tya.constants.LavaThemeKey
import echo.music.iad1tya.constants.LiquidThemeKey
import echo.music.iad1tya.constants.CityThemeKey
import echo.music.iad1tya.constants.PlayerBackgroundStyle
import echo.music.iad1tya.constants.PlayerBackgroundStyleKey
import androidx.compose.material.icons.rounded.Eco

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeScreen(navController: NavController, highlightKey: String? = null) {
  val (natureTheme, onNatureThemeChange) = rememberPreference(NatureThemeKey, defaultValue = false)
  val (lavaTheme, onLavaThemeChange) = rememberPreference(LavaThemeKey, defaultValue = false)
  val (liquidTheme, onLiquidThemeChange) = rememberPreference(LiquidThemeKey, defaultValue = false)
  val (cityTheme, onCityThemeChange) = rememberPreference(CityThemeKey, defaultValue = false)
  val (_, onPlayerBgChange) =
    echo.music.iad1tya.utils.rememberEnumPreference(
      PlayerBackgroundStyleKey,
      defaultValue = PlayerBackgroundStyle.LIQUID_GLASS,
    )
  val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, DarkMode.ON)
  val (pureBlack, onPureBlackChangeRaw) = rememberPreference(PureBlackKey, defaultValue = false)
  val (_, onPureBlackMiniPlayerChange) =
    rememberPreference(PureBlackMiniPlayerKey, defaultValue = false)

  val onPureBlackChange: (Boolean) -> Unit = { enabled ->
    onPureBlackChangeRaw(enabled)
    onPureBlackMiniPlayerChange(enabled)
  }
  val (selectedThemeColorInt, onSelectedThemeColorChange) =
    rememberPreference(SelectedThemeColorKey, DefaultThemeColor.toArgb())
  val (_, onDynamicThemeChange) = rememberPreference(DynamicThemeKey, defaultValue = false)

  val selectedThemeColor = Color(selectedThemeColorInt)

  val handleColorSelection: (Color) -> Unit = { color ->
    onSelectedThemeColorChange(color.toArgb())
    onDynamicThemeChange(color == DefaultThemeColor)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.theme_colors), fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = { navController.navigateUp() }) {
            Icon(
              painterResource(R.drawable.arrow_back),
              contentDescription = stringResource(R.string.cd_back)
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
          )
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                  MaterialTheme.colorScheme.surface
                )
            )
          )
          .padding(innerPadding),
      contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      item {
        Text(
          text = stringResource(R.string.theme_mode),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            ThemeModeCard(
              modifier = Modifier.weight(1f),
              title = "Pattukal",
              icon = Icons.Rounded.DarkMode,
              isSelected = darkMode != DarkMode.OFF && !pureBlack && !natureTheme && !lavaTheme && !liquidTheme && !cityTheme,
              onClick = {
                onNatureThemeChange(false)
                onLavaThemeChange(false)
                onLiquidThemeChange(false)
                onCityThemeChange(false)
                onDarkModeChange(DarkMode.ON)
                onPureBlackChange(false)
              }
            )
            ThemeModeCard(
              modifier = Modifier.weight(1f),
              title = "Light",
              icon = Icons.Rounded.LightMode,
              isSelected = darkMode == DarkMode.OFF,
              onClick = {
                onNatureThemeChange(false)
                onLavaThemeChange(false)
                onLiquidThemeChange(false)
                onCityThemeChange(false)
                onDarkModeChange(DarkMode.OFF)
                onPureBlackChange(false)
              }
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            ThemeModeCard(
              modifier = Modifier.weight(1f),
              title = "AMOLED",
              icon = Icons.Rounded.Contrast,
              isSelected = pureBlack && !natureTheme && !lavaTheme && !liquidTheme && !cityTheme,
              onClick = {
                onNatureThemeChange(false)
                onLavaThemeChange(false)
                onLiquidThemeChange(false)
                onCityThemeChange(false)
                onDarkModeChange(DarkMode.ON)
                onPureBlackChange(true)
              }
            )
            ThemeModeCard(
              modifier = Modifier.weight(1f),
              title = "Nature",
              icon = Icons.Rounded.Eco,
              isSelected = natureTheme && !pureBlack && !lavaTheme && !liquidTheme && !cityTheme,
              onClick = {
                onNatureThemeChange(true)
                onLavaThemeChange(false)
                onLiquidThemeChange(false)
                onCityThemeChange(false)
                onDarkModeChange(DarkMode.ON)
                onPureBlackChange(false)
              }
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            ThemeModeCard(
              modifier = Modifier.weight(1f),
              title = "AMOLED Lava",
              icon = Icons.Rounded.Contrast,
              isSelected = lavaTheme && !liquidTheme && !cityTheme,
              onClick = {
                onNatureThemeChange(false)
                onLavaThemeChange(true)
                onLiquidThemeChange(false)
                onCityThemeChange(false)
                onDarkModeChange(DarkMode.ON)
                onPureBlackChange(false)
              }
            )
            ThemeModeCard(
              modifier = Modifier.weight(1f),
              title = "Liquid Sound",
              icon = Icons.Rounded.Contrast,
              isSelected = liquidTheme,
              onClick = {
                onNatureThemeChange(false)
                onLavaThemeChange(false)
                onCityThemeChange(false)
                onLiquidThemeChange(true)
                onDarkModeChange(DarkMode.ON)
                onPureBlackChange(false)
                onPlayerBgChange(PlayerBackgroundStyle.LIQUID_SOUND)
              }
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            ThemeModeCard(
              modifier = Modifier.weight(1f),
              title = "Beat City",
              icon = Icons.Rounded.Contrast,
              isSelected = cityTheme,
              onClick = {
                onNatureThemeChange(false)
                onLavaThemeChange(false)
                onLiquidThemeChange(false)
                onCityThemeChange(true)
                onDarkModeChange(DarkMode.ON)
                onPureBlackChange(false)
                onPlayerBgChange(PlayerBackgroundStyle.BEAT_CITY)
              }
            )
            Spacer(Modifier.weight(1f))
          }
        }
      }

      item {
        HorizontalDivider(
          modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
      }

      item {
        Text(
          text = stringResource(R.string.color_palette),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
        )

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(32.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f)
            ),
          elevation = CardDefaults.cardElevation(0.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
          ) {
            val isDynamic = selectedThemeColor == DefaultThemeColor
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .clickable {
                    if (!isDynamic) {
                      handleColorSelection(DefaultThemeColor)
                    } else {
                      handleColorSelection(Color(0xFF1E88E5))
                    }
                  }
                  .padding(8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                Box(
                  modifier =
                    Modifier.size(40.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(MaterialTheme.colorScheme.primaryContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    painter = painterResource(R.drawable.palette),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                  )
                }
                Text(
                  text = stringResource(R.string.palette_dynamic),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              Switch(
                checked = isDynamic,
                onCheckedChange = { checked ->
                  if (checked) {
                    handleColorSelection(DefaultThemeColor)
                  } else {
                    handleColorSelection(Color(0xFF1E88E5))
                  }
                }
              )
            }

            AnimatedVisibility(visible = !isDynamic) {
              HsvColorPicker(
                initialColor =
                  if (selectedThemeColor == DefaultThemeColor) Color(0xFF1E88E5)
                  else selectedThemeColor,
                onColorCommit = { handleColorSelection(it) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun ThemeModeCard(
  modifier: Modifier = Modifier,
  title: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val scale by
    animateFloatAsState(
      targetValue = if (isSelected) 1.02f else 1f,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "scale"
    )
  val borderWidth by
    animateDpAsState(
      targetValue = if (isSelected) 2.dp else 1.dp,
      animationSpec = spring(stiffness = Spring.StiffnessMedium),
      label = "borderWidth"
    )

  val borderColor =
    if (isSelected) {
      MaterialTheme.colorScheme.primary
    } else {
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

  val backgroundBrush =
    if (isSelected) {
      Brush.linearGradient(
        colors =
          listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
          )
      )
    } else {
      Brush.linearGradient(
        colors =
          listOf(
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.surfaceContainerLow
          )
      )
    }

  Box(
    modifier =
      modifier
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .clip(RoundedCornerShape(24.dp))
        .background(backgroundBrush)
        .border(borderWidth, borderColor, RoundedCornerShape(24.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 24.dp, horizontal = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint =
          if (isSelected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(32.dp)
      )
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color =
          if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
fun HsvColorPicker(
  modifier: Modifier = Modifier,
  initialColor: Color,
  onColorCommit: (Color) -> Unit
) {
  var hsv by remember {
    val h = FloatArray(3)
    android.graphics.Color.colorToHSV(initialColor.toArgb(), h)
    mutableStateOf(h)
  }

  LaunchedEffect(initialColor) {
    val currentC = android.graphics.Color.HSVToColor(hsv)
    if (currentC != initialColor.toArgb()) {
      val h = FloatArray(3)
      android.graphics.Color.colorToHSV(initialColor.toArgb(), h)
      hsv = h
    }
  }

  var hue by remember { mutableFloatStateOf(hsv[0]) }
  var saturation by remember { mutableFloatStateOf(hsv[1]) }
  var value by remember { mutableFloatStateOf(hsv[2]) }

  LaunchedEffect(hsv) {
    hue = hsv[0]
    saturation = hsv[1]
    value = hsv[2]
  }

  fun getLocalColor(): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))

  fun commitColor() {
    hsv = floatArrayOf(hue, saturation, value)
    onColorCommit(getLocalColor())
  }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(60.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(getLocalColor())
    )

    CustomColorSlider(
      value = hue,
      onValueChange = { hue = it },
      onValueChangeFinished = { commitColor() },
      valueRange = 0f..360f,
      label = "Hue",
      brush =
        Brush.horizontalGradient(
          colors =
            listOf(
              Color.Red,
              Color.Yellow,
              Color.Green,
              Color.Cyan,
              Color.Blue,
              Color.Magenta,
              Color.Red
            )
        )
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomColorSlider(
  value: Float,
  onValueChange: (Float) -> Unit,
  onValueChangeFinished: () -> Unit,
  valueRange: ClosedFloatingPointRange<Float>,
  label: String,
  brush: Brush
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    Box(
      modifier =
        Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(16.dp)).background(brush),
      contentAlignment = Alignment.CenterStart
    ) {
      Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        colors =
          SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color.Transparent,
            inactiveTrackColor = Color.Transparent,
          ),
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}
