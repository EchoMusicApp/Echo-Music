package echo.music.iad1tya.extensions

// Only toEnum from the original StringExt.kt (the rest of that file needs Android's SQLite classes).
inline fun <reified T : Enum<T>> String?.toEnum(defaultValue: T): T =
  if (this == null) {
    defaultValue
  } else {
    try {
      enumValueOf(this)
    } catch (e: IllegalArgumentException) {
      defaultValue
    }
  }
