package iad1tya.echo.music.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.edit
import iad1tya.echo.music.utils.dataStore
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import iad1tya.echo.music.R
import iad1tya.echo.music.data.EchoBrainRepository
import iad1tya.echo.music.engine.EchoBrainEngine
import iad1tya.echo.music.engine.brain.*
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.Material3SettingsItem
import iad1tya.echo.music.ui.component.Material3SettingsGroup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoBrainScreen(
    navController: NavController,
    engine: EchoBrainEngine,
    repository: EchoBrainRepository, highlightKey: String? = null) {
    val scrollState = androidx.compose.foundation.rememberScrollState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userBrain by remember { mutableStateOf<UserBrain?>(null) }
    var persona by remember { mutableStateOf<EchoBrainPersona?>(null) }
    var isLoaded by remember { mutableStateOf(false) }
    val isEnabled by engine.isEnabled.collectAsState()

    LaunchedEffect(Unit) {
        userBrain = engine.getBrainSnapshot()
        userBrain?.let { persona = engine.neuroEngine.getPersona(it) }
        delay(300)
        isLoaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.echo_brain_beta)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            userBrain = engine.getBrainSnapshot()
                            userBrain?.let { persona = engine.neuroEngine.getPersona(it) }
                        }
                    }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (userBrain == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            AnimatedVisibility(
                visible = isLoaded,
                enter = fadeIn() + slideInVertically { it / 6 }
            ) {
                val brain = userBrain!!
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(Modifier.height(8.dp))

                    Material3SettingsGroup(scrollState = scrollState, 
                        items = listOf(
                            Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.echo_brain_enable)),
                                title = { Text(stringResource(R.string.echo_brain_enable)) },
                                description = { Text(stringResource(R.string.echo_brain_enable_desc)) },
                                icon = rememberVectorPainter(Icons.Outlined.AutoAwesome),
                                trailingContent = {
                                    val coroutineScope = rememberCoroutineScope()
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { isChecked ->
                                            engine.isEnabled.value = isChecked
                                            coroutineScope.launch {
                                                context.dataStore.edit { prefs ->
                                                    prefs[iad1tya.echo.music.constants.EchoBrainEnabledKey] = isChecked
                                                }
                                            }
                                        }
                                    )
                                },
                                onClick = { 
                                    engine.isEnabled.value = !isEnabled 
                                    scope.launch {
                                        context.dataStore.edit { prefs ->
                                            prefs[iad1tya.echo.music.constants.EchoBrainEnabledKey] = !isEnabled
                                        }
                                    }
                                }
                            )
                        )
                    )
                    
                    Text(
                        text = stringResource(R.string.echo_brain_profile_statistics),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    
                    val initiateText = stringResource(R.string.echo_brain_initiate)
                    Material3SettingsGroup(scrollState = scrollState, 
                        items = listOf(
                            Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.echo_brain_learning_level)),
                                title = { Text(stringResource(R.string.echo_brain_learning_level)) },
                                description = { Text(persona?.title ?: initiateText) },
                                icon = rememberVectorPainter(Icons.Outlined.Psychology)
                            ),
                            Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.echo_brain_total_interactions)),
                                title = { Text(stringResource(R.string.echo_brain_total_interactions)) },
                                description = { Text(stringResource(R.string.echo_brain_interactions_tracked, brain.totalInteractions)) },
                                icon = rememberVectorPainter(Icons.Outlined.Analytics)
                            ),
                            Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.echo_brain_discovered_genres)),
                                title = { Text(stringResource(R.string.echo_brain_discovered_genres)) },
                                description = { Text(stringResource(R.string.echo_brain_distinct_genres_identified, brain.topicAffinities.size)) },
                                icon = rememberVectorPainter(Icons.Outlined.LibraryMusic)
                            ),
                            Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.echo_brain_analyzed_artists)),
                                title = { Text(stringResource(R.string.echo_brain_analyzed_artists)) },
                                description = { Text(stringResource(R.string.echo_brain_artists_mapped, brain.artistScores.size)) },
                                icon = rememberVectorPainter(Icons.Outlined.Person)
                            )
                        )
                    )
                    
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
