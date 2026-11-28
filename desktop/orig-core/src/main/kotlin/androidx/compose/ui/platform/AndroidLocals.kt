package androidx.compose.ui.platform

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.staticCompositionLocalOf

// Desktop versions of Android's LocalContext / LocalConfiguration.
// shortcut: the configuration is a fixed 1280x720 window until the desktop main window provides the real size.
val LocalContext = staticCompositionLocalOf { Context() }
val LocalConfiguration = staticCompositionLocalOf { Configuration() }

val LocalView = staticCompositionLocalOf { android.view.View() }
