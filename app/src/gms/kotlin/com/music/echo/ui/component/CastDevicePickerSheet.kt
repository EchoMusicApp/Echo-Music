package echo.music.iad1tya.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import echo.music.iad1tya.R
import timber.log.Timber

/**
 * Heuristic Cast device type based on route name/description keywords.
 */
internal enum class CastDeviceType(val icon: Int, val connectedIcon: Int) {
    TV(R.drawable.cast_tv, R.drawable.cast_tv_connected),
    SPEAKER(R.drawable.cast_speaker, R.drawable.cast_speaker_connected),
    CHROMECAST(R.drawable.cast, R.drawable.cast_connected),
    UNKNOWN(R.drawable.cast, R.drawable.cast_connected);

    companion object {
        private val TV_KEYWORDS = listOf(
            "tv", "bravia", "webos", "tizen", "fire tv", "android tv",
            "google tv", "smart tv", "display", "shield",
            "monitor", "hisense", "tcl", "samsung tv", "lg tv", "sony tv",
            "vizio", "insignia", "toshiba tv", "jvc", "philips tv",
            "androidtv"
        )
        private val SPEAKER_KEYWORDS = listOf(
            "home", "nest", "speaker", "audio", "max", "mini",
            "echo", "alexa", "sonos", "harman", "jabra",
            "bose", "bang", "olufsen", "marshall", "jbl",
            "soundbar", "sound bar", "subwoofer", "receiver",
            "amplifier", "amp", "hifi", "stereo"
        )
        private val CHROMECAST_KEYWORDS = listOf(
            "chromecast", "ecast"
        )

        fun fromName(name: String, description: String?): CastDeviceType {
            val text = "$name ${description ?: ""}".lowercase()
            return when {
                // Check the specific "Chromecast with Google TV" phrasing before the
                // generic Chromecast match so it classifies as a TV.
                TV_KEYWORDS.any { text.contains(it) } -> TV
                CHROMECAST_KEYWORDS.any { text.contains(it) } -> CHROMECAST
                SPEAKER_KEYWORDS.any { text.contains(it) } -> SPEAKER
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Custom Cast device picker bottom sheet with Material3 design.
 *
 * Uses MediaRouter callbacks for real-time device discovery and connects
 * via MediaRouter.selectRoute() with fresh route references — same
 * mechanism the system MediaRouteChooserDialog uses internally.
 */
@Composable
fun CastDevicePickerSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // MediaRouter setup for device discovery
    var mediaRouter by remember { mutableStateOf<MediaRouter?>(null) }
    val discoveredRoutes = remember { mutableStateListOf<MediaRouter.RouteInfo>() }
    var isScanning by remember { mutableStateOf(true) }

    // Build the route selector once
    val selector = remember {
        MediaRouteSelector.Builder()
            .addControlCategory(
                CastMediaControlIntent.categoryForCast(
                    CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
                )
            )
            .build()
    }

    // Set up MediaRouter callback for real-time discovery
    DisposableEffect(Unit) {
        val router = MediaRouter.getInstance(context)
        mediaRouter = router

        val callback = object : MediaRouter.Callback() {
            override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
                if (route.matchesSelector(selector) && !route.isDefault) {
                    if (discoveredRoutes.none { it.id == route.id }) {
                        discoveredRoutes.add(route)
                        Timber.d("Cast route added: name=${route.name}, description=${route.description}, extras=${route.extras}")
                    }
                }
            }

            override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
                discoveredRoutes.removeAll { it.id == route.id }
                Timber.d("Cast route removed: ${route.name}")
            }

            override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
                val index = discoveredRoutes.indexOfFirst { it.id == route.id }
                if (index >= 0) {
                    discoveredRoutes[index] = route
                } else if (route.matchesSelector(selector) && !route.isDefault) {
                    discoveredRoutes.add(route)
                }
            }
        }

        router.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)

        // Initial scan
        router.routes.filter { it.matchesSelector(selector) && !it.isDefault }.forEach { route ->
            if (discoveredRoutes.none { it.id == route.id }) {
                discoveredRoutes.add(route)
            }
        }

        onDispose {
            router.removeCallback(callback)
        }
    }

    // Keep scanning for a discovery window before showing empty state
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000L)
        isScanning = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.cast),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Cast to",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Scanning indicator ──────────────────────────────────────
        AnimatedVisibility(visible = isScanning) {
            Column {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // ── Content ─────────────────────────────────────────────────
        when {
            isScanning && discoveredRoutes.isEmpty() -> {
                // Scanning state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            discoveredRoutes.isEmpty() -> {
                // No devices found
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.cast),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No devices found",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Make sure your device is on the same Wi-Fi network",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                // Device list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = discoveredRoutes,
                        key = { it.id }
                    ) { route ->
                        CastDeviceItem(
                            route = route,
                            onClick = {
                                Timber.d("User selected Cast route: ${route.name} (${route.id})")
                                // Find the freshest route reference and select it
                                val freshRoute = mediaRouter?.routes?.firstOrNull { r ->
                                    r.id == route.id
                                } ?: route
                                try {
                                    mediaRouter?.selectRoute(freshRoute)
                                    onDismiss()
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to select Cast route")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CastDeviceItem(
    route: MediaRouter.RouteInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = route.isSelected
    val deviceType = remember(route.name, route.description) {
        CastDeviceType.fromName(route.name, route.description)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        // Device icon with background
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    if (isSelected) deviceType.connectedIcon else deviceType.icon
                ),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Device info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = route.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!route.description.isNullOrBlank()) {
                Text(
                    text = route.description ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Status indicator
        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Connected",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
