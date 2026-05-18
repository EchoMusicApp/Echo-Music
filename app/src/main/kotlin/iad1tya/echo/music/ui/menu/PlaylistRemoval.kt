

package iad1tya.echo.music.ui.menu

import iad1tya.echo.music.db.entities.PlaylistSongMap
import iad1tya.echo.music.innertube.YouTube

suspend fun removeSongFromRemotePlaylist(
    playlistBrowseId: String,
    playlistSongMap: PlaylistSongMap,
): Result<Unit> = runCatching {
    val setVideoIds =
        playlistSongMap.setVideoId?.let(::listOf)
            ?: YouTube.playlistEntrySetVideoIds(playlistBrowseId, playlistSongMap.songId).getOrThrow()

    setVideoIds
        .distinct()
        .forEach { setVideoId ->
            YouTube.removeFromPlaylist(playlistBrowseId, playlistSongMap.songId, setVideoId).getOrThrow()
        }
}
