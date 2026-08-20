#!/bin/bash
sed -i '' -e '/fun getHomeItemList/i\
    private var hasAutoRetriedHome = false\
' /Users/aditya/Development/EchoMusic/composeApp/src/commonMain/kotlin/echo/music/iad1tya/viewModel/HomeViewModel.kt
