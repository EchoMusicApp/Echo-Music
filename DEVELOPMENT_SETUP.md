# Echo Music - Developer Setup Guide

This guide will help you set up the Echo Music project for development.

## Prerequisites

- **Android Studio**: Latest stable version
- **Android SDK**: API level 21+ (Android 5.0+)
- **Java**: JDK 17 or higher
- **Git**: For version control

## Quick Setup

### 1. Clone the Repository

```bash
git clone https://github.com/iad1tya/Echo-Music.git
cd Echo-Music
```

### 2. Configure Local Properties

```bash
# Copy the template
cp local.properties.template local.properties

# Edit local.properties and set your Android SDK path
# Example: sdk.dir=/Users/yourusername/Library/Android/sdk
```

### 3. Configure Firebase (Optional)

If you want to use Firebase Analytics and Crashlytics:

```bash
# Copy the Firebase template
cp app/google-services.json.template app/google-services.json

# Edit google-services.json with your Firebase project configuration
# See FIREBASE_SETUP_GUIDE.md for detailed instructions
```

### 4. Build the Project

```bash
# Build debug version
./gradlew assembleDebug

# Or build specific variants
./gradlew assembleFossDebug
./gradlew assembleFullDebug
```

## Project Structure

```
Echo-Music/
├── app/                    # Main application module
├── aiService/              # AI service module
├── kotlinYtmusicScraper/  # YouTube Music scraper
├── spotify/               # Spotify integration
├── ffmpeg-kit/            # Audio processing
├── assets/                # App assets and screenshots
├── fastlane/              # App store deployment
└── docs/                  # Documentation
```

## Build Variants

The project supports two build variants:

- **FOSS**: Free and open-source version (`iad1tya.echo.music`)
- **Full**: Full-featured version (`iad1tya.echo.music.dev`)

## Development Workflow

### 1. Running the App

```bash
# Install debug APK
./gradlew installDebug

# Or run specific variant
./gradlew installFossDebug
./gradlew installFullDebug
```

### 2. Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

### 3. Code Quality

```bash
# Run linting
./gradlew lint

# Check for code issues
./gradlew check
```

## Configuration Files

### Required Files

- `local.properties` - Android SDK configuration (create from template)
- `google-services.json` - Firebase configuration (optional, create from template)

### Template Files

- `local.properties.template` - SDK configuration template
- `google-services.json.template` - Firebase configuration template
- `keystore.properties.template` - Release signing template

## Firebase Setup (Optional)

If you want to use Firebase features:

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add your Android app with package name `iad1tya.echo.music`
3. Download `google-services.json` and place it in `app/` directory
4. For Full variant, add second app with package name `iad1tya.echo.music.dev`
5. Merge configurations in the same `google-services.json` file

See `FIREBASE_SETUP_GUIDE.md` for detailed instructions.

## Release Build

### 1. Generate Keystore

```bash
# Create keystore directory
mkdir keystore

# Generate keystore
keytool -genkey -v -keystore keystore/echo-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias echo-release-key
```

### 2. Configure Signing

```bash
# Copy keystore template
cp keystore.properties.template keystore.properties

# Edit keystore.properties with your keystore details
```

### 3. Build Release

```bash
# Build release APK
./gradlew assembleRelease

# Build release AAB (for Play Store)
./gradlew bundleRelease
```

## Troubleshooting

### Common Issues

1. **SDK location not found**
   - Ensure `local.properties` exists and has correct `sdk.dir` path
   - Check that Android SDK is properly installed

2. **Build fails with Firebase errors**
   - Ensure `google-services.json` is properly configured
   - Check package names match your Firebase project

3. **Gradle sync fails**
   - Check internet connection
   - Clear Gradle cache: `./gradlew clean`
   - Invalidate caches in Android Studio

### Debug Commands

```bash
# Clean project
./gradlew clean

# Check dependencies
./gradlew dependencies

# Run with debug info
./gradlew assembleDebug --info

# Check for issues
./gradlew check --continue
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

See `CONTRIBUTING.md` for detailed guidelines.

## Support

- **Issues**: [GitHub Issues](https://github.com/iad1tya/Echo-Music/issues)
- **Discussions**: [GitHub Discussions](https://github.com/iad1tya/Echo-Music/discussions)
- **Documentation**: Check the `docs/` directory

## License

This project is licensed under the MIT License - see `LICENSE` file for details.

---

**Happy coding!** 🎵