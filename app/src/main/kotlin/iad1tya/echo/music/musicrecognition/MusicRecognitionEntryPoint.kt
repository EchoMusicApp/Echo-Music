

package iad1tya.echo.music.musicrecognition

import androidx.navigation.NavHostController

const val MusicRecognitionRoute = "music_recognition"
const val ACTION_MUSIC_RECOGNITION = "iad1tya.echo.music.action.MUSIC_RECOGNITION"
const val MusicRecognitionAutoStartRequestKey = "music_recognition_auto_start_request"

fun NavHostController.openMusicRecognition(
    autoStartRequestId: Long = System.currentTimeMillis(),
) {
    val currentRoute = currentDestination?.route
    if (currentRoute != MusicRecognitionRoute && !popBackStack(MusicRecognitionRoute, inclusive = false)) {
        navigate(MusicRecognitionRoute) {
            launchSingleTop = true
        }
    }

    getBackStackEntry(MusicRecognitionRoute).savedStateHandle[MusicRecognitionAutoStartRequestKey] =
        autoStartRequestId
}
