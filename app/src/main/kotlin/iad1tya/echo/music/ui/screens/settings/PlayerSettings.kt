package iad1tya.echo.music.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.AudioNormalizationKey
import iad1tya.echo.music.constants.AudioQuality
import iad1tya.echo.music.constants.AudioQualityKey
import iad1tya.echo.music.constants.AudioOffload
import iad1tya.echo.music.constants.AutoDownloadOnLikeKey
import iad1tya.echo.music.constants.AutoLoadMoreKey
import iad1tya.echo.music.constants.CrossfadeDurationKey
import iad1tya.echo.music.constants.CrossfadeEnabledKey
import iad1tya.echo.music.constants.CrossfadeGaplessKey
import iad1tya.echo.music.constants.DisableLoadMoreWhenRepeatAllKey
import iad1tya.echo.music.constants.AutoSkipNextOnErrorKey
import iad1tya.echo.music.constants.DoubleTapToLikeKey
import iad1tya.echo.music.constants.DownloadAutoRetryKey
import iad1tya.echo.music.constants.DownloadChargingOnlyKey
import iad1tya.echo.music.constants.DownloadRetryLimitKey
import iad1tya.echo.music.constants.DownloadWifiOnlyKey
import iad1tya.echo.music.constants.GestureDoubleTapSeekKey
import iad1tya.echo.music.constants.GestureVerticalControlsKey
import iad1tya.echo.music.constants.KeepScreenOn
import iad1tya.echo.music.constants.MusicHapticsEnabledKey
import iad1tya.echo.music.constants.PauseOnMute
import iad1tya.echo.music.constants.PersistentQueueKey
import iad1tya.echo.music.constants.PreventDuplicateTracksInQueueKey
import iad1tya.echo.music.constants.RememberShuffleAndRepeatKey
import iad1tya.echo.music.constants.ResumeOnBluetoothConnectKey
import iad1tya.echo.music.constants.SimilarContent
import iad1tya.echo.music.constants.SkipSilenceKey
import iad1tya.echo.music.constants.AudioEngineMode
import iad1tya.echo.music.constants.AudioEngineModeKey
import iad1tya.echo.music.constants.ProEqEnabledKey
import iad1tya.echo.music.constants.ProEqGainDbKey
import iad1tya.echo.music.constants.SpatialAudioEnabledKey
import iad1tya.echo.music.constants.SpatialAudioStrengthKey
import iad1tya.echo.music.constants.AudioArEnabledKey
import iad1tya.echo.music.constants.AudioArAutoCalibrateKey
import iad1tya.echo.music.constants.AudioArSensitivityKey
import iad1tya.echo.music.constants.ForceStopOnTaskClearKey
import iad1tya.echo.music.constants.StopMusicOnTaskClearKey
import iad1tya.echo.music.constants.TTSAnnouncementEnabledKey
import iad1tya.echo.music.constants.TapAlbumArtForLyricsKey
import iad1tya.echo.music.constants.HistoryDuration
import iad1tya.echo.music.constants.SeekExtraSeconds
import iad1tya.echo.music.ui.component.EnumListPreference
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.ListPreference
import iad1tya.echo.music.ui.component.PreferenceGroupTitle
import iad1tya.echo.music.ui.component.SliderPreference
import iad1tya.echo.music.ui.component.SwitchPreference
import iad1tya.echo.music.ui.component.ButtonPreference
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.utils.rememberEnumPreference
import iad1tya.echo.music.utils.rememberPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val onRecenterAudioClick = remember { { } }

    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )

    val (persistentQueue, onPersistentQueueChange) = rememberPreference(
        PersistentQueueKey,
        defaultValue = true
    )
    val (skipSilence, onSkipSilenceChange) = rememberPreference(
        SkipSilenceKey,
        defaultValue = false
    )
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(
        AudioNormalizationKey,
        defaultValue = true
    )

    val (audioOffload, onAudioOffloadChange) = rememberPreference(
        key = AudioOffload,
        defaultValue = false
    )
    val (audioEngineMode, onAudioEngineModeChange) = rememberEnumPreference(
        AudioEngineModeKey,
        defaultValue = AudioEngineMode.STANDARD
    )
    val (proEqEnabled, onProEqEnabledChange) = rememberPreference(
        ProEqEnabledKey,
        defaultValue = false
    )
    val (proEqGain, onProEqGainChange) = rememberPreference(
        ProEqGainDbKey,
        defaultValue = 0f
    )
    val (spatialAudioEnabled, onSpatialAudioEnabledChange) = rememberPreference(
        SpatialAudioEnabledKey,
        defaultValue = false
    )
    val (spatialAudioStrength, onSpatialAudioStrengthChange) = rememberPreference(
        SpatialAudioStrengthKey,
        defaultValue = 500
    )
    val (audioArEnabled, onAudioArEnabledChange) = rememberPreference(
        AudioArEnabledKey,
        defaultValue = false
    )
    val (audioArAutoCalibrate, onAudioArAutoCalibrateChange) = rememberPreference(
        AudioArAutoCalibrateKey,
        defaultValue = false
    )
    val (audioArSensitivity, onAudioArSensitivityChange) = rememberPreference(
        AudioArSensitivityKey,
        defaultValue = 1f
    )

    val (seekExtraSeconds, onSeekExtraSeconds) = rememberPreference(
        SeekExtraSeconds,
        defaultValue = false
    )

    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(
        AutoLoadMoreKey,
        defaultValue = true
    )
    val (disableLoadMoreWhenRepeatAll, onDisableLoadMoreWhenRepeatAllChange) = rememberPreference(
        DisableLoadMoreWhenRepeatAllKey,
        defaultValue = false
    )
    val (autoDownloadOnLike, onAutoDownloadOnLikeChange) = rememberPreference(
        AutoDownloadOnLikeKey,
        defaultValue = false
    )
    val (similarContentEnabled, similarContentEnabledChange) = rememberPreference(
        key = SimilarContent,
        defaultValue = true
    )
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(
        AutoSkipNextOnErrorKey,
        defaultValue = false
    )
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(
        StopMusicOnTaskClearKey,
        defaultValue = true
    )
    val (forceStopOnTaskClear, onForceStopOnTaskClearChange) = rememberPreference(
        ForceStopOnTaskClearKey,
        defaultValue = false
    )
    val (tapAlbumArtForLyrics, onTapAlbumArtForLyricsChange) = rememberPreference(
        TapAlbumArtForLyricsKey,
        defaultValue = false
    )
    val (doubleTapToLike, onDoubleTapToLikeChange) = rememberPreference(
        DoubleTapToLikeKey,
        defaultValue = false
    )
    val (gestureDoubleTapSeek, onGestureDoubleTapSeekChange) = rememberPreference(
        GestureDoubleTapSeekKey,
        defaultValue = true
    )
    val (gestureVerticalControls, onGestureVerticalControlsChange) = rememberPreference(
        GestureVerticalControlsKey,
        defaultValue = false
    )
    val (historyDuration, onHistoryDurationChange) = rememberPreference(
        HistoryDuration,
        defaultValue = 30f
    )

    // New feature preferences
    val (crossfadeEnabled, onCrossfadeEnabledChange) = rememberPreference(
        CrossfadeEnabledKey,
        defaultValue = false
    )
    val (crossfadeDuration, onCrossfadeDurationChange) = rememberPreference(
        CrossfadeDurationKey,
        defaultValue = 3f
    )
    val (crossfadeGapless, onCrossfadeGaplessChange) = rememberPreference(
        CrossfadeGaplessKey,
        defaultValue = false
    )
    val (resumeOnBluetooth, onResumeOnBluetoothChange) = rememberPreference(
        ResumeOnBluetoothConnectKey,
        defaultValue = false
    )
    val (pauseOnMute, onPauseOnMuteChange) = rememberPreference(
        PauseOnMute,
        defaultValue = false
    )
    val (keepScreenOn, onKeepScreenOnChange) = rememberPreference(
        KeepScreenOn,
        defaultValue = false
    )
    val (preventDuplicates, onPreventDuplicatesChange) = rememberPreference(
        PreventDuplicateTracksInQueueKey,
        defaultValue = false
    )
    val (rememberShuffleRepeat, onRememberShuffleRepeatChange) = rememberPreference(
        RememberShuffleAndRepeatKey,
        defaultValue = true
    )

    val (ttsAnnouncementEnabled, onTtsAnnouncementEnabledChange) = rememberPreference(
        TTSAnnouncementEnabledKey,
        defaultValue = false
    )

    val (musicHapticsEnabled, onMusicHapticsEnabledChange) = rememberPreference(
        MusicHapticsEnabledKey,
        defaultValue = false
    )
    val (downloadWifiOnly, onDownloadWifiOnlyChange) = rememberPreference(
        DownloadWifiOnlyKey,
        defaultValue = false
    )
    val (downloadChargingOnly, onDownloadChargingOnlyChange) = rememberPreference(
        DownloadChargingOnlyKey,
        defaultValue = false
    )
    val (downloadAutoRetry, onDownloadAutoRetryChange) = rememberPreference(
        DownloadAutoRetryKey,
        defaultValue = true
    )
    val (downloadRetryLimit, onDownloadRetryLimitChange) = rememberPreference(
        DownloadRetryLimitKey,
        defaultValue = 2
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.player)
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.audio_quality)) },
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            selectedValue = audioQuality,
            onValueSelected = onAudioQualityChange,
            valueText = {
                when (it) {
                    AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                    AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                    AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                }
            }
        )

        SliderPreference(
            title = { Text(stringResource(R.string.history_duration)) },
            icon = { Icon(painterResource(R.drawable.history), null) },
            value = historyDuration,
            onValueChange = onHistoryDurationChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.skip_silence)) },
            icon = { Icon(painterResource(R.drawable.fast_forward), null) },
            checked = skipSilence,
            onCheckedChange = onSkipSilenceChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.audio_normalization)) },
            icon = { Icon(painterResource(R.drawable.volume_up), null) },
            checked = audioNormalization,
            onCheckedChange = onAudioNormalizationChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.audio_offload)) },
            description = stringResource(R.string.audio_offload_description),
            icon = { Icon(painterResource(R.drawable.speed), null) },
            checked = audioOffload,
            onCheckedChange = onAudioOffloadChange
        )

        EnumListPreference(
            title = { Text("Audio engine") },
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            selectedValue = audioEngineMode,
            onValueSelected = onAudioEngineModeChange,
            valueText = {
                when (it) {
                    AudioEngineMode.STANDARD -> "Standard"
                    AudioEngineMode.HIFI_EXPERIMENTAL -> "Hi-Fi Experimental"
                }
            }
        )

        SwitchPreference(
            title = { Text("Pro EQ") },
            description = "Enable gain staging and louder/cleaner response profile",
            icon = { Icon(painterResource(R.drawable.equalizer), null) },
            checked = proEqEnabled,
            onCheckedChange = onProEqEnabledChange
        )

        if (proEqEnabled) {
            val preampOptions = listOf(-8f, -4f, 0f, 4f, 8f)
            ListPreference(
                title = { Text("Preamp gain") },
                icon = { Icon(painterResource(R.drawable.volume_up), null) },
                selectedValue = preampOptions.minByOrNull { kotlin.math.abs(it - proEqGain) } ?: 0f,
                values = preampOptions,
                valueText = { "${it.roundToInt()} dB" },
                onValueSelected = onProEqGainChange
            )
        }

        SwitchPreference(
            title = { Text("Spatial audio") },
            description = "Virtualizer-based spatial widening for headphones",
            icon = { Icon(painterResource(R.drawable.headphone_custom), null) },
            checked = spatialAudioEnabled,
            onCheckedChange = onSpatialAudioEnabledChange
        )

        if (spatialAudioEnabled) {
            val spatialOptions = listOf(0, 250, 500, 750, 1000)
            ListPreference(
                title = { Text("Spatial strength") },
                icon = { Icon(painterResource(R.drawable.tune), null) },
                selectedValue = spatialOptions.minByOrNull { kotlin.math.abs(it - spatialAudioStrength) } ?: 500,
                values = spatialOptions,
                valueText = {
                    when (it) {
                        0 -> "Off"
                        250 -> "Low"
                        500 -> "Medium"
                        750 -> "High"
                        else -> "Max"
                    }
                },
                onValueSelected = onSpatialAudioStrengthChange
            )
        }

        // Audio AR (Spatial Audio Augmented Reality)
        SwitchPreference(
            title = { Text("Spatial Audio AR") },
            description = "Rotate soundstage based on device movement (needs headphones)",
            icon = { Icon(painterResource(R.drawable.headphone_custom), null) },
            checked = audioArEnabled,
            onCheckedChange = onAudioArEnabledChange
        )

        if (audioArEnabled) {
            SwitchPreference(
                title = { Text("Auto-calibration") },
                description = "Automatically adjust center point when head is stable",
                icon = { Icon(painterResource(R.drawable.tune), null) },
                checked = audioArAutoCalibrate,
                onCheckedChange = onAudioArAutoCalibrateChange
            )

            val sensitivityOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f)
            ListPreference(
                title = { Text("Soundstage depth (sensitivity)") },
                icon = { Icon(painterResource(R.drawable.tune), null) },
                selectedValue = sensitivityOptions.minByOrNull { kotlin.math.abs(it - audioArSensitivity) } ?: 1f,
                values = sensitivityOptions,
                valueText = { String.format("%.2fx", it) },
                onValueSelected = onAudioArSensitivityChange
            )

            ButtonPreference(
                title = { Text("Recenter Audio") },
                description = "Set current device orientation as front",
                icon = { Icon(painterResource(R.drawable.tune), null) },
                onClick = onRecenterAudioClick
            )
        }

        SwitchPreference(
            title = { Text("Crossfade") },
            description = "Smooth crossfade between tracks",
            icon = { Icon(painterResource(R.drawable.waves), null) },
            checked = crossfadeEnabled,
            onCheckedChange = onCrossfadeEnabledChange
        )

        if (crossfadeEnabled) {
            SliderPreference(
                title = { Text("Crossfade duration") },
                icon = { Icon(painterResource(R.drawable.timer), null) },
                value = crossfadeDuration,
                onValueChange = onCrossfadeDurationChange,
            )

            SwitchPreference(
                title = { Text("Skip crossfade for same album") },
                description = "Gapless playback for consecutive tracks in the same album",
                icon = { Icon(painterResource(R.drawable.album), null) },
                checked = crossfadeGapless,
                onCheckedChange = onCrossfadeGaplessChange
            )
        }

        SwitchPreference(
            title = { Text("Resume on Bluetooth connect") },
            description = "Auto-resume playback when connecting Bluetooth audio device",
            icon = { Icon(painterResource(R.drawable.bluetooth), null) },
            checked = resumeOnBluetooth,
            onCheckedChange = onResumeOnBluetoothChange
        )

        SwitchPreference(
            title = { Text("Pause on mute") },
            description = "Pause playback when volume is muted",
            icon = { Icon(painterResource(R.drawable.volume_off), null) },
            checked = pauseOnMute,
            onCheckedChange = onPauseOnMuteChange
        )

        SwitchPreference(
            title = { Text("Keep screen on") },
            description = "Prevent screen timeout while playing",
            icon = { Icon(painterResource(R.drawable.brightness_high), null) },
            checked = keepScreenOn,
            onCheckedChange = onKeepScreenOnChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.seek_seconds_addup)) },
            description = stringResource(R.string.seek_seconds_addup_description),
            icon = { Icon(painterResource(R.drawable.arrow_forward), null) },
            checked = seekExtraSeconds,
            onCheckedChange = onSeekExtraSeconds
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.queue)
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.persistent_queue)) },
            description = stringResource(R.string.persistent_queue_desc),
            icon = { Icon(painterResource(R.drawable.queue_music), null) },
            checked = persistentQueue,
            onCheckedChange = onPersistentQueueChange
        )

        SwitchPreference(
            title = { Text("Remember shuffle & repeat") },
            description = "Persist shuffle and repeat modes across app restarts",
            icon = { Icon(painterResource(R.drawable.shuffle), null) },
            checked = rememberShuffleRepeat,
            onCheckedChange = onRememberShuffleRepeatChange
        )

        SwitchPreference(
            title = { Text("Prevent duplicate tracks") },
            description = "Remove existing queue items when adding duplicates",
            icon = { Icon(painterResource(R.drawable.block), null) },
            checked = preventDuplicates,
            onCheckedChange = onPreventDuplicatesChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.auto_load_more)) },
            description = stringResource(R.string.auto_load_more_desc),
            icon = { Icon(painterResource(R.drawable.playlist_add), null) },
            checked = autoLoadMore,
            onCheckedChange = onAutoLoadMoreChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.disable_load_more_when_repeat_all)) },
            description = stringResource(R.string.disable_load_more_when_repeat_all_desc),
            icon = { Icon(painterResource(R.drawable.repeat), null) },
            checked = disableLoadMoreWhenRepeatAll,
            onCheckedChange = onDisableLoadMoreWhenRepeatAllChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.auto_download_on_like)) },
            description = stringResource(R.string.auto_download_on_like_desc),
            icon = { Icon(painterResource(R.drawable.download), null) },
            checked = autoDownloadOnLike,
            onCheckedChange = onAutoDownloadOnLikeChange
        )

        SwitchPreference(
            title = { Text("Downloads on Wi-Fi only") },
            description = "Pause download queue automatically when Wi-Fi is unavailable",
            icon = { Icon(painterResource(R.drawable.download), null) },
            checked = downloadWifiOnly,
            onCheckedChange = onDownloadWifiOnlyChange
        )

        SwitchPreference(
            title = { Text("Downloads while charging only") },
            description = "Require charging state for large background download batches",
            icon = { Icon(painterResource(R.drawable.storage), null) },
            checked = downloadChargingOnly,
            onCheckedChange = onDownloadChargingOnlyChange
        )

        SwitchPreference(
            title = { Text("Auto retry failed downloads") },
            description = "Automatically requeue transient failures",
            icon = { Icon(painterResource(R.drawable.restore), null) },
            checked = downloadAutoRetry,
            onCheckedChange = onDownloadAutoRetryChange
        )

        if (downloadAutoRetry) {
            ListPreference(
                title = { Text("Download retry limit") },
                icon = { Icon(painterResource(R.drawable.timer), null) },
                selectedValue = downloadRetryLimit.coerceIn(1, 5),
                values = listOf(1, 2, 3, 4, 5),
                valueText = { "$it attempts" },
                onValueSelected = onDownloadRetryLimitChange
            )
        }

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_similar_content)) },
            description = stringResource(R.string.similar_content_desc),
            icon = { Icon(painterResource(R.drawable.similar), null) },
            checked = similarContentEnabled,
            onCheckedChange = similarContentEnabledChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
            description = stringResource(R.string.auto_skip_next_on_error_desc),
            icon = { Icon(painterResource(R.drawable.skip_next), null) },
            checked = autoSkipNextOnError,
            onCheckedChange = onAutoSkipNextOnErrorChange
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.misc)
        )

        SwitchPreference(
            title = { Text("Tap album art for lyrics") },
            description = "Click on album art in player to show/hide lyrics",
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = tapAlbumArtForLyrics,
            onCheckedChange = onTapAlbumArtForLyricsChange
        )

        SwitchPreference(
            title = { Text("Double tap to like") },
            description = "Double tap on album art to like the song",
            icon = { Icon(painterResource(R.drawable.favorite), null) },
            checked = doubleTapToLike,
            onCheckedChange = onDoubleTapToLikeChange
        )

        SwitchPreference(
            title = { Text("Double tap seek") },
            description = "Double tap left/right artwork half to seek backward/forward",
            icon = { Icon(painterResource(R.drawable.fast_forward), null) },
            checked = gestureDoubleTapSeek,
            onCheckedChange = onGestureDoubleTapSeekChange
        )

        SwitchPreference(
            title = { Text("Vertical gesture controls") },
            description = "Swipe up/down on artwork for volume and brightness",
            icon = { Icon(painterResource(R.drawable.swipe), null) },
            checked = gestureVerticalControls,
            onCheckedChange = onGestureVerticalControlsChange
        )

        SwitchPreference(
            title = { Text("TTS song announcement") },
            description = "Announce song title and artist when track changes",
            icon = { Icon(painterResource(R.drawable.notification), null) },
            checked = ttsAnnouncementEnabled,
            onCheckedChange = onTtsAnnouncementEnabledChange
        )

        SwitchPreference(
            title = { Text("Music haptics") },
            description = "Vibrate to the beat of the music",
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            checked = musicHapticsEnabled,
            onCheckedChange = onMusicHapticsEnabledChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
            icon = { Icon(painterResource(R.drawable.clear_all), null) },
            checked = stopMusicOnTaskClear,
            onCheckedChange = onStopMusicOnTaskClearChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.force_stop_on_task_clear)) },
            description = stringResource(R.string.force_stop_on_task_clear_desc),
            icon = { Icon(painterResource(R.drawable.close), null) },
            checked = forceStopOnTaskClear,
            onCheckedChange = onForceStopOnTaskClearChange
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
                                25f,
                                25f,
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
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        TopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.player_and_audio),
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
