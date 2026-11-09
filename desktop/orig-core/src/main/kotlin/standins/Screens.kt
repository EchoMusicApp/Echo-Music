@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.constants.DataSyncIdKey
import echo.music.iad1tya.constants.InnerTubeCookieKey
import echo.music.iad1tya.constants.VisitorDataKey
import echo.music.iad1tya.utils.rememberPreference

@Composable
private fun Unavailable(navController: NavController, title: String, text: String) {
  Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { pad ->
    Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
      Text(text)
      Button(onClick = { navController.navigateUp() }, modifier = Modifier.padding(top = 16.dp)) { Text("Back") }
    }
  }
}

/** Desktop has no WebView to log in with, so the YouTube cookie is pasted in (browser dev tools, request header "cookie"). */
@Composable
fun LoginScreen(navController: NavController) {
  var cookie by rememberPreference(InnerTubeCookieKey, "")
  var visitorData by rememberPreference(VisitorDataKey, "")
  var dataSyncId by rememberPreference(DataSyncIdKey, "")
  var c by remember { mutableStateOf(cookie) }
  var v by remember { mutableStateOf(visitorData) }
  var d by remember { mutableStateOf(dataSyncId) }
  Scaffold(topBar = { TopAppBar(title = { Text("YouTube Music login") }) }) { pad ->
    Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("Paste the cookie header of a signed-in music.youtube.com request.")
      OutlinedTextField(c, { c = it }, label = { Text("Cookie") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
      OutlinedTextField(v, { v = it }, label = { Text("Visitor data (optional)") }, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(d, { d = it }, label = { Text("Data sync id (optional)") }, modifier = Modifier.fillMaxWidth())
      Button(onClick = { cookie = c.trim(); visitorData = v.trim(); dataSyncId = d.trim(); navController.navigateUp() }) { Text("Save") }
    }
  }
}

@Composable
fun SpotifyImportScreen(navController: NavController) =
  Unavailable(navController, "Spotify import", "Spotify import needs an in-app browser login and is not available on desktop yet.")
