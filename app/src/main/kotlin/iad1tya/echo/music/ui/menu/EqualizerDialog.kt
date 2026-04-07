@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package iad1tya.echo.music.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.EqualizerBandLevelsMbKey
import iad1tya.echo.music.constants.EqualizerBassBoostEnabledKey
import iad1tya.echo.music.constants.EqualizerBassBoostStrengthKey
import iad1tya.echo.music.constants.EqualizerCustomProfilesJsonKey
import iad1tya.echo.music.constants.EqualizerEnabledKey
import iad1tya.echo.music.constants.EqualizerOutputGainEnabledKey
import iad1tya.echo.music.constants.EqualizerOutputGainMbKey
import iad1tya.echo.music.constants.EqualizerSelectedProfileIdKey
import iad1tya.echo.music.constants.EqualizerVirtualizerEnabledKey
import iad1tya.echo.music.constants.EqualizerVirtualizerStrengthKey
import iad1tya.echo.music.playback.EqProfile
import iad1tya.echo.music.playback.EqProfilesPayload
import iad1tya.echo.music.playback.EqualizerJson
import iad1tya.echo.music.playback.decodeBandLevelsMb
import iad1tya.echo.music.playback.decodeProfilesPayload
import iad1tya.echo.music.playback.encodeBandLevelsMb
import iad1tya.echo.music.playback.encodeProfilesPayload
import iad1tya.echo.music.playback.resampleLevelsByIndex
import iad1tya.echo.music.ui.component.ListDialog
import iad1tya.echo.music.ui.component.TextFieldDialog
import iad1tya.echo.music.utils.rememberPreference
import android.widget.Toast
import androidx.compose.foundation.layout.width
import java.util.Locale
import java.util.UUID

