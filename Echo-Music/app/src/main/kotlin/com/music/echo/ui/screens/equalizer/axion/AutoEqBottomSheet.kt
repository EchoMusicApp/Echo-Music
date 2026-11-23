package echo.music.iad1tya.ui.screens.equalizer.axion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import java.net.URLEncoder
import echo.music.iad1tya.R

@Serializable
data class AutoEqItem(val name: String, val path: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoEqBottomSheet(
    onDismissRequest: () -> Unit,
    onApplyEq: (FloatArray) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<AutoEqItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var applyLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val jsonStr = context.resources.openRawResource(R.raw.autoeq_index)
                    .bufferedReader().use { it.readText() }
                items = Json { ignoreUnknownKeys = true }.decodeFromString(jsonStr)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val filteredItems = remember(query, items) {
        if (query.isBlank()) items.take(100)
        else items.filter { it.name.contains(query, ignoreCase = true) }.take(100)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 16.dp)
        ) {
            Text(
                text = "AutoEq Profiles",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search your headphones...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (applyLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(filteredItems) { item ->
                        ListItem(
                            headlineContent = { Text(item.name) },
                            supportingContent = { Text(item.path.split("/").firstOrNull() ?: "") },
                            modifier = Modifier.clickable {
                                applyLoading = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val nameEncoded = URLEncoder.encode(item.name, "UTF-8").replace("+", "%20")
                                        // Some paths in the index already have %20, so we just append
                                        val url = "https://raw.githubusercontent.com/jaakkopasanen/AutoEq/master/results/${item.path}/${nameEncoded}%20GraphicEQ.txt"
                                        val response = URL(url).readText()
                                        android.util.Log.d("AutoEq", "Fetched ${response.length} bytes for ${item.name}")
                                        // Parse graphic eq string: "GraphicEQ: 20 -0.3; 21 -0.2; ..."
                                        val values = response.replace("GraphicEQ: ", "").split(";")
                                        val parsedBands = values.mapNotNull {
                                            val parts = it.trim().split(" ").filter { it.isNotBlank() }
                                            if (parts.size >= 2) {
                                                val freq = parts[0].toFloatOrNull()
                                                val gain = parts[1].toFloatOrNull()
                                                if (freq != null && gain != null) freq to gain else null
                                            } else null
                                        }
                                        android.util.Log.d("AutoEq", "Parsed ${parsedBands.size} bands")
                                        
                                        // Map to our 10 bands
                                        val targetFrequencies = listOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
                                        val resultGains = FloatArray(10)
                                        for (i in targetFrequencies.indices) {
                                            val targetFreq = targetFrequencies[i]
                                            val closest = parsedBands.minByOrNull { kotlin.math.abs(it.first - targetFreq) }
                                            resultGains[i] = closest?.second ?: 0f
                                        }
                                        android.util.Log.d("AutoEq", "Mapped to 10 bands: ${resultGains.joinToString()}")
                                        withContext(Dispatchers.Main) {
                                            onApplyEq(resultGains)
                                            onDismissRequest()
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("AutoEq", "Error applying EQ", e)
                                        withContext(Dispatchers.Main) { applyLoading = false }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
