




package iad1tya.echo.music.lyrics

import android.content.Context
import iad1tya.echo.music.kugou.KuGou
import iad1tya.echo.music.constants.EnableKugouKey
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get

object KuGouLyricsProvider : LyricsProvider {
    override val name = "Kugou"
    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableKugouKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = KuGou.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        KuGou.getAllPossibleLyricsOptions(title, artist, duration, callback)
    }
}
