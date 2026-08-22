package echo.music.iad1tya.expect.ui

import androidx.compose.runtime.Composable

@Composable
actual fun openEqResult(audioSessionId: Int): OpenEqLauncher =
    object : OpenEqLauncher {
        override fun launch() {
        }
    }
