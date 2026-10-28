package echo.music.iad1tya.constants

import android.os.Bundle
import androidx.media3.session.SessionCommand

object MediaSessionConstants {
  const val ACTION_TOGGLE_LIBRARY = "TOGGLE_LIBRARY"
  const val ACTION_TOGGLE_START_RADIO = "TOGGLE_START_RADIO"
  const val ACTION_TOGGLE_LIKE = "TOGGLE_LIKE"
  const val ACTION_TOGGLE_SHUFFLE = "TOGGLE_SHUFFLE"
  const val ACTION_TOGGLE_REPEAT_MODE = "TOGGLE_REPEAT_MODE"
  val CommandToggleLibrary = SessionCommand(ACTION_TOGGLE_LIBRARY, Bundle.EMPTY)
  val CommandToggleLike = SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY)
  val CommandToggleStartRadio = SessionCommand(ACTION_TOGGLE_START_RADIO, Bundle.EMPTY)
  val CommandToggleShuffle = SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY)
  val CommandToggleRepeatMode = SessionCommand(ACTION_TOGGLE_REPEAT_MODE, Bundle.EMPTY)

  // Browse actions shown next to songs on Android Auto; the item's media id
  // arrives in the command args.
  const val ACTION_ITEM_LIKE = "ITEM_LIKE"
  const val ACTION_ITEM_PLAY_NEXT = "ITEM_PLAY_NEXT"
  val CommandItemLike = SessionCommand(ACTION_ITEM_LIKE, Bundle.EMPTY)
  val CommandItemPlayNext = SessionCommand(ACTION_ITEM_PLAY_NEXT, Bundle.EMPTY)
}
