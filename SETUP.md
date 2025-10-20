# Setup Instructions

This document provides instructions for setting up the Echo Music project for development.

## Prerequisites

- Android Studio (latest version recommended)
- Android SDK (API level as specified in `build.gradle.kts`)
- JDK 11 or higher
- Git

## Initial Setup

### 1. Clone the Repository

```bash
git clone https://github.com/iad1tya/Echo-Music.git
cd Echo-Music
```

### 2. Configure Local Properties

Create a `local.properties` file from the example template:

```bash
cp local.properties.example local.properties
```

Edit `local.properties` and set your Android SDK path:

```properties
sdk.dir=/path/to/your/android/sdk
```

**Example paths:**
- macOS: `/Users/username/Library/Android/sdk`
- Linux: `/home/username/Android/sdk`
- Windows: `C:\\Users\\username\\AppData\\Local\\Android\\sdk`

### 3. Configure Firebase (Optional)

Firebase is used for analytics and crash reporting. If you want to use these features:

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app to your Firebase project
3. Download the `google-services.json` file
4. Place it in the `app/` directory

Alternatively, copy the example template and fill in your credentials:

```bash
cp app/google-services.json.example app/google-services.json
```

**Note:** If you skip Firebase setup, the app will still build and run, but analytics and crash reporting will be disabled.

### 4. Configure Release Signing (Optional)

For release builds, you need to configure signing credentials. Set these as environment variables or in `gradle.properties`:

```bash
# Environment variables
export KEYSTORE_PATH=/path/to/your/keystore.jks
export STORE_PASSWORD=your_store_password
export KEY_ALIAS=your_key_alias
export KEY_PASSWORD=your_key_password
```

Or add to `gradle.properties` (never commit this file):

```properties
KEYSTORE_PATH=/path/to/your/keystore.jks
STORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

### 5. Build the Project

Open the project in Android Studio or build from the command line:

```bash
# For debug build
./gradlew assembleDebug

# For FOSS debug build (without Firebase)
./gradlew assembleFossDebug

# For release build (requires signing configuration)
./gradlew assembleRelease
```

## Important Files

### Confidential Files (Never commit these)

- `local.properties` - Contains your local SDK path
- `app/google-services.json` - Contains Firebase credentials
- `*.keystore` - Contains signing keys for release builds
- `gradle.properties` - May contain signing credentials

These files are already listed in `.gitignore` and should never be committed to version control.

### Template Files (Safe to commit)

- `local.properties.example` - Template for local properties
- `app/google-services.json.example` - Template for Firebase configuration

## Troubleshooting

### Build Fails with "SDK location not found"

Make sure you've created `local.properties` with the correct SDK path.

### Firebase-related Build Errors

If you're not using Firebase, you can build the FOSS (Free and Open Source) variant:

```bash
./gradlew assembleFossDebug
```

### Gradle Sync Issues

Try cleaning and rebuilding:

```bash
./gradlew clean
./gradlew build
```

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
