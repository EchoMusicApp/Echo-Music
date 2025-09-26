# Echo Music - Developer Setup Guide

This guide will help you set up the Echo Music project for development.

## 📋 Prerequisites

- **Android Studio**: Arctic Fox (2020.3.1) or later
- **Android SDK**: API level 26 (Android 8.0) or later
- **Kotlin**: 2.2.10 or later
- **Java**: JDK 17 or later
- **Git**: For version control

## 🚀 Initial Setup

### 1. Clone the Repository
```bash
git clone https://github.com/iad1tya/Echo-Music.git
cd Echo-Music
```

### 2. Open in Android Studio
- Launch Android Studio
- Select "Open an existing project"
- Navigate to the Echo-Music directory
- Click "OK"

### 3. Sync Project
- Android Studio will automatically sync the project
- Wait for the sync to complete
- Resolve any dependency issues if prompted

## 🔧 Configuration

### Firebase Setup (Optional)

1. **Create Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Click "Create a project"
   - Follow the setup wizard

2. **Add Android App**
   - Click "Add app" → Android
   - Package name: `iad1tya.echo.music`
   - Download `google-services.json`

3. **Replace Template**
   ```bash
   # Replace the template with your actual file
   cp your-downloaded-google-services.json app/google-services.json
   ```
   
   **⚠️ Security Note**: The `google-services.json` file contains sensitive API keys and is automatically ignored by git. Never commit this file to version control.

4. **Enable Services**
   - Enable Analytics
   - Enable Crashlytics
   - Enable Performance Monitoring (optional)

### Sentry Setup (Optional)

1. **Create Sentry Project**
   - Go to [Sentry.io](https://sentry.io/)
   - Create a new project
   - Select "Android" platform

2. **Get DSN**
   - Copy your DSN from the Sentry project settings

3. **Configure Local Properties**
   ```bash
   # Add to local.properties
   echo "SENTRY_DSN=your_sentry_dsn_here" >> local.properties
   ```

## 🏗️ Build Configuration

### Build Variants

The project has two main build variants:

- **FOSS** (`fossDebug`, `fossRelease`): Free and Open Source Software variant
- **Full** (`fullDebug`, `fullRelease`): Complete version with all services

### Building the App

```bash
# Debug build (FOSS variant)
./gradlew assembleFossDebug

# Release build (FOSS variant)
./gradlew assembleFossRelease

# Debug build (Full variant)
./gradlew assembleFullDebug

# Release build (Full variant)
./gradlew assembleFullRelease
```

## 🧪 Testing

### Unit Tests
```bash
# Run all unit tests
./gradlew test

# Run tests for specific variant
./gradlew testFossDebugUnitTest
```

### Lint Checks
```bash
# Run lint analysis
./gradlew lintFossDebug

# Fix lint issues
./gradlew lintFixFossDebug
```

## 📱 Running on Device/Emulator

### Prerequisites
- Enable Developer Options on your Android device
- Enable USB Debugging
- Or use Android Emulator

### Steps
1. Connect device or start emulator
2. In Android Studio, select your device
3. Click "Run" button or press `Shift + F10`

## 🔍 Debugging

### Logcat
- Use Android Studio's Logcat to view app logs
- Filter by package: `iad1tya.echo.music`

### Crash Reports
- Check Firebase Crashlytics (if configured)
- Check Sentry (if configured)
- Check local crash logs in app's internal storage

## 📦 Dependencies

### Key Dependencies
- **Jetpack Compose**: Modern UI toolkit
- **Media3**: Media playback
- **Room**: Local database
- **Koin**: Dependency injection
- **Coil**: Image loading
- **Ktor**: Network requests

### Version Management
- All dependencies are managed in `gradle/libs.versions.toml`
- Use version catalog for consistent dependency management

## 🎨 UI Development

### Theme System
- Material Design 3 implementation
- Dynamic theming support
- Custom color schemes

### Components
- Reusable UI components in `ui/component/`
- Screen-specific UI in `ui/screen/`
- Theme definitions in `ui/theme/`

## 🗄️ Database

### Room Database
- Local music database
- Playlist management
- Offline content storage

### Migrations
- Database migrations in `data/db/`
- Schema files in `app/schemas/`

## 🌐 Network

### API Integration
- YouTube Music API
- Spotify API
- Custom scraping for additional features

### Offline Support
- Download management
- Offline playback
- Sync when online

## 🔒 Security

### API Keys
- No hardcoded API keys
- Use environment variables or local properties
- Firebase configuration in separate file

### Privacy
- User-controlled analytics
- Optional crash reporting
- No personal data collection without consent

## 🚀 Deployment

### Release Process
1. Update version in `gradle/libs.versions.toml`
2. Run release build: `./gradlew assembleFossRelease`
3. Test the release APK
4. Sign and distribute

### Signing
- Configure signing in `app/build.gradle.kts`
- Use keystore for release builds
- Never commit keystore files

## 🐛 Troubleshooting

### Common Issues

1. **Build Failures**
   - Check Android SDK version
   - Verify Kotlin version compatibility
   - Clean and rebuild: `./gradlew clean build`

2. **Firebase Issues**
   - Verify `google-services.json` is in correct location
   - Check package name matches Firebase project
   - Ensure Firebase services are enabled

3. **Dependency Issues**
   - Sync project: `./gradlew --refresh-dependencies`
   - Check internet connection
   - Clear Gradle cache: `./gradlew clean`

4. **Runtime Issues**
   - Check device logs in Logcat
   - Verify permissions are granted
   - Test on different devices/emulators

### Getting Help
- Check [Troubleshooting Guide](TROUBLESHOOTING.md)
- Open [GitHub Issue](https://github.com/yourusername/Echo-Music/issues)
- Join [Discussions](https://github.com/yourusername/Echo-Music/discussions)

## 📚 Additional Resources

- [Android Developer Documentation](https://developer.android.com/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Firebase Documentation](https://firebase.google.com/docs)
- [Sentry Documentation](https://docs.sentry.io/)

---

Happy coding! 🎵