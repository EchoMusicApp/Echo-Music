#!/bin/bash
sed -i '' -e '/viewModel.getLocation()/a\
\
        lifecycleScope.launch {\
            androidx.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {\
                viewModel.showRewardedAds.collect {\
                    playRewardedAds(3)\
                }\
            }\
        }\
' /Users/aditya/Development/EchoMusic/androidApp/src/main/java/echo/music/iad1tya/MainActivity.kt
