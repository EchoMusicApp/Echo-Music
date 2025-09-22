# Echo Music - Setup Guide

This guide will help you set up Echo Music for development and building.

## Prerequisites

- Android Studio Arctic Fox or later
- Android SDK (API 26+)
- Java 17 or later
- Git

## Initial Setup

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/Echo-Music.git
cd Echo-Music
```

### 2. Configure Android SDK
Create a `local.properties` file in the root directory:
```properties
# Android SDK location
sdk.dir=/path/to/your/android/sdk

# Sentry DSN for crash reporting (optional)
SENTRY_DSN=your_sentry_dsn_here

# Sentry Auth Token for uploading proguard mappings (optional)
SENTRY_AUTH_TOKEN=your_sentry_auth_token_here
```

### 3. Configure Google Services
You need to set up Firebase for the app to work properly.

#### For FOSS Debug Build:
1. Copy `app/src/foss/debug/google-services.json.template` to `app/src/foss/debug/google-services.json`
2. Replace the placeholder values with your actual Firebase project configuration:
   - `YOUR_PROJECT_NUMBER`: Your Firebase project number
   - `YOUR_PROJECT_ID`: Your Firebase project ID
   - `YOUR_MOBILE_SDK_APP_ID`: Your mobile SDK app ID for production package
   - `YOUR_MOBILE_SDK_APP_ID_DEBUG`: Your mobile SDK app ID for debug package
   - `YOUR_API_KEY`: Your Firebase API key

#### For Full Release Build:
1. Copy `app/src/full/release/google-services.json.template` to `app/src/full/release/google-services.json`
2. Replace the placeholder values with your actual Firebase project configuration

### 4. Firebase Setup
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use an existing one
3. Add an Android app with package name `iad1tya.echo.music`
4. Download the `google-services.json` file
5. For debug builds, also add package name `iad1tya.echo.music.dev`
6. Replace the template files with your actual configuration

## Building the App

### Debug Build (FOSS)
```bash
./gradlew assembleFossDebug
```

### Release Build (Full)
```bash
./gradlew assembleFullRelease
```

### App Bundle (for Play Store)
```bash
./gradlew bundleFullRelease
```

## Project Structure

```
Echo-Music/
├── app/                          # Main application module
│   ├── src/
│   │   ├── main/                 # Main source code
│   │   ├── foss/debug/           # FOSS debug configuration
│   │   └── full/release/         # Full release configuration
│   └── build.gradle.kts          # App build configuration
├── kotlinYtmusicScraper/         # YouTube Music scraper library
├── aiService/                    # AI service module
├── spotify/                      # Spotify integration module
├── gradle/                       # Gradle configuration
│   └── libs.versions.toml        # Dependency versions
├── CHANGELOG.md                  # Version history
├── README.md                     # Project documentation
├── SETUP.md                      # This setup guide
└── LICENSE                       # GPL-3.0 license
```

## Build Variants

- **FOSS Debug**: Open source version for development (`iad1tya.echo.music.dev`)
- **FOSS Release**: Open source version for distribution (`iad1tya.echo.music`)
- **Full Debug**: Full version with all features for development
- **Full Release**: Full version with all features for production

## Dependencies

The project uses the following main dependencies:
- **Jetpack Compose**: Modern UI toolkit
- **Media3 ExoPlayer**: Media playback
- **Room**: Local database
- **Koin**: Dependency injection
- **Coil**: Image loading
- **Ktor**: Network requests
- **NewPipe Extractor**: YouTube content extraction

## Development

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Follow the existing code structure

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

### Linting
```bash
# Run lint checks
./gradlew lint
```

## Troubleshooting

### Build Issues
1. **SDK not found**: Make sure `local.properties` has the correct SDK path
2. **Google Services error**: Ensure `google-services.json` files are properly configured
3. **Gradle sync issues**: Try cleaning and rebuilding the project

### Runtime Issues
1. **App crashes on startup**: Check if all required permissions are granted
2. **Media not playing**: Verify internet connection and YouTube Music access
3. **Widget not updating**: Check if the app has notification permissions

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0).

## Support

For issues and questions:
- Create an issue on GitHub
- Check the existing issues first
- Provide detailed information about your problem

---

**Note**: This is a fork of [SimpMusic](https://github.com/maxrave-dev/SimpMusic) with modifications and improvements. Please respect the original project's license and attribution requirements.