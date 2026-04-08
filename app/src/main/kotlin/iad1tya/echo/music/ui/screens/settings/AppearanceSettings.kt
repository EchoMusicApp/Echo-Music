package iad1tya.echo.music.ui.screens.settings

import android.os.Build
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.ChipSortTypeKey
import iad1tya.echo.music.constants.DarkModeKey
import iad1tya.echo.music.constants.DefaultOpenTabKey
import iad1tya.echo.music.constants.DensityScale
import iad1tya.echo.music.constants.DensityScaleKey
import iad1tya.echo.music.constants.GridItemSize
import iad1tya.echo.music.constants.GridItemsSizeKey
import iad1tya.echo.music.constants.LibraryFilter
import iad1tya.echo.music.constants.LyricsClickKey
import iad1tya.echo.music.constants.LyricsLineSpacingKey
import iad1tya.echo.music.constants.LyricsScrollKey
import iad1tya.echo.music.constants.LyricsTextPositionKey
import iad1tya.echo.music.constants.LyricsTextSizeKey
import iad1tya.echo.music.constants.LyricsAnimationStyle
import iad1tya.echo.music.constants.LyricsAnimationStyleKey
import iad1tya.echo.music.constants.UseLyricsV2Key
import iad1tya.echo.music.constants.LyricsGlowEffectKey
import iad1tya.echo.music.constants.AppleMusicLyricsBlurKey
import iad1tya.echo.music.constants.ThumbnailCornerRadiusKey
import iad1tya.echo.music.constants.EnableHighRefreshRateKey
import iad1tya.echo.music.constants.HidePlayerThumbnailKey
import iad1tya.echo.music.constants.CropAlbumArtKey
import iad1tya.echo.music.constants.PureBlackMiniPlayerKey
import iad1tya.echo.music.constants.UseSystemFontKey
import iad1tya.echo.music.constants.UseNewMiniPlayerDesignKey
import iad1tya.echo.music.constants.UseNewPlayerDesignKey
import iad1tya.echo.music.constants.PlayerBackgroundStyle
import iad1tya.echo.music.constants.PlayerBackgroundStyleKey
import iad1tya.echo.music.constants.PlayerButtonsStyle
import iad1tya.echo.music.constants.PlayerButtonsStyleKey
import iad1tya.echo.music.constants.UseNewLibraryDesignKey
import iad1tya.echo.music.constants.ArchiveTuneCanvasKey
import iad1tya.echo.music.constants.SliderStyle
import iad1tya.echo.music.constants.SliderStyleKey
import iad1tya.echo.music.constants.SlimNavBarKey
import iad1tya.echo.music.constants.OldNavbarStyleKey
import iad1tya.echo.music.constants.ShowLikedPlaylistKey
import iad1tya.echo.music.constants.ShowDownloadedPlaylistKey
import iad1tya.echo.music.constants.ShowTopPlaylistKey
import iad1tya.echo.music.constants.ShowCachedPlaylistKey
import iad1tya.echo.music.constants.ShowUploadedPlaylistKey
import iad1tya.echo.music.constants.ShowHomeCategoryChipsKey
import iad1tya.echo.music.constants.ShowTagsInLibraryKey
import iad1tya.echo.music.constants.SwipeThumbnailKey
import iad1tya.echo.music.constants.SwipeSensitivityKey
import iad1tya.echo.music.constants.SwipeToSongKey
import iad1tya.echo.music.constants.SwipeToRemoveSongKey
import iad1tya.echo.music.ui.component.DefaultDialog
import iad1tya.echo.music.ui.component.EnumListPreference
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.ListPreference
import iad1tya.echo.music.ui.component.PlayerSliderTrack
import iad1tya.echo.music.ui.component.PreferenceEntry
import iad1tya.echo.music.ui.component.PreferenceGroupTitle
import iad1tya.echo.music.ui.component.SwitchPreference
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.utils.rememberEnumPreference
import iad1tya.echo.music.utils.rememberPreference
import me.saket.squiggles.SquigglySlider
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    // Dark mode preference
    val (darkMode, onDarkModeChange) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO,
    )
    val (pureBlackMiniPlayer, onPureBlackMiniPlayerChange) = rememberPreference(
        PureBlackMiniPlayerKey, defaultValue = false
    )
    val (useNewMiniPlayerDesign, onUseNewMiniPlayerDesignChange) = rememberPreference(
        UseNewMiniPlayerDesignKey, defaultValue = true
    )

    LaunchedEffect(useNewMiniPlayerDesign) {
        if (!useNewMiniPlayerDesign) {
            onUseNewMiniPlayerDesignChange(true)
        }
    }
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(
        HidePlayerThumbnailKey, defaultValue = false
    )
    val (cropAlbumArt, onCropAlbumArtChange) = rememberPreference(
        CropAlbumArtKey, defaultValue = false
    )
    val (archiveTuneCanvasEnabled, onArchiveTuneCanvasEnabledChange) = rememberPreference(
        ArchiveTuneCanvasKey,
        defaultValue = false
    )
    val (thumbnailCornerRadius, onThumbnailCornerRadiusChange) = rememberPreference(
        ThumbnailCornerRadiusKey, defaultValue = 3f
    )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.BLUR,
        )
    val (useNewLibraryDesign, onUseNewLibraryDesignChange) = rememberPreference(
        UseNewLibraryDesignKey,
        defaultValue = false
    )
    val (useNewPlayerDesign, onUseNewPlayerDesignChange) = rememberPreference(
        UseNewPlayerDesignKey,
        defaultValue = true
    )
    val (useSystemFont, onUseSystemFontChange) = rememberPreference(
        UseSystemFontKey,
        defaultValue = false
    )
    val (enableHighRefreshRate, onEnableHighRefreshRateChange) = rememberPreference(
        EnableHighRefreshRateKey,
        defaultValue = false
    )
    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(
        SliderStyleKey,
        defaultValue = SliderStyle.DEFAULT
    )
    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(
        PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )
    val (lyricsTextPosition, onLyricsTextPositionChange) = rememberEnumPreference(
        LyricsTextPositionKey,
        defaultValue = LyricsPosition.CENTER
    )
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(
        DefaultOpenTabKey,
        defaultValue = NavigationTab.HOME
    )
    val (appleMusicLyricsBlur, onAppleMusicLyricsBlurChange) = rememberPreference(
        AppleMusicLyricsBlurKey,
        defaultValue = true
    )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) = rememberPreference(LyricsScrollKey, defaultValue = true)
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, defaultValue = 20f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, defaultValue = 2f)
    val (lyricsAnimationStyle, onLyricsAnimationStyleChange) = rememberEnumPreference(
        LyricsAnimationStyleKey, defaultValue = LyricsAnimationStyle.VIVIMUSIC_1
    )
    val (useLyricsV2, onUseLyricsV2Change) = rememberPreference(
        UseLyricsV2Key,
        defaultValue = false
    )
    val (lyricsGlowEffect, onLyricsGlowEffectChange) = rememberPreference(
        LyricsGlowEffectKey, defaultValue = false
    )
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(
        SwipeThumbnailKey,
        defaultValue = true
    )
    val (swipeSensitivity, onSwipeSensitivityChange) = rememberPreference(
        SwipeSensitivityKey,
        defaultValue = 0.73f
    )
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(
        GridItemsSizeKey,
        defaultValue = GridItemSize.SMALL
    )

    val (slimNav, onSlimNavChange) = rememberPreference(
        SlimNavBarKey,
        defaultValue = false
    )
    val (oldNavbarStyle, onOldNavbarStyleChange) = rememberPreference(
        OldNavbarStyleKey,
        defaultValue = false
    )
    val (swipeToSong, onSwipeToSongChange) = rememberPreference(
        SwipeToSongKey,
        defaultValue = false
    )

    val (swipeToRemoveSong, onSwipeToRemoveSongChange) = rememberPreference(
        SwipeToRemoveSongKey,
        defaultValue = false
    )

    val (showLikedPlaylist, onShowLikedPlaylistChange) = rememberPreference(
        ShowLikedPlaylistKey,
        defaultValue = true
    )
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) = rememberPreference(
        ShowDownloadedPlaylistKey,
        defaultValue = true
    )
    val (showTopPlaylist, onShowTopPlaylistChange) = rememberPreference(
        ShowTopPlaylistKey,
        defaultValue = true
    )
    val (showCachedPlaylist, onShowCachedPlaylistChange) = rememberPreference(
        ShowCachedPlaylistKey,
        defaultValue = true
    )
    val (showUploadedPlaylist, onShowUploadedPlaylistChange) = rememberPreference(
        ShowUploadedPlaylistKey,
        defaultValue = true
    )
    val (showHomeCategoryChips, onShowHomeCategoryChipsChange) = rememberPreference(
        ShowHomeCategoryChipsKey,
        defaultValue = true
    )
    val (showTagsInLibrary, onShowTagsInLibraryChange) = rememberPreference(
        ShowTagsInLibraryKey,
        defaultValue = true
    )

    val availableBackgroundStyles = PlayerBackgroundStyle.entries

    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(
        key = ChipSortTypeKey,
        defaultValue = LibraryFilter.LIBRARY
    )

    val sharedPreferences = remember { context.getSharedPreferences("echo_music_settings", Context.MODE_PRIVATE) }
    val prefDensityScale = remember(sharedPreferences) {
        sharedPreferences.getFloat("density_scale_factor", 1.0f)
    }
    val (densityScale, setDensityScale) = rememberPreference(DensityScaleKey, defaultValue = prefDensityScale)
    var showRestartDialog by rememberSaveable { mutableStateOf(false) }
    var showDensityScaleDialog by rememberSaveable { mutableStateOf(false) }

    val onDensityScaleChange: (Float) -> Unit = { newScale ->
        setDensityScale(newScale)
        sharedPreferences.edit().putFloat("density_scale_factor", newScale).apply()
        showRestartDialog = true
    }

    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.DEFAULT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.DEFAULT)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.default_),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.SQUIGGLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.SQUIGGLY)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    SquigglySlider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.squiggly),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.SLIM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.SLIM)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = SliderDefaults.colors()
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {}
                                )
                            }
                    )

                    Text(
                        text = stringResource(R.string.slim),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.theme),
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.dark_mode)) },
            icon = { Icon(painterResource(R.drawable.dark_mode), null) },
            selectedValue = darkMode,
            onValueSelected = onDarkModeChange,
            valueText = {
                when (it) {
                    DarkMode.ON -> stringResource(R.string.dark_mode_on)
                    DarkMode.OFF -> stringResource(R.string.dark_mode_off)
                    DarkMode.AUTO -> stringResource(R.string.dark_mode_auto)
                }
            },
        )

        SwitchPreference(
            title = { Text("Use System Font") },
            description = "Use the device font instead of app typography",
            icon = { Icon(painterResource(R.drawable.palette), null) },
            checked = useSystemFont,
            onCheckedChange = onUseSystemFontChange,
        )

        PreferenceEntry(
            title = { Text("UI Density Scale") },
            description = "Current: ${DensityScale.fromValue(densityScale).label}",
            icon = { Icon(painterResource(R.drawable.tune), null) },
            onClick = { showDensityScaleDialog = true },
        )

        SwitchPreference(
            title = { Text("High Refresh Rate") },
            description = "Enable higher frame rate for smoother animations",
            icon = { Icon(painterResource(R.drawable.speed), null) },
            checked = enableHighRefreshRate,
            onCheckedChange = onEnableHighRefreshRateChange,
        )

        if (showDensityScaleDialog) {
            DefaultDialog(
                onDismiss = { showDensityScaleDialog = false },
                buttons = {
                    TextButton(onClick = { showDensityScaleDialog = false }) {
                        Text(text = stringResource(android.R.string.cancel))
                    }
                }
            ) {
                Column {
                    DensityScale.entries.forEach { scale ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDensityScaleChange(scale.value)
                                    showDensityScaleDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = scale.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (densityScale == scale.value)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        if (showRestartDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showRestartDialog = false },
                title = { Text("Restart Required") },
                text = { Text("The app needs to restart for the density change to take effect.") },
                confirmButton = {
                    TextButton(onClick = {
                        showRestartDialog = false
                        // Restart the app
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        Runtime.getRuntime().exit(0)
                    }) {
                        Text("Restart Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestartDialog = false }) {
                        Text("Later")
                    }
                }
            )
        }

        PreferenceGroupTitle(
            title = stringResource(R.string.player),
        )

        SwitchPreference(
            title = { Text("New Library Design") },
            description = "Enable the new library design",
            icon = { Icon(painterResource(R.drawable.grid_view), null) },
            checked = useNewLibraryDesign,
            onCheckedChange = onUseNewLibraryDesignChange,
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.player_background_style)) },
            icon = { Icon(painterResource(R.drawable.gradient), null) },
            selectedValue = playerBackground,
            onValueSelected = onPlayerBackgroundChange,
            valueText = {
                when (it) {
                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    PlayerBackgroundStyle.BLUR -> "Blur"
                    PlayerBackgroundStyle.GLOW_ANIMATED -> "Glow Animated"
                }
            },
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.player_buttons_style)) },
            icon = { Icon(painterResource(R.drawable.palette), null) },
            selectedValue = playerButtonsStyle,
            onValueSelected = onPlayerButtonsStyleChange,
            valueText = {
                when (it) {
                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                    PlayerButtonsStyle.SECONDARY -> stringResource(R.string.secondary_color_style)
                    PlayerButtonsStyle.TERTIARY -> "Tertiary"
                }
            },
        )

        SwitchPreference(
            title = { Text("Hide Player Thumbnail") },
            description = "Hide album art in the player",
            icon = { Icon(painterResource(R.drawable.hide_image), null) },
            checked = hidePlayerThumbnail,
            onCheckedChange = onHidePlayerThumbnailChange,
        )

        SwitchPreference(
            title = { Text("Crop Thumbnail to Square") },
            description = "Crop album art to a square",
            icon = { Icon(painterResource(R.drawable.insert_photo), null) },
            checked = cropAlbumArt,
            onCheckedChange = onCropAlbumArtChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.archivetune_canvas)) },
            description = stringResource(R.string.archivetune_canvas_desc),
            icon = { Icon(painterResource(R.drawable.insert_photo), null) },
            checked = archiveTuneCanvasEnabled,
            onCheckedChange = onArchiveTuneCanvasEnabledChange,
        )

        // Thumbnail corner radius slider
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Thumbnail corner radius: ${thumbnailCornerRadius.roundToInt()}dp",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.Slider(
                    value = thumbnailCornerRadius,
                    onValueChange = onThumbnailCornerRadiusChange,
                    valueRange = 0f..32f,
                )
            }
        }

        PreferenceEntry(
            title = { Text(stringResource(R.string.player_slider_style)) },
            description =
                when (sliderStyle) {
                    SliderStyle.DEFAULT -> stringResource(R.string.default_)
                    SliderStyle.SQUIGGLY -> stringResource(R.string.squiggly)
                    SliderStyle.SLIM -> stringResource(R.string.slim)
                },
            icon = { Icon(painterResource(R.drawable.sliders), null) },
            onClick = {
                showSliderOptionDialog = true
            },
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
            icon = { Icon(painterResource(R.drawable.swipe), null) },
            checked = swipeThumbnail,
            onCheckedChange = onSwipeThumbnailChange,
        )

        AnimatedVisibility(swipeThumbnail) {
            var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }
            
            if (showSensitivityDialog) {
                var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }
                
                DefaultDialog(
                    onDismiss = { 
                        tempSensitivity = swipeSensitivity
                        showSensitivityDialog = false 
                    },
                    buttons = {
                        TextButton(
                            onClick = { 
                                tempSensitivity = 0.73f
                            }
                        ) {
                            Text(stringResource(R.string.reset))
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        TextButton(
                            onClick = { 
                                tempSensitivity = swipeSensitivity
                                showSensitivityDialog = false 
                            }
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }
                        TextButton(
                            onClick = { 
                                onSwipeSensitivityChange(tempSensitivity)
                                showSensitivityDialog = false 
                            }
                        ) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.swipe_sensitivity),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
    
                        Text(
                            text = stringResource(R.string.sensitivity_percentage, (tempSensitivity * 100).roundToInt()),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
    
                        Slider(
                            value = tempSensitivity,
                            onValueChange = { tempSensitivity = it },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            PreferenceEntry(
                title = { Text(stringResource(R.string.swipe_sensitivity)) },
                description = stringResource(R.string.sensitivity_percentage, (swipeSensitivity * 100).roundToInt()),
                icon = { Icon(painterResource(R.drawable.tune), null) },
                onClick = { showSensitivityDialog = true }
            )
        }

        EnumListPreference(
            title = { Text(stringResource(R.string.lyrics_text_position)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            selectedValue = lyricsTextPosition,
            onValueSelected = onLyricsTextPositionChange,
            valueText = {
                when (it) {
                    LyricsPosition.LEFT -> stringResource(R.string.left)
                    LyricsPosition.CENTER -> stringResource(R.string.center)
                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                }
            },
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_click_change)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = lyricsClick,
            onCheckedChange = onLyricsClickChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = lyricsScroll,
            onCheckedChange = onLyricsScrollChange,
        )

        EnumListPreference(
            title = { Text("Lyrics Animation Style") },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            selectedValue = lyricsAnimationStyle,
            onValueSelected = onLyricsAnimationStyleChange,
            valueText = {
                when (it) {
                    LyricsAnimationStyle.NONE -> "None"
                    LyricsAnimationStyle.FADE -> "Fade"
                    LyricsAnimationStyle.GLOW -> "Glow"
                    LyricsAnimationStyle.SLIDE -> "Slide"
                    LyricsAnimationStyle.KARAOKE -> "Karaoke"
                    LyricsAnimationStyle.APPLE -> "Apple"
                    LyricsAnimationStyle.VIVIMUSIC_1 -> "Glowing Words"
                }
            },
        )

        SwitchPreference(
            title = { Text("Lyrics V2 (Experimental)") },
            description = "Enable Lyrics V2 engine",
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = useLyricsV2,
            onCheckedChange = onUseLyricsV2Change,
        )

        SwitchPreference(
            title = { Text("Lyrics Glow Effect") },
            description = "Add glow effect to active lyrics line",
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = lyricsGlowEffect,
            onCheckedChange = onLyricsGlowEffectChange,
        )

        AnimatedVisibility(lyricsAnimationStyle == LyricsAnimationStyle.VIVIMUSIC_1) {
            SwitchPreference(
                title = { Text("Apple Music Lyrics Blur") },
                description = "Progressive blur on inactive lyrics lines",
                icon = { Icon(painterResource(R.drawable.lyrics), null) },
                checked = appleMusicLyricsBlur,
                onCheckedChange = onAppleMusicLyricsBlurChange,
            )
        }

        // Lyrics text size slider
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Lyrics text size: ${lyricsTextSize.toInt()}sp",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.Slider(
                    value = lyricsTextSize,
                    onValueChange = onLyricsTextSizeChange,
                    valueRange = 12f..40f,
                )
            }
        }

        // Lyrics line spacing slider
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Lyrics line spacing: ${lyricsLineSpacing.toInt()}dp",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.Slider(
                    value = lyricsLineSpacing,
                    onValueChange = onLyricsLineSpacingChange,
                    valueRange = 0f..24f,
                )
            }
        }

        PreferenceGroupTitle(
            title = stringResource(R.string.misc),
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.default_open_tab)) },
            icon = { Icon(painterResource(R.drawable.nav_bar), null) },
            selectedValue = defaultOpenTab,
            onValueSelected = onDefaultOpenTabChange,
            valueText = {
                when (it) {
                    NavigationTab.HOME -> stringResource(R.string.home)
                    NavigationTab.SEARCH -> stringResource(R.string.search)
                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                    NavigationTab.FIND -> stringResource(R.string.find_song)
                }
            },
        )

        ListPreference(
            title = { Text(stringResource(R.string.default_lib_chips)) },
            icon = { Icon(painterResource(R.drawable.tab), null) },
            selectedValue = defaultChip,
            values = listOf(
                LibraryFilter.LIBRARY, LibraryFilter.PLAYLISTS, LibraryFilter.SONGS,
                LibraryFilter.ALBUMS, LibraryFilter.ARTISTS, LibraryFilter.LOCAL_MEDIA
            ),
            valueText = {
                when (it) {
                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                    LibraryFilter.LOCAL_MEDIA -> stringResource(R.string.local_media)
                }
            },
            onValueSelected = onDefaultChipChange,
        )

        SwitchPreference(
            title = { Text("Show Home Category Chips") },
            description = "Display quick category chips on home",
            icon = { Icon(painterResource(R.drawable.home_outlined), null) },
            checked = showHomeCategoryChips,
            onCheckedChange = onShowHomeCategoryChipsChange,
        )

        SwitchPreference(
            title = { Text("Show Tags in Library") },
            description = "Show tag filters in library views",
            icon = { Icon(painterResource(R.drawable.tune), null) },
            checked = showTagsInLibrary,
            onCheckedChange = onShowTagsInLibraryChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.swipe_song_to_add)) },
            icon = { Icon(painterResource(R.drawable.swipe), null) },
            checked = swipeToSong,
            onCheckedChange = onSwipeToSongChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.swipe_song_to_remove)) },
            icon = { Icon(painterResource(R.drawable.swipe), null) },
            checked = swipeToRemoveSong,
            onCheckedChange = onSwipeToRemoveSongChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.slim_navbar)) },
            icon = { Icon(painterResource(R.drawable.nav_bar), null) },
            checked = slimNav,
            onCheckedChange = onSlimNavChange
        )

        SwitchPreference(
            title = { Text("Old Navbar") },
            description = "Switch back to the previous navbar style",
            icon = { Icon(painterResource(R.drawable.nav_bar), null) },
            checked = oldNavbarStyle,
            onCheckedChange = onOldNavbarStyleChange
        )

        SwitchPreference(
            title = { Text("Old Music Page") },
            description = "Use the previous music player page layout",
            icon = { Icon(painterResource(R.drawable.queue_music), null) },
            checked = !useNewPlayerDesign,
            onCheckedChange = { useOldMusicPage -> onUseNewPlayerDesignChange(!useOldMusicPage) }
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.grid_cell_size)) },
            icon = { Icon(painterResource(R.drawable.grid_view), null) },
            selectedValue = gridItemSize,
            onValueSelected = onGridItemSizeChange,
            valueText = {
                when (it) {
                    GridItemSize.BIG -> stringResource(R.string.big)
                    GridItemSize.SMALL -> stringResource(R.string.small)
                }
            },
        )

        val (showFindInNavbar, onShowFindInNavbarChange) = rememberPreference(
            iad1tya.echo.music.constants.ShowFindInNavbarKey,
            defaultValue = true
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.find_song)) },
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            checked = showFindInNavbar,
            onCheckedChange = onShowFindInNavbarChange
        )
    }

    Box {
        // Blurred gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .zIndex(10f)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.graphicsLayer {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                36f,
                                36f,
                                android.graphics.Shader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                        }
                    } else {
                        Modifier
                    }
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        TopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.appearance),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily(Font(R.font.zalando_sans_expanded)),
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            modifier = Modifier.zIndex(11f)
        )
    }
}

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
    FIND,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}
