@file:OptIn(ExperimentalMaterial3Api::class)

package com.music.echo.ui.screens.settings.lossless

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.echo.lossless.DeezerCredentials
import com.music.echo.lossless.LosslessProvider
import com.music.echo.lossless.LosslessSourcesRepository
import echo.music.iad1tya.R
import kotlinx.coroutines.launch

@Composable
fun LosslessSourcesScreen(
  navController: NavController,
  repository: LosslessSourcesRepository
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val state by repository.state.collectAsState()

  var selectedProviderForDisconnect by remember { mutableStateOf<LosslessProvider?>(null) }
  var showManualDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            stringResource(R.string.settings_category_lossless_title),
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          IconButton(onClick = { navController.navigateUp() }) {
            Icon(
              painterResource(R.drawable.arrow_back),
              contentDescription = stringResource(R.string.cd_back)
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        )
      )
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(paddingValues),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Top Master Toggle Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
          )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { scope.launch { repository.setPreferLossless(!state.preferLossless) } }
              .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                painter = painterResource(R.drawable.badge_lossless),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.prefer_lossless_audio_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = stringResource(R.string.prefer_lossless_audio_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
              checked = state.preferLossless,
              onCheckedChange = { checked ->
                scope.launch { repository.setPreferLossless(checked) }
              }
            )
          }
        }
      }

      // Provider Accounts Section Header
      item {
        Text(
          text = "Streaming Accounts (Tap to Sign In)",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )
      }

      // Deezer Card
      item {
        ProviderAccountCard(
          provider = LosslessProvider.DEEZER,
          isConnected = state.deezer.isValid,
          subtitle = if (state.deezer.isValid) {
            "Connected as ${state.deezer.userName ?: "Deezer User"} • FLAC 1411 kbps"
          } else {
            "Sign in directly in the app. Session cookie is saved automatically."
          },
          onClick = {
            if (state.deezer.isValid) {
              selectedProviderForDisconnect = LosslessProvider.DEEZER
            } else {
              navController.navigate("settings/lossless/login/${LosslessProvider.DEEZER.name}")
            }
          }
        )
      }

      // Tidal Card
      item {
        ProviderAccountCard(
          provider = LosslessProvider.TIDAL,
          isConnected = state.tidal.isValid,
          subtitle = if (state.tidal.isValid) {
            "Connected as ${state.tidal.userName ?: "Tidal User"} • Hi-Res FLAC"
          } else {
            "Sign in directly in the app. Session is captured automatically."
          },
          onClick = {
            if (state.tidal.isValid) {
              selectedProviderForDisconnect = LosslessProvider.TIDAL
            } else {
              navController.navigate("settings/lossless/login/${LosslessProvider.TIDAL.name}")
            }
          }
        )
      }

      // Qobuz Card
      item {
        ProviderAccountCard(
          provider = LosslessProvider.QOBUZ,
          isConnected = state.qobuz.isValid,
          subtitle = if (state.qobuz.isValid) {
            "Connected as ${state.qobuz.userName ?: "Qobuz Studio"} • 24-Bit / 192kHz"
          } else {
            "Sign in directly in the app. Credentials captured automatically."
          },
          onClick = {
            if (state.qobuz.isValid) {
              selectedProviderForDisconnect = LosslessProvider.QOBUZ
            } else {
              navController.navigate("settings/lossless/login/${LosslessProvider.QOBUZ.name}")
            }
          }
        )
      }

      // Apple Music Card
      item {
        ProviderAccountCard(
          provider = LosslessProvider.APPLE_MUSIC,
          isConnected = state.appleMusic.isValid,
          subtitle = if (state.appleMusic.isValid) {
            "Connected • Apple Lossless (ALAC)"
          } else {
            "Sign in with your Apple ID. Token fetched automatically."
          },
          onClick = {
            if (state.appleMusic.isValid) {
              selectedProviderForDisconnect = LosslessProvider.APPLE_MUSIC
            } else {
              navController.navigate("settings/lossless/login/${LosslessProvider.APPLE_MUSIC.name}")
            }
          }
        )
      }

      // Information card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
          )
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(
              imageVector = Icons.Rounded.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Sign in directly in the app without having to copy-paste tokens. Connected accounts resolve bit-exact lossless audio from their streaming servers with automatic fallback to standard audio when unavailable.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 18.sp
            )
          }
        }
      }

      // Manual / Advanced Entry Option
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { showManualDialog = true }
            .padding(vertical = 12.dp, horizontal = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.Tune,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Advanced / Manual ARL & Token Entry",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }

  // Disconnect Confirmation Dialog
  selectedProviderForDisconnect?.let { provider ->
    AlertDialog(
      onDismissRequest = { selectedProviderForDisconnect = null },
      title = { Text("Disconnect ${provider.displayName}?") },
      text = {
        Text("Lossless playback for this provider will be disabled. You can reconnect anytime by signing in.")
      },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              repository.disconnect(provider)
              selectedProviderForDisconnect = null
              Toast.makeText(context, "${provider.displayName} disconnected", Toast.LENGTH_SHORT).show()
            }
          }
        ) {
          Text("Disconnect", color = MaterialTheme.colorScheme.onError)
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedProviderForDisconnect = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Manual ARL / Token fallback dialog
  if (showManualDialog) {
    ManualCredentialsDialog(
      onDismiss = { showManualDialog = false },
      onSaveDeezer = { arl ->
        scope.launch {
          val res = repository.verifyAndSaveDeezer(arl)
          if (res.isSuccess) {
            Toast.makeText(context, "Connected to Deezer!", Toast.LENGTH_SHORT).show()
            showManualDialog = false
          } else {
            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed", Toast.LENGTH_SHORT).show()
          }
        }
      }
    )
  }
}

@Composable
private fun ProviderAccountCard(
  provider: LosslessProvider,
  isConnected: Boolean,
  subtitle: String,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(
            if (isConnected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          painter = painterResource(
            if (provider == LosslessProvider.APPLE_MUSIC) R.drawable.ic_apple_lossless
            else R.drawable.badge_lossless
          ),
          contentDescription = null,
          tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(24.dp)
        )
      }
      Spacer(modifier = Modifier.width(16.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = provider.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(8.dp))
          if (isConnected) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "Hi-Fi Active",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Icon(
        painter = painterResource(
          if (isConnected) R.drawable.check else R.drawable.arrow_forward
        ),
        contentDescription = null,
        tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

@Composable
private fun ManualCredentialsDialog(
  onDismiss: () -> Unit,
  onSaveDeezer: (String) -> Unit
) {
  var arl by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Manual Deezer ARL Entry") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "If Deezer is unavailable in your region, sign in from any browser (using a VPN), copy the 'arl' cookie value and paste it below:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
          value = arl,
          onValueChange = { arl = it },
          label = { Text("ARL Cookie") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSaveDeezer(arl) },
        enabled = arl.isNotBlank()
      ) {
        Text("Verify & Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
