package iad1tya.echo.music.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

@Composable
fun ReleaseNotesCard() {
    var releaseNotes by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        releaseNotes = fetchReleaseNotesText()
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.release_notes),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            releaseNotes.forEach { note ->
                Text(
                    text = "• $note",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

suspend fun fetchReleaseNotesText(): List<String> {
    return withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("https://api.github.com/repos/iad1tya/Echo-Music/releases/latest")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-cache")
            
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            
            val json = org.json.JSONObject(responseText)
            val body = json.getString("body")
            
            // Parse markdown body into list items
            body.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map { line ->
                    // Remove markdown formatting
                    line.replace(Regex("^[*-]\\s+"), "")
                        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // Bold
                        .replace(Regex("\\*(.*?)\\*"), "$1") // Italic
                        .replace(Regex("`(.*?)`"), "$1") // Code
                }
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            listOf("Error loading release notes: ${e.message}")
        }
    }
}
