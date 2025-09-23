# Echo Music

<div align="center">
  <img src="Echo_github.png" alt="Echo Music Logo" width="200"/>
</div>

<div align="center">
  <h3>A modern, feature-rich music streaming app for Android</h3>
  <p>Stream music from YouTube Music and Spotify with advanced features like AI song suggestions, synced lyrics, and offline playback.</p>
</div>

<div align="center">
  <a href="https://github.com/iad1tya/Echo-Music">
    <img src="https://img.shields.io/github/stars/iad1tya/Echo-Music?style=social" alt="GitHub stars"/>
  </a>
  <a href="https://github.com/iad1tya/Echo-Music">
    <img src="https://img.shields.io/github/forks/iad1tya/Echo-Music?style=social" alt="GitHub forks"/>
  </a>
  <a href="https://github.com/iad1tya/Echo-Music/issues">
    <img src="https://img.shields.io/github/issues/iad1tya/Echo-Music" alt="GitHub issues"/>
  </a>
  <a href="https://github.com/iad1tya/Echo-Music/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/iad1tya/Echo-Music" alt="License"/>
  </a>
</div>

## Screenshots

<div align="center">
  <img src="Screenshots/sc_1.png" alt="Screenshot 1" width="200"/>
  <img src="Screenshots/sc_2.png" alt="Screenshot 2" width="200"/>
  <img src="Screenshots/sc_3.png" alt="Screenshot 3" width="200"/>
  <img src="Screenshots/sc_4.png" alt="Screenshot 4" width="200"/>
</div>

## What Does Echo Music Do?

Echo Music is a comprehensive music streaming application that provides:

### Music Streaming
- **YouTube Music Integration**: Stream music from YouTube Music and YouTube for free, without ads
- **Spotify Integration**: Access Spotify's vast music library with Canvas support
- **Background Playback**: Continue listening while using other apps
- **High-Quality Audio**: Support for various audio formats and quality settings

### User Experience
- **Modern UI**: Built with Jetpack Compose and Material Design 3
- **Dark Theme**: Beautiful dark mode interface
- **Multi-language Support**: Available in 20+ languages
- **Welcome Flow**: Personalized onboarding experience with user name setup

### Discovery & Search
- **Smart Search**: Search everything on YouTube and Spotify
- **AI Song Suggestions**: Get personalized music recommendations
- **Browse Categories**: Explore Home, Charts, Podcasts, Moods & Genres
- **Trending Content**: Stay updated with the latest music trends
- **Recently Played**: Quick access to your recent music

### Advanced Features
- **Synced Lyrics**: Real-time lyrics from multiple sources (LRCLIB, Spotify, YouTube Transcript)
- **Smart Lyrics Defaults**: Automatic lyrics provider selection based on content type
- **Offline Playback**: Download music for offline listening
- **Playlist Management**: Create, edit, and sync custom playlists
- **Sleep Timer**: Set automatic sleep timer for bedtime listening
- **Android Auto Support**: Seamless integration with Android Auto
- **Artist Notifications**: Get notifications from followed artists

### Technical Features
- **SponsorBlock Integration**: Skip sponsored content automatically
- **1080p Video Support**: High-quality video playback with subtitles
- **Multi-Account Support**: Support for multiple YouTube accounts
- **FFmpeg Integration**: Advanced audio/video processing capabilities
- **Caching System**: Intelligent caching for faster loading and offline support

## Quick Start

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK (API 26+)
- Java 17 or later

### Setup
1. **Clone the repository**
   ```bash
   git clone https://github.com/iad1tya/Echo-Music.git
   cd Echo-Music
   ```

2. **Configure Android SDK**
   Copy the template and set your SDK path:
   ```bash
   cp local.properties.template local.properties
   ```
   Edit `local.properties` and set your Android SDK path:
   ```properties
   sdk.dir=/path/to/your/android/sdk
   ```

3. **Configure Firebase (Optional)**
   Copy the template and configure with your project details:
   ```bash
   cp app/google-services.json.template app/google-services.json
   ```
   Edit `app/google-services.json` with your Firebase project configuration:
   - Replace `YOUR_PROJECT_NUMBER` with your Firebase project number
   - Replace `your-firebase-project-id` with your Firebase project ID
   - Replace `YOUR_MOBILE_SDK_APP_ID` with your Firebase app ID
   - Replace `YOUR_API_KEY` with your Firebase API key
   - See [FIREBASE_SETUP.md](FIREBASE_SETUP.md) for detailed instructions

4. **Build the app**
   ```bash
   ./gradlew assembleFossDebug
   ```

## Architecture

Echo Music is built using modern Android development practices:

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Koin
- **Database**: Room
- **Networking**: Ktor + OkHttp
- **Image Loading**: Coil
- **Navigation**: Navigation Component
- **Background Tasks**: WorkManager

### Modules

- **app**: Main application module with UI and core functionality
- **kotlinYtmusicScraper**: YouTube Music API integration and scraping
- **spotify**: Spotify API integration and authentication
- **aiService**: AI-powered features and services
- **ffmpeg-kit**: Audio/video processing and format conversion

## Running the App

### Debug Build
```bash
./gradlew assembleFossDebug
./gradlew installFossDebug
```

### Release Build
```bash
./gradlew assembleFossRelease
```

## Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes
4. Run tests: `./gradlew test`
5. Commit your changes: `git commit -m 'Add amazing feature'`
6. Push to the branch: `git push origin feature/amazing-feature`
7. Open a Pull Request

## License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0) - see the [LICENSE](LICENSE) file for details.

## Privacy

Your privacy is important to us. Please read our [Privacy Policy](PRIVACY_POLICY.md) to understand how we collect, use, and protect your information.

## Bug Reports & Feature Requests

- **Bug Reports**: [Create an issue](https://github.com/iad1tya/Echo-Music/issues/new?template=bug_report.md)
- **Feature Requests**: [Create an issue](https://github.com/iad1tya/Echo-Music/issues/new?template=feature_request.md)
- **General Discussion**: [GitHub Discussions](https://github.com/iad1tya/Echo-Music/discussions)

## Credits

### Original Project
Echo Music is a fork of [SimpMusic](https://github.com/maxrave-dev/SimpMusic), originally developed by [maxrave-dev](https://github.com/maxrave-dev).

### Acknowledgments
- **YouTube Music** for providing the music streaming platform
- **Spotify** for their API and music library
- **NewPipe** for YouTube extraction capabilities
- **ExoPlayer** for media playback
- **Jetpack Compose** team for the modern UI framework
- **All contributors** who help make Echo Music better

### Developer
Echo Music is developed and maintained by [iad1tya](https://github.com/iad1tya).

---

<div align="center">
  <p>Made with ❤️ by Echo Music Team</p>
  <p>⭐ Star this repository if you like it!</p>
</div>