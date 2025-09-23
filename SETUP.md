# Echo Music - Setup Guide

## Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 26 or later
- Java 17 or later
- Git

## Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/Echo-Music.git
cd Echo-Music
```

### 2. Configure Local Properties
Copy the template and configure your local settings:
```bash
cp local.properties.template local.properties
```

Edit `local.properties` and set your Android SDK path:
```properties
sdk.dir=/path/to/your/android/sdk
```

### 3. Firebase Configuration
Copy the Firebase template and configure with your project details:
```bash
cp app/google-services.json.template app/google-services.json
```

Edit `app/google-services.json` with your Firebase project configuration:
- Replace `YOUR_PROJECT_NUMBER` with your Firebase project number
- Replace `your-firebase-project-id` with your Firebase project ID
- Replace `YOUR_MOBILE_SDK_APP_ID` with your Firebase app ID
- Replace `YOUR_API_KEY` with your Firebase API key

### 4. Build the Project
```bash
./gradlew assembleFullDebug
```

### 5. Run on Device/Emulator
```bash
./gradlew installFullDebug
```

## Project Structure

- `app/` - Main application module
- `kotlinYtmusicScraper/` - YouTube Music scraping module
- `spotify/` - Spotify integration module
- `aiService/` - AI service module
- `ffmpeg-kit/` - FFmpeg integration (currently disabled)

## Build Variants

- `fossDebug` - FOSS version (debug)
- `fossRelease` - FOSS version (release)
- `fullDebug` - Full version with all features (debug)
- `fullRelease` - Full version with all features (release)

## Features

- Music streaming from YouTube Music
- Spotify integration
- AI-powered features
- Material Design 3 UI
- Dark theme support
- Widget support
- Background playback
- Offline caching

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Privacy Policy

See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) for information about data collection and usage.

## Security

See [SECURITY.md](SECURITY.md) for security-related information and reporting vulnerabilities.