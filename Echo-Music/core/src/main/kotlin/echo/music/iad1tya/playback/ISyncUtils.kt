package echo.music.iad1tya.playback

import echo.music.iad1tya.db.entities.SongEntity

interface ISyncUtils {
  fun likeSong(song: SongEntity)
}
