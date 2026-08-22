

package echo.music.iad1tya.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.BuildConfig
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.AudioNormalizationKey
import echo.music.iad1tya.constants.AudioOffload
import echo.music.iad1tya.constants.AudioQuality
import echo.music.iad1tya.constants.AudioQualityKey
import echo.music.iad1tya.constants.AutoDownloadOnLikeKey
import echo.music.iad1tya.constants.AutomixCrossfadeKey
import echo.music.iad1tya.constants.AutomixDebugOverlayKey
import echo.music.iad1tya.constants.CrossfadeDurationKey
import echo.music.iad1tya.constants.CrossfadeEnabledKey
import echo.music.iad1tya.constants.CrossfadeGaplessKey
import echo.music.iad1tya.constants.AutoLoadMoreKey
import echo.music.iad1tya.constants.AutoSkipNextOnErrorKey
import echo.music.iad1tya.constants.DisableLoadMoreWhenRepeatAllKey
import echo.music.iad1tya.constants.EnableGoogleCastKey
import echo.music.iad1tya.constants.HistoryDuration
import echo.music.iad1tya.constants.KeepScreenOn
import echo.music.iad1tya.constants.PauseOnMute
import echo.music.iad1tya.constants.PersistentQueueKey
import echo.music.iad1tya.constants.PersistentShuffleAcrossQueuesKey
import echo.music.iad1tya.constants.PreventDuplicateTracksInQueueKey
import echo.music.iad1tya.constants.RememberShuffleAndRepeatKey
import echo.music.iad1tya.constants.ResumeOnBluetoothConnectKey
import echo.music.iad1tya.constants.SeekExtraSeconds
import echo.music.iad1tya.constants.ShufflePlaylistFirstKey
import echo.music.iad1tya.constants.SimilarContent

import echo.music.iad1tya.constants.SkipSilenceInstantKey
import echo.music.iad1tya.constants.SkipSilenceKey
import echo.music.iad1tya.constants.StopMusicOnTaskClearKey
import echo.music.iad1tya.constants.EnableExportAsMp3Key

