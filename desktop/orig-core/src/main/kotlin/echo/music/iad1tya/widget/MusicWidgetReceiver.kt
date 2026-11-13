package echo.music.iad1tya.widget

// Desktop stand-in for the Android home-screen widget receiver. Windows has no such widgets,
// so this keeps only the action names the player code refers to.
open class MusicWidgetReceiver {
   companion object {
     const val ACTION_PLAY_PAUSE = "com.music.echo.widget.PLAY_PAUSE"
     const val ACTION_LIKE = "com.music.echo.widget.LIKE"
     const val ACTION_NEXT = "com.music.echo.widget.NEXT"
     const val ACTION_PREVIOUS = "com.music.echo.widget.PREVIOUS"
     const val ACTION_SHUFFLE = "com.music.echo.widget.SHUFFLE"
     const val ACTION_REPEAT = "com.music.echo.widget.REPEAT"
     const val ACTION_MUTE = "com.music.echo.widget.MUTE"
     const val ACTION_SKIP_TO_QUEUE_ITEM = "com.music.echo.widget.SKIP_TO_QUEUE_ITEM"
     const val ACTION_UPDATE_WIDGET = "com.music.echo.widget.UPDATE_WIDGET"
  }
}
