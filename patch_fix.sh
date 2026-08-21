#!/bin/bash
sed -i '' -e 's/androidx.lifecycle.repeatOnLifecycle/lifecycle.repeatOnLifecycle/g' /Users/aditya/Development/EchoMusic/androidApp/src/main/java/echo/music/iad1tya/MainActivity.kt
sed -i '' -e '/import androidx.lifecycle.lifecycleScope/a\
import androidx.lifecycle.repeatOnLifecycle\
' /Users/aditya/Development/EchoMusic/androidApp/src/main/java/echo/music/iad1tya/MainActivity.kt
