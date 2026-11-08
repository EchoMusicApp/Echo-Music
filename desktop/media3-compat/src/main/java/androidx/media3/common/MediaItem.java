package androidx.media3.common;

import android.net.Uri;
import android.os.Bundle;
import org.jetbrains.annotations.Nullable;

/** A playable item (same public shape as Media3's MediaItem for the parts the app uses). */
public final class MediaItem {
  public static final String DEFAULT_MEDIA_ID = "";
  public static final MediaItem EMPTY = new Builder().build();

  public final String mediaId;
  @Nullable public final LocalConfiguration localConfiguration;
  public final MediaMetadata mediaMetadata;
  public final RequestMetadata requestMetadata;

  private MediaItem(String mediaId, @Nullable LocalConfiguration lc, MediaMetadata md, RequestMetadata rm) {
    this.mediaId = mediaId; this.localConfiguration = lc; this.mediaMetadata = md; this.requestMetadata = rm;
  }

  public Builder buildUpon() { return new Builder(this); }

  public static final class LocalConfiguration {
    public final Uri uri;
    @Nullable public final String mimeType;
    @Nullable public final String customCacheKey;
    @Nullable public final Object tag;
    LocalConfiguration(Uri uri, @Nullable String mimeType, @Nullable String customCacheKey, @Nullable Object tag) {
      this.uri = uri; this.mimeType = mimeType; this.customCacheKey = customCacheKey; this.tag = tag;
    }
  }

  public static final class RequestMetadata {
    public static final RequestMetadata EMPTY = new RequestMetadata(null, null, null);
    @Nullable public final Uri mediaUri;
    @Nullable public final String searchQuery;
    @Nullable public final Bundle extras;
    public RequestMetadata(@Nullable Uri mediaUri, @Nullable String searchQuery, @Nullable Bundle extras) {
      this.mediaUri = mediaUri; this.searchQuery = searchQuery; this.extras = extras;
    }

    public static final class Builder {
      @Nullable private Uri mediaUri;
      @Nullable private String searchQuery;
      @Nullable private Bundle extras;
      public Builder setMediaUri(@Nullable Uri v) { mediaUri = v; return this; }
      public Builder setSearchQuery(@Nullable String v) { searchQuery = v; return this; }
      public Builder setExtras(@Nullable Bundle v) { extras = v; return this; }
      public RequestMetadata build() { return new RequestMetadata(mediaUri, searchQuery, extras); }
    }
  }

  public static final class Builder {
    @Nullable private String mediaId;
    @Nullable private Uri uri;
    @Nullable private String mimeType;
    @Nullable private String customCacheKey;
    @Nullable private Object tag;
    private MediaMetadata mediaMetadata = MediaMetadata.EMPTY;
    private RequestMetadata requestMetadata = RequestMetadata.EMPTY;

    public Builder() {}

    private Builder(MediaItem item) {
      mediaId = item.mediaId; mediaMetadata = item.mediaMetadata; requestMetadata = item.requestMetadata;
      if (item.localConfiguration != null) {
        uri = item.localConfiguration.uri; mimeType = item.localConfiguration.mimeType;
        customCacheKey = item.localConfiguration.customCacheKey; tag = item.localConfiguration.tag;
      }
    }

    public Builder setMediaId(String v) { mediaId = v; return this; }
    public Builder setUri(@Nullable String v) { uri = v == null ? null : Uri.parse(v); return this; }
    public Builder setUri(@Nullable Uri v) { uri = v; return this; }
    public Builder setMimeType(@Nullable String v) { mimeType = v; return this; }
    public Builder setCustomCacheKey(@Nullable String v) { customCacheKey = v; return this; }
    public Builder setTag(@Nullable Object v) { tag = v; return this; }
    public Builder setMediaMetadata(MediaMetadata v) { mediaMetadata = v; return this; }
    public Builder setRequestMetadata(RequestMetadata v) { requestMetadata = v; return this; }

    public MediaItem build() {
      LocalConfiguration lc = uri != null ? new LocalConfiguration(uri, mimeType, customCacheKey, tag) : null;
      return new MediaItem(mediaId != null ? mediaId : DEFAULT_MEDIA_ID, lc, mediaMetadata, requestMetadata);
    }
  }
}