import echo.music.iad1tya.constants.PreloadNextSongEnabledKey
import echo.music.iad1tya.constants.PreloadNextSongLimitKey
import echo.music.iad1tya.constants.PreloadLyricsEnabledKey

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import echo.music.iad1tya.ui.component.DefaultDialog
import echo.music.iad1tya.ui.component.EnumDialog
import echo.music.iad1tya.ui.component.IconButton
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
import echo.music.iad1tya.ui.utils.backToMain
import echo.music.iad1tya.utils.rememberEnumPreference
import echo.music.iad1tya.utils.rememberPreference
import kotlin.math.roundToInt
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
highlightKey: String? = null) {
    val scrollState = androidx.compose.foundation.rememberScrollState()

    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.OPUS
    )

    val (crossfadeEnabled, onCrossfadeEnabledChange) = rememberPreference(
        CrossfadeEnabledKey,
        defaultValue = false
    )
    val (crossfadeDuration, onCrossfadeDurationChange) = rememberPreference(
        CrossfadeDurationKey,
        defaultValue = 5f
    )
    val (automixCrossfade, onAutomixCrossfadeChange) = rememberPreference(
        AutomixCrossfadeKey,
        defaultValue = false
    )
    val (automixDebugOverlay, onAutomixDebugOverlayChange) = rememberPreference(
        AutomixDebugOverlayKey,
        defaultValue = false
    )
    val (crossfadeGapless, onCrossfadeGaplessChange) = rememberPreference(
        CrossfadeGaplessKey,
        defaultValue = true
    )
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(
        PersistentQueueKey,
        defaultValue = true
    )
    val (skipSilence, onSkipSilenceChange) = rememberPreference(
        SkipSilenceKey,
        defaultValue = false
    )
    val (skipSilenceInstant, onSkipSilenceInstantChange) = rememberPreference(
        SkipSilenceInstantKey,
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


    val (preloadNextSongEnabled, onPreloadNextSongEnabledChange) = rememberPreference(
        key = PreloadNextSongEnabledKey,
        defaultValue = true
    )

    val (preloadNextSongLimit, onPreloadNextSongLimitChange) = rememberPreference(
        key = PreloadNextSongLimitKey,
        defaultValue = 10
    )

    val (preloadLyricsEnabled, onPreloadLyricsEnabledChange) = rememberPreference(
        key = PreloadLyricsEnabledKey,
        defaultValue = true
    )

    val (dataSaverEnabled, onDataSaverEnabledChange) = rememberPreference(
        key = echo.music.iad1tya.constants.DataSaverEnabledKey,
        defaultValue = false
    )

    val (enableExportAsMp3, onEnableExportAsMp3Change) = rememberPreference(
        key = EnableExportAsMp3Key,
        defaultValue = false
    )

    val context = androidx.compose.ui.platform.LocalContext.current

    val (enableGoogleCast, onEnableGoogleCastChange) = rememberPreference(
        key = EnableGoogleCastKey,
        defaultValue = true
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
    val (persistentShuffleAcrossQueues, onPersistentShuffleAcrossQueuesChange) = rememberPreference(
        PersistentShuffleAcrossQueuesKey,
        defaultValue = false
    )
    val (rememberShuffleAndRepeat, onRememberShuffleAndRepeatChange) = rememberPreference(
        RememberShuffleAndRepeatKey,
        defaultValue = true
    )
    val (shufflePlaylistFirst, onShufflePlaylistFirstChange) = rememberPreference(
        ShufflePlaylistFirstKey,
        defaultValue = false
    )
    val (preventDuplicateTracksInQueue, onPreventDuplicateTracksInQueueChange) = rememberPreference(
        PreventDuplicateTracksInQueueKey,
        defaultValue = false
    )
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(
        StopMusicOnTaskClearKey,
        defaultValue = true
    )
    val (pauseOnMute, onPauseOnMuteChange) = rememberPreference(
        PauseOnMute,
        defaultValue = false
    )
    val (resumeOnBluetoothConnect, onResumeOnBluetoothConnectChange) = rememberPreference(
        ResumeOnBluetoothConnectKey,
        defaultValue = false
    )
    val (keepScreenOn, onKeepScreenOnChange) = rememberPreference(
        KeepScreenOn,
        defaultValue = false
    )
    val (historyDuration, onHistoryDurationChange) = rememberPreference(
        HistoryDuration,
        defaultValue = 1f
    )

    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showDownloadQualityDialog by remember { mutableStateOf(false) }

    val (downloadQuality, onDownloadQualityChange) = rememberEnumPreference(
        echo.music.iad1tya.constants.DownloadQualityKey,
        defaultValue = echo.music.iad1tya.constants.DownloadQuality.YOUTUBE
    )

    if (showAudioQualityDialog) {
        EnumDialog(
            onDismiss = { showAudioQualityDialog = false },
            onSelect = {
                onAudioQualityChange(it)
                showAudioQualityDialog = false
            },
            title = stringResource(R.string.audio_quality),
            current = audioQuality,
            values = listOf(AudioQuality.OPUS),
            valueText = {
                when (it) {
                    AudioQuality.OPUS -> "Opus"
                    else -> ""
                }
            },
            valueDescription = {
                ""
            }
        )
    }

    if (showDownloadQualityDialog) {
        EnumDialog(
            onDismiss = { showDownloadQualityDialog = false },
            onSelect = {
                onDownloadQualityChange(it)
                showDownloadQualityDialog = false
            },
            title = stringResource(R.string.download_quality_title),
            current = downloadQuality,
            values = listOf(echo.music.iad1tya.constants.DownloadQuality.YOUTUBE),
            valueText = {
                when (it) {
                    echo.music.iad1tya.constants.DownloadQuality.YOUTUBE -> "YouTube Music (AAC/Default)"
                    else -> ""
                }
            }
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal
                )
            )
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        var showCrossfadeBetaDialog by remember { mutableStateOf(false) }

        if (showCrossfadeBetaDialog) {
            DefaultDialog(
                onDismiss = { showCrossfadeBetaDialog = false },
                title = { Text(stringResource(R.string.crossfade_beta_title)) },
                buttons = {
                    TextButton(onClick = { showCrossfadeBetaDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = {
                        showCrossfadeBetaDialog = false
                        onCrossfadeEnabledChange(true)
                    }) {
                        Text(stringResource(R.string.enable))
                    }
                }
            ) {
                Text(stringResource(R.string.crossfade_beta_message))
            }
        }





        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )



        Material3SettingsGroup(
            scrollState = scrollState,
            title = "Data Saver",
            items = buildList {
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == "Data Saver Mode (Beta)"),
                    icon = painterResource(R.drawable.offline),
                    title = { Text("Data Saver Mode (Beta)") },
                    description = { Text("Disable lyrics, videos, preloading, background syncs, and force Opus audio to save data.") },
                    trailingContent = {
                        Switch(
                            checked = dataSaverEnabled,
                            onCheckedChange = onDataSaverEnabledChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (dataSaverEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onDataSaverEnabledChange(!dataSaverEnabled) }
                ))
            }
        )

        Material3SettingsGroup(scrollState = scrollState, 
            title = stringResource(R.string.player),
            items = buildList {
                add(Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.audio_quality)),
                    icon = painterResource(R.drawable.graphic_eq),
                    title = { Text(stringResource(R.string.audio_quality)) },
                    description = {
                        Text(
                            when (audioQuality) {
                                AudioQuality.OPUS -> "Opus"
                                else -> "Opus"
                            }
                        )
                    },
                    onClick = { showAudioQualityDialog = true }
                ))
                
                add(Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.download_quality_title)),
                    icon = painterResource(R.drawable.download),
                    title = { Text(stringResource(R.string.download_quality_title)) },
                    description = {
                        Text(
                            when (downloadQuality) {
                                echo.music.iad1tya.constants.DownloadQuality.YOUTUBE -> "YouTube Music (AAC/Default)"
                                else -> "YouTube Music (AAC/Default)"
                            }
                        )
                    },
                    onClick = { showDownloadQualityDialog = true }
                ))


                add(Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.crossfade)),
                    icon = painterResource(R.drawable.linear_scale),
                    title = { Text(stringResource(R.string.crossfade)) },
                    description = { 
                        Text(stringResource(R.string.crossfade_desc)) 
                    },
                    showBadge = true,
                    trailingContent = {
                        Switch(
                            checked = crossfadeEnabled,
                            onCheckedChange = {
                                if (!crossfadeEnabled) {
                                    showCrossfadeBetaDialog = true
                                } else {
                                    onCrossfadeEnabledChange(false)
                                }
                            },
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (crossfadeEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = {
                        if (!crossfadeEnabled) {
                            showCrossfadeBetaDialog = true
                        } else {
                            onCrossfadeEnabledChange(false)
                        }
                    }
                ))
                if (crossfadeEnabled) {
                    add(Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.crossfade_duration)),
                        icon = painterResource(R.drawable.timer),
                        title = { Text(stringResource(R.string.crossfade_duration)) },
                        description = {
                            Column {
                                Text(pluralStringResource(R.plurals.seconds, crossfadeDuration.toInt(), crossfadeDuration.toInt()))
                                Slider(
                                    value = crossfadeDuration,
                                    onValueChange = onCrossfadeDurationChange,
                                    valueRange = 1f..15f,
                                    steps = 14
                                )
                            }
                        }
                    ))
                    add(Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.crossfade_gapless)),
                        icon = painterResource(R.drawable.album),
                        title = { Text(stringResource(R.string.crossfade_gapless)) },
                        description = { Text(stringResource(R.string.crossfade_gapless_desc)) },
                        trailingContent = {
                            Switch(
                                checked = crossfadeGapless,
                                onCheckedChange = onCrossfadeGaplessChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (crossfadeGapless) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onCrossfadeGaplessChange(!crossfadeGapless) }
                    ))
                    add(Material3SettingsItem(
                        isHighlighted = highlightKey == stringResource(R.string.automix),
                        icon = painterResource(R.drawable.graphic_eq),
                        title = { Text(stringResource(R.string.automix)) },
                        description = { Text(stringResource(R.string.automix_desc)) },
                        trailingContent = {
                            Switch(
                                checked = automixCrossfade,
                                onCheckedChange = onAutomixCrossfadeChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (automixCrossfade) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onAutomixCrossfadeChange(!automixCrossfade) }
                    ))
                    if (automixCrossfade) {
                        add(Material3SettingsItem(
                            isHighlighted = highlightKey == stringResource(R.string.automix_debug),
                            icon = painterResource(R.drawable.bug_report),
                            title = { Text(stringResource(R.string.automix_debug)) },
                            description = { Text(stringResource(R.string.automix_debug_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = automixDebugOverlay,
                                    onCheckedChange = onAutomixDebugOverlayChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(
                                                id = if (automixDebugOverlay) R.drawable.check else R.drawable.close
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            },
                            onClick = { onAutomixDebugOverlayChange(!automixDebugOverlay) }
                        ))
                    }
                }
                add(Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.history_duration)),
                    icon = painterResource(R.drawable.history),
                    title = { Text(stringResource(R.string.history_duration)) },
                    description = {
                        Slider(
                            value = historyDuration,
                            onValueChange = { onHistoryDurationChange(it.roundToInt().toFloat()) },
                            valueRange = 1f..100f,
                            steps = 9
                        )
                    },
                    trailingContent = {
                        Text(text = historyDuration.roundToInt().toString())
                    }
                ))
                add(Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.skip_silence)),
                    icon = painterResource(R.drawable.fast_forward),
                    title = { Text(stringResource(R.string.skip_silence)) },
                    description = { Text(stringResource(R.string.skip_silence_desc)) },
                    trailingContent = {
                        Switch(
                            checked = skipSilence,
                            onCheckedChange = onSkipSilenceChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (skipSilence) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSkipSilenceChange(!skipSilence) }
                ))
                add(Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.skip_silence_instant)),
                    icon = painterResource(R.drawable.skip_next),
                    title = { Text(stringResource(R.string.skip_silence_instant)) },
                    description = { Text(stringResource(R.string.skip_silence_instant_desc)) },
                    trailingContent = {
                        Switch(
                            checked = skipSilenceInstant,
                            onCheckedChange = { onSkipSilenceInstantChange(it) },
                            enabled = skipSilence,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (skipSilenceInstant) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { if (skipSilence) onSkipSilenceInstantChange(!skipSilenceInstant) }
                ))
                add(Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.audio_normalization)),
                    icon = painterResource(R.drawable.volume_up),
                    title = { Text(stringResource(R.string.audio_normalization)) },
                    description = { Text(stringResource(R.string.audio_normalization_desc)) },
                    trailingContent = {
                        Switch(
                            checked = audioNormalization,
                            onCheckedChange = onAudioNormalizationChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (audioNormalization) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onAudioNormalizationChange(!audioNormalization) }
                ))
                add(Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.audio_offload)),
                    icon = painterResource(R.drawable.graphic_eq),
                    title = { Text(stringResource(R.string.audio_offload)) },
                    description = {
                        Text(
                            if (crossfadeEnabled) stringResource(R.string.audio_offload_disabled_by_crossfade)
                            else stringResource(R.string.audio_offload_description)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = if (crossfadeEnabled) false else audioOffload,
                            onCheckedChange = onAudioOffloadChange,
                            enabled = !crossfadeEnabled,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (!crossfadeEnabled && audioOffload) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { if (!crossfadeEnabled) onAudioOffloadChange(!audioOffload) }
                ))
                

                add(Material3SettingsItem(
    isHighlighted = (highlightKey == "Preload Next Song"),
                    icon = painterResource(R.drawable.skip_next),
                    title = { Text("Preload Next Song") },
                    description = { Text("Cache the next song for gapless playback") },
                    trailingContent = {
                        Switch(
                            checked = preloadNextSongEnabled,
                            onCheckedChange = onPreloadNextSongEnabledChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (preloadNextSongEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onPreloadNextSongEnabledChange(!preloadNextSongEnabled) }
                ))

                if (preloadNextSongEnabled) {
                    add(Material3SettingsItem(
    isHighlighted = (highlightKey == "Preload Limit"),
                        icon = painterResource(R.drawable.library_music),
                        title = { Text("Preload Limit") },
                        description = {
                            Slider(
                                value = preloadNextSongLimit.toFloat(),
                                onValueChange = { onPreloadNextSongLimitChange(it.roundToInt()) },
                                valueRange = 1f..10f,
                                steps = 9
                            )
                        },
                        trailingContent = {
                            Text(text = preloadNextSongLimit.toString())
                        }
                    ))
                    
                    add(Material3SettingsItem(
    isHighlighted = (highlightKey == "Preload Lyrics"),
                        icon = painterResource(R.drawable.queue_music),
                        title = { Text("Preload Lyrics") },
                        description = { Text("Also cache lyrics for the preloaded songs") },
                        trailingContent = {
                            Switch(
                                checked = preloadLyricsEnabled,
                                onCheckedChange = onPreloadLyricsEnabledChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (preloadLyricsEnabled) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onPreloadLyricsEnabledChange(!preloadLyricsEnabled) }
                    ))
                }
                
                if (BuildConfig.CAST_AVAILABLE) {
                    add(Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.google_cast)),
                        icon = painterResource(R.drawable.cast),
                        title = { Text(stringResource(R.string.google_cast)) },
                        description = { Text(stringResource(R.string.google_cast_description)) },
                        trailingContent = {
                            Switch(
                                checked = enableGoogleCast,
                                onCheckedChange = onEnableGoogleCastChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (enableGoogleCast) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onEnableGoogleCastChange(!enableGoogleCast) }
                    ))
                }
                add(Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.seek_seconds_addup)),
                    icon = painterResource(R.drawable.arrow_forward),
                    title = { Text(stringResource(R.string.seek_seconds_addup)) },
                    description = { Text(stringResource(R.string.seek_seconds_addup_description)) },
                    trailingContent = {
                        Switch(
                            checked = seekExtraSeconds,
                            onCheckedChange = onSeekExtraSeconds,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (seekExtraSeconds) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSeekExtraSeconds(!seekExtraSeconds) }
                ))
                add(Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.echo_equalizer)),
                    icon = painterResource(R.drawable.echoequlizer),
                    title = { Text(stringResource(R.string.echo_equalizer)) },
                    description = { Text(stringResource(R.string.echo_equalizer_desc)) },
                    onClick = { navController.navigate("settings/equalizer") }
                ))
            }
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(scrollState = scrollState, 
            title = stringResource(R.string.queue),
            items = listOf(
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.persistent_queue)),
                    icon = painterResource(R.drawable.queue_music),
                    title = { Text(stringResource(R.string.persistent_queue)) },
                    description = { Text(stringResource(R.string.persistent_queue_desc)) },
                    trailingContent = {
                        Switch(
                            checked = persistentQueue,
                            onCheckedChange = onPersistentQueueChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (persistentQueue) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onPersistentQueueChange(!persistentQueue) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.auto_load_more)),
                    icon = painterResource(R.drawable.playlist_add),
                    title = { Text(stringResource(R.string.auto_load_more)) },
                    description = { Text(stringResource(R.string.auto_load_more_desc)) },
                    trailingContent = {
                        Switch(
                            checked = autoLoadMore,
                            onCheckedChange = onAutoLoadMoreChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (autoLoadMore) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onAutoLoadMoreChange(!autoLoadMore) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.disable_load_more_when_repeat_all)),
                    icon = painterResource(R.drawable.repeat),
                    title = { Text(stringResource(R.string.disable_load_more_when_repeat_all)) },
                    description = { Text(stringResource(R.string.disable_load_more_when_repeat_all_desc)) },
                    trailingContent = {
                        Switch(
                            checked = disableLoadMoreWhenRepeatAll,
                            onCheckedChange = onDisableLoadMoreWhenRepeatAllChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (disableLoadMoreWhenRepeatAll) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onDisableLoadMoreWhenRepeatAllChange(!disableLoadMoreWhenRepeatAll) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.auto_download_on_like)),
                    icon = painterResource(R.drawable.download),
                    title = { Text(stringResource(R.string.auto_download_on_like)) },
                    description = { Text(stringResource(R.string.auto_download_on_like_desc)) },
                    trailingContent = {
                        Switch(
                            checked = autoDownloadOnLike,
                            onCheckedChange = onAutoDownloadOnLikeChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (autoDownloadOnLike) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onAutoDownloadOnLikeChange(!autoDownloadOnLike) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.enable_similar_content)),
                    icon = painterResource(R.drawable.similar),
                    title = { Text(stringResource(R.string.enable_similar_content)) },
                    description = { Text(stringResource(R.string.similar_content_desc)) },
                    trailingContent = {
                        Switch(
                            checked = similarContentEnabled,
                            onCheckedChange = similarContentEnabledChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (similarContentEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { similarContentEnabledChange(!similarContentEnabled) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.persistent_shuffle_title)),
                    icon = painterResource(R.drawable.shuffle),
                    title = { Text(stringResource(R.string.persistent_shuffle_title)) },
                    description = { Text(stringResource(R.string.persistent_shuffle_desc)) },
                    trailingContent = {
                        Switch(
                            checked = persistentShuffleAcrossQueues,
                            onCheckedChange = onPersistentShuffleAcrossQueuesChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (persistentShuffleAcrossQueues) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onPersistentShuffleAcrossQueuesChange(!persistentShuffleAcrossQueues) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.remember_shuffle_and_repeat)),
                    icon = painterResource(R.drawable.shuffle),
                    title = { Text(stringResource(R.string.remember_shuffle_and_repeat)) },
                    description = { Text(stringResource(R.string.remember_shuffle_and_repeat_desc)) },
                    trailingContent = {
                        Switch(
                            checked = rememberShuffleAndRepeat,
                            onCheckedChange = onRememberShuffleAndRepeatChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (rememberShuffleAndRepeat) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onRememberShuffleAndRepeatChange(!rememberShuffleAndRepeat) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.shuffle_playlist_first)),
                    icon = painterResource(R.drawable.shuffle),
                    title = { Text(stringResource(R.string.shuffle_playlist_first)) },
                    description = { Text(stringResource(R.string.shuffle_playlist_first_desc)) },
                    trailingContent = {
                        Switch(
                            checked = shufflePlaylistFirst,
                            onCheckedChange = onShufflePlaylistFirstChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (shufflePlaylistFirst) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShufflePlaylistFirstChange(!shufflePlaylistFirst) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.prevent_duplicate_tracks_in_queue)),
                    icon = painterResource(R.drawable.queue_music),
                    title = { Text(stringResource(R.string.prevent_duplicate_tracks_in_queue)) },
                    description = { Text(stringResource(R.string.prevent_duplicate_tracks_in_queue_desc)) },
                    trailingContent = {
                        Switch(
                            checked = preventDuplicateTracksInQueue,
                            onCheckedChange = onPreventDuplicateTracksInQueueChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (preventDuplicateTracksInQueue) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onPreventDuplicateTracksInQueueChange(!preventDuplicateTracksInQueue) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.auto_skip_next_on_error)),
                    icon = painterResource(R.drawable.skip_next),
                    title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
                    description = { Text(stringResource(R.string.auto_skip_next_on_error_desc)) },
                    trailingContent = {
                        Switch(
                            checked = autoSkipNextOnError,
                            onCheckedChange = onAutoSkipNextOnErrorChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (autoSkipNextOnError) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onAutoSkipNextOnErrorChange(!autoSkipNextOnError) }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(scrollState = scrollState, 
            title = stringResource(R.string.misc),
            items = listOf(
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.stop_music_on_task_clear)),
                    icon = painterResource(R.drawable.clear_all),
                    title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
                    description = { Text(stringResource(R.string.stop_music_on_task_clear_desc)) },
                    trailingContent = {
                        Switch(
                            checked = stopMusicOnTaskClear,
                            onCheckedChange = onStopMusicOnTaskClearChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (stopMusicOnTaskClear) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onStopMusicOnTaskClearChange(!stopMusicOnTaskClear) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.pause_music_when_media_is_muted)),
                    icon = painterResource(R.drawable.volume_off_pause),
                    title = { Text(stringResource(R.string.pause_music_when_media_is_muted)) },
                    description = { Text(stringResource(R.string.pause_music_when_media_is_muted_desc)) },
                    trailingContent = {
                        Switch(
                            checked = pauseOnMute,
                            onCheckedChange = onPauseOnMuteChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (pauseOnMute) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onPauseOnMuteChange(!pauseOnMute) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.resume_on_bluetooth_connect)),
                    icon = painterResource(R.drawable.bluetooth),
                    title = { Text(stringResource(R.string.resume_on_bluetooth_connect)) },
                    description = { Text(stringResource(R.string.resume_on_bluetooth_connect_desc)) },
                    trailingContent = {
                        Switch(
                            checked = resumeOnBluetoothConnect,
                            onCheckedChange = onResumeOnBluetoothConnectChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (resumeOnBluetoothConnect) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onResumeOnBluetoothConnectChange(!resumeOnBluetoothConnect) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.keep_screen_on_when_player_is_expanded)),
                    icon = painterResource(R.drawable.screenshot),
                    title = { Text(stringResource(R.string.keep_screen_on_when_player_is_expanded)) },
                    description = { Text(stringResource(R.string.keep_screen_on_when_player_is_expanded_desc)) },
                    trailingContent = {
                        Switch(
                            checked = keepScreenOn,
                            onCheckedChange = onKeepScreenOnChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (keepScreenOn) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onKeepScreenOnChange(!keepScreenOn) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.export_desc)),
                    icon = painterResource(R.drawable.file_export),
                    title = { Text(stringResource(R.string.export_desc)) },
                    description = { Text("Show 'Export as MP3' in menus") },
                    trailingContent = {
                        Switch(
                            checked = enableExportAsMp3,
                            onCheckedChange = onEnableExportAsMp3Change,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (enableExportAsMp3) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onEnableExportAsMp3Change(!enableExportAsMp3) }
                ),
                Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.youtube_decryption_settings)),
                    icon = painterResource(R.drawable.settings),
                    title = { Text(stringResource(R.string.youtube_decryption_settings)) },
                    description = { Text(stringResource(R.string.youtube_decryption_desc)) },
                    onClick = { navController.navigate("settings/echo_extractor") }
                )
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
    
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.player_and_audio)) },
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
        }
    )
}
