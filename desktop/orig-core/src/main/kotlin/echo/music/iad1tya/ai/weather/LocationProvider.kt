package echo.music.iad1tya.ai.weather

import android.content.Context
import android.location.Location

// shortcut: desktop has no GPS; returns null so callers fall back to their no-location behaviour. Swap for an IP-based lookup if wanted.
object LocationProvider {
  suspend fun getLocation(context: Context): Location? = null
}
