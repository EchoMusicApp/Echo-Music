    suspend fun fetchRepository() = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(repoUrl).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!body.isNullOrEmpty()) {
                val manifest = try {
                    json.decodeFromString<EchoExtensionManifest>(body)
                } catch (e: Exception) {
                    val list = json.decodeFromString<List<EchoExtensionItem>>(body)
                    EchoExtensionManifest(list)
                }

                val installedPackages = prefs.getStringSet("installed_packages", emptySet()) ?: emptySet()

                val updatedList = manifest.extensions.map { ext ->
                    val file = File(extensionsDir, "${ext.packageName}.apk")
                    ext.copy(isInstalled = installedPackages.contains(ext.packageName) || file.exists())
                }

                withContext(Dispatchers.Main) {
                    _availableExtensions.value = updatedList
                }
                return@withContext
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback Default Extensions agar internet fetch fail ho jaye
        val fallbackList = listOf(
            EchoExtensionItem(
                id = "spotify",
                name = "Spotify",
                packageName = "echo.extension.spotify",
                version = "1.0.0",
                description = "Spotify Music & Playlists Platform",
                downloadUrl = "https://raw.githubusercontent.com/itsmechinmoy/echo-extensions/main/apks/spotify.apk",
                colorHex = "#1DB954",
                requiresLogin = true
            ),
            EchoExtensionItem(
                id = "gaana",
                name = "Gaana",
                packageName = "echo.extension.gaana",
                version = "1.0.0",
                description = "Gaana Music Platform",
                downloadUrl = "https://raw.githubusercontent.com/itsmechinmoy/echo-extensions/main/apks/gaana.apk",
                colorHex = "#E7232A",
                requiresLogin = false
            ),
            EchoExtensionItem(
                id = "jiosaavn",
                name = "JioSaavn",
                packageName = "echo.extension.jiosaavn",
                version = "1.0.0",
                description = "JioSaavn Music Platform",
                downloadUrl = "https://raw.githubusercontent.com/itsmechinmoy/echo-extensions/main/apks/jiosaavn.apk",
                colorHex = "#282828",
                requiresLogin = false
            )
        )

        val installedPackages = prefs.getStringSet("installed_packages", emptySet()) ?: emptySet()
        val finalFallback = fallbackList.map { ext ->
            val file = File(extensionsDir, "${ext.packageName}.apk")
            ext.copy(isInstalled = installedPackages.contains(ext.packageName) || file.exists())
        }

        withContext(Dispatchers.Main) {
            _availableExtensions.value = finalFallback
        }
    }
