package iad1tya.echo.music.ui.screens.settings

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.models.SongItem
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.models.MediaMetadata
import iad1tya.echo.music.playback.queues.ListQueue
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.utils.LosslessTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
fun LosslessTrack.toMediaItem(thumbnailUrl: String? = null) = androidx.media3.common.MediaItem.Builder()
    .setMediaId(url)
    .setUri(url)
    .setCustomCacheKey(url)
    .setTag(
        MediaMetadata(
            id = url,
            title = song,
            artists = listOf(MediaMetadata.Artist(id = null, name = artist)),
            duration = 0,
            album = null,
            thumbnailUrl = thumbnailUrl
        )
    )
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata.Builder()
            .setTitle(song)
            .setArtist(artist)
            .setIsPlayable(true)
            .setArtworkUri(thumbnailUrl?.let { Uri.parse(it) })
            .build()
    )
    .build()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LosslessContributeScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: LosslessContributeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentTracks by viewModel.recentTracks.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current

    var showWebView by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSong by remember { mutableStateOf<SongItem?>(null) }
    var songExistsError by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            var name = "unknown.flac"
            var size = 0L
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    name = c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                    size = c.getLong(c.getColumnIndexOrThrow(android.provider.OpenableColumns.SIZE))
                }
            }
            if (!name.lowercase().endsWith(".flac")) {
                Toast.makeText(context, context.getString(R.string.file_format_error), Toast.LENGTH_SHORT).show()
                return@let
            }
            if (size > 99 * 1024 * 1024) {
                Toast.makeText(context, context.getString(R.string.file_size_error), Toast.LENGTH_SHORT).show()
                return@let
            }
            selectedFileUri = it
            selectedFileName = name
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lossless_music_hub)) },
                navigationIcon = {
                    IconButton(
                        onClick = { if (showWebView) showWebView = false else navController.navigateUp() },
                        onLongClick = { navController.backToMain() }
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is LosslessContributeState.LoggedIn) {
                        val state = uiState as LosslessContributeState.LoggedIn
                        IconButton(onClick = { viewModel.logout() }) {
                            AsyncImage(
                                model = state.avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        if (showWebView) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url ?: return false
                                if (url.toString().startsWith(LosslessContributeViewModel.REDIRECT_URI)) {
                                    showWebView = false
                                    viewModel.handleOAuthRedirect(url)
                                    return true
                                }
                                return false
                            }
                        }
                    }
                },
                update = { webView ->
                    val url = viewModel.getAuthUrl()
                    if (url.isNotEmpty()) {
                        webView.loadUrl(url)
                    } else {
                        Toast.makeText(context, "GitHub Client ID is not configured.", Toast.LENGTH_LONG).show()
                        showWebView = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is LosslessContributeState.NotLoggedIn, LosslessContributeState.Initial -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Icon(
                            painter = painterResource(R.drawable.cloud),
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Contribute to Lossless",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Help us build the ultimate high-fidelity audio library! Connect your account to upload and verify 16-bit or 24-bit FLAC files directly to the Echo Music ecosystem. Ensure files are properly tagged and follow our quality guidelines.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                        Button(
                            onClick = { showWebView = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Icon(painterResource(R.drawable.github), contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Connect Account")
                        }
                        
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        LosslessHubGoalCard()
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        if (recentTracks.isNotEmpty()) {
                            Text(
                                text = "Recently Added",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                recentTracks.forEach { track ->
                                // Extract username from URL assuming format username-filename.flac
                                val fileName = track.url.substringAfterLast("/")
                                val addedBy = fileName.substringBefore("-").replaceFirstChar { it.uppercase() }
                                
                                var thumbnailUrl by remember(track.url) { mutableStateOf<String?>(null) }
                                LaunchedEffect(track.url) {
                                    if (thumbnailUrl == null) {
                                        withContext(Dispatchers.IO) {
                                            val res = com.music.innertube.YouTube.search("${track.song} ${track.artist}", com.music.innertube.YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                            thumbnailUrl = (res?.items?.firstOrNull() as? com.music.innertube.models.SongItem)?.thumbnail
                                        }
                                    }
                                }
                                
                                    ListItem(
                                        headlineContent = { Text(track.song, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        supportingContent = { Text("${track.artist} • added by $addedBy", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        leadingContent = {
                                            if (thumbnailUrl != null) {
                                                AsyncImage(
                                                    model = thumbnailUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    painter = painterResource(R.drawable.music_note),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp).padding(8.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp)),
                                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(iad1tya.echo.music.constants.MiniPlayerHeight + 32.dp))
                    }
                }
                
                is LosslessContributeState.LoggedIn -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (selectedSong == null) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it
                                songExistsError = false
                                viewModel.searchInnerTube(it)
                            },
                            placeholder = { Text("Search for a song...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            leadingIcon = {
                                Icon(painterResource(R.drawable.search), contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { 
                                        searchQuery = "" 
                                        viewModel.searchInnerTube("")
                                    }) {
                                        Icon(painterResource(R.drawable.close), contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            )
                        )
                        
                        if (songExistsError) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.error),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "This song already exists in the lossless library.",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        
                        AnimatedVisibility(visible = searchResults.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(top = 16.dp)
                            ) {
                                items(searchResults) { song ->
                                    val title = song.title
                                    val artist = song.artists.joinToString(", ") { it.name }
                                    
                                    ListItem(
                                        headlineContent = { 
                                            Text(
                                                text = title,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.titleMedium
                                            ) 
                                        },
                                        supportingContent = { 
                                            Text(
                                                text = artist,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ) 
                                        },
                                        leadingContent = {
                                            AsyncImage(
                                                model = song.thumbnail,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                coroutineScope.launch {
                                                    songExistsError = false
                                                    val doesNotExist = viewModel.verifySongDoesNotExist(title, artist)
                                                    if (doesNotExist) {
                                                        selectedSong = song
                                                    } else {
                                                        songExistsError = true
                                                    }
                                                }
                                            },
                                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                                    )
                                }
                            }
                        }
                    } else {
                        // Song selected
                        Text(
                            text = "Selected Track",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = selectedSong?.thumbnail,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedSong?.title ?: "",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = selectedSong?.artists?.joinToString(", ") { it.name } ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { 
                                    selectedSong = null 
                                    selectedFileUri = null
                                    selectedFileName = ""
                                }) {
                                    Icon(painterResource(R.drawable.close), contentDescription = "Clear")
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Audio File",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = if (selectedFileUri == null) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.primaryContainer,
                            onClick = { filePickerLauncher.launch("audio/flac") }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(if (selectedFileUri == null) R.drawable.cloud else R.drawable.done),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = if (selectedFileUri == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (selectedFileName.isEmpty()) "Tap to select .flac" else selectedFileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selectedFileUri == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer,
                                    textAlign = TextAlign.Center
                                )
                                if (selectedFileUri == null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Maximum file size 99MB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                if (selectedFileUri != null && selectedSong != null) {
                                    coroutineScope.launch {
                                        val bytes = withContext(Dispatchers.IO) {
                                            context.contentResolver.openInputStream(selectedFileUri!!)?.readBytes()
                                        }
                                        if (bytes != null) {
                                            viewModel.submitTrack(
                                                songTitle = selectedSong!!.title,
                                                artistName = selectedSong!!.artists.joinToString(", ") { it.name },
                                                fileBytes = bytes,
                                                fileName = selectedFileName
                                            )
                                        } else {
                                            Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = selectedFileUri != null && selectedSong != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Text(
                                "Upload Track",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                
                is LosslessContributeState.Uploading -> {
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(56.dp),
                                strokeWidth = 4.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = "Working...", 
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.message, 
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(2f))
                }
                
                is LosslessContributeState.Success -> {
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.done),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Upload Complete!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your track has been successfully submitted and will be available soon.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            FilledTonalButton(
                                onClick = { uriHandler.openUri(state.prUrl) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("View Submission Details")
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = { 
                                    selectedFileUri = null
                                    selectedFileName = ""
                                    searchQuery = ""
                                    selectedSong = null
                                    viewModel.logout() 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Upload Another Track")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(2f))
                }
                
                is LosslessContributeState.Error -> {
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.error),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Upload Failed",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.message, 
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            FilledTonalButton(
                                onClick = { viewModel.logout() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(2f))
                }
            }
        }
    }
}

@Composable
private fun LosslessHubGoalCard() {
    val context = LocalContext.current
    val fundingState by iad1tya.echo.music.utils.FundingRepository.fundingState.collectAsState()

    LaunchedEffect(Unit) {
        iad1tya.echo.music.utils.FundingRepository.fetchFundingProgress()
    }

    AnimatedVisibility(visible = fundingState != null) {
        fundingState?.let { data ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.graphic_eq),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Server Funding",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "$${data.raised.toInt()} / $200",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val progress by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = (data.raised.toFloat() / 200f).coerceIn(0f, 1f),
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    )

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )

                    FilledTonalButton(
                        onClick = { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/lossless"))) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Support Lossless Music")
                    }
                }
            }
        }
    }
}

