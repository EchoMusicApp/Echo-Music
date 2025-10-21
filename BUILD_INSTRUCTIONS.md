# Build Instructions for Echo Music on M30s

## Device Information
- **Model**: Samsung SM-M307F (Galaxy M30s)
- **Architecture**: arm64-v8a
- **Device ID**: RZ8M90F41BL

## Quick Build Commands

### Build Universal Debug APK (Recommended)
```bash
./gradlew assembleUniversalDebug
```
Output: `app/build/outputs/apk/universal/debug/app-universal-debug.apk`

### Build ARM64 Optimized APK
```bash
./gradlew assembleArm64Debug
```
Output: `app/build/outputs/apk/arm64/debug/app-arm64-debug.apk`

### Install on Connected Device
```bash
# Universal build
adb install -r app/build/outputs/apk/universal/debug/app-universal-debug.apk

# ARM64 optimized build
adb install -r app/build/outputs/apk/arm64/debug/app-arm64-debug.apk
```

### Build and Install in One Command
```bash
./gradlew installUniversalDebug
# or
./gradlew installArm64Debug
```

## Configuration Files Created

1. **local.properties** - Android SDK path configuration
2. **app/google-services.json** - Firebase configuration (template)
3. **app/persistent-debug.keystore** - Debug signing key

## Build Variants

The app supports multiple build variants:
- **universal**: All architectures (armeabi-v7a, arm64-v8a, x86, x86_64)
- **arm64**: ARM 64-bit only (recommended for M30s)
- **armeabi**: ARM 32-bit only
- **x86**: x86 32-bit (emulator)
- **x86_64**: x86 64-bit (emulator)

## Troubleshooting

### Check Connected Devices
```bash
adb devices
```

### Check Device Architecture
```bash
adb shell getprop ro.product.cpu.abi
```

### Clean Build
```bash
./gradlew clean
./gradlew assembleUniversalDebug
```

### View Build Logs
```bash
./gradlew assembleUniversalDebug --info
```

## Notes

- The M30s uses **arm64-v8a** architecture, so both universal and arm64 builds will work
- The arm64 build will be smaller and potentially more optimized
- Firebase analytics and crash reporting are optional (configured via google-services.json)
- Debug builds use the persistent-debug.keystore for consistent signing