@Composable
fun EqualizerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val eqCapabilities by playerConnection.service.eqCapabilities.collectAsState()

    val (eqEnabled, setEqEnabled) = rememberPreference(EqualizerEnabledKey, defaultValue = false)
    val (selectedProfileId, setSelectedProfileId) = rememberPreference(EqualizerSelectedProfileIdKey, defaultValue = "flat")
    val (bandLevelsRaw, setBandLevelsRaw) = rememberPreference(EqualizerBandLevelsMbKey, defaultValue = "")
    val (outputGainEnabled, setOutputGainEnabled) = rememberPreference(EqualizerOutputGainEnabledKey, defaultValue = false)
    val (outputGainMb, setOutputGainMb) = rememberPreference(EqualizerOutputGainMbKey, defaultValue = 0)
    val (bassBoostEnabled, setBassBoostEnabled) = rememberPreference(EqualizerBassBoostEnabledKey, defaultValue = false)
    val (bassBoostStrength, setBassBoostStrength) = rememberPreference(EqualizerBassBoostStrengthKey, defaultValue = 0)
    val (virtualizerEnabled, setVirtualizerEnabled) = rememberPreference(EqualizerVirtualizerEnabledKey, defaultValue = false)
    val (virtualizerStrength, setVirtualizerStrength) = rememberPreference(EqualizerVirtualizerStrengthKey, defaultValue = 0)
    val (customProfilesJson, setCustomProfilesJson) = rememberPreference(EqualizerCustomProfilesJsonKey, defaultValue = "")

    val caps = eqCapabilities
    val bandCount = caps?.bandCount ?: 0
    val minMb = caps?.minBandLevelMb ?: -1500
    val maxMb = caps?.maxBandLevelMb ?: 1500

    var outputGainLocal by rememberSaveable { mutableIntStateOf(outputGainMb) }
    LaunchedEffect(outputGainMb) { outputGainLocal = outputGainMb }

    var bassBoostStrengthLocal by rememberSaveable { mutableIntStateOf(bassBoostStrength) }
    LaunchedEffect(bassBoostStrength) { bassBoostStrengthLocal = bassBoostStrength }

    var virtualizerStrengthLocal by rememberSaveable { mutableIntStateOf(virtualizerStrength) }
    LaunchedEffect(virtualizerStrength) { virtualizerStrengthLocal = virtualizerStrength }

    var bandLevelsMb by remember { mutableStateOf<List<Int>>(emptyList()) }
    LaunchedEffect(bandLevelsRaw, bandCount) {
        bandLevelsMb = resampleLevelsByIndex(decodeBandLevelsMb(bandLevelsRaw), bandCount)
    }

    val profiles = remember(customProfilesJson) { decodeProfilesPayload(customProfilesJson).profiles }
    val activeProfileId = selectedProfileId.removePrefix("profile:").takeIf { selectedProfileId.startsWith("profile:") }
    val activeProfile = remember(profiles, activeProfileId) { profiles.firstOrNull { it.id == activeProfileId } }

    var showSaveProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showManageProfilesDialog by rememberSaveable { mutableStateOf(false) }
    var showImportProfilesDialog by rememberSaveable { mutableStateOf(false) }

    if (showSaveProfileDialog) {
        TextFieldDialog(
            title = { Text(text = "Save profile") },
            placeholder = { Text(text = "Profile name") },
            onDone = { name ->
                val trimmed = name.trim()
                if (trimmed.isNotBlank()) {
                    val newProfile = EqProfile(
                        id = UUID.randomUUID().toString(),
                        name = trimmed,
                        bandCenterFreqHz = caps?.centerFreqHz.orEmpty(),
                        bandLevelsMb = bandLevelsMb,
                        outputGainMb = outputGainLocal,
                        bassBoostStrength = bassBoostStrengthLocal,
                        virtualizerStrength = virtualizerStrengthLocal,
                    )
                    val updatedPayload = EqProfilesPayload(profiles = (profiles + newProfile).distinctBy { it.id }.sortedBy { it.name.lowercase() })
                    setCustomProfilesJson(encodeProfilesPayload(updatedPayload))
                    setSelectedProfileId("profile:${newProfile.id}")
                }
            },
            onDismiss = { showSaveProfileDialog = false },
        )
    }

    if (showImportProfilesDialog) {
        TextFieldDialog(
            title = { Text(text = "Import profiles") },
            placeholder = { Text(text = "Paste equalizer profiles JSON") },
            singleLine = false,
            maxLines = 10,
            isInputValid = { it.trim().isNotBlank() },
            onDone = { raw ->
                val trimmed = raw.trim()
                val payload = decodeProfilesPayload(trimmed).takeIf { it.profiles.isNotEmpty() }
                    ?: runCatching { EqProfilesPayload(EqualizerJson.json.decodeFromString<List<EqProfile>>(trimmed)) }.getOrNull()
                    ?: EqProfilesPayload()

                if (payload.profiles.isEmpty()) {
                    Toast.makeText(context, "Couldn\'t import profiles", Toast.LENGTH_SHORT).show()
                    return@TextFieldDialog
                }

                val existingIds = profiles.map { it.id }.toMutableSet()
                val normalizedImported = payload.profiles.map { p ->
                    val baseName = p.name.trim().ifBlank { "Imported profile" }
                    val incomingId = p.id.trim()
                    val finalId = if (incomingId.isBlank() || !existingIds.add(incomingId)) {
                        generateSequence { UUID.randomUUID().toString() }.first { existingIds.add(it) }
                    } else {
                        incomingId
                    }
                    p.copy(id = finalId, name = baseName)
                }

                val updatedPayload = EqProfilesPayload(profiles = (profiles + normalizedImported).distinctBy { it.id }.sortedBy { it.name.lowercase() })
                setCustomProfilesJson(encodeProfilesPayload(updatedPayload))
                normalizedImported.firstOrNull()?.id?.let { setSelectedProfileId("profile:$it") }
                Toast.makeText(context, "Imported ${normalizedImported.size} profiles", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showImportProfilesDialog = false },
        )
    }

    if (showManageProfilesDialog) {
        ListDialog(onDismiss = { showManageProfilesDialog = false }, modifier = Modifier.fillMaxWidth()) {
            items(
                items = profiles,
                key = { it.id },
            ) { profile ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable {
                        setEqEnabled(true)
                        setBandLevelsRaw(encodeBandLevelsMb(profile.bandLevelsMb))
                        setOutputGainMb(profile.outputGainMb)
                        setOutputGainEnabled(profile.outputGainMb != 0)
                        setBassBoostStrength(profile.bassBoostStrength)
                        setBassBoostEnabled(profile.bassBoostStrength != 0)
                        setVirtualizerStrength(profile.virtualizerStrength)
                        setVirtualizerEnabled(profile.virtualizerStrength != 0)
                        setSelectedProfileId("profile:${profile.id}")
                        showManageProfilesDialog = false
                    }.padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = profile.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = "Custom profile", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        val updatedPayload = EqProfilesPayload(profiles = profiles.filterNot { it.id == profile.id })
                        setCustomProfilesJson(encodeProfilesPayload(updatedPayload))
                        if (selectedProfileId == "profile:${profile.id}") setSelectedProfileId("manual")
                    }) {
                        Icon(painter = painterResource(R.drawable.delete), contentDescription = null)
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(text = "Equalizer") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(painter = painterResource(R.drawable.close), contentDescription = null)
                        }
                    },
                    actions = {
                        Switch(
                            checked = eqEnabled,
                            onCheckedChange = {
                                setEqEnabled(it)
                                if (it && selectedProfileId.isBlank()) setSelectedProfileId("manual")
                            },
                            thumbContent = {
                                Icon(painter = painterResource(id = if (eqEnabled) R.drawable.check else R.drawable.close), contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize))
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface),
                )

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
                ) {
                    Spacer(Modifier.height(12.dp))

                    if (caps == null || bandCount <= 0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text(text = "Open a track to load equalizer bands.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = onDismiss) { Text(text = "OK") }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        return@Column
                    }

                    SectionCard(
                        title = "Presets",
                        trailing = {
                            TextButton(onClick = { showImportProfilesDialog = true }) { Text(text = "Import") }
                        },
                    ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).horizontalScroll(rememberScrollState())) {
                            FilterChip(
                                selected = selectedProfileId == "flat",
                                onClick = {
                                    playerConnection.service.applyEqFlatPreset()
                                    setSelectedProfileId("flat")
                                },
                                label = { Text(text = "Flat") },
                                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                border = null,
                            )
                            Spacer(Modifier.width(8.dp))

                            caps.systemPresets.forEachIndexed { index, name ->
                                FilterChip(
                                    selected = selectedProfileId == "system:$index",
                                    onClick = {
                                        playerConnection.service.applySystemEqPreset(index)
                                        setSelectedProfileId("system:$index")
                                    },
                                    label = { Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    border = null,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    SectionCard(
                        title = "Profiles",
                        trailing = {
                            TextButton(onClick = { showManageProfilesDialog = true }) { Text(text = "Manage") }
                        },
                    ) {
                        val subtitle = when {
                            selectedProfileId == "flat" -> "Flat"
                            selectedProfileId.startsWith("system:") -> "System preset"
                            activeProfile != null -> activeProfile.name
                            else -> "Manual"
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = subtitle, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = "Current preset", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { showSaveProfileDialog = true }) { Text(text = "Save") }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    SectionCard(title = "Bands", trailing = {
                        TextButton(onClick = {
                            setSelectedProfileId("manual")
                            setBandLevelsRaw(encodeBandLevelsMb(List(bandCount) { 0 }))
                        }) { Text(text = "Reset") }
                    }) {
                        caps.centerFreqHz.forEachIndexed { band, hz ->
                            val value = bandLevelsMb.getOrNull(band) ?: 0
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp)) {
                                Text(text = formatHz(hz), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(64.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Slider(
                                    value = value.toFloat().coerceIn(minMb.toFloat(), maxMb.toFloat()),
                                    onValueChange = { newValue ->
                                        val coerced = newValue.toInt().coerceIn(minMb, maxMb)
                                        bandLevelsMb = bandLevelsMb.toMutableList().apply {
                                            while (size < bandCount) add(0)
                                            set(band, coerced)
                                        }
                                    },
                                    onValueChangeFinished = {
                                        setSelectedProfileId("manual")
                                        setBandLevelsRaw(encodeBandLevelsMb(bandLevelsMb))
                                        setEqEnabled(true)
                                    },
                                    valueRange = minMb.toFloat()..maxMb.toFloat(),
                                    colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(text = formatDb(value / 100f), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End, modifier = Modifier.width(72.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    SectionCard(title = "Output gain") {
                        ToggleSliderRow(
                            enabled = outputGainEnabled,
                            onEnabledChange = {
                                setSelectedProfileId("manual")
                                setOutputGainEnabled(it)
                            },
                            value = outputGainLocal,
                            onValueChange = { outputGainLocal = it },
                            valueRange = -1500..1500,
                            formatValue = { formatDb(it / 100f) },
                            onValueChangeFinished = {
                                setSelectedProfileId("manual")
                                setOutputGainMb(outputGainLocal)
                            },
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    SectionCard(title = "Bass boost") {
                        ToggleSliderRow(
                            enabled = bassBoostEnabled,
                            onEnabledChange = {
                                setSelectedProfileId("manual")
                                setBassBoostEnabled(it)
                            },
                            value = bassBoostStrengthLocal,
                            onValueChange = { bassBoostStrengthLocal = it },
                            valueRange = 0..1000,
                            formatValue = { "${it / 10}%" },
                            onValueChangeFinished = {
                                setSelectedProfileId("manual")
                                setBassBoostStrength(bassBoostStrengthLocal)
                            },
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    SectionCard(title = "Virtualizer") {
                        ToggleSliderRow(
                            enabled = virtualizerEnabled,
                            onEnabledChange = {
                                setSelectedProfileId("manual")
                                setVirtualizerEnabled(it)
                            },
                            value = virtualizerStrengthLocal,
                            onValueChange = { virtualizerStrengthLocal = it },
                            valueRange = 0..1000,
                            formatValue = { "${it / 10}%" },
                            onValueChangeFinished = {
                                setSelectedProfileId("manual")
                                setVirtualizerStrength(virtualizerStrengthLocal)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun ToggleSliderRow(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    formatValue: (Int) -> String,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            thumbContent = {
                Icon(painter = painterResource(id = if (enabled) R.drawable.check else R.drawable.close), contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize))
            },
        )
        Spacer(Modifier.width(12.dp))
        Slider(
            value = value.toFloat().coerceIn(valueRange.first.toFloat(), valueRange.last.toFloat()),
            onValueChange = { onValueChange(it.toInt().coerceIn(valueRange.first, valueRange.last)) },
            onValueChangeFinished = { onValueChangeFinished?.invoke() },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            enabled = enabled,
            colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(text = formatValue(value), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End, modifier = Modifier.width(72.dp))
    }
}

private fun formatHz(hz: Int): String = if (hz >= 1000) "${hz / 1000}kHz" else "${hz}Hz"

private fun formatDb(db: Float): String = String.format(Locale.getDefault(), "%+.1f dB", db)