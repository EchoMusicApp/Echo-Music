package iad1tya.echo.music.utils

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber

object JioSaavnFallback {
    private val client = OkHttpClient()

    private val SERVERS = listOf(
        "jiosaavn-api.pc-adityadav9532.workers.dev",
        "jiosaavn-api.mac-adityadav9532.workers.dev",
        "saavn.sumit.co",
        "jiosaavn-apix.arcadopredator.workers.dev",
        "api.music.vispark.in"
    )

    private fun getDomains(context: Context): List<String> {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "default_id"
        val hash = kotlin.math.abs(androidId.hashCode())
        val startIndex = hash % SERVERS.size
        
        val orderedServers = mutableListOf<String>()
        for (i in SERVERS.indices) {
            orderedServers.add(SERVERS[(startIndex + i) % SERVERS.size])
        }
        return orderedServers
    }

    private fun strictTitleMatch(expected: String, actual: String): Boolean {
        val expectedWords = expected.lowercase().replace(Regex("[^a-z0-9 ]"), " ").split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
        val actualWords = actual.lowercase().replace(Regex("[^a-z0-9 ]"), " ").split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
        
        if (expectedWords.isEmpty() || actualWords.isEmpty()) return false
        if (expectedWords == actualWords) return true
        
        // Prevent false positives for completely different versions like remixes or covers
        val modifiers = setOf("remix", "cover", "lofi", "acoustic", "instrumental", "slowed", "reverb", "karaoke", "version", "mix", "mashup", "unplugged", "live", "edit", "8d", "bass")
        
        if (actualWords.containsAll(expectedWords)) {
            val actualModifiers = actualWords.intersect(modifiers)
            val expectedModifiers = expectedWords.intersect(modifiers)
            if (actualModifiers.isNotEmpty() && !expectedModifiers.containsAll(actualModifiers)) {
                return false
            }
            return true
        }

        if (expectedWords.containsAll(actualWords)) {
            return true
        }
        
        val expectedClean = expected.lowercase().replace(Regex("[^a-z0-9]"), "")
        val actualClean = actual.lowercase().replace(Regex("[^a-z0-9]"), "")
        if (expectedClean == actualClean) return true
        
        return false
    }

    private fun strictArtistMatch(expected: String, actual: String): Boolean {
        val expectedClean = expected.lowercase().replace(Regex("[^a-z0-9]"), "")
        val actualClean = actual.lowercase().replace(Regex("[^a-z0-9]"), "")
        if (expectedClean.isEmpty() || actualClean.isEmpty()) return false
        
        if (expectedClean == actualClean) return true
        if (expectedClean.length > 4 && actualClean.contains(expectedClean)) return true
        if (actualClean.length > 4 && expectedClean.contains(actualClean)) return true
        
        return false
    }

    suspend fun resolveAgeRestrictedSong(context: Context, title: String, artist: String): String? = withContext(Dispatchers.IO) {
        suspend fun searchSaavn(searchQuery: String, expectedTitle: String, expectedArtist: String): String? {
            val domains = getDomains(context)
            for (domain in domains) {
                try {
                    val url = "https://$domain/api/search/songs?query=${java.net.URLEncoder.encode(searchQuery, "UTF-8")}"
                    Timber.tag("JioSaavnFallback").d("Requesting URL: $url")
                    val request = Request.Builder().url(url).build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        Timber.tag("JioSaavnFallback").e("JioSaavn request failed for $domain: ${response.code}")
                        continue
                    }

                    val bodyString = response.body?.string()
                    if (bodyString == null) {
                        continue
                    }
                    val json = JSONObject(bodyString)

                    if (json.getBoolean("success")) {
                        val dataObj = json.optJSONObject("data")
                        val results = dataObj?.optJSONArray("results") ?: json.optJSONArray("data")?.let { 
                            if (it.length() > 0) it.getJSONObject(0).optJSONArray("results") else null 
                        } ?: json.getJSONObject("data").getJSONArray("results")
                        
                        for (i in 0 until results.length()) {
                            val result = results.getJSONObject(i)
                            val resultName = result.optString("name", "")
                            
                            val titleMatches = expectedTitle.isEmpty() || strictTitleMatch(expectedTitle, resultName)
                            
                            var artistMatches = false
                            if (expectedArtist.isEmpty()) {
                                artistMatches = true
                            } else {
                                val artistsObj = result.optJSONObject("artists")
                                if (artistsObj != null) {
                                    val primaryArtists = artistsObj.optJSONArray("primary")
                                    val allArtists = artistsObj.optJSONArray("all")
                                    
                                    val checkArtists = { arr: org.json.JSONArray? ->
                                        if (arr != null) {
                                            for (j in 0 until arr.length()) {
                                                val aName = arr.getJSONObject(j).optString("name", "")
                                                if (strictArtistMatch(expectedArtist, aName)) {
                                                    artistMatches = true
                                                    break
                                                }
                                            }
                                        }
                                    }
                                    checkArtists(primaryArtists)
                                    if (!artistMatches) checkArtists(allArtists)
                                }
                                
                                if (!artistMatches) {
                                    val primaryStr = result.optString("primaryArtists", "")
                                    val singersStr = result.optString("singers", "")
                                    if (strictArtistMatch(expectedArtist, primaryStr) || strictArtistMatch(expectedArtist, singersStr)) {
                                        artistMatches = true
                                    }
                                }
                            }

                            if (titleMatches && artistMatches) {
                                val downloadUrls = result.optJSONArray("downloadUrl")
                                if (downloadUrls != null && downloadUrls.length() > 0) {
                                    Timber.tag("JioSaavnFallback").d("Found incredibly strict match for $expectedTitle by $expectedArtist from $domain")
                                    val bestQualityUrlObj = downloadUrls.getJSONObject(downloadUrls.length() - 1)
                                    return bestQualityUrlObj.getString("url")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error resolving JioSaavn fallback for $domain")
                }
            }
            return null
        }

        val fullQuery = "$title $artist".trim()
        var result = searchSaavn(fullQuery, title, artist)
        
        if (result == null && artist.isNotEmpty()) {
            Timber.tag("JioSaavnFallback").d("Strict search with artist failed, trying title only for search query: $title")
            result = searchSaavn(title, title, artist)
        }
        
        return@withContext result
    }
}
