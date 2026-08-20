#!/bin/bash
./gradlew androidApp:assembleDebug --no-daemon
adb install -r androidApp/build/outputs/apk/debug/androidApp-universal-debug.apk
adb shell am start -n echo.music.iad1tya.dev/echo.music.iad1tya.MainActivity
