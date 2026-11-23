package com.music.echo.ui.screens.settings.cloud

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.echo.cloud.JellyfinPlaylist
import com.music.echo.cloud.JellyfinRepository
import echo.music.iad1tya.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JellyfinLoginScreen(
  navController: NavController,
  repository: JellyfinRepository
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  var serverUrl by remember { mutableStateOf("") }
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var isPasswordVisible by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Jellyfin") },
        navigationIcon = {
          IconButton(onClick = navController::navigateUp) {
            Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
          }
        }
      )
    }
  ) { padding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier.padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_jellyfin),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(56.dp)
        )
      }

      Text(
        text = "Connect Jellyfin Server",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Stream bit-exact lossless FLAC directly from your personal Jellyfin media library.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp)
      )

      Spacer(modifier = Modifier.height(24.dp))

      OutlinedTextField(
        value = serverUrl,
        onValueChange = { serverUrl = it; errorMessage = null },
        label = { Text("Server URL & Port") },
        placeholder = { Text("http://192.168.1.100:8096") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        leadingIcon = {
          Icon(painterResource(R.drawable.globe), contentDescription = null)
        }
      )

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SuggestionChip(
          onClick = { if (!serverUrl.startsWith("http")) serverUrl = "http://$serverUrl" },
          label = { Text("Add http://") }
        )
        SuggestionChip(
          onClick = { if (!serverUrl.startsWith("http")) serverUrl = "https://$serverUrl" },
          label = { Text("Add https://") }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = username,
        onValueChange = { username = it; errorMessage = null },
        label = { Text("Username") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        leadingIcon = {
          Icon(painterResource(R.drawable.person), contentDescription = null)
        }
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = password,
        onValueChange = { password = it; errorMessage = null },
        label = { Text("Password") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        leadingIcon = {
          Icon(painterResource(R.drawable.lock), contentDescription = null)
        },
        trailingIcon = {
          IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
            Icon(
              painter = painterResource(if (isPasswordVisible) R.drawable.visibility else R.drawable.visibility_off),
              contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
            )
          }
        }
      )

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = errorMessage ?: "",
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = {
          if (serverUrl.isBlank() || username.isBlank()) {
            errorMessage = "Please enter server URL and username."
            return@Button
          }
          val normalizedUrl = if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            "http://$serverUrl"
          } else serverUrl

          isLoading = true
          errorMessage = null
          scope.launch {
            val result = repository.login(normalizedUrl, username, password)
            isLoading = false
            result.fold(
              onSuccess = {
                Toast.makeText(context, "Connected to Jellyfin server!", Toast.LENGTH_SHORT).show()
                navController.navigate("settings/jellyfin_dashboard") {
                  popUpTo("settings/jellyfin_login") { inclusive = true }
                }
              },
              onFailure = {
                errorMessage = it.localizedMessage ?: "Failed to connect to Jellyfin server. Please check your credentials and URL."
              }
            )
          }
        },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(25.dp)
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Connecting...")
        } else {
          Text("Connect Jellyfin", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JellyfinDashboardScreen(
  navController: NavController,
  repository: JellyfinRepository
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val serverUrl by repository.serverUrlFlow.collectAsState()
  val username by repository.usernameFlow.collectAsState()

  var isSyncing by remember { mutableStateOf(false) }
  var isTesting by remember { mutableStateOf(false) }
  var playlists by remember { mutableStateOf<List<JellyfinPlaylist>>(emptyList()) }

  LaunchedEffect(Unit) {
    repository.getPlaylists().onSuccess {
      playlists = it
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Jellyfin") },
        navigationIcon = {
          IconButton(onClick = navController::navigateUp) {
            Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
          }
        }
      )
    }
  ) { padding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(padding)
          .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              painter = painterResource(R.drawable.ic_jellyfin),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = username ?: "Connected Account",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = serverUrl ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            AssistChip(
              onClick = {},
              label = { Text("Online", color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold) },
              leadingIcon = {
                Box(
                  modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2E7D32))
                )
              }
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                isTesting = true
                scope.launch {
                  val result = repository.ping()
                  isTesting = false
                  result.fold(
                    onSuccess = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                    onFailure = { Toast.makeText(context, "Ping failed: ${it.message}", Toast.LENGTH_SHORT).show() }
                  )
                }
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            ) {
              if (isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              } else {
                Text("Test Ping")
              }
            }

            Button(
              onClick = {
                isSyncing = true
                scope.launch {
                  val result = repository.syncPlaylistsAndSongs()
                  isSyncing = false
                  result.fold(
                    onSuccess = { count ->
                      Toast.makeText(context, "Synced $count Jellyfin songs to Library!", Toast.LENGTH_SHORT).show()
                      repository.getPlaylists().onSuccess { playlists = it }
                    },
                    onFailure = {
                      Toast.makeText(context, "Sync failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                  )
                }
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            ) {
              if (isSyncing) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Syncing...")
              } else {
                Text("Sync to Library")
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Jellyfin Playlists (${playlists.size})",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )

      Spacer(modifier = Modifier.height(8.dp))

      if (playlists.isEmpty()) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
          Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              painter = painterResource(R.drawable.queue_music),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No playlists found",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Tap 'Sync to Library' to import audio from your server",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(playlists) { pl ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                val coverUrl = if (pl.hasCover) repository.getItemImageUrl(pl.id) else null
                if (!coverUrl.isNullOrBlank()) {
                  AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                  )
                } else {
                  Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      painter = painterResource(R.drawable.queue_music),
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = pl.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                  )
                  Text(
                    text = "${pl.songCount} tracks · Lossless Direct Stream",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedButton(
        onClick = {
          repository.logout()
          Toast.makeText(context, "Disconnected from Jellyfin", Toast.LENGTH_SHORT).show()
          navController.navigateUp()
        },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text("Disconnect Server")
      }
    }
  }
}
