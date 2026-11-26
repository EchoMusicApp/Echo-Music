package echo.music.iad1tya.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.lansync.LanSyncManager
import echo.music.iad1tya.ui.component.IconButton
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
import echo.music.iad1tya.ui.utils.backToMain
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoConnectSettingsScreen(
  navController: NavController,
  scrollBehavior: TopAppBarScrollBehavior,
) {
  val context = LocalContext.current
  val lanManager = remember(context) { LanSyncManager.getInstance(context) }

  val peers by lanManager.peers.collectAsState()
  val pairedPeerId by lanManager.pairedPeerId.collectAsState()
  val pairedPeerName by lanManager.pairedPeerName.collectAsState()
  val activeRemotePeer by lanManager.activeRemotePeer.collectAsState()
  val pairingSession by lanManager.pairingSession.collectAsState()
  val syncBusy by lanManager.syncBusy.collectAsState()
  val syncDetails by lanManager.syncDetails.collectAsState()
  val syncLogs by lanManager.syncLogs.collectAsState()
  val statusMessage by lanManager.statusMessage.collectAsState()

  val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

  Column(modifier = Modifier.fillMaxSize()) {
    TopAppBar(
      title = { Text("Echo Connect") },
      navigationIcon = {
        IconButton(
          onClick = navController::navigateUp,
          onLongClick = navController::backToMain,
        ) {
          Icon(
            painter = painterResource(R.drawable.arrow_back),
            contentDescription = null,
          )
        }
      },
      scrollBehavior = scrollBehavior,
    )

    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
          .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Hero / Overview Card
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.padding(16.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            modifier = Modifier.size(46.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                painter = painterResource(R.drawable.ic_echo_connect),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Echo Connect",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text =
                "Auto-syncs Google account, local & Spotify playlists, podcasts, and playback every 5 minutes.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      if (statusMessage != null) {
        Text(
          text = statusMessage!!,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
      }

      // Active 6-digit pairing code card (60s auto-refresh)
      pairingSession?.let { session ->
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surfaceContainer,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "Pair with ${session.peer.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
              )
              TextButton(onClick = { lanManager.cancelPairing() }) { Text("Cancel") }
            }
            Text(
              text = "Enter this code in the popup on ${session.peer.name}:",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
              text = session.code.chunked(3).joinToString(" "),
              style =
                MaterialTheme.typography.displaySmall.copy(
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 4.sp,
                ),
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
              progress = { (session.secondsLeft / 60f).coerceIn(0f, 1f) },
              modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
              text = "Refreshes in ${session.secondsLeft}s",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Paired Device + Playback Handoff & Force Sync
      if (!pairedPeerId.isNullOrBlank()) {
        val onlinePeer = peers.firstOrNull { it.id == pairedPeerId }
        val isLive = activeRemotePeer?.id == pairedPeerId
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surfaceContainer,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(
                painter = painterResource(R.drawable.ic_echo_connect),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = pairedPeerName ?: "Paired Desktop",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text =
                    when {
                      isLive -> "Playing / Controlling remotely"
                      onlinePeer != null -> "Bound • Online on Wi-Fi"
                      else -> "Bound • Waiting for Wi-Fi"
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            if (onlinePeer != null) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                FilledTonalButton(
                  onClick = {
                    if (isLive) lanManager.disconnectRemotePeer()
                    else lanManager.handoffToPeer(onlinePeer)
                  },
                  modifier = Modifier.weight(1f),
                ) {
                  Text(
                    if (isLive) "Disconnect" else "Play on ${onlinePeer.name}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                OutlinedButton(
                  onClick = { lanManager.continueFromPeer(onlinePeer) },
                  modifier = Modifier.weight(1f),
                ) {
                  Text("Move to Phone", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
            }

            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Button(
                onClick = { lanManager.forceSyncNow(onlinePeer) },
                enabled = !syncBusy && onlinePeer != null,
                modifier = Modifier.weight(1f),
              ) {
                if (syncBusy) {
                  CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                  Spacer(Modifier.width(8.dp))
                  Text("Syncing...")
                } else {
                  Icon(
                    Icons.Filled.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(Modifier.width(6.dp))
                  Text("Force Sync")
                }
              }
              OutlinedButton(
                onClick = { lanManager.unpair(onlinePeer) },
                modifier = Modifier.weight(1f),
              ) {
                Text("Unpair")
              }
            }
          }
        }
      }

      // Available Devices using native Material3SettingsGroup
      Material3SettingsGroup(
        title = "Available Devices",
        items =
          if (peers.isEmpty()) {
            listOf(
              Material3SettingsItem(
                icon = painterResource(R.drawable.ic_echo_connect),
                title = { Text("Searching Wi-Fi...") },
                description = { Text("Open Echo Music on your Desktop on the same Wi-Fi") },
                onClick = {},
              ),
            )
          } else {
            peers.map { peer ->
              val isPaired = peer.id == pairedPeerId
              val peerIcon =
                if (peer.platform.equals("android", ignoreCase = true)) Icons.Filled.PhoneAndroid
                else Icons.Filled.Computer
              Material3SettingsItem(
                customIcon = {
                  Icon(
                    imageVector = peerIcon,
                    contentDescription = null,
                    tint =
                      if (isPaired) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                },
                title = { Text(peer.name) },
                description = {
                  Text(
                    if (isPaired) "Bound • ${peer.host}" else "Tap to pair • ${peer.host}",
                  )
                },
                onClick = {
                  if (!isPaired) {
                    lanManager.startPairing(peer)
                  }
                },
              )
            }
          },
      )

      // Sync Details Card
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Sync Details",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
          )
          val lastText =
            if (syncDetails.lastSyncAtMs > 0L) timeFmt.format(Date(syncDetails.lastSyncAtMs))
            else "Never"
          SyncDetailRow("Last Sync", lastText)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
          SyncDetailRow("Google Account", syncDetails.accountStatus)
          SyncDetailRow("Local Playlists", syncDetails.localPlaylistsStatus)
          SyncDetailRow("Spotify Playlists", syncDetails.spotifyPlaylistsStatus)
          SyncDetailRow("Podcast Episodes", syncDetails.episodesStatus)

          if (syncLogs.isNotEmpty()) {
            HorizontalDivider(
              modifier = Modifier.padding(vertical = 4.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
            Text(
              text = "Recent Activity",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            syncLogs.take(8).forEach { entry ->
              Text(
                text = entry,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
private fun SyncDetailRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
