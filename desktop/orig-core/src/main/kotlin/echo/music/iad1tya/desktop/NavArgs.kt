package echo.music.iad1tya.desktop

import androidx.savedstate.SavedState
import androidx.savedstate.read

// Android's bundle.getString(key) for the desktop navigation library's SavedState.
fun navString(args: SavedState?, key: String): String? = args?.read { getStringOrNull(key) }
fun navBoolean(args: SavedState?, key: String): Boolean? = args?.read { getBooleanOrNull(key) }
