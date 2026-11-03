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

  // Player buttons shown while a podcast episode plays.
  const val ACTION_EPISODE_SKIP_BACK = "EPISODE_SKIP_BACK"
  const val ACTION_EPISODE_SKIP_FORWARD = "EPISODE_SKIP_FORWARD"
  const val ACTION_EPISODE_SPEED = "EPISODE_SPEED"
  val CommandEpisodeSkipBack = SessionCommand(ACTION_EPISODE_SKIP_BACK, Bundle.EMPTY)
  val CommandEpisodeSkipForward = SessionCommand(ACTION_EPISODE_SKIP_FORWARD, Bundle.EMPTY)
  val CommandEpisodeSpeed = SessionCommand(ACTION_EPISODE_SPEED, Bundle.EMPTY)

  // Browse actions shown next to songs on Android Auto; the item's media id
  // arrives in the command args.
  const val ACTION_ITEM_LIKE = "ITEM_LIKE"
  const val ACTION_ITEM_PLAY_NEXT = "ITEM_PLAY_NEXT"
  const val ACTION_ITEM_DISMISS = "ITEM_DISMISS"
  const val ACTION_ITEM_FOLLOW = "ITEM_FOLLOW"
  const val ACTION_ITEM_UNFOLLOW = "ITEM_UNFOLLOW"
  val CommandItemLike = SessionCommand(ACTION_ITEM_LIKE, Bundle.EMPTY)
  val CommandItemPlayNext = SessionCommand(ACTION_ITEM_PLAY_NEXT, Bundle.EMPTY)
  val CommandItemDismiss = SessionCommand(ACTION_ITEM_DISMISS, Bundle.EMPTY)
  val CommandItemFollow = SessionCommand(ACTION_ITEM_FOLLOW, Bundle.EMPTY)
  val CommandItemUnfollow = SessionCommand(ACTION_ITEM_UNFOLLOW, Bundle.EMPTY)
}
