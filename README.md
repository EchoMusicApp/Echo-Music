<div align="center">
  <img src="assets/Echo_github.png" alt="Echo Music Logo" />
</div>

<div align="center">
  <h1>Echo Music</h1>
  <h3>A modern, feature-rich music streaming app for Android</h3>
  <p>Stream music from YouTube Music and Spotify with AI song suggestions, synced lyrics, and offline playback.</p>
  <p><strong>Current Version: v1.7.1</strong></p>
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
</div>

## 📱 Screenshots

<div align="center">
  <img src="assets/Screenshots/sc_1.png" alt="Home Screen" width="200"/>
  <img src="assets/Screenshots/sc_2.png" alt="Music Player" width="200"/>
  <img src="assets/Screenshots/sc_3.png" alt="Playlist Management" width="200"/>
  <img src="assets/Screenshots/sc_4.png" alt="Settings" width="200"/>
</div>

## ✨ Features

### 🎵 Music Streaming
- **YouTube Music & Spotify Integration**: Stream music from both platforms
- **Background Playback**: Continue listening while using other apps
- **Offline Playback**: Download music for offline listening
- **High-Quality Audio**: Support for various audio formats and bit-perfect USB DAC support

### 🎨 User Experience
- **Modern UI**: Built with Jetpack Compose and Material Design 3
- **Dark Theme**: Beautiful dark mode with Material You support
- **Multi-language Support**: Available in 20+ languages
- **Customizable Interface**: Adjustable themes and layouts

### 🔍 Discovery & Search
- **Smart Search**: Search across YouTube and Spotify
- **AI Song Suggestions**: Get personalized music recommendations
- **Browse Categories**: Explore Home, Charts, Podcasts, Moods & Genres
- **Recently Played**: Quick access to your recent music

### 🎯 Advanced Features
- **Synced Lyrics**: Real-time lyrics with translation support
- **Playlist Management**: Create, edit, and sync custom playlists
- **Sleep Timer**: Set automatic sleep timer for bedtime listening
- **Widget Support**: Home screen widget for quick access

## 🚀 Installation

### Option 1: Download Pre-built APK
1. Go to [Releases](https://github.com/iad1tya/Echo-Music/releases/latest)
2. Download the latest APK file
3. Install on your Android device

### Option 2: Build from Source
1. **Clone the repository**
   ```bash
   git clone https://github.com/iad1tya/Echo-Music.git
   cd Echo-Music
   ```

2. **Configure Android SDK**
   ```bash
   cp local.properties.template local.properties
   ```
   Edit `local.properties` and set your Android SDK path:
   ```properties
   sdk.dir=/path/to/your/android/sdk
   ```

3. **Configure Firebase (Optional)**
   ```bash
   cp app/google-services.json.template app/google-services.json
   ```
   Replace placeholder values with your Firebase project details.

4. **Build the Project**
   ```bash
   ./gradlew assembleFossDebug
   ```

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Make your changes** following our coding standards
4. **Run tests**: `./gradlew test`
5. **Commit your changes**: `git commit -m 'Add amazing feature'`
6. **Push to the branch**: `git push origin feature/amazing-feature`
7. **Open a Pull Request**

### Development Guidelines
- Follow Kotlin coding conventions
- Write meaningful commit messages
- Add tests for new features
- Update documentation as needed

## 📄 License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)** - see the [LICENSE](LICENSE) file for details.

## 🔒 Privacy

Your privacy is important to us. Echo Music includes user-controlled privacy features:
- **Analytics**: Optional usage analytics (disabled by default)
- **Crash Reports**: Optional crash reporting (disabled by default)
- **Personal Data**: No personal data is collected without consent

## 🆘 Support

- **Bug Reports & Feature Requests**: [GitHub Issues](https://github.com/iad1tya/Echo-Music/issues)
- **Community**: [GitHub Discussions](https://github.com/iad1tya/Echo-Music/discussions)
- **Documentation**: [SETUP.md](SETUP.md) | [CONTRIBUTING.md](CONTRIBUTING.md) | [TROUBLESHOOTING.md](TROUBLESHOOTING.md)


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

<div align="center">
  <p>📧 Contact: <a href="mailto:hello@echomusic.fun">hello@echomusic.fun</a></p>
</div>