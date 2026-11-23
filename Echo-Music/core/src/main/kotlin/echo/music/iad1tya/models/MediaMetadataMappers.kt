package echo.music.iad1tya.models

import com.music.innertube.models.SongItem
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.db.entities.SongEntity
import echo.music.iad1tya.ui.utils.resize

fun MediaMetadata.toSongEntity() =
  SongEntity(
    id = id,
    title = title,
    duration = duration,
    thumbnailUrl = thumbnailUrl,
    albumId = album?.id,
    albumName = album?.title,
    explicit = explicit,
    liked = liked,
    likedDate = likedDate,
    inLibrary = inLibrary,
    libraryAddToken = libraryAddToken,
    libraryRemoveToken = libraryRemoveToken,
    isVideo = isVideoSong
  )

fun Song.toMediaMetadata() =
  MediaMetadata(
    id = song.id,
    title = song.title,
    artists = artists.map { MediaMetadata.Artist(id = it.id, name = it.name) },
    duration = song.duration,
    thumbnailUrl = song.thumbnailUrl,
    album =
      album?.let { MediaMetadata.Album(id = it.id, title = it.title) }
        ?: song.albumId?.let { albumId ->
          MediaMetadata.Album(id = albumId, title = song.albumName.orEmpty())
        },
    explicit = song.explicit,
    musicVideoType = if (song.isVideo) "MUSIC_VIDEO_TYPE_OMV" else null,
    suggestedBy = null,
    source = QueueItemSource.USER,
  )

fun SongItem.toMediaMetadata() =
  MediaMetadata(
    id = id,
    title = title,
    artists = artists.map { MediaMetadata.Artist(id = it.id, name = it.name) },
    duration = duration ?: -1,
    thumbnailUrl = thumbnail.resize(1200, 1200),
    album = album?.let { MediaMetadata.Album(id = it.id, title = it.name) },
    explicit = explicit,
    setVideoId = setVideoId,
    musicVideoType = musicVideoType,
    libraryAddToken = libraryAddToken,
    libraryRemoveToken = libraryRemoveToken,
    suggestedBy = null,
    source = QueueItemSource.USER
  )
