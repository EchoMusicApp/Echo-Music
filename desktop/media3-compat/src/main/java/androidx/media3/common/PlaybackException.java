package androidx.media3.common;

import org.jetbrains.annotations.Nullable;

/** Playback failure with a Media3-style error code. */
public class PlaybackException extends Exception {
  public static final int ERROR_CODE_UNSPECIFIED = 1000;
  public static final int ERROR_CODE_FAILED_RUNTIME_CHECK = 1004;
  public static final int ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE = 2003;
  public static final int ERROR_CODE_IO_NO_PERMISSION = 2006;
  public static final int ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED = 2007;
  public static final int ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE = 2008;
  public static final int ERROR_CODE_PARSING_MANIFEST_MALFORMED = 3002;
  public static final int ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED = 3003;
  public static final int ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED = 3004;
  public static final int ERROR_CODE_DECODER_INIT_FAILED = 4001;
  public static final int ERROR_CODE_DECODER_QUERY_FAILED = 4002;
  public static final int ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES = 4004;
  public static final int ERROR_CODE_DECODING_FORMAT_UNSUPPORTED = 4005;
  public static final int ERROR_CODE_AUDIO_TRACK_INIT_FAILED = 5001;
  public static final int ERROR_CODE_AUDIO_TRACK_WRITE_FAILED = 5002;
  public static final int ERROR_CODE_DRM_UNSPECIFIED = 6000;
  public static final int ERROR_CODE_DRM_SCHEME_UNSUPPORTED = 6001;
  public static final int ERROR_CODE_DRM_PROVISIONING_FAILED = 6002;
  public static final int ERROR_CODE_DRM_CONTENT_ERROR = 6003;
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
