<div align="center">
  <img src="assets/Echo_github.png" alt="Echo Music Logo" width="150"/>
</div>

<div align="center">
  <h1>Echo Music</h1>
  <h3>A Modern, Feature-Rich Music Streaming App for Android</h3>
  <p>Stream music from YouTube Music with AI-powered song suggestions, synced lyrics, and offline playback.</p>
  <p><strong>Current Version: v2.0.1</strong></p>
</div>

<div align="center">
  <a href="https://github.com/iad1tya/Echo-Music/releases/latest">
    <img src="https://img.shields.io/badge/Download-Latest%20Release-brightgreen" alt="Download Latest Release"/>
  </a>
  <a href="https://github.com/iad1tya/Echo-Music/issues">
    <img src="https://img.shields.io/github/issues/iad1tya/Echo-Music" alt="GitHub issues"/>
  </a>
  <a href="https://github.com/iad1tya/Echo-Music/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/iad1tya/Echo-Music" alt="License"/>
  </a>
  <br>
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22iad1tya.echo.music%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fiad1tya%2FEcho-Music%22%2C%22author%22%3A%22iad1tya%22%2C%22name%22%3A%22Echo%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Afalse%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Atrue%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Atrue%2C%5C%22releaseTitleAsVersion%5C%22%3Atrue%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Atrue%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Atrue%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22github-creds%5C%22%3A%5C%22%5C%22%2C%5C%22GHReqPrefix%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3A%22GitHub%22%7D%0A%0A">
    <img src="assets/obtainium.png" alt="Add to Obtainium" width="150"/>
  </a>
</div>

---

## Screenshots

<div align="center">
  <img src="assets/Screenshots/sc_1.png" alt="Home Screen" width="200"/>
  <img src="assets/Screenshots/sc_2.png" alt="Music Player" width="200"/>
  <img src="assets/Screenshots/sc_3.png" alt="Playlist Management" width="200"/>
  <img src="assets/Screenshots/sc_4.png" alt="Settings" width="200"/>
</div>

---

## Key Features

### Music Streaming

* **YouTube Music Integration:** Stream music seamlessly from YouTube Music.
* **Video/Audio Playback:** Switch effortlessly between video and audio modes.
* **Background Playback:** Continue listening while using other apps.
* **Offline Playback:** Download songs for offline listening.

### User Experience

* **Modern Interface:** Developed with Jetpack Compose and Material Design 3.
* **Multi-language Support:** Available in over 20 languages.
* **Customizable Interface:** Adjust colors, layouts, and appearance.

### Discovery and Search

* **Smart Search:** Powerful search across YouTube Music.
* **Best Recommendations:** Personalized song suggestions based on your listening habits.
* **Browsing:** Explore curated categories such as Home, Charts, Podcasts, Moods, and Genres.
* **Recently Played:** Access your recent tracks instantly.

### Advanced Features

* **Synced Lyrics:** Real-time lyric display with translation support.
* **Playlist Management:** Create, edit, sync, and organize playlists with intuitive long-press actions.
* **Sleep Timer:** Automatically stop playback after a set duration.
* **Widgets:** Quick access from your home screen.
* **Firebase Integration:** Optional analytics and crash reporting for improved app stability.

---

## Installation

### Option 1: Download Pre-Built APK

1. Visit the [Releases Page](https://github.com/iad1tya/Echo-Music/releases/latest).
2. Download the latest APK file.
3. Install it on your Android device.

### Option 2: Build from Source

1. **Clone the Repository**

   ```bash
   git clone https://github.com/iad1tya/Echo-Music.git
   cd Echo-Music
   ```

2. **Configure Android SDK**

   ```bash
   cp local.properties.example local.properties
   ```

   Update `local.properties` with your Android SDK path:

   ```properties
   sdk.dir=/path/to/your/android/sdk
   ```

3. **Optional: Configure Firebase**

   ```bash
   cp app/google-services.json.example app/google-services.json
   ```

   Replace placeholders with your Firebase project credentials.

4. **Build the Project**

   ```bash
   ./gradlew assembleFossDebug
   ```

---

## Contributing

We welcome community contributions.

1. **Fork** the repository.
2. **Create a feature branch:**

   ```bash
   git checkout -b feature/new-feature
   ```
3. **Implement your changes** following our coding guidelines.
4. **Run tests:**

   ```bash
   ./gradlew test
   ```
5. **Commit and push:**

   ```bash
   git commit -m "Add new feature"
   git push origin feature/new-feature
   ```
6. **Open a Pull Request** on GitHub.

---

## Development Guidelines

* Follow Kotlin best practices.
* Write clear and meaningful commit messages.
* Include tests for new functionality.
* Update relevant documentation.

---

## License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.
See the [LICENSE](LICENSE) file for details.

---

## Privacy

Echo Music is designed with user privacy in mind:

* **Analytics:** Optional and disabled by default.
* **Crash Reports:** Optional and disabled by default.
* **Personal Data:** No personal information is collected without consent.

---

## Documentation

* **Report Issues or Request Features:** [GitHub Issues](https://github.com/iad1tya/Echo-Music/issues)
* **Additional Documentation:**
  [CONTRIBUTING.md](CONTRIBUTING.md) | [PRIVACY_POLICY.md](PRIVACY_POLICY.md) | [SECURITY.md](SECURITY.md)

---

## ☕ Support the Project
<div align="center">
  <a href="https://buymeacoffee.com/iad1tya">
    <img src="assets/bmac.png" alt="Buy Me a Coffee" width="150"/>
  </a>
  <a href="https://discord.gg/eNFNHaWN97">
    <img src="assets/discord.png" alt="Discord Community" width="150"/>
  </a>
</div>

---

## Contact

<div align="center">
  <p>📧 Email: <a href="mailto:hello@echomusic.fun">hello@echomusic.fun</a></p>
  <p>Made with ❤️ by <a href="https://iad1tya.cyou">Aditya</a></p>
</div>
