# Echo Music - Development Setup Guide

This guide will help you set up the Echo Music Android project for development.

## Prerequisites

- Android Studio (latest stable version)
- JDK 17 or higher
- Android SDK (API level 26+)
- Git

## Initial Setup

### 1. Clone the Repository
```bash
git clone https://github.com/iad1tya/Echo-Music.git
cd Echo-Music
```

### 2. Configure Local Properties
Create a `local.properties` file in the root directory:
```properties
# Android SDK Configuration
sdk.dir=/path/to/your/android/sdk

# Optional: Custom build configurations
# org.gradle.jvmargs=-Xmx2048m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
# org.gradle.parallel=true
# org.gradle.daemon=true
# org.gradle.configureondemand=true
```

### 3. Firebase Configuration (Optional)
If you want to use Firebase Analytics and Crashlytics:

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app with package name `iad1tya.echo.music`
3. Download the `google-services.json` file
4. Place it in the `app/` directory
5. Replace the placeholder values in `app/google-services.json.template` with your actual Firebase configuration

### 4. Build Configuration
Update `gradle.properties` if needed:
```properties
# Uncomment and set your Java home path
# org.gradle.java.home=/path/to/your/java/home
```

## Building the Project

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build (requires signing configuration)
```bash
./gradlew assembleRelease
```

## Project Structure

```
Echo-Music/
├── app/                    # Main application module
├── aiService/             # AI service module
├── kotlinYtmusicScraper/ # YouTube Music scraper
├── spotify/              # Spotify integration
├── ffmpeg-kit/           # Audio processing
└── assets/               # App assets and screenshots
```

## Development Guidelines

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Follow the existing architecture patterns

### Testing
- Write unit tests for business logic
- Test UI components with Compose testing
- Use mock objects for external dependencies

### Firebase Integration
The project includes Firebase Analytics and Crashlytics:
- Analytics tracks user behavior and app usage
- Crashlytics monitors crashes and non-fatal exceptions
- Both services respect user privacy settings

### Contributing
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## Troubleshooting

### Build Issues
- Ensure you have the correct JDK version (17+)
- Clean and rebuild: `./gradlew clean build`
- Check Android SDK installation

### Firebase Issues
- Verify `google-services.json` is in the correct location
- Check Firebase project configuration
- Ensure package names match

### Dependencies
- All dependencies are managed through `gradle/libs.versions.toml`
- Use the version catalog for consistency
- Update dependencies regularly for security

## Security Notes

- Never commit sensitive files like `local.properties`, `google-services.json`, or keystore files
- Use environment variables for API keys in CI/CD
- Review all dependencies for security vulnerabilities
- Follow Android security best practices

## Support

For issues and questions:
- Check existing issues in the repository
- Create a new issue with detailed information
- Follow the code of conduct

## License

This project is licensed under the MIT License - see the LICENSE file for details.
