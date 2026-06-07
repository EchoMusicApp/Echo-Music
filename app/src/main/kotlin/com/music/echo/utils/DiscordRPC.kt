package iad1tya.echo.music.utils

import android.content.Context
import iad1tya.echo.music.db.entities.Song

class DiscordRPC(
    val context: Context,
    token: String,
) {
    fun isRpcRunning(): Boolean = false
    fun closeRPC() {}
    suspend fun updateSong(
        song: Song,
        currentPlaybackTimeMillis: Long,
        playbackSpeed: Float = 1.0f,
        useDetails: Boolean = false,
        status: String = "online",
        button1Text: String = "",
        button1Visible: Boolean = true,
        button2Text: String = "",
        button2Visible: Boolean = true,
        activityType: String = "listening",
        activityName: String = "",
    ): Result<Unit> = Result.success(Unit)

    fun close() {}

    companion object {
        fun resolveVariables(text: String, song: Song): String = text
    }
}
