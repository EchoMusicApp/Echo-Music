package androidx.media3.common;

import org.jetbrains.annotations.Nullable;

/** Playback failure with a Media3-style error code. */
public class PlaybackException extends Exception {
  public static final int ERROR_CODE_UNSPECIFIED = 1000;
  public static final int ERROR_CODE_REMOTE_ERROR = 1001;
  public static final int ERROR_CODE_BEHIND_LIVE_WINDOW = 1002;
  public static final int ERROR_CODE_TIMEOUT = 1003;
  public static final int ERROR_CODE_IO_UNSPECIFIED = 2000;
  public static final int ERROR_CODE_IO_NETWORK_CONNECTION_FAILED = 2001;
  public static final int ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT = 2002;
  public static final int ERROR_CODE_IO_BAD_HTTP_STATUS = 2004;
  public static final int ERROR_CODE_IO_FILE_NOT_FOUND = 2005;
  public static final int ERROR_CODE_PARSING_CONTAINER_MALFORMED = 3001;
  public static final int ERROR_CODE_DECODING_FAILED = 4003;

  public final int errorCode;

  public PlaybackException(@Nullable String message, @Nullable Throwable cause, int errorCode) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCodeName() { return "ERROR_CODE_" + errorCode; }
}
