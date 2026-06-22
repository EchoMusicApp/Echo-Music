package iad1tya.echo.music.ui.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import iad1tya.echo.music.R
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import androidx.compose.material3.TopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class ServiceStatus(
    val name: String, 
    val url: () -> String, 
    val displayUrl: () -> String = url,
    var status: Status = Status.CHECKING,
    var latencyMs: Long? = null
) {
    enum class Status {
        ONLINE, OFFLINE, CHECKING
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UptimeScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
highlightKey: String? = null) {
    val client = remember { OkHttpClient() }
    val musicServices = remember {
        mutableStateListOf(
            ServiceStatus("YouTube Music", { "https://music.youtube.com" }),
            ServiceStatus("JioSaavn", { com.music.jiosaavn.DeviceRouter.getCurrentServer() }, { "Server ${com.music.jiosaavn.DeviceRouter.getCurrentServerIndex() + 1}" }),
            ServiceStatus("Qobuz", { "https://qobuz.kennyy.com.br" })
        )
    }

    val canvasServices = remember {
        mutableStateListOf(
            ServiceStatus("Echo Canvas", { "https://canvas.echomusic.fun" }),
            ServiceStatus("Tidal Canvas", { "https://api.tidal.com/v1/" })
        )
    }

    val lyricsServices = remember {
        mutableStateListOf(
            ServiceStatus("LRCLib", { "https://lrclib.net" }),
            ServiceStatus("BetterLyrics", { "https://lyrics-api.boidu.dev" }),
            ServiceStatus("Unison", { "https://unison.boidu.dev" }),
            ServiceStatus("Paxsenix", { "https://lyrics.paxsenix.org" }),
            ServiceStatus("KuGou", { "https://lyrics.kugou.com" }),
            ServiceStatus("YouLyPlus", { "https://lyricsplus.prjktla.my.id" }),
            ServiceStatus("SimpMusic", { "https://api-lyrics.simpmusic.org" })
        )
    }

    val otherServices = remember {
        mutableStateListOf(
            ServiceStatus("Apple Music API", { "https://amp-api.music.apple.com" }),
            ServiceStatus("Echo Find (Shazam)", { "https://amp.shazam.com" })
        )
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            listOf(musicServices, canvasServices, lyricsServices, otherServices).forEach { list ->
                list.forEachIndexed { index, service ->
                    list[index] = service.copy(status = ServiceStatus.Status.CHECKING, latencyMs = null)
                    
                    var latency: Long? = null
                    val isOnline = withContext(Dispatchers.IO) {
                        try {
                            val startTime = System.currentTimeMillis()
                            val request = Request.Builder().url(service.url()).head().build()
                            client.newCall(request).execute().use { response ->
                                latency = System.currentTimeMillis() - startTime
                                response.isSuccessful || response.isRedirect || response.code in 200..405
                            }
                        } catch (e: Exception) {
                            false
                        }
                    }
                    list[index] = service.copy(
                        status = if (isOnline) ServiceStatus.Status.ONLINE else ServiceStatus.Status.OFFLINE,
                        latencyMs = if (isOnline) latency else null
                    )
                }
            }
            delay(60_000L) // 1 minute
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.service_uptime)) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = LocalPlayerAwareWindowInsets.current
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.music_providers),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(musicServices) { service ->
                ServiceStatusCard(service)
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.canvas_providers),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(canvasServices) { service ->
                ServiceStatusCard(service)
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.lyrics_providers),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(lyricsServices) { service ->
                ServiceStatusCard(service)
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.other_services),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(otherServices) { service ->
                ServiceStatusCard(service)
            }
        }
    }
}

@Composable
fun ServiceStatusCard(service: ServiceStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.displayUrl(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statusColor = when (service.status) {
                    ServiceStatus.Status.ONLINE -> Color(0xFF4CAF50)
                    ServiceStatus.Status.OFFLINE -> Color(0xFFF44336)
                    ServiceStatus.Status.CHECKING -> MaterialTheme.colorScheme.primary
                }
                val statusText = when (service.status) {
                    ServiceStatus.Status.ONLINE -> stringResource(R.string.status_online) + if (service.latencyMs != null) " (${service.latencyMs}ms)" else ""
                    ServiceStatus.Status.OFFLINE -> stringResource(R.string.status_offline)
                    ServiceStatus.Status.CHECKING -> stringResource(R.string.status_checking)
                }
                
                AnimatedContent(
                    targetState = service.status,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "statusIcon"
                ) { status ->
                    if (status == ServiceStatus.Status.CHECKING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = statusColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                id = if (status == ServiceStatus.Status.ONLINE) R.drawable.check else R.drawable.error
                            ),
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
