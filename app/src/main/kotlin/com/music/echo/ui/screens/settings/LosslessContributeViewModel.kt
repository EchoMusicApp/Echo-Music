package iad1tya.echo.music.ui.screens.settings

import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.utils.LosslessTrack
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import iad1tya.echo.music.utils.LosslessAPI
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import iad1tya.echo.music.utils.dataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.firstOrNull
import iad1tya.echo.music.constants.LosslessGithubTokenKey
import iad1tya.echo.music.constants.LosslessGithubUsernameKey
import iad1tya.echo.music.constants.LosslessGithubAvatarKey

sealed class LosslessContributeState {
    object Initial : LosslessContributeState()
    object NotLoggedIn : LosslessContributeState()
    data class LoggedIn(val username: String, val avatarUrl: String) : LosslessContributeState()
    data class Uploading(val message: String) : LosslessContributeState()
    data class Success(val prUrl: String) : LosslessContributeState()
    data class Error(val message: String) : LosslessContributeState()
}

@HiltViewModel
class LosslessContributeViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<LosslessContributeState>(LosslessContributeState.NotLoggedIn)
    val uiState: StateFlow<LosslessContributeState> = _uiState.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .build()
        
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        val GITHUB_CLIENT_ID = iad1tya.echo.music.BuildConfig.GH_CLIENT_ID
        val GITHUB_CLIENT_SECRET = iad1tya.echo.music.BuildConfig.GH_CLIENT_SECRET
        const val REDIRECT_URI = "echomusic://oauth2callback"
        
        const val TARGET_OWNER = "EchoMusicApp"
        const val TARGET_REPO = "Lossless-Database"
        const val GITHUB_API_URL = "https://api.github.com"
    }

    private var accessToken: String? = null
    
    private var searchJob: Job? = null
    private val _searchResults = MutableStateFlow<List<SongItem>>(emptyList())
    val searchResults: StateFlow<List<SongItem>> = _searchResults.asStateFlow()
    
    private val _recentTracks = MutableStateFlow<List<LosslessTrack>>(emptyList())
    val recentTracks: StateFlow<List<LosslessTrack>> = _recentTracks.asStateFlow()
    
    private val _totalTracks = MutableStateFlow(0)
    val totalTracks: StateFlow<Int> = _totalTracks.asStateFlow()

    init {
        fetchRecentTracks()
        loadSavedSession()
    }
    
    private fun loadSavedSession() {
        viewModelScope.launch {
            val prefs = context.dataStore.data.firstOrNull()
            val token = prefs?.get(LosslessGithubTokenKey)
            val username = prefs?.get(LosslessGithubUsernameKey)
            val avatar = prefs?.get(LosslessGithubAvatarKey)
            
            if (!token.isNullOrEmpty() && !username.isNullOrEmpty() && !avatar.isNullOrEmpty()) {
                accessToken = token
                _uiState.value = LosslessContributeState.LoggedIn(username, avatar)
            }
        }
    }

    private fun fetchRecentTracks() {
        viewModelScope.launch {
            try {
                _recentTracks.value = LosslessAPI.getRecentTracks(15)
                _totalTracks.value = LosslessAPI.getTotalTracksCount()
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch recent tracks")
            }
        }
    }
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()
    
    fun searchInnerTube(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        
        searchJob = viewModelScope.launch {
            delay(500)
            _isSearching.value = true
            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
            _searchResults.value = result?.items?.filterIsInstance<SongItem>() ?: emptyList()
            _isSearching.value = false
        }
    }
    
    suspend fun verifySongDoesNotExist(title: String, artist: String): Boolean {
        return LosslessAPI.search(title, artist) == null
    }

    fun getAuthUrl(): String {
        if (GITHUB_CLIENT_ID.isBlank()) return ""
        return "https://github.com/login/oauth/authorize?client_id=$GITHUB_CLIENT_ID&redirect_uri=$REDIRECT_URI&scope=public_repo%20workflow"
    }

    fun handleOAuthRedirect(uri: Uri) {
        val code = uri.getQueryParameter("code")
        if (code != null) {
            exchangeCodeForToken(code)
        } else {
            _uiState.value = LosslessContributeState.Error("OAuth authorization code missing.")
        }
    }
    
    fun logout() {
        accessToken = null
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs.remove(LosslessGithubTokenKey)
                prefs.remove(LosslessGithubUsernameKey)
                prefs.remove(LosslessGithubAvatarKey)
            }
            _uiState.value = LosslessContributeState.NotLoggedIn
        }
    }

    private fun exchangeCodeForToken(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = LosslessContributeState.Uploading("Authenticating with GitHub...")
            try {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = """
                    {
                        "client_id": "$GITHUB_CLIENT_ID",
                        "client_secret": "$GITHUB_CLIENT_SECRET",
                        "code": "$code",
                        "redirect_uri": "$REDIRECT_URI"
                    }
                """.trimIndent().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://github.com/login/oauth/access_token")
                    .post(requestBody)
                    .addHeader("Accept", "application/json")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val jsonObject = json.parseToJsonElement(responseBody).jsonObject
                        val token = jsonObject["access_token"]?.toString()?.replace("\"", "")
                        
                        if (token != null) {
                            accessToken = token
                            fetchUserProfile()
                        } else {
                            _uiState.value = LosslessContributeState.Error("Failed to parse access token.")
                        }
                    }
                } else {
                    _uiState.value = LosslessContributeState.Error("Authentication failed.")
                }
            } catch (e: Exception) {
                Timber.e(e, "OAuth exchange error")
                _uiState.value = LosslessContributeState.Error("Authentication error: ${e.message}")
            }
        }
    }

    private suspend fun fetchUserProfile() {
        try {
            val request = Request.Builder()
                .url("$GITHUB_API_URL/user")
                .get()
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    val userObj = json.parseToJsonElement(body).jsonObject
                    val username = userObj["login"]?.toString()?.replace("\"", "") ?: "Unknown"
                    val avatar = userObj["avatar_url"]?.toString()?.replace("\"", "") ?: ""
                    _uiState.value = LosslessContributeState.LoggedIn(username, avatar)
                }
            } else {
                _uiState.value = LosslessContributeState.Error("Failed to fetch GitHub profile.")
            }
        } catch (e: Exception) {
            Timber.e(e, "User profile fetch error")
            _uiState.value = LosslessContributeState.Error("Failed to fetch profile: ${e.message}")
        }
    }

    fun submitTrack(songTitle: String, artistName: String, fileBytes: ByteArray, fileName: String) {
        val currentState = _uiState.value
        if (currentState !is LosslessContributeState.LoggedIn || accessToken == null) {
            return
        }

        val username = currentState.username

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Fork Repository
                _uiState.value = LosslessContributeState.Uploading("Connecting to server...")
                val forkResult = forkRepository()
                val forkOwner = forkResult.first
                val forkName = forkResult.second

                // Wait for fork to be ready (up to 15 seconds)
                _uiState.value = LosslessContributeState.Uploading("Preparing secure connection...")
                var isForkReady = false
                for (i in 1..5) {
                    val checkReq = Request.Builder()
                        .url("$GITHUB_API_URL/repos/$forkOwner/$forkName")
                        .get()
                        .addHeader("Authorization", "Bearer $accessToken")
                        .build()
                    val checkResp = httpClient.newCall(checkReq).execute()
                    if (checkResp.isSuccessful) {
                        isForkReady = true
                        break
                    }
                    kotlinx.coroutines.delay(3000)
                }
                if (!isForkReady) {
                    throw Exception("Repository sync timed out. Please try again.")
                }

                // 2. Sync Fork to prevent stale PR branches that modify unrelated files
                _uiState.value = LosslessContributeState.Uploading("Syncing repository with upstream...")
                syncFork(forkOwner, forkName)

                // 3. Create Branch
                _uiState.value = LosslessContributeState.Uploading("Initializing secure transfer...")
                val safeSong = songTitle.replace(Regex("[^a-zA-Z0-9]"), "-").take(20)
                val branchName = "lossless-${username.lowercase()}-$safeSong-${System.currentTimeMillis()}"
                createBranch(forkOwner, forkName, branchName)

                // 4. Upload File
                _uiState.value = LosslessContributeState.Uploading("Uploading high-fidelity audio (this may take a few minutes)...")
                val timestamp = System.currentTimeMillis()
                val safeFileName = "${username.lowercase()}-${timestamp}-${fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}"
                val targetPath = "Music/$safeFileName"
                uploadFile(forkOwner, forkName, branchName, targetPath, fileBytes, songTitle)

                // 5. Create submission JSON
                _uiState.value = LosslessContributeState.Uploading("Updating server database...")
                val trackUrl = "https://raw.githubusercontent.com/EchoMusicApp/Lossless-Database/main/$targetPath"
                val jsonFileName = safeFileName.substringBeforeLast(".") + ".json"
                createSubmissionJson(forkOwner, forkName, branchName, songTitle, artistName, trackUrl, jsonFileName)

                // 6. Create PR
                _uiState.value = LosslessContributeState.Uploading("Finalizing submission...")
                val prUrl = createPullRequest(forkOwner, forkName, branchName, songTitle, artistName, targetPath)

                _uiState.value = LosslessContributeState.Success(prUrl)

            } catch (e: Exception) {
                Timber.e(e, "Submission pipeline error")
                _uiState.value = LosslessContributeState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    private suspend fun forkRepository(): Pair<String, String> {
        val request = Request.Builder()
            .url("$GITHUB_API_URL/repos/$TARGET_OWNER/$TARGET_REPO/forks")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Failed to fork repository")
        
        val body = response.body?.string() ?: throw Exception("Empty response from fork API")
        val obj = json.parseToJsonElement(body).jsonObject
        val owner = obj["owner"]?.jsonObject?.get("login")?.toString()?.replace("\"", "") ?: throw Exception("Fork owner not found")
        val name = obj["name"]?.toString()?.replace("\"", "") ?: TARGET_REPO
        return Pair(owner, name)
    }

    private suspend fun syncFork(forkOwner: String, forkName: String) {
        val syncJson = "{\"branch\":\"main\"}"
        val syncRequest = Request.Builder()
            .url("$GITHUB_API_URL/repos/$forkOwner/$forkName/merge-upstream")
            .post(syncJson.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        val response = httpClient.newCall(syncRequest).execute()
        if (!response.isSuccessful) {
            val code = response.code
            val errorText = response.body?.string() ?: ""
            if (code == 409) {
                throw Exception("Your fork has conflicting changes. Please delete the '$forkName' repository from your GitHub account and try again.")
            } else {
                throw Exception("Failed to sync fork (HTTP $code): $errorText")
            }
        }
    }

    private suspend fun createBranch(forkOwner: String, forkName: String, branchName: String) {
        val refRequest = Request.Builder()
            .url("$GITHUB_API_URL/repos/$TARGET_OWNER/$TARGET_REPO/git/ref/heads/main")
            .get()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        val refResponse = httpClient.newCall(refRequest).execute()
        if (!refResponse.isSuccessful) throw Exception("Failed to get main branch SHA")
        
        val refBody = refResponse.body?.string() ?: ""
        val mainSha = json.parseToJsonElement(refBody).jsonObject["object"]?.jsonObject?.get("sha")?.toString()?.replace("\"", "")
        
        val branchJson = """
            {
                "ref": "refs/heads/$branchName",
                "sha": "$mainSha"
            }
        """.trimIndent()
        
        val branchRequest = Request.Builder()
            .url("$GITHUB_API_URL/repos/$forkOwner/$forkName/git/refs")
            .post(branchJson.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        
        val branchResponse = httpClient.newCall(branchRequest).execute()
        if (!branchResponse.isSuccessful) {
            val errorText = branchResponse.body?.string() ?: ""
            if (!errorText.contains("already exists")) {
                throw Exception("Failed to create branch at ${branchRequest.url}: $errorText")
            }
        }
    }

    private suspend fun uploadFile(forkOwner: String, forkName: String, branchName: String, targetPath: String, fileBytes: ByteArray, songTitle: String) {
        val base64Content = Base64.encodeToString(fileBytes, Base64.NO_WRAP)
        val uploadJson = buildJsonObject {
            put("message", "feat: upload lossless track for $songTitle")
            put("content", base64Content)
            put("branch", branchName)
        }.toString()

        val uploadRequest = Request.Builder()
            .url("$GITHUB_API_URL/repos/$forkOwner/$forkName/contents/$targetPath")
            .put(uploadJson.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val uploadResponse = httpClient.newCall(uploadRequest).execute()
        if (!uploadResponse.isSuccessful) throw Exception("Failed to upload file to GitHub")
    }

    private suspend fun createSubmissionJson(forkOwner: String, forkName: String, branchName: String, songTitle: String, artistName: String, trackUrl: String, jsonFileName: String) {
        val jsonContent = """
            {
              "song": "$songTitle",
              "artist": "$artistName",
              "url": "$trackUrl"
            }
        """.trimIndent()
        
        val newContentBase64 = Base64.encodeToString(jsonContent.toByteArray(), Base64.NO_WRAP)
        
        val putJson = buildJsonObject {
            put("message", "feat: submit $songTitle data")
            put("content", newContentBase64)
            put("branch", branchName)
        }.toString()

        val putRequest = Request.Builder()
            .url("$GITHUB_API_URL/repos/$forkOwner/$forkName/contents/submissions/$jsonFileName")
            .put(putJson.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val putResponse = httpClient.newCall(putRequest).execute()
        if (!putResponse.isSuccessful) throw Exception("Failed to create submission JSON")
    }

    private suspend fun createPullRequest(forkOwner: String, forkName: String, branchName: String, songTitle: String, artistName: String, targetPath: String): String {
        val prTitle = "feat: add lossless track for $songTitle — $artistName"
        val prBody = "This Pull Request was submitted automatically via the Echo Music native app.\n\n### \uD83C\uDFB5 Submission Metadata\n* **Category:** Music\n* **Track URL / Path:** `$targetPath`\n\n### \uD83C\uDFB6 Song Entries\n| Song Title | Artist |\n|---|---|\n| $songTitle | $artistName |"
        
        val prJson = buildJsonObject {
            put("title", prTitle)
            put("head", "$forkOwner:$branchName")
            put("base", "main")
            put("body", prBody)
        }.toString()

        val prRequest = Request.Builder()
            .url("$GITHUB_API_URL/repos/$TARGET_OWNER/$TARGET_REPO/pulls")
            .post(prJson.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
            
        val prResponse = httpClient.newCall(prRequest).execute()
        if (!prResponse.isSuccessful) throw Exception("Failed to create Pull Request")
        
        val prBodyStr = prResponse.body?.string() ?: ""
        return json.parseToJsonElement(prBodyStr).jsonObject["html_url"]?.toString()?.replace("\"", "") ?: ""
    }

}
